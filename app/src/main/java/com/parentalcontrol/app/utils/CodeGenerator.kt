package com.parentalcontrol.app.utils

import java.security.SecureRandom

object CodeGenerator {

    private val random = SecureRandom()

    fun generateSixDigitCode(): String {
        return (100000 + random.nextInt(900000)).toString()
    }
}
