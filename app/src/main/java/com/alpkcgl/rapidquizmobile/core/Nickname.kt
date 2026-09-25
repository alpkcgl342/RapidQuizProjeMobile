package com.alpkcgl.rapidquizmobile.core

/** Takma ad kuralları — backend `common/nickname.py` ve web `lib/nickname.ts` ile aynı. */
object Nickname {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 20

    private val ALLOWED = Regex("^[A-Za-z0-9ÇĞİÖŞÜçğıöşüÂÎÛâîû_\\- ]+$")
    private val SPACES = Regex("\\s+")

    enum class Error { LENGTH, CHARACTERS }

    /** Baş/son boşlukları kırpar, ardışık boşlukları teke indirir. */
    fun normalize(value: String): String = value.replace(SPACES, " ").trim()

    /** Geçerliyse null, değilse hata türü. */
    fun validate(value: String): Error? {
        val nickname = normalize(value)
        return when {
            nickname.length !in MIN_LENGTH..MAX_LENGTH -> Error.LENGTH
            !ALLOWED.matches(nickname) -> Error.CHARACTERS
            else -> null
        }
    }
}
