package com.pinoriginal.app

import com.pinoriginal.app.net.PinterestPageParser
import org.junit.Assert.assertEquals
import org.junit.Test

class PinterestPageParserTest {
    @Test fun prefersExplicitOriginalFromJsonLdOverOgResize() {
        val html = """
            <html><head>
              <meta property="og:image" content="https://i.pinimg.com/736x/dd/c8/5e/hash.jpg">
              <meta property="og:image:width" content="736">
              <meta property="og:image:height" content="1308">
              <script type="application/ld+json">
                {"@type":"SocialMediaPosting","image":"https://i.pinimg.com/originals/dd/c8/5e/hash.png"}
              </script>
            </head></html>
        """.trimIndent()

        val group = PinterestPageParser.parse(html).single()

        assertEquals("https://i.pinimg.com/originals/dd/c8/5e/hash.png", group[0].url)
        assertEquals("https://i.pinimg.com/736x/dd/c8/5e/hash.jpg", group[1].url)
        assertEquals(736, group[1].width)
        assertEquals(1308, group[1].height)
    }
}
