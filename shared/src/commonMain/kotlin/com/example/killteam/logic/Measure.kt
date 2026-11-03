package com.example.killteam.logic

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlinx.serialization.Serializable

/** Point in image (pixel) coordinates. */
data class Pt(val x: Double, val y: Double)

@Serializable
enum class LengthUnit(val label: String, val mm: Double, val decimals: Int) {
    Inch("in", 25.4, 1),
    Cm("cm", 10.0, 1),
    Mm("mm", 1.0, 0),
}

/** Kill Team measuring templates: a distance counts as within when it does not exceed the template. */
@Serializable
enum class RuleDistance(val label: String, val inches: Int) {
    Circle("Circle 1\"", 1),
    Triangle("Triangle 2\"", 2),
    Square("Square 3\"", 3),
    Pentagon("Pentagon 6\"", 6),
    ;

    val mm: Double get() = inches * 25.4

    fun contains(distanceMm: Double): Boolean = distanceMm <= mm + 0.05
}

@Serializable
data class MeasureSettings(
    val unit: LengthUnit = LengthUnit.Inch,
    val rule: RuleDistance? = null,
    val noticeSeen: Boolean = false,
)

data class SizePreset(val label: String, val mm: Double)

data class PlanePreset(val label: String, val widthMm: Double, val heightMm: Double)

object MeasurePresets {
    val references = listOf(
        SizePreset("25 mm base", 25.0),
        SizePreset("28.5 mm base", 28.5),
        SizePreset("32 mm base", 32.0),
        SizePreset("40 mm base / objective marker", 40.0),
        SizePreset("50 mm base", 50.0),
        SizePreset("1 inch", 25.4),
    )
    val planes = listOf(
        PlanePreset("Killzone 30\" x 22\"", 762.0, 558.8),
        PlanePreset("60 x 60 cm", 600.0, 600.0),
    )
}

/** Converts image points to real distances in millimetres on the table plane. */
sealed interface Scale {
    fun distanceMm(a: Pt, b: Pt): Double
}

class LinearScale(val mmPerPx: Double) : Scale {
    override fun distanceMm(a: Pt, b: Pt): Double = hypot(a.x - b.x, a.y - b.y) * mmPerPx
}

/** Maps the image onto the table plane (millimetres) so angled shots are corrected. */
class PlaneScale(private val homography: Homography) : Scale {
    override fun distanceMm(a: Pt, b: Pt): Double {
        val pa = homography.map(a)
        val pb = homography.map(b)
        return hypot(pa.x - pb.x, pa.y - pb.y)
    }
}

/** Projective transform with h33 = 1, stored row by row (8 values). */
class Homography(private val h: DoubleArray) {
    fun map(p: Pt): Pt {
        val w = h[6] * p.x + h[7] * p.y + 1.0
        if (abs(w) < 1e-12) return Pt(Double.NaN, Double.NaN)
        return Pt((h[0] * p.x + h[1] * p.y + h[2]) / w, (h[3] * p.x + h[4] * p.y + h[5]) / w)
    }

    companion object {
        /** Transform taking the four [src] points onto the four [dst] points, or null for a degenerate quad. */
        fun from(src: List<Pt>, dst: List<Pt>): Homography? {
            if (src.size != 4 || dst.size != 4) return null
            val m = Array(8) { DoubleArray(9) }
            for (i in 0 until 4) {
                val (x, y) = src[i]
                val (u, v) = dst[i]
                m[2 * i] = doubleArrayOf(x, y, 1.0, 0.0, 0.0, 0.0, -x * u, -y * u, u)
                m[2 * i + 1] = doubleArrayOf(0.0, 0.0, 0.0, x, y, 1.0, -x * v, -y * v, v)
            }
            for (col in 0 until 8) {
                var pivot = col
                for (r in col + 1 until 8) if (abs(m[r][col]) > abs(m[pivot][col])) pivot = r
                if (abs(m[pivot][col]) < 1e-9) return null
                val tmp = m[col]; m[col] = m[pivot]; m[pivot] = tmp
                for (r in 0 until 8) {
                    if (r == col) continue
                    val f = m[r][col] / m[col][col]
                    for (c in col until 9) m[r][c] -= f * m[col][c]
                }
            }
            return Homography(DoubleArray(8) { m[it][8] / m[it][it] })
        }
    }
}

object Measure {
    fun pixelDistance(a: Pt, b: Pt): Double = hypot(a.x - b.x, a.y - b.y)

    /** Scale from two image points that are [knownMm] apart on the table. */
    fun linearScale(a: Pt, b: Pt, knownMm: Double): LinearScale? {
        val px = pixelDistance(a, b)
        if (px < 1.0 || !(knownMm > 0.0)) return null
        return LinearScale(knownMm / px)
    }

    /** Scale from the four corners of a rectangle (top-left, top-right, bottom-right, bottom-left in the image). */
    fun planeScale(corners: List<Pt>, widthMm: Double, heightMm: Double): PlaneScale? {
        if (corners.size != 4 || !(widthMm > 0.0) || !(heightMm > 0.0)) return null
        val dst = listOf(Pt(0.0, 0.0), Pt(widthMm, 0.0), Pt(widthMm, heightMm), Pt(0.0, heightMm))
        return Homography.from(corners, dst)?.let { PlaneScale(it) }
    }

    fun segments(points: List<Pt>, scale: Scale): List<Double> =
        points.zipWithNext { a, b -> scale.distanceMm(a, b) }

    fun pathMm(points: List<Pt>, scale: Scale): Double = segments(points, scale).sum()

    /** True when the reference line is so short that pixel errors dominate (under 5 % of the image width). */
    fun calibrationTooShort(a: Pt, b: Pt, imageWidthPx: Double): Boolean =
        imageWidthPx > 0 && pixelDistance(a, b) < imageWidthPx * 0.05

    /** Ratio of the longer to the shorter of two opposite edges; large values mean a strongly angled shot. */
    fun obliqueRatio(corners: List<Pt>): Double {
        if (corners.size != 4) return 1.0
        fun ratio(a: Double, b: Double) = if (minOf(a, b) < 1e-6) Double.POSITIVE_INFINITY else maxOf(a, b) / minOf(a, b)
        val top = pixelDistance(corners[0], corners[1])
        val bottom = pixelDistance(corners[3], corners[2])
        val left = pixelDistance(corners[0], corners[3])
        val right = pixelDistance(corners[1], corners[2])
        return maxOf(ratio(top, bottom), ratio(left, right))
    }

    fun format(mm: Double, unit: LengthUnit): String {
        if (mm.isNaN() || mm.isInfinite()) return "?"
        val suffix = if (unit == LengthUnit.Inch) "\"" else " ${unit.label}"
        return fixed(mm / unit.mm, unit.decimals) + suffix
    }

    fun fixed(v: Double, decimals: Int): String {
        val f = 10.0.pow(decimals)
        val scaled = (abs(v) * f).roundToLong()
        val sign = if (v < 0 && scaled != 0L) "-" else ""
        if (decimals == 0) return sign + scaled
        val whole = scaled / f.toLong()
        val frac = (scaled % f.toLong()).toString().padStart(decimals, '0')
        return "$sign$whole.$frac"
    }

    /** Parses a number typed by the user; accepts a comma as decimal separator. */
    fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}
