package com.parentalcontrol.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeGeneratorTest {

    @Test
    fun generateSixDigitCode_returnsSixDigits() {
        val code = CodeGenerator.generateSixDigitCode()
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }
}
