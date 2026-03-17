package com.child.app.child.analysis

object RiskAnalyzer {

    private val riskyWords = listOf(

        "porn",
        "xxx",
        "casino",
        "bet",
        "drugs"
    )

    fun isRiskyLink(url: String): Boolean {

        for (word in riskyWords) {

            if (url.contains(word)) {

                return true
            }
        }

        return false
    }
}