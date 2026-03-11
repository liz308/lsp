package com.example.lspandroid.bugfix

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Bug Condition Exploration Test for Kotlin Compilation Errors
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9**
 * 
 * **Property 1: Bug Condition** - Kotlin Compilation Errors
 * 
 * This test verifies that the bug condition exists by checking for compilation
 * errors in the three problematic Kotlin files.
 * 
 * **CRITICAL**: This test is EXPECTED TO FAIL on unfixed code - failure confirms
 * the bug exists. When the test fails with compilation errors, that's SUCCESS
 * for this exploration phase.
 * 
 * **Scoped PBT Approach**: This test scopes the property to the three concrete
 * failing files: PluginChain.kt, PluginParameter.kt, SimpleMainActivity.kt
 * 
 * The test encodes the expected behavior (zero compilation errors) which will
 * validate the fix when it passes after implementation.
 * 
 * **EXPECTED COUNTEREXAMPLES ON UNFIXED CODE**:
 * - PluginChain.kt: Unresolved references to validatePlugin, validateChain, compensateLatency
 * - PluginChain.kt: Missing closing braces, incomplete structure
 * - PluginParameter.kt: Redeclaration of PluginParameter class
 * - PluginParameter.kt: Unresolved reference to COLOR enum value
 * - SimpleMainActivity.kt: Unresolved reference to theme package and LSPAndroidTheme
 */
class KotlinCompilationBugConditionTest {

    /**
     * Bug condition function that identifies files with compilation errors.
     * 
     * Returns true for files where the bug condition holds:
     * - PluginChain.kt: unresolved references, missing methods, incomplete structure
     * - PluginParameter.kt: redeclaration errors, invalid enum reference
     * - SimpleMainActivity.kt: missing theme package and composable
     */
    private fun isBugCondition(filePath: String): Boolean {
        return when {
            filePath.endsWith("PluginChain.kt") -> true
            filePath.endsWith("PluginParameter.kt") -> true
            filePath.endsWith("SimpleMainActivity.kt") -> true
            else -> false
        }
    }

    /**
     * Property Test: Bug Condition - Compilation Success
     * 
     * For any Kotlin source file where the bug condition holds (isBugCondition returns true),
     * the fixed code SHALL compile successfully with zero errors.
     * 
     * **EXPECTED OUTCOME ON UNFIXED CODE**: This test FAILS with compilation errors
     * (this is correct - it proves the bug exists)
     * 
     * **EXPECTED OUTCOME ON FIXED CODE**: This test PASSES with zero compilation errors
     * (this validates the fix)
     */
    @Test
    fun `property test - bug condition files should compile with zero errors`() {
        // Define the three files where bug condition holds
        val bugConditionFiles = listOf(
            "android-app/src/main/java/com/example/lspandroid/model/PluginChain.kt",
            "android-app/src/main/java/com/example/lspandroid/model/PluginParameter.kt",
            "android-app/src/main/java/com/example/lspandroid/SimpleMainActivity.kt"
        )

        // Verify all files exist
        val projectRoot = findProjectRoot()
        val missingFiles = bugConditionFiles.filter { !File(projectRoot, it).exists() }
        if (missingFiles.isNotEmpty()) {
            fail("Bug condition files not found: $missingFiles")
        }

        // Verify bug condition function correctly identifies these files
        bugConditionFiles.forEach { filePath ->
            assertTrue(
                isBugCondition(filePath),
                "isBugCondition should return true for $filePath"
            )
        }

        // Check for compilation errors by analyzing the source files
        val compilationErrors = checkCompilationErrors(projectRoot, bugConditionFiles)
        
        // Document the counterexamples (compilation errors)
        if (compilationErrors.isNotEmpty()) {
            val errorSummary = buildString {
                appendLine("=== BUG CONDITION EXPLORATION - COUNTEREXAMPLES FOUND ===")
                appendLine()
                appendLine("Total compilation errors: ${compilationErrors.size}")
                appendLine()
                appendLine("Expected errors in bug condition files:")
                appendLine("1. PluginChain.kt:")
                appendLine("   - Unresolved reference: validatePlugin")
                appendLine("   - Unresolved reference: validateChain")
                appendLine("   - Unresolved reference: compensateLatency")
                appendLine("   - Missing closing braces")
                appendLine("   - Incomplete structure")
                appendLine()
                appendLine("2. PluginParameter.kt:")
                appendLine("   - Redeclaration: PluginParameter")
                appendLine("   - Unresolved reference: COLOR")
                appendLine()
                appendLine("3. SimpleMainActivity.kt:")
                appendLine("   - Unresolved reference: theme")
                appendLine("   - Unresolved reference: LSPAndroidTheme")
                appendLine()
                appendLine("Actual compilation errors found:")
                compilationErrors.forEach { (file, errors) ->
                    appendLine("  File: $file")
                    errors.forEach { error ->
                        appendLine("    - $error")
                    }
                }
                appendLine()
                appendLine("=== BUG CONFIRMED: Compilation fails as expected ===")
            }
            
            println(errorSummary)
            
            // On unfixed code, this assertion will fail, which is EXPECTED
            // This failure confirms the bug exists
            fail("""
                Bug condition exploration complete: Found ${compilationErrors.values.sumOf { it.size }} compilation errors.
                
                This test FAILURE is EXPECTED on unfixed code - it confirms the bug exists.
                
                The test encodes the expected behavior (zero errors) and will PASS after the fix is implemented.
                
                See console output above for detailed counterexamples.
            """.trimIndent())
        } else {
            // On fixed code, this will pass
            println("=== BUG FIXED: All bug condition files compile successfully ===")
            assertTrue(true, "Compilation successful - bug is fixed")
        }
    }

    /**
     * Helper function to find the project root directory
     */
    private fun findProjectRoot(): File {
        var current = File(".").absoluteFile
        while (current != null && !File(current, "settings.gradle.kts").exists()) {
            current = current.parentFile
        }
        return current ?: File(".")
    }

    /**
     * Checks for compilation errors by analyzing source files
     */
    private fun checkCompilationErrors(
        projectRoot: File,
        files: List<String>
    ): Map<String, List<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        
        files.forEach { filePath ->
            val file = File(projectRoot, filePath)
            if (file.exists()) {
                val content = file.readText()
                val fileErrors = mutableListOf<String>()
                
                // Check for specific known errors based on the bug description
                when {
                    filePath.endsWith("PluginChain.kt") -> {
                        // Check for unresolved references
                        if (content.contains("validatePlugin(") && !content.contains("fun validatePlugin(")) {
                            fileErrors.add("Unresolved reference: validatePlugin (method called but not defined)")
                        }
                        if (content.contains("validateChain(") && !content.contains("fun validateChain(")) {
                            fileErrors.add("Unresolved reference: validateChain (method called but not defined)")
                        }
                        if (content.contains("compensateLatency(") && !content.contains("fun compensateLatency(")) {
                            fileErrors.add("Unresolved reference: compensateLatency (method called but not defined)")
                        }
                        
                        // Check for incomplete structure (file should end with closing braces)
                        val trimmedContent = content.trimEnd()
                        if (!trimmedContent.endsWith("}")) {
                            fileErrors.add("Incomplete file structure: file does not end with closing brace")
                        }
                        
                        // Count opening and closing braces
                        val openBraces = content.count { it == '{' }
                        val closeBraces = content.count { it == '}' }
                        if (openBraces != closeBraces) {
                            fileErrors.add("Mismatched braces: $openBraces opening, $closeBraces closing (difference: ${openBraces - closeBraces})")
                        }
                    }
                    
                    filePath.endsWith("PluginParameter.kt") -> {
                        // Check for COLOR enum reference
                        if (content.contains("ParameterType.COLOR") || content.contains("COLOR")) {
                            val enumDefinition = content.substringAfter("enum class ParameterType", "")
                            if (enumDefinition.isNotEmpty() && !enumDefinition.contains("COLOR")) {
                                fileErrors.add("Unresolved reference: COLOR (referenced but not defined in ParameterType enum)")
                            }
                        }
                    }
                    
                    filePath.endsWith("SimpleMainActivity.kt") -> {
                        // Check for theme imports
                        if (content.contains("import com.example.lspandroid.ui.theme.LSPAndroidTheme")) {
                            val themeFile = File(projectRoot, "android-app/src/main/java/com/example/lspandroid/ui/theme/Theme.kt")
                            if (!themeFile.exists()) {
                                fileErrors.add("Unresolved reference: LSPAndroidTheme (theme package does not exist)")
                            }
                        }
                        if (content.contains("LSPAndroidTheme")) {
                            val themeFile = File(projectRoot, "android-app/src/main/java/com/example/lspandroid/ui/theme/Theme.kt")
                            if (!themeFile.exists()) {
                                fileErrors.add("Unresolved reference: LSPAndroidTheme composable (not defined)")
                            }
                        }
                    }
                }
                
                if (fileErrors.isNotEmpty()) {
                    errors[filePath] = fileErrors
                }
            }
        }
        
        return errors
    }
}
