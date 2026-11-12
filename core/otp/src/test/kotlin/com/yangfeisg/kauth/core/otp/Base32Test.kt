package com.yangfeisg.kauth.core.otp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Base32Test {
    @Test
    fun `decodes ignoring case, grouping and padding`() {
        val expected = "12345678901234567890".toByteArray()
        assertArrayEquals(expected, Base32.decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"))
        assertArrayEquals(expected, Base32.decode("gezd gnbv gy3t qojq gezd-gnbv-gy3t-qojq"))
        assertArrayEquals("fo".toByteArray(), Base32.decode("MZXQ===="))
    }

    @Test
    fun `rejects empty keys and characters outside the alphabet`() {
        assertFalse(Base32.isValid(""))
        assertFalse(Base32.isValid("   "))
        assertFalse(Base32.isValid("ABC1DEFG"))
        assertTrue(Base32.isValid("JBSWY3DPEHPK3PXP"))
    }
}
