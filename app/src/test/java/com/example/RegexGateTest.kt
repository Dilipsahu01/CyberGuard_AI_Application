package com.example

import com.example.pipeline.RegexGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class RegexGateTest {

    private lateinit var regexGate: RegexGate

    @Before
    fun setUp() {
        regexGate = RegexGate()
    }

    @Test
    fun testEmptyString() {
        val (score, words) = regexGate.check("")
        assertEquals(0, score)
        assertEquals("", words)
    }

    @Test
    fun testNoMatch() {
        val (score, words) = regexGate.check("Hello this is a normal call with no bad words.")
        assertEquals(0, score)
        assertEquals("", words)
    }

    @Test
    fun testOtpMatch() {
        val (score, words) = regexGate.check("Please share your OTP immediately.")
        assertEquals(35, score)
        assertEquals("OTP", words.uppercase())
    }

    @Test
    fun testDigitalArrestMatch() {
        val (score, words) = regexGate.check("You are under digital arrest.")
        assertEquals(40, score)
        assertTrue(words.contains("digital arrest", ignoreCase = true))
    }

    @Test
    fun testMultipleMatchesAndReset() {
        val (score1, words1) = regexGate.check("Please share your otp")
        assertEquals(35, score1)
        
        val (score2, words2) = regexGate.check("You are under digital arrest")
        // Total score should accumulate
        assertEquals(75, score2)
        assertTrue(words2.contains("digital arrest", ignoreCase = true))

        regexGate.reset()
        
        val (score3, words3) = regexGate.check("Hello")
        assertEquals(0, score3)
        assertEquals("", words3)
    }

    @Test
    fun testScoreCappedAt100() {
        regexGate.check("otp") // 35
        regexGate.check("digital arrest") // 40
        regexGate.check("anydesk") // 35
        
        // 35 + 40 + 35 = 110, capped at 100
        val (score, _) = regexGate.check("cbi") // 30
        assertEquals(100, score)
    }

    @Test
    fun testFuzzyAndTrickyBoundaries() {
        // "otp" is a word boundary match, so "footprint" should NOT trigger it
        val (score1, _) = regexGate.check("He left a footprint")
        assertEquals(0, score1)
        
        // "anydesk" should trigger even if surrounded by weird punctuation
        val (score2, words2) = regexGate.check("download %anydesk% immediately!!!")
        assertEquals(35, score2)
        assertTrue(words2.contains("anydesk", ignoreCase = true))
        
        // "police coming" allows up to 20 chars between
        regexGate.reset()
        val (score3, words3) = regexGate.check("The police are currently coming to your house.")
        assertEquals(30, score3)
        assertTrue(words3.contains("police", ignoreCase = true))
        assertTrue(words3.contains("coming", ignoreCase = true))
    }

    @Test
    fun testMassiveSpamText() {
        val spamString = buildString {
            for (i in 1..10000) {
                append("normal conversation ")
            }
            append(" digital arrest ")
            for (i in 1..10000) {
                append("more random noise ")
            }
        }
        val (score, words) = regexGate.check(spamString)
        assertEquals(40, score)
        assertTrue(words.contains("digital arrest", ignoreCase = true))
    }

    @Test
    fun testConcurrencyAndThreadSafety() {
        val numThreads = 100
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(numThreads)
        
        for (i in 0 until numThreads) {
            executor.submit {
                // Different threads pushing different scam words simultaneously
                if (i % 2 == 0) {
                    regexGate.check("otp")
                } else {
                    regexGate.check("digital arrest")
                }
                latch.countDown()
            }
        }
        
        latch.await() // Wait for all threads to finish
        
        val (score, words) = regexGate.check("")
        // 35 (otp) + 40 (digital arrest) = 75
        assertEquals(75, score)
        assertTrue(words.contains("otp", ignoreCase = true))
        assertTrue(words.contains("digital arrest", ignoreCase = true))
        
        executor.shutdown()
    }
}
