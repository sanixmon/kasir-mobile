package com.kasir.mobile.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun testParseValidSemVer() {
        val v = SemVer.parse("v1.0.3")
        assertEquals(SemVer(1, 0, 3), v)

        val v2 = SemVer.parse("1.2.0")
        assertEquals(SemVer(1, 2, 0), v2)

        val vWithBuild = SemVer.parse("v1.0.2-8")
        assertEquals(SemVer(1, 0, 2), vWithBuild)
    }

    @Test
    fun testParseInvalidReturnsNull() {
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("invalid"))
    }

    @Test
    fun testComparison() {
        val v102 = SemVer.parse("1.0.2")!!
        val v103 = SemVer.parse("v1.0.3")!!
        val v110 = SemVer.parse("1.1.0")!!
        val v200 = SemVer.parse("2.0.0")!!

        assertTrue(v103 > v102)
        assertTrue(v110 > v103)
        assertTrue(v200 > v110)
        assertFalse(v102 > v103)
        assertFalse(v102 > v102)
    }

    @Test
    fun testPreReleaseTagDoesNotTriggerFalseUpdate() {
        val current = SemVer.parse("1.0.2")!!
        val remoteTag = SemVer.parse("v1.0.2-8")!!

        // Same base SemVer -> should NOT be considered an update
        assertFalse(remoteTag > current)
    }
}
