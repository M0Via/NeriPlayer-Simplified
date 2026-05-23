package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class KaraokeLyricsViewTest {

    @Test
    fun `resolveFocusedLineScrollMode snaps only on initial layout`() {
        assertEquals(
            FocusedLineScrollMode.Snap,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            FocusedLineScrollMode.Animate,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            FocusedLineScrollMode.Animate,
            resolveFocusedLineScrollMode(previousAutoScrollIndex = 5, targetIndex = 42)
        )
    }

    @Test
    fun `resolveFocusedLinePlacementSuppressionMs keeps jump transitions suppressed longer`() {
        assertEquals(
            80L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            0L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            220L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 5, targetIndex = 42)
        )
        assertEquals(
            0L,
            resolveFocusedLinePlacementSuppressionMs(previousAutoScrollIndex = 6, targetIndex = 6)
        )
    }

    @Test
    fun `shouldSuppressLinePlacementAnimation only disables placement for manual scroll or explicit suppression`() {
        assertEquals(
            true,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = true,
                suppressPlacementAnimation = false
            )
        )
        assertEquals(
            true,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = false,
                suppressPlacementAnimation = true
            )
        )
        assertEquals(
            false,
            shouldSuppressLinePlacementAnimation(
                isManualScrolling = false,
                suppressPlacementAnimation = false
            )
        )
    }

    @Test
    fun `shouldAnimateVisibleFocusedLineScroll only animates large visible seek jumps`() {
        assertEquals(
            false,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = null,
                targetIndex = 6
            )
        )
        assertEquals(
            false,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = 5,
                targetIndex = 6
            )
        )
        assertEquals(
            true,
            shouldAnimateVisibleFocusedLineScroll(
                previousAutoScrollIndex = 5,
                targetIndex = 42
            )
        )
    }

    @Test
    fun `resolveFocusedLineViewportDelta keeps target line anchored to configured viewport offset`() {
        assertEquals(
            0f,
            resolveFocusedLineViewportDelta(
                itemOffset = 420,
                viewportStartOffset = 0,
                stableOffsetPx = 300,
                keepAliveZonePx = 120f
            ),
            0.001f
        )
        assertEquals(
            24f,
            resolveFocusedLineViewportDelta(
                itemOffset = 444,
                viewportStartOffset = 0,
                stableOffsetPx = 300,
                keepAliveZonePx = 120f
            ),
            0.001f
        )
    }

    @Test
    fun `shouldRealignFocusedLineAfterLayout skips adjacent line transitions`() {
        assertEquals(
            true,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = null, targetIndex = 6)
        )
        assertEquals(
            false,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 5, targetIndex = 6)
        )
        assertEquals(
            true,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 5, targetIndex = 42)
        )
        assertEquals(
            false,
            shouldRealignFocusedLineAfterLayout(previousAutoScrollIndex = 6, targetIndex = 6)
        )
    }
}
