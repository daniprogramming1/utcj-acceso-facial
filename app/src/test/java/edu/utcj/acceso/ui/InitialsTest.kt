package edu.utcj.acceso.ui

import edu.utcj.acceso.util.initialsOf
import edu.utcj.acceso.util.shortName
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

    @Test
    fun shortNameForPublicScreens() {
        assertEquals("Ana S.", shortName("Ana Sofía López"))
        assertEquals("Luis R.", shortName("  Luis   Ramírez Torres "))
        assertEquals("Ángel N.", shortName("Ángel núñez"))
        assertEquals("Carlos", shortName("Carlos"))
        assertEquals("", shortName("   "))
    }
}
