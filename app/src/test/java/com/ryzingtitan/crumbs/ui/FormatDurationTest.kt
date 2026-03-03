package com.ryzingtitan.crumbs.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {

    private val method = Class.forName("com.ryzingtitan.crumbs.ui.LocationScreenKt")
        .getDeclaredMethod("formatDuration", Long::class.javaPrimitiveType)
        .also { it.isAccessible = true }

    private fun fmt(ms: Long): String = method.invoke(null, ms) as String

    @Test
    fun formatDuration_zeroDuration() {
        assertEquals("0m 0s", fmt(0L))
    }

    @Test
    fun formatDuration_seconds() {
        assertEquals("0m 45s", fmt(45_000L))
    }

    @Test
    fun formatDuration_oneMinute() {
        assertEquals("1m 0s", fmt(60_000L))
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        assertEquals("1m 15s", fmt(75_000L))
    }

    @Test
    fun formatDuration_exactlyOneHour() {
        assertEquals("1h 0m 0s", fmt(3_600_000L))
    }

    @Test
    fun formatDuration_overOneHour() {
        assertEquals("1h 1m 1s", fmt(3_661_000L))
    }

    @Test
    fun formatDuration_multipleHours() {
        assertEquals("2h 2m 2s", fmt(7_322_000L))
    }
}
