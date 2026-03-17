package com.child.app.child

object AgeCalculator {

    fun getAge(dob: Long): Int {

        val now = java.util.Calendar.getInstance()
        val birth = java.util.Calendar.getInstance()

        birth.timeInMillis = dob

        return now.get(java.util.Calendar.YEAR) -
                birth.get(java.util.Calendar.YEAR)
    }
}