package com.example.killteam

import com.example.killteam.logic.LengthUnit
import com.example.killteam.logic.Measure
import com.example.killteam.logic.Pt
import com.example.killteam.logic.RuleDistance
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MeasureTest {
    private fun near(expected: Double, actual: Double, tol: Double = 0.01) =
        assertTrue(abs(expected - actual) <= tol, "expected $expected but was $actual")

    @Test
    fun linearScaleConvertsPixelsToMillimetres() {
        val s = assertNotNull(Measure.linearScale(Pt(0.0, 0.0), Pt(100.0, 0.0), 40.0))
        near(0.4, s.mmPerPx, 1e-9)
        near(200.0, s.distanceMm(Pt(0.0, 0.0), Pt(300.0, 400.0)))
    }

    @Test
    fun linearScaleRejectsInvalidInput() {
        assertNull(Measure.linearScale(Pt(5.0, 5.0), Pt(5.0, 5.0), 40.0))
        assertNull(Measure.linearScale(Pt(0.0, 0.0), Pt(10.0, 0.0), 0.0))
        assertNull(Measure.linearScale(Pt(0.0, 0.0), Pt(10.0, 0.0), Double.NaN))
    }

    @Test
    fun pathAndBetweenPoints() {
        val s = assertNotNull(Measure.linearScale(Pt(0.0, 0.0), Pt(10.0, 0.0), 10.0))
        val pts = listOf(Pt(0.0, 0.0), Pt(30.0, 40.0), Pt(30.0, 90.0), Pt(-30.0, 90.0))
        assertEquals(listOf(50.0, 50.0, 60.0), Measure.segments(pts, s))
        near(160.0, Measure.pathMm(pts, s))
        near(s.distanceMm(pts[2], pts[0]), s.distanceMm(pts[0], pts[2]))
        near(94.87, s.distanceMm(pts[0], pts[2]), 0.01)
        assertEquals(0.0, Measure.pathMm(listOf(Pt(1.0, 1.0)), s))
    }

    @Test
    fun planeScaleCorrectsPerspective() {
        val corners = listOf(Pt(200.0, 100.0), Pt(600.0, 100.0), Pt(700.0, 400.0), Pt(100.0, 400.0))
        val s = assertNotNull(Measure.planeScale(corners, 762.0, 558.8))
        near(762.0, s.distanceMm(corners[0], corners[1]), 0.001)
        near(558.8, s.distanceMm(corners[1], corners[2]), 0.001)
        near(762.0, s.distanceMm(corners[3], corners[2]), 0.001)
        near(558.8, s.distanceMm(corners[0], corners[3]), 0.001)
    }

    @Test
    fun planeScaleOnFlatRectangleIsUniform() {
        val corners = listOf(Pt(0.0, 0.0), Pt(400.0, 0.0), Pt(400.0, 300.0), Pt(0.0, 300.0))
        val s = assertNotNull(Measure.planeScale(corners, 800.0, 600.0))
        near(100.0, s.distanceMm(Pt(0.0, 0.0), Pt(50.0, 0.0)))
        near(500.0, s.distanceMm(Pt(0.0, 0.0), Pt(200.0, 150.0)))
    }

    @Test
    fun planeScaleRejectsDegenerateInput() {
        val line = listOf(Pt(0.0, 0.0), Pt(10.0, 0.0), Pt(20.0, 0.0), Pt(30.0, 0.0))
        assertNull(Measure.planeScale(line, 100.0, 100.0))
        assertNull(Measure.planeScale(line.take(3), 100.0, 100.0))
        val rect = listOf(Pt(0.0, 0.0), Pt(10.0, 0.0), Pt(10.0, 10.0), Pt(0.0, 10.0))
        assertNull(Measure.planeScale(rect, 0.0, 100.0))
    }

    @Test
    fun unitsAndFormatting() {
        assertEquals("1.0\"", Measure.format(25.4, LengthUnit.Inch))
        assertEquals("4.5\"", Measure.format(114.3, LengthUnit.Inch))
        assertEquals("11.4 cm", Measure.format(114.3, LengthUnit.Cm))
        assertEquals("114 mm", Measure.format(114.3, LengthUnit.Mm))
        assertEquals("0.0\"", Measure.format(0.0, LengthUnit.Inch))
        assertEquals("?", Measure.format(Double.NaN, LengthUnit.Inch))
        assertEquals("1.05", Measure.fixed(1.05, 2))
        assertEquals("0.05", Measure.fixed(0.05, 2))
    }

    @Test
    fun parsesNumbers() {
        assertEquals(28.5, Measure.parseNumber("28,5"))
        assertEquals(40.0, Measure.parseNumber(" 40 "))
        assertNull(Measure.parseNumber("abc"))
        assertNull(Measure.parseNumber("NaN"))
    }

    @Test
    fun ruleDistancesIncludeTheLimit() {
        assertTrue(RuleDistance.Circle.contains(25.4))
        assertTrue(RuleDistance.Circle.contains(20.0))
        assertTrue(!RuleDistance.Circle.contains(26.0))
        assertTrue(RuleDistance.Triangle.contains(50.8))
        assertTrue(!RuleDistance.Triangle.contains(52.0))
        assertTrue(RuleDistance.Square.contains(76.2))
        assertTrue(!RuleDistance.Square.contains(77.0))
        assertTrue(RuleDistance.Pentagon.contains(152.4))
        assertTrue(!RuleDistance.Pentagon.contains(153.5))
    }

    @Test
    fun calibrationHints() {
        assertTrue(Measure.calibrationTooShort(Pt(0.0, 0.0), Pt(30.0, 0.0), 1000.0))
        assertTrue(!Measure.calibrationTooShort(Pt(0.0, 0.0), Pt(300.0, 0.0), 1000.0))
        val flat = listOf(Pt(0.0, 0.0), Pt(400.0, 0.0), Pt(400.0, 300.0), Pt(0.0, 300.0))
        near(1.0, Measure.obliqueRatio(flat), 1e-9)
        val angled = listOf(Pt(200.0, 0.0), Pt(300.0, 0.0), Pt(400.0, 300.0), Pt(0.0, 300.0))
        assertTrue(Measure.obliqueRatio(angled) > 2.0)
    }
}
