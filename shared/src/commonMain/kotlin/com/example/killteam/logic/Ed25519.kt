package com.example.killteam.logic

/** Ed25519 signature check (RFC 8032), a port of the verification path of TweetNaCl. Only public data goes in. */
internal object Ed25519 {
    private const val MASK16 = 0xffffL

    private fun gf(vararg v: Long) = LongArray(16).also { v.copyInto(it) }

    private val GF0 = gf()
    private val GF1 = gf(1)

    // Curve constants, derived once from their definitions to avoid typo-prone tables.
    private val D: LongArray by lazy {
        val t = gf(0xdb41, 1) // 121665
        val u = gf(0xdb42, 1) // 121666
        mul(sub(GF0, t), inv(u))
    }
    private val I: LongArray by lazy { sqrtMinusOne() }

    private val L = longArrayOf(
        0xed, 0xd3, 0xf5, 0x5c, 0x1a, 0x63, 0x12, 0x58, 0xd6, 0x9c, 0xf7, 0xa2, 0xde, 0xf9, 0xde, 0x14,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x10,
    )

    private fun add(a: LongArray, b: LongArray) = LongArray(16) { a[it] + b[it] }
    private fun sub(a: LongArray, b: LongArray) = LongArray(16) { a[it] - b[it] }

    private fun carry(o: LongArray) {
        for (i in 0 until 16) {
            o[i] += 1L shl 16
            val c = o[i] shr 16
            if (i < 15) o[i + 1] += c - 1 else o[0] += c - 1 + 37 * (c - 1)
            o[i] -= c shl 16
        }
    }

    private fun mul(a: LongArray, b: LongArray): LongArray {
        val t = LongArray(31)
        for (i in 0 until 16) for (j in 0 until 16) t[i + j] += a[i] * b[j]
        for (i in 0 until 15) t[i] += 38 * t[i + 16]
        val o = LongArray(16) { t[it] }
        carry(o)
        carry(o)
        return o
    }

    private fun sqr(a: LongArray) = mul(a, a)

    private fun inv(x: LongArray): LongArray {
        var c = x.copyOf()
        for (a in 253 downTo 0) {
            c = sqr(c)
            if (a != 2 && a != 4) c = mul(c, x)
        }
        return c
    }

    // x^((p-5)/8)
    private fun pow2523(x: LongArray): LongArray {
        var c = x.copyOf()
        for (a in 250 downTo 0) {
            c = sqr(c)
            if (a != 1) c = mul(c, x)
        }
        return c
    }

    // 2 is a non-residue, so 2^((p-1)/4) = (2^((p-5)/8))^2 * 2 squares to -1.
    private fun sqrtMinusOne(): LongArray {
        val two = gf(2)
        return mul(sqr(pow2523(two)), two)
    }

    private fun pack(n: LongArray): ByteArray {
        var t = n.copyOf()
        carry(t); carry(t); carry(t)
        val m = LongArray(16)
        for (j in 0 until 2) {
            m[0] = t[0] - 0xffed
            for (i in 1 until 15) {
                m[i] = t[i] - 0xffff - ((m[i - 1] shr 16) and 1)
                m[i - 1] = m[i - 1] and MASK16
            }
            m[15] = t[15] - 0x7fff - ((m[14] shr 16) and 1)
            val b = (m[15] shr 16) and 1
            m[14] = m[14] and MASK16
            if (b == 0L) t = m.copyOf()
        }
        val o = ByteArray(32)
        for (i in 0 until 16) {
            o[2 * i] = (t[i] and 0xff).toByte()
            o[2 * i + 1] = (t[i] shr 8).toByte()
        }
        return o
    }

    private fun unpack(n: ByteArray): LongArray {
        val o = LongArray(16) { (n[2 * it].toLong() and 0xff) + ((n[2 * it + 1].toLong() and 0xff) shl 8) }
        o[15] = o[15] and 0x7fff
        return o
    }

    private fun parity(a: LongArray) = pack(a)[0].toInt() and 1
    private fun same(a: LongArray, b: LongArray) = pack(a).contentEquals(pack(b))

    private fun addPoints(p: Array<LongArray>, q: Array<LongArray>) {
        val d2 = add(D, D)
        val a = mul(sub(p[1], p[0]), sub(q[1], q[0]))
        val b = mul(add(p[0], p[1]), add(q[0], q[1]))
        val c = mul(mul(p[3], q[3]), d2)
        val dd = mul(p[2], q[2]).let { add(it, it) }
        val e = sub(b, a)
        val f = sub(dd, c)
        val g = add(dd, c)
        val h = add(b, a)
        p[0] = mul(e, f)
        p[1] = mul(h, g)
        p[2] = mul(g, f)
        p[3] = mul(e, h)
    }

    private fun scalarMult(q: Array<LongArray>, s: ByteArray): Array<LongArray> {
        var p = arrayOf(GF0.copyOf(), GF1.copyOf(), GF1.copyOf(), GF0.copyOf())
        var qq = Array(4) { q[it].copyOf() }
        for (i in 255 downTo 0) {
            val bit = (s[i / 8].toInt() shr (i and 7)) and 1
            if (bit == 1) { val tmp = p; p = qq; qq = tmp }
            addPoints(qq, p)
            addPoints(p, p)
            if (bit == 1) { val tmp = p; p = qq; qq = tmp }
        }
        return p
    }

    private val BASE_ENCODED = ByteArray(32) { 0x66 }.also { it[0] = 0x58 }

    private fun packPoint(p: Array<LongArray>): ByteArray {
        val zi = inv(p[2])
        val tx = mul(p[0], zi)
        val ty = mul(p[1], zi)
        val r = pack(ty)
        r[31] = (r[31].toInt() xor (parity(tx) shl 7)).toByte()
        return r
    }

    /** Decodes a point and negates it; null if the bytes are not on the curve. */
    private fun unpackNeg(p: ByteArray): Array<LongArray>? {
        val r1 = unpack(p)
        val r2 = GF1.copyOf()
        var num = sqr(r1)
        var den = mul(num, D)
        num = sub(num, r2)
        den = add(r2, den)
        val den2 = sqr(den)
        val den4 = sqr(den2)
        val den6 = mul(den4, den2)
        var t = mul(mul(den6, num), den)
        t = pow2523(t)
        t = mul(mul(mul(t, num), den), den)
        var r0 = mul(t, den)
        var chk = mul(sqr(r0), den)
        if (!same(chk, num)) r0 = mul(r0, I)
        chk = mul(sqr(r0), den)
        if (!same(chk, num)) return null
        if (parity(r0) == ((p[31].toInt() shr 7) and 1)) r0 = sub(GF0, r0)
        return arrayOf(r0, r1, r2, mul(r0, r1))
    }

    private fun reduce(h: ByteArray): ByteArray {
        val x = LongArray(64) { h[it].toLong() and 0xff }
        for (i in 63 downTo 32) {
            var carry = 0L
            var j = i - 32
            while (j < i - 12) {
                x[j] += carry - 16 * x[i] * L[j - (i - 32)]
                carry = (x[j] + 128) shr 8
                x[j] -= carry shl 8
                j++
            }
            x[j] += carry
            x[i] = 0
        }
        var carry = 0L
        for (j in 0 until 32) {
            x[j] += carry - (x[31] shr 4) * L[j]
            carry = x[j] shr 8
            x[j] = x[j] and 255
        }
        for (j in 0 until 32) x[j] -= carry * L[j]
        val r = ByteArray(32)
        for (i in 0 until 32) {
            x[i + 1] += x[i] shr 8
            r[i] = (x[i] and 255).toByte()
        }
        return r
    }

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false
        val negA = unpackNeg(publicKey) ?: return false
        val hash = Sha512.digest(signature.copyOfRange(0, 32) + publicKey + message)
        val p = scalarMult(negA, reduce(hash))
        val b = unpackNeg(BASE_ENCODED) ?: return false
        // unpackNeg returned -B; negate x and t to get B.
        val base = arrayOf(sub(GF0, b[0]), b[1], b[2], sub(GF0, b[3]))
        val q = scalarMult(base, signature.copyOfRange(32, 64))
        addPoints(p, q)
        return packPoint(p).contentEquals(signature.copyOfRange(0, 32))
    }
}
