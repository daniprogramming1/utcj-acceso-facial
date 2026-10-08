package edu.utcj.acceso.ui

import edu.utcj.acceso.util.initialsOf
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun initials() {
        assertEquals("AL", initialsOf("Ana López"))
        assertEquals("MJ", initialsOf("  maría   josé  pérez "))
        assertEquals("CA", initialsOf("Carlos"))
        assertEquals("?", initialsOf("   "))
        assertEquals("ÁN", initialsOf("Ángel Núñez"))
    }
}
