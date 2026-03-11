package com.example.lspandroid.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PluginChain data structure.
 * Tests plugin management, ordering, and chain operations.
 */
class PluginChainTest {
    private lateinit var chain: PluginChain

    @Before
    fun setUp() {
        chain = PluginChain()
    }

    @Test
    fun testEmptyChain() {
        assertTrue(chain.isEmpty())
        assertEquals(0, chain.size())
        assertTrue(chain.getPlugins().isEmpty())
    }

    @Test
    fun testAddPlugin() {
        val plugin = ChainedPlugin(
            pluginId = "eq_1",
            pluginName = "Parametric EQ",
            handle = 1L
        )

        assertTrue(chain.addPlugin(plugin))
        assertEquals(1, chain.size())
        assertFalse(chain.isEmpty())
        assertEquals(plugin, chain.getPlugin(0))
    }

    @Test
    fun testAddMultiplePlugins() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)
        val limiter = ChainedPlugin("limiter_1", "Limiter", 3L)

        assertTrue(chain.addPlugin(eq))
        assertTrue(chain.addPlugin(comp))
        assertTrue(chain.addPlugin(limiter))

        assertEquals(3, chain.size())
        assertEquals(eq, chain.getPlugin(0))
        assertEquals(comp, chain.getPlugin(1))
        assertEquals(limiter, chain.getPlugin(2))
    }

    @Test
    fun testInsertPlugin() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)
        val limiter = ChainedPlugin("limiter_1", "Limiter", 3L)

        chain.addPlugin(eq)
        chain.addPlugin(limiter)

        // Insert compressor between EQ and Limiter
        assertTrue(chain.insertPlugin(1, comp))

        assertEquals(3, chain.size())
        assertEquals(eq, chain.getPlugin(0))
        assertEquals(comp, chain.getPlugin(1))
        assertEquals(limiter, chain.getPlugin(2))
    }

    @Test
    fun testInsertPluginAtStart() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        assertTrue(chain.insertPlugin(0, comp))

        assertEquals(2, chain.size())
        assertEquals(comp, chain.getPlugin(0))
        assertEquals(eq, chain.getPlugin(1))
    }

    @Test
    fun testInsertPluginInvalidPosition() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)

        assertFalse(chain.insertPlugin(-1, plugin))
        assertFalse(chain.insertPlugin(1, plugin))  // Chain is empty

        chain.addPlugin(plugin)
        assertFalse(chain.insertPlugin(-1, plugin))
        assertFalse(chain.insertPlugin(2, plugin))  // Out of bounds
    }

    @Test
    fun testRemovePlugin() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)

        val removed = chain.removePlugin(0)
        assertEquals(eq, removed)
        assertEquals(1, chain.size())
        assertEquals(comp, chain.getPlugin(0))
    }

    @Test
    fun testRemovePluginInvalidPosition() {
        assertNull(chain.removePlugin(0))
        assertNull(chain.removePlugin(-1))

        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertNull(chain.removePlugin(1))
        assertNull(chain.removePlugin(-1))
    }

    @Test
    fun testRemovePluginById() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)

        assertTrue(chain.removePluginById("eq_1"))
        assertEquals(1, chain.size())
        assertEquals(comp, chain.getPlugin(0))

        assertFalse(chain.removePluginById("nonexistent"))
    }

    @Test
    fun testMovePlugin() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)
        val limiter = ChainedPlugin("limiter_1", "Limiter", 3L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)
        chain.addPlugin(limiter)

        // Move limiter to position 0
        assertTrue(chain.movePlugin(2, 0))

        assertEquals(limiter, chain.getPlugin(0))
        assertEquals(eq, chain.getPlugin(1))
        assertEquals(comp, chain.getPlugin(2))
    }

    @Test
    fun testMovePluginInvalidPosition() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertFalse(chain.movePlugin(-1, 0))
        assertFalse(chain.movePlugin(0, -1))
        assertFalse(chain.movePlugin(1, 0))
        assertFalse(chain.movePlugin(0, 1))
    }

    @Test
    fun testMovePluginSamePosition() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertTrue(chain.movePlugin(0, 0))  // No-op should succeed
        assertEquals(plugin, chain.getPlugin(0))
    }

    @Test
    fun testSetPluginBypassed() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertTrue(chain.setPluginBypassed(0, true))
        assertTrue(chain.getPlugin(0)?.isBypassed ?: false)

        assertTrue(chain.setPluginBypassed(0, false))
        assertFalse(chain.getPlugin(0)?.isBypassed ?: true)
    }

    @Test
    fun testSetPluginBypassedInvalidPosition() {
        assertFalse(chain.setPluginBypassed(0, true))
        assertFalse(chain.setPluginBypassed(-1, true))
    }

    @Test
    fun testSetPluginParameter() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertTrue(chain.setPluginParameter(0, 0, 12.5f))
        assertEquals(12.5f, chain.getPlugin(0)?.parameters?.get(0), 0.001f)

        assertTrue(chain.setPluginParameter(0, 1, 1000.0f))
        assertEquals(1000.0f, chain.getPlugin(0)?.parameters?.get(1), 0.001f)
    }

    @Test
    fun testSetPluginParameterInvalidPosition() {
        assertFalse(chain.setPluginParameter(0, 0, 12.5f))
        assertFalse(chain.setPluginParameter(-1, 0, 12.5f))
    }

    @Test
    fun testFindPluginById() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)

        assertEquals(eq, chain.findPluginById("eq_1"))
        assertEquals(comp, chain.findPluginById("comp_1"))
        assertNull(chain.findPluginById("nonexistent"))
    }

    @Test
    fun testGetPluginPosition() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)

        assertEquals(0, chain.getPluginPosition("eq_1"))
        assertEquals(1, chain.getPluginPosition("comp_1"))
        assertEquals(-1, chain.getPluginPosition("nonexistent"))
    }

    @Test
    fun testContainsPlugin() {
        val plugin = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        chain.addPlugin(plugin)

        assertTrue(chain.containsPlugin("eq_1"))
        assertFalse(chain.containsPlugin("nonexistent"))
    }

    @Test
    fun testGetActivePlugins() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L, isBypassed = true)
        val limiter = ChainedPlugin("limiter_1", "Limiter", 3L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)
        chain.addPlugin(limiter)

        val active = chain.getActivePlugins()
        assertEquals(2, active.size)
        assertTrue(active.contains(eq))
        assertTrue(active.contains(limiter))
        assertFalse(active.contains(comp))
    }

    @Test
    fun testGetBypassedPlugins() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L, isBypassed = true)
        val limiter = ChainedPlugin("limiter_1", "Limiter", 3L, isBypassed = true)

        chain.addPlugin(eq)
        chain.addPlugin(comp)
        chain.addPlugin(limiter)

        val bypassed = chain.getBypassedPlugins()
        assertEquals(2, bypassed.size)
        assertTrue(bypassed.contains(comp))
        assertTrue(bypassed.contains(limiter))
        assertFalse(bypassed.contains(eq))
    }

    @Test
    fun testClear() {
        chain.addPlugin(ChainedPlugin("eq_1", "Parametric EQ", 1L))
        chain.addPlugin(ChainedPlugin("comp_1", "Compressor", 2L))

        assertEquals(2, chain.size())
        chain.clear()
        assertEquals(0, chain.size())
        assertTrue(chain.isEmpty())
    }

    @Test
    fun testValidate() {
        val eq = ChainedPlugin("eq_1", "Parametric EQ", 1L)
        val comp = ChainedPlugin("comp_1", "Compressor", 2L)

        chain.addPlugin(eq)
        chain.addPlugin(comp)

        assertTrue(chain.validate())

        // Add duplicate ID
        chain.addPlugin(ChainedPlugin("eq_1", "Another EQ", 3L))
        assertFalse(chain.validate())
    }

    @Test
    fun testValidateEmptyName() {
        val invalidPlugin = ChainedPlugin("eq_1", "", 1L)
        chain.addPlugin(invalidPlugin)

        assertFalse(chain.validate())
    }

    @Test
    fun testValidateEmptyId() {
        val invalidPlugin = ChainedPlugin("", "Parametric EQ", 1L)
        chain.addPlugin(invalidPlugin)

        assertFalse(chain.validate())
    }
}
