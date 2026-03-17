package com.child.app.child.analysis

object BehaviourAnalyzer {

    fun calculateScore(

        gamingRisk: String,
        socialRisk: String,
        sleepRisk: String

    ): String {

        if (
            gamingRisk == "High Risk" ||
            socialRisk == "High Risk" ||
            sleepRisk == "High Risk"
        ) {

            return "High Risk"
        }

        if (
            gamingRisk == "Moderate" ||
            socialRisk == "Moderate"
        ) {

            return "Moderate"
        }

        return "Healthy"
    }
}