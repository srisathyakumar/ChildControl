package com.child.app.child

object ContentFilter {

    private val blockedWords = listOf(
        "porn",
        "xxx",
        "casino",
        "bet",
        "drug",
        "sex"
    )

    fun isBlocked(text: String): Boolean {

        return blockedWords.any {
            text.contains(it, true)
        }
    }
}