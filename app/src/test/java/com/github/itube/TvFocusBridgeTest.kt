package com.github.itube

import androidx.compose.ui.focus.FocusRequester
import com.github.itube.ui.tv.TvFocusBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TvFocusBridgeTest {

    @Test fun skipsDetachedCandidatesAndOrdersByPriority() {
        val bridge = TvFocusBridge()
        bridge.activeRoute = "tab:Home"

        val detachedTop = FocusRequester()
        val lowerPriority = FocusRequester()
        val attachedHead = FocusRequester()

        bridge.registerEntry(
            route = "tab:Home",
            requester = detachedTop,
            centeredEmptyState = false,
            priority = 0,
            key = "top-offscreen",
            isAttached = { false },
        )
        bridge.registerEntry(
            route = "tab:Home",
            requester = lowerPriority,
            centeredEmptyState = false,
            priority = 4,
            key = "card-4",
            isAttached = { true },
        )
        bridge.registerEntry(
            route = "tab:Home",
            requester = attachedHead,
            centeredEmptyState = false,
            priority = 1,
            key = "card-1",
            isAttached = { true },
        )

        assertSame(attachedHead, bridge.currentEntryRequester())
        assertSame(attachedHead, bridge.exitToContentRequester())
    }

    @Test fun exitToContentReturnsNullWhenOnlyScrolledDeepItemsExist() {
        val bridge = TvFocusBridge()
        bridge.activeRoute = "tab:Home"

        val scrolledItem = FocusRequester()
        val headerItem = FocusRequester()

        bridge.registerHeader("tab:Home", headerItem, isAttached = { true })
        bridge.registerEntry(
            route = "tab:Home",
            requester = scrolledItem,
            centeredEmptyState = false,
            priority = 24,
            key = "deep-item",
            isAttached = { true },
        )

        // Deep scrolled rows let Compose geometric 2D focus search pick the closest card on screen
        // rather than jumping all the way back up to a single pinned top requester.
        assertSame(scrolledItem, bridge.currentEntryRequester())
        assertNull(bridge.exitToContentRequester())
    }

    @Test fun exitToContentFallsBackToHeaderWhenNoContentAttached() {
        val bridge = TvFocusBridge()
        bridge.activeRoute = "tab:Subscriptions"

        val detachedContent = FocusRequester()
        val header = FocusRequester()

        bridge.registerHeader("tab:Subscriptions", header, isAttached = { true })
        bridge.registerEntry(
            route = "tab:Subscriptions",
            requester = detachedContent,
            centeredEmptyState = false,
            priority = 0,
            key = "sub-0",
            isAttached = { false },
        )

        assertNull(bridge.currentEntryRequester())
        assertSame(header, bridge.exitToContentRequester())
    }

    @Test fun centeredEmptyStateFlagTracksHighestPriorityAttachedCandidate() {
        val bridge = TvFocusBridge()
        bridge.activeRoute = "tab:Library"

        val emptyReq = FocusRequester()
        bridge.registerEntry(
            route = "tab:Library",
            requester = emptyReq,
            centeredEmptyState = true,
            priority = 0,
            key = "empty",
            isAttached = { true },
        )

        assertTrue(bridge.isActiveCenteredEmptyState)
        assertSame(emptyReq, bridge.exitToContentRequester())

        bridge.unregisterEntry("tab:Library", emptyReq, "empty")
        assertFalse(bridge.isActiveCenteredEmptyState)
        assertNull(bridge.currentEntryRequester())
        assertEquals(null, bridge.currentHeaderRequester())
    }
}
