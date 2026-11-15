package com.github.itube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortsViewportTest {

    @Test fun centersPortraitShortOn1080pTvWithPillarboxing() {
        val spec = computeShortsViewportSpec(
            containerWidthPx = 1920,
            containerHeightPx = 1080,
            rawVideoAspect = 9f / 16f,
        )
        assertEquals(608, spec.stageWidthPx)
        assertEquals(1080, spec.stageHeightPx)
        assertTrue(spec.isPillarboxed)
        assertEquals(9f / 16f, spec.effectiveAspect, 0.001f)
    }

    @Test fun centersPortraitShortOn720pTvWithoutHorizontalCrop() {
        val spec = computeShortsViewportSpec(
            containerWidthPx = 1280,
            containerHeightPx = 720,
            rawVideoAspect = 9f / 16f,
        )
        assertEquals(405, spec.stageWidthPx)
        assertEquals(720, spec.stageHeightPx)
        assertTrue(spec.isPillarboxed)
    }

    @Test fun portraitPhoneViewportPreservesFullWidthOrTallAspectWithoutPillarbox() {
        val spec = computeShortsViewportSpec(
            containerWidthPx = 1080,
            containerHeightPx = 1920,
            rawVideoAspect = 9f / 16f,
        )
        assertEquals(1080, spec.stageWidthPx)
        assertEquals(1920, spec.stageHeightPx)
        assertFalse(spec.isPillarboxed)

        val tallPhone = computeShortsViewportSpec(
            containerWidthPx = 1080,
            containerHeightPx = 2400,
            rawVideoAspect = 9f / 16f,
        )
        assertEquals(1080, tallPhone.stageWidthPx)
        assertEquals(1920, tallPhone.stageHeightPx)
        assertFalse(tallPhone.isPillarboxed)
    }

    @Test fun invalidOrMissingAspectFallsBackSafelyTo9By16() {
        val zeroAspect = computeShortsViewportSpec(1920, 1080, 0f)
        assertEquals(608, zeroAspect.stageWidthPx)
        assertEquals(1080, zeroAspect.stageHeightPx)
        assertTrue(zeroAspect.isPillarboxed)

        val nanAspect = computeShortsViewportSpec(1920, 1080, Float.NaN)
        assertEquals(608, nanAspect.stageWidthPx)
        assertEquals(1080, nanAspect.stageHeightPx)
        assertTrue(nanAspect.isPillarboxed)

        val emptyBounds = computeShortsViewportSpec(0, 1080, 9f / 16f)
        assertEquals(0, emptyBounds.stageWidthPx)
        assertEquals(0, emptyBounds.stageHeightPx)
    }

    @Test fun landscapeShortDoesNotOverflowContainer() {
        val spec = computeShortsViewportSpec(1920, 1080, 16f / 9f)
        assertEquals(1920, spec.stageWidthPx)
        assertEquals(1080, spec.stageHeightPx)
        assertFalse(spec.isPillarboxed)
    }
}
