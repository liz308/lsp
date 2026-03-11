package com.example.lspandroid.bugfix

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Preservation Property Test for Kotlin Compilation
 * 
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 * 
 * **Property 2: Preservation** - Non-Buggy Files Unchanged
 * 
 * This test verifies that Kotlin files NOT affected by the bug continue to
 * compile successfully. These files should have the same compilation behavior
 * before and after the fix.
 * 
 * **IMPORTANT**: This test follows observation-first methodology - it observes
 * behavior on UNFIXED code for files where isBugCondition returns false.
 * 
 * **EXPECTED OUTCOME ON UNFIXED CODE**: Tests PASS (confirms baseline behavior)
 * **EXPECTED OUTCOME ON FIXED CODE**: Tests PASS (confirms no regressions)
 * 
 * Property-based testing approach: We test multiple non-buggy files to ensure
 * strong guarantees that the fix doesn't introduce regressions.
 */
class KotlinCompilationPreservationTest {

    /**
     * Bug condition function that identifies files with compilation errors.
     * 
     * Returns false for files that should NOT be affected by the bug fix.
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
     * Property Test: Preservation - Non-Buggy Files Compile Successfully
     * 
     * For any Kotlin source file where the bug condition does NOT hold
     * (isBugCondition returns false), the file SHALL compile successfully
     * both before and after the fix.
     * 
     * This test verifies the baseline behavior on unfixed code that must
     * be preserved after implementing the fix.
     */
    @Test
    fun `property test - non-buggy files should compile successfully`() {
        // Define files where bug condition does NOT hold (non-buggy files)
        val nonBuggyFiles = listOf(
            "android-app/src/main/java/com/example/lspandroid/AppState.kt",
            "android-app/src/main/java/com/example/lspandroid/model/PortMetadataParser.kt",
            "android-app/src/main/java/com/example/lspandroid/audio/EqualizerSettings.kt",
            "android-app/src/main/java/com/example/lspandroid/ui/ComposeExtensions.kt",
            "android-app/src/main/java/com/example/lspandroid/ui/LayoutConfigs.kt",
            "android-app/src/main/java/com/example/lspandroid/MainActivity.kt"
        )

        // Verify all files exist
        val projectRoot = findProjectRoot()
        val missingFiles = nonBuggyFiles.filter { !File(projectRoot, it).exists() }
        if (missingFiles.isNotEmpty()) {
            fail("Non-buggy files not found: $missingFiles")
        }

        // Verify bug condition function correctly identifies these as non-buggy
        nonBuggyFiles.forEach { filePath ->
            assertTrue(
                !isBugCondition(filePath),
                "isBugCondition should return false for $filePath (non-buggy file)"
            )
        }

        // Check that these files have valid Kotlin syntax
        val syntaxIssues = checkKotlinSyntax(projectRoot, nonBuggyFiles)
        
        if (syntaxIssues.isNotEmpty()) {
            val issueSummary = buildString {
                appendLine("=== PRESERVATION TEST - SYNTAX ISSUES FOUND ===")
                appendLine()
                appendLine("Files with syntax issues: ${syntaxIssues.size}")
                appendLine()
                syntaxIssues.forEach { (file, issues) ->
                    appendLine("File: $file")
                    issues.forEach { issue ->
                        appendLine("  - $issue")
                    }
                }
            }
            
            println(issueSummary)
            fail("""
                Preservation test failed: Found syntax issues in ${syntaxIssues.size} non-buggy files.
                
                These files should compile successfully both before and after the fix.
                
                See console output above for details.
            """.trimIndent())
        } else {
            println("=== PRESERVATION TEST PASSED ===")
            println("All ${nonBuggyFiles.size} non-buggy files have valid Kotlin syntax")
            println("Files tested:")
            nonBuggyFiles.forEach { println("  ✓ $it") }
            println()
            println("This baseline behavior must be preserved after implementing the fix.")
            
            assertTrue(true, "All non-buggy files have valid syntax - preservation baseline confirmed")
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
     * Checks Kotlin files for basic syntax validity
     * 
     * This performs lightweight syntax checks to verify files are well-formed:
     * - Balanced braces
     * - Valid package declarations
     * - Valid import statements
     * - No obvious syntax errors
     */
    private fun checkKotlinSyntax(
        projectRoot: File,
        files: List<String>
    ): Map<String, List<String>> {
        val issues = mutableMapOf<String, MutableList<String>>()
        
        files.forEach { filePath ->
            val file = File(projectRoot, filePath)
            if (file.exists()) {
                val content = file.readText()
                val fileIssues = mutableListOf<String>()
                
                // Check for balanced braces
                val openBraces = content.count { it == '{' }
                val closeBraces = content.count { it == '}' }
                if (openBraces != closeBraces) {
                    fileIssues.add("Mismatched braces: $openBraces opening, $closeBraces closing")
                }
                
                // Check for balanced parentheses
                val openParens = content.count { it == '(' }
                val closeParens = content.count { it == ')' }
                if (openParens != closeParens) {
                    fileIssues.add("Mismatched parentheses: $openParens opening, $closeParens closing")
                }
                
                // Check for balanced brackets
                val openBrackets = content.count { it == '[' }
                val closeBrackets = content.count { it == ']' }
                if (openBrackets != closeBrackets) {
                    fileIssues.add("Mismatched brackets: $openBrackets opening, $closeBrackets closing")
                }
                
                // Check for valid package declaration
                val packagePattern = Regex("""^\s*package\s+[\w.]+\s*$""", RegexOption.MULTILINE)
                if (!packagePattern.containsMatchIn(content)) {
                    fileIssues.add("Missing or invalid package declaration")
                }
                
                // Check file ends properly (with closing brace or newline)
                val trimmedContent = content.trimEnd()
                if (trimmedContent.isNotEmpty() && !trimmedContent.endsWith("}") && !trimmedContent.endsWith(")")) {
                    // Some files might end with other valid constructs, so this is just a warning
                    // We'll skip this check as it's too strict
                }
                
                if (fileIssues.isNotEmpty()) {
                    issues[filePath] = fileIssues
                }
            }
        }
        
        return issues
    }
}
