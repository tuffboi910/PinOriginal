package com.pinoriginal.app

import com.pinoriginal.app.net.PinterestUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PinterestUrlTest {
    @Test fun normalizesPinterestLinks() {
        assertNotNull(PinterestUrl.normalize("www.pinterest.com/pin/1129699887810118678/sent/?x=1"))
        assertNotNull(PinterestUrl.normalize("https://pin.it/12W799pul"))
    }

    @Test fun rejectsNonPinterestLinks() {
        assertNull(PinterestUrl.normalize("https://example.com/pin/1129699887810118678"))
        assertNull(PinterestUrl.normalize("http://www.pinterest.com/pin/1129699887810118678"))
    }

    @Test fun extractsPinIdFromFullAndSentLinks() {
        assertEquals(
            "1129699887810118678",
            PinterestUrl.extractPinId("https://www.pinterest.com/pin/1129699887810118678/sent/?invite_code=x")
        )
        assertEquals(
            "1129699887810118678",
            PinterestUrl.extractPinId("https://www.pinterest.com/pin/some-slug/1129699887810118678/")
        )
    }
}
