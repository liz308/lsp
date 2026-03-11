package com.example.lspandroid.ui.utils

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ScreenSizeUtils.
 * Tests tablet detection and column count calculations.
 */
class ScreenSizeUtilsTest {

    @Test
    fun `getColumnCountFromWindowSize returns 1 for compact width`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
        val columnCount = ScreenSizeUtils.getColumnCountFromWindowSize(windowSizeClass)
        assertEquals(1, columnCount)
    }

    @Test
    fun `getColumnCountFromWindowSize returns 2 for medium width`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(650.dp, 800.dp))
        val columnCount = ScreenSizeUtils.getColumnCountFromWindowSize(windowSizeClass)
        assertEquals(2, columnCount)
    }

    @Test
    fun `getColumnCountFromWindowSize returns 3 for expanded width`() {
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(900.dp, 800.dp))
        val columnCount = ScreenSizeUtils.getColumnCountFromWindowSize(windowSizeClass)
        assertEquals(3, columnCount)
    }
}
