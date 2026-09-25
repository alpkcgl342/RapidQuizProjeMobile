package com.alpkcgl.rapidquizmobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknameTest {

    @Test
    fun `bosluklari normalize eder`() {
        assertEquals("Ali Alp", Nickname.normalize("  Ali    Alp "))
    }

    @Test
    fun `gecerli takma adlar`() {
        assertNull(Nickname.validate("Şimşek_42"))
        assertNull(Nickname.validate("çağrı-ığü"))
        assertNull(Nickname.validate("  Ab  "))
    }

    @Test
    fun `uzunluk kurali normalize edilmis metne uygulanir`() {
        assertEquals(Nickname.Error.LENGTH, Nickname.validate(" a "))
        assertEquals(Nickname.Error.LENGTH, Nickname.validate("a".repeat(21)))
    }

    @Test
    fun `izin verilmeyen karakterler`() {
        assertEquals(Nickname.Error.CHARACTERS, Nickname.validate("ali@x"))
        assertEquals(Nickname.Error.CHARACTERS, Nickname.validate("emoji😀"))
    }
}
