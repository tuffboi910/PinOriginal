package com.pinoriginal.app

import com.pinoriginal.app.data.ImageSource
import com.pinoriginal.app.data.PinImageCandidate
import com.pinoriginal.app.net.CandidateRanker
import org.junit.Assert.assertEquals
import org.junit.Test

class CandidateRankerTest {
    @Test fun choosesLargestPixelAreaFirst() {
        val smallOriginal = PinImageCandidate("https://i.pinimg.com/originals/a/b/c/x.jpg", 1000, 1000, "image/jpeg", 5000, ImageSource.VerifiedOriginalCandidate, true)
        val largeVariant = PinImageCandidate("https://i.pinimg.com/1200x/a/b/c/y.jpg", 1600, 1200, "image/jpeg", 4000, ImageSource.PinterestVariant, true)

        assertEquals(largeVariant, CandidateRanker.best(listOf(smallOriginal, largeVariant)))
    }

    @Test fun prefersVerifiedOriginalWhenAreaTies() {
        val official = PinImageCandidate("https://i.pinimg.com/1200x/a/b/c/x.jpg", 1200, 1600, "image/jpeg", 9000, ImageSource.PinterestVariant, true)
        val original = PinImageCandidate("https://i.pinimg.com/originals/a/b/c/x.jpg", 1200, 1600, "image/jpeg", 8000, ImageSource.VerifiedOriginalCandidate, true)

        assertEquals(original, CandidateRanker.best(listOf(official, original)))
    }
}
