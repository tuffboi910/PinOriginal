package com.pinoriginal.app

import com.pinoriginal.app.net.PinterestExtractor
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OriginalCandidateTest {
    @Test fun derivesOriginalFromPinimgHashPath() {
        val actual = derive("https://i.pinimg.com/736x/aa/bb/cc/aabbccddeeff.jpg")
        assertEquals("https://i.pinimg.com/originals/aa/bb/cc/aabbccddeeff.jpg", actual)
    }

    @Test fun rejectsNonPinimgUrls() {
        assertNull(derive("https://evil.test/736x/aa/bb/cc/aabbccddeeff.jpg"))
    }

    private fun derive(url: String): String? {
        val extractor = PinterestExtractor(OkHttpClient())
        val method = PinterestExtractor::class.java.getDeclaredMethod("deriveOriginalCandidate", String::class.java)
        method.isAccessible = true
        return method.invoke(extractor, url) as String?
    }
}
