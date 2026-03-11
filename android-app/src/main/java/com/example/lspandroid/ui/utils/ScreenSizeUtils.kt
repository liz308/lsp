package com.example.lspandroid.ui.utils

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility object for screen size detection and responsive layout calculations.
 * Supports tablet detection and adaptive column count based on screen width.
 * 
 * Requirement 16: Theme and Accessibility - Multi-column layout for tablets
 */
object ScreenSizeUtils {
    
    /**
     * Determines if the current device is a tablet based on screen width.
     * Tablets are defined as devices with width >= 600dp.
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        return screenWidthDp >= 600.dp
    }
    
    /**
     * Determines if the device is a large tablet (width >= 720dp).
     */
    @Composable
    fun isLargeTablet(): Boolean {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        return screenWidthDp >= 720.dp
    }
    
    /**
     * Gets the optimal column count based on screen width.
     * - Phone (< 600dp): 1 column
     * - Tablet (600-719dp): 2 columns
     * - Large tablet (>= 720dp): 3 columns
     */
    @Composable
    fun getColumnCount(): Int {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        
        return when {
            screenWidthDp >= 720.dp -> 3
            screenWidthDp >= 600.dp -> 2
            else -> 1
        }
    }
    
    /**
     * Gets the optimal column count for a specific section based on WindowSizeClass.
     * This provides more granular control using Material 3's WindowSizeClass API.
     */
    fun getColumnCountFromWindowSize(windowSizeClass: WindowSizeClass): Int {
        return when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> 1  // Phone portrait
            WindowWidthSizeClass.Medium -> 2   // Phone landscape or small tablet
            WindowWidthSizeClass.Expanded -> 3 // Tablet or desktop
            else -> 1
        }
    }
    
    /**
     * Gets the screen width in dp.
     */
    @Composable
    fun getScreenWidthDp(): Dp {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp.dp
    }
    
    /**
     * Gets the screen height in dp.
     */
    @Composable
    fun getScreenHeightDp(): Dp {
        val configuration = LocalConfiguration.current
        return configuration.screenHeightDp.dp
    }
    
    /**
     * Calculates optimal spacing between columns based on screen width.
     */
    @Composable
    fun getColumnSpacing(): Dp {
        val screenWidth = getScreenWidthDp()
        return when {
            screenWidth >= 720.dp -> 16.dp
            screenWidth >= 600.dp -> 12.dp
            else -> 8.dp
        }
    }
    
    /**
     * Calculates optimal padding for content based on screen width.
     */
    @Composable
    fun getContentPadding(): Dp {
        val screenWidth = getScreenWidthDp()
        return when {
            screenWidth >= 720.dp -> 24.dp
            screenWidth >= 600.dp -> 20.dp
            else -> 16.dp
        }
    }
}
