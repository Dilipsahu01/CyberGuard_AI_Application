package com.example

import com.example.utils.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun testNormalizeWithCountryCode() {
        val number = "+91 98765 43210"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeWithoutCountryCode() {
        val number = "9876543210"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeWithDashesAndSpaces() {
        val number = "+91-987-654-3210"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeLongerNumber() {
        val number = "0119876543210"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testAreEqual() {
        val number1 = "+91 98765 43210"
        val number2 = "9876543210"
        assertTrue(PhoneNumberUtils.areEqual(number1, number2))
    }

    @Test
    fun testNormalizeWithExtremePunctuation() {
        val number = "++91(  ) 9--8.7_6*5# 4/3\\2@1!0"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeWithEmojisAndUnicode() {
        val number = "📱+91📞98765 😈 43210 🇮🇳"
        val expected = "9876543210"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeShortNumber() {
        val number = "100"
        val expected = "100"
        assertEquals(expected, PhoneNumberUtils.normalize(number))
    }

    @Test
    fun testNormalizeEmptyAndBlank() {
        assertEquals("", PhoneNumberUtils.normalize(""))
        assertEquals("", PhoneNumberUtils.normalize("    "))
        assertEquals("", PhoneNumberUtils.normalize("!@#$%^&*()"))
    }

}
