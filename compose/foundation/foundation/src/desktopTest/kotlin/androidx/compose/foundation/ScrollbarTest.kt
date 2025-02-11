/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation

import androidx.compose.foundation.gestures.LocalScrollConfig
import androidx.compose.foundation.gestures.ScrollConfig
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MouseInjectionScope
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith


@Suppress("WrapUnaryOperator")
@OptIn(ExperimentalTestApi::class)
@RunWith(Theories::class)
class ScrollbarTest {

    @get:Rule
    val rule = createComposeRule()

    @Theory
    fun `drag slider to the middle`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp)
        }
    }

    @Theory
    fun `drag slider when it is hidden`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag slider to the edges`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag outside slider`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(10f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    fun `drag outside slider and back`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            val scale = 2f  // Content distance to corresponding scrollbar distance
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 10.dp * scale, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            // While thumb is at the top, drag it up and then down the same distance. Content should not move.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, -50f))
                moveBy(Offset(0f, 50f))
                release()
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)

            // While thumb is at the top, drag it up and then down a bit more. Content should move by the diff.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, -50f))
                moveBy(Offset(0f, 51f))
                release()
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-1.dp * scale)

            // Drag thumb exactly to the end. Content should be at the bottom.
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 25f))
                press()
                moveBy(Offset(0f, 50f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp * scale)

            // While thumb is at the bottom, drag it down and then up the same distance. Content should not move
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 75f))
                press()
                moveBy(Offset(0f, 50f))
                moveBy(Offset(0f, -50f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-50.dp * scale)

            // While thumb is at the bottom, drag it down and then up a bit more. Content should move by the diff
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 75f))
                press()
                moveBy(Offset(0f, 50f))
                moveBy(Offset(0f, -51f))
                release()
            }
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-49.dp * scale)

        }
    }

    @Theory
    fun `drag slider with varying size items`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent(scrollbarImpl) {
                LazyTestBox(state = listState, size = 100.dp, scrollbarWidth = 10.dp){
                    item {
                        Box(Modifier.size(20.dp))
                    }
                    item {
                        Box(Modifier.size(180.dp))
                    }
                    item {
                        Box(Modifier.size(20.dp))
                    }
                    item {
                        Box(Modifier.size(180.dp))
                    }
                }
            }
            rule.awaitIdle()


            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 5f))
                press()
            }

            // Scroll all the way down, one pixel at a time. Make sure the content moves "up" every time.
            for (i in 1..100){
                val firstVisibleItemIndexBefore = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetBefore = listState.firstVisibleItemScrollOffset
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    moveBy(Offset(0f, 1f))
                }
                rule.awaitIdle()
                val firstVisibleItemIndexAfter = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetAfter = listState.firstVisibleItemScrollOffset

                if (firstVisibleItemIndexAfter < firstVisibleItemIndexBefore)
                    throw AssertionError(
                        "First visible item index decreased on iteration $i while dragging down; " +
                        "before=$firstVisibleItemIndexBefore, after=$firstVisibleItemIndexAfter"
                    )
                else if ((firstVisibleItemIndexAfter == firstVisibleItemIndexBefore) &&
                    (firstVisibleItemScrollOffsetAfter < firstVisibleItemScrollOffsetBefore))
                    throw AssertionError(
                        "First visible item offset decreased on iteration $i while dragging down; " +
                            "item index=$firstVisibleItemIndexAfter, " +
                            "offset before=$firstVisibleItemScrollOffsetBefore, " +
                            "offset after=$firstVisibleItemScrollOffsetAfter"
                    )
            }

            // Scroll back all the way up, one pixel at a time. Make sure the content moves "down" every time
            for (i in 1..100){
                val firstVisibleItemIndexBefore = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetBefore = listState.firstVisibleItemScrollOffset
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    moveBy(Offset(0f, -1f))
                }
                rule.awaitIdle()
                val firstVisibleItemIndexAfter = listState.firstVisibleItemIndex
                val firstVisibleItemScrollOffsetAfter = listState.firstVisibleItemScrollOffset

                if (firstVisibleItemIndexAfter > firstVisibleItemIndexBefore)
                    throw AssertionError(
                        "First visible item index increased on iteration $i while dragging up; " +
                            "before=$firstVisibleItemIndexBefore, after=$firstVisibleItemIndexAfter"
                    )
                else if ((firstVisibleItemIndexAfter == firstVisibleItemIndexBefore) &&
                    (firstVisibleItemScrollOffsetAfter > firstVisibleItemScrollOffsetBefore))
                    throw AssertionError(
                        "First visible item offset increased on iteration $i while dragging up; " +
                            "item index=$firstVisibleItemIndexAfter, " +
                            "offset before=$firstVisibleItemScrollOffsetBefore, " +
                            "offset after=$firstVisibleItemScrollOffsetAfter"
                    )
            }

            rule.onNodeWithTag("scrollbar").performMouseInput {
                release()
            }
        }
    }

    @Theory
    fun `scroll lazy column to bottom with content padding`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent(scrollbarImpl) {
                LazyTestBox(
                    state = listState,
                    size = 100.dp,
                    childSize = 10.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    contentPadding = PaddingValues(vertical = 25.dp)
                )
            }
            rule.awaitIdle()

            // Drag to the bottom
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 20f), end = Offset(0f, 80f))
            }

            rule.awaitIdle()

            // Note that if the scrolling is incorrect, this can fail not only with a wrong value, but also by not
            // finding the box node, as it may have not scrolled into view.
            // Last box should be at containerSize - bottomPadding - boxSize
            rule.onNodeWithTag("box19").assertTopPositionInRootIsEqualTo(100.dp - 25.dp - 10.dp)
        }
    }

    @Test
    fun `thumb size on scrollbar smaller than viewport`() {
        runBlocking(Dispatchers.Main) {
            val scrollState = ScrollState(0)
            rule.setContent {
                TestBox(
                    scrollState = scrollState,
                    size = 200.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    scrollbarHeight = 100.dp,
                )
            }
            rule.awaitIdle()

            // Thumb should be half the height of the scrollbar, as the viewport (200.dp) is half the height of the
            // content (400.dp). So clicking on the top half of the scrollbar should do nothing.
            for (offset in 1..50){
                // Use moveTo -> press -> awaitIdle -> test -> release because click doesn't appear to work
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    moveTo(position = Offset(0f, offset.toFloat()))
                    press()
                }
                rule.awaitIdle()
                assertEquals(0, scrollState.value)
                rule.onNodeWithTag("scrollbar").performMouseInput {
                    release()
                }
            }

            // Clicking one pixel below the thumb should scroll the content by one viewport
            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(position = Offset(0f, 51f))
                press()
            }
            rule.awaitIdle()
            assertEquals(200, scrollState.value)
            rule.onNodeWithTag("scrollbar").performMouseInput {
                release()
            }
        }
    }

    // See https://github.com/JetBrains/compose-jb/issues/2640
    @Test
    fun `drag scrollbar to bottom with content padding`() {
        runBlocking(Dispatchers.Main) {
            val listState = LazyListState()
            rule.setContent {
                LazyTestBox(
                    state = listState,
                    size = 300.dp,
                    scrollbarWidth = 10.dp,
                    contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp),
                ){
                    val childHeights = listOf(100.dp, 200.dp, 75.dp)
                    items(childHeights.size){ index ->
                        Box(Modifier.size(childHeights[index]))
                    }
                }
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 5f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()

            // Test whether the scrollbar is at the bottom by trying to drag it up by the last pixel.
            // If it's not at the bottom, the drag will not succeed
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 299f), end = Offset(0f, 0f))
            }
            rule.awaitIdle()

            assertEquals(true, listState.canScrollForward)
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.first()
            assertEquals(0, firstVisibleItem.index)
            assertEquals(0, firstVisibleItem.offset)
        }
    }

    // TODO(demin): write a test when we support DesktopComposeTestRule.mainClock:
    //  see https://github.com/JetBrains/compose-jb/issues/637
//    fun `move mouse to the slider and drag it`() {
//        ...
//        rule.performMouseMove(0, 25)
//        rule.mainClock.advanceTimeByFrame()
//        press(Offset(0f, 25f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 30f))
//        rule.mainClock.advanceTimeByFrame()
//        moveTo(Offset(0f, 50f))
//        rule.mainClock.advanceTimeByFrame()
//        release()
//        ...
//    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `mouseScroll over slider`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(0, 25, 1f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
        }
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `mouseScroll over scrollbar outside slider`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(0, 99, 1f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-10.dp)
        }
    }

    // TODO(demin): enable after we resolve b/171889442
    @Ignore("Enable after we resolve b/171889442")
    @Theory
    fun `vertical mouseScroll over horizontal scrollbar `(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            // TODO(demin): write tests for vertical mouse scrolling over
            //  horizontalScrollbar for the case when we have two-way scrollable content:
            //  Modifier.verticalScrollbar(...).horizontalScrollbar(...)
            //  Content should scroll vertically.
        }
    }

    @Theory
    fun `mouseScroll over column then drag to the beginning`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 10, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.performMouseScroll(20, 25, 10f)
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `press on scrollbar outside slider`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 26f))
                press()
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
            }
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `press on the end of scrollbar outside slider`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                TestBox(size = 100.dp, childSize = 20.dp, childCount = 20, scrollbarWidth = 10.dp)
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                moveTo(Offset(0f, 99f))
                press()
            }

            tryUntilSucceeded {
                rule.awaitIdle()
                rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-300.dp)
            }
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("JUnitMalformedDeclaration")
    fun `dynamically change content then drag slider to the end`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            val isContentVisible = mutableStateOf(false)
            rule.setContent(scrollbarImpl) {
                TestBox(
                    size = 100.dp,
                    scrollbarWidth = 10.dp
                ) {
                    if (isContentVisible.value) {
                        repeat(10) {
                            Box(Modifier.size(20.dp).testTag("box$it"))
                        }
                    }
                }
            }
            rule.awaitIdle()

            isContentVisible.value = true
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 500f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(-100.dp)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll by less than one page in lazy list`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarImpl) {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 11f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll in reversed lazy list`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarImpl) {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp,
                    reverseLayout = true
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, 88f))
            }
            rule.awaitIdle()
            assertEquals(2, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll by more than one page in lazy list`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarImpl) {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 26f))
            }
            rule.awaitIdle()
            assertEquals(5, state.firstVisibleItemIndex)
            assertEquals(4, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    @Test(timeout = 3000)
    @Suppress("SameParameterValue", "JUnitMalformedDeclaration")
    fun `scroll outside of scrollbar bounds in lazy list`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            lateinit var state: LazyListState

            rule.setContent(scrollbarImpl) {
                state = rememberLazyListState()
                LazyTestBox(
                    state,
                    size = 100.dp,
                    childSize = 20.dp,
                    childCount = 20,
                    scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 0f), end = Offset(0f, 10000f))
            }
            rule.awaitIdle()
            assertEquals(15, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)

            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 99f), end = Offset(0f, -10000f))
            }
            rule.awaitIdle()
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Theory
    fun `drag lazy slider when it is hidden`(scrollbarImpl: ScrollbarImpl) {
        runBlocking(Dispatchers.Main) {
            rule.setContent(scrollbarImpl) {
                LazyTestBox(
                    size = 100.dp, childSize = 20.dp, childCount = 1, scrollbarWidth = 10.dp
                )
            }
            rule.awaitIdle()
            rule.onNodeWithTag("scrollbar").performMouseInput {
                instantDrag(start = Offset(0f, 25f), end = Offset(0f, 50f))
            }
            rule.awaitIdle()
            rule.onNodeWithTag("box0").assertTopPositionInRootIsEqualTo(0.dp)
        }
    }

    private suspend fun tryUntilSucceeded(block: suspend () -> Unit) {
        while (true) {
            try {
                block()
                break
            } catch (e: Throwable) {
                delay(10)
            }
        }
    }

    @OptIn(InternalTestApi::class, ExperimentalComposeUiApi::class)
    private fun ComposeTestRule.performMouseScroll(x: Int, y: Int, delta: Float) {
        (this as DesktopComposeTestRule).scene.sendPointerEvent(
            PointerEventType.Scroll,
            Offset(x.toFloat(), y.toFloat()),
            scrollDelta = Offset(x = 0f, y = delta),
            nativeEvent = awtWheelEvent()
        )
    }

    @OptIn(ExperimentalComposeUiApi::class, InternalTestApi::class)
    private fun ComposeTestRule.performMouseMove(x: Int, y: Int) {
        (this as DesktopComposeTestRule).scene.sendPointerEvent(
            PointerEventType.Move,
            Offset(x.toFloat(), y.toFloat())
        )
    }

    @Composable
    private fun TestBox(
        scrollState: ScrollState = rememberScrollState(),
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
        scrollbarHeight: Dp = size
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            Column(
                Modifier.fillMaxSize().testTag("column").verticalScroll(scrollState)
            ) {
                repeat(childCount) {
                    Box(Modifier.size(childSize).testTag("box$it"))
                }
            }

            ScrollbarImplLocal.current.VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .height(scrollbarHeight)
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun TestBox(
        size: Dp,
        scrollbarWidth: Dp,
        scrollableContent: @Composable ColumnScope.() -> Unit
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            val state = rememberScrollState()

            Column(
                Modifier.fillMaxSize().testTag("column").verticalScroll(state),
                content = scrollableContent
            )

            ScrollbarImplLocal.current.VerticalScrollbar(
                scrollState = state,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun LazyTestBox(
        state: LazyListState = rememberLazyListState(),
        size: Dp,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
        content: LazyListScope.() -> Unit
    ) = withTestEnvironment {
        Box(Modifier.size(size)) {
            LazyColumn(
                Modifier.fillMaxSize().testTag("column"),
                state,
                contentPadding = contentPadding,
                reverseLayout = reverseLayout,
                content = content
            )

            ScrollbarImplLocal.current.VerticalScrollbar(
                scrollState = state,
                reverseLayout = reverseLayout,
                modifier = Modifier
                    .width(scrollbarWidth)
                    .fillMaxHeight()
                    .testTag("scrollbar")
            )
        }
    }

    @Composable
    private fun LazyTestBox(
        state: LazyListState = rememberLazyListState(),
        size: Dp,
        childSize: Dp,
        childCount: Int,
        scrollbarWidth: Dp,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        reverseLayout: Boolean = false,
    ) = LazyTestBox(
        state = state,
        size = size,
        scrollbarWidth = scrollbarWidth,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
    ) {
        items((0 until childCount).toList()) {
            Box(Modifier.size(childSize).testTag("box$it"))
        }
    }

    private fun MouseInjectionScope.instantDrag(start: Offset, end: Offset) {
        moveTo(start)
        press()
        moveTo(end)
        release()
    }

    @Composable
    private fun withTestEnvironment(content: @Composable () -> Unit) = CompositionLocalProvider(
        LocalScrollbarStyle provides ScrollbarStyle(
            minimalHeight = 16.dp,
            thickness = 8.dp,
            shape = RectangleShape,
            hoverDurationMillis = 300,
            unhoverColor = Color.Black,
            hoverColor = Color.Red
        ),
        LocalScrollConfig provides TestConfig,
        content = content
    )

    companion object{

        // The old and new scrollbar implementations, allowing us to run the same tests on both
        // Tests that should run on both, should:
        // 1. Be marked as `@Theory`
        // 2. Take a ScrollbarImpl argument
        // 3. Set the argument as the ScrollbarImplLocal, typically via
        //    ComposeContentTestRule.setContent(ScrollbarImpl, @Composable () -> Unit)
        // Tests that should only run on the new implementation should just be marked with `@Test` as usual.

        @JvmField
        @DataPoint
        val NewScrollbarImpl: ScrollbarImpl = NewScrollbar

        @JvmField
        @DataPoint
        val OldScrollbarImpl: ScrollbarImpl = OldScrollbar

    }

}

/**
 * Abstracts the implementation of the scrollbar (actually just the adapter) to allow us to test both the new and old
 * adapters.
 */
sealed class ScrollbarImpl {

    // @Composable abstract functions can't have default arguments, so we're forced to delegate to another function
    @Composable
    fun VerticalScrollbar(
        scrollState: ScrollState,
        modifier: Modifier = Modifier,
        reverseLayout: Boolean = false,
        style: ScrollbarStyle = LocalScrollbarStyle.current,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    ) = VerticalScrollbarImpl(
        scrollState = scrollState,
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource
    )

    @Composable
    protected abstract fun VerticalScrollbarImpl(
        scrollState: ScrollState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    )

    @Composable
    fun VerticalScrollbar(
        scrollState: LazyListState,
        modifier: Modifier = Modifier,
        reverseLayout: Boolean = false,
        style: ScrollbarStyle = LocalScrollbarStyle.current,
        interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    ) = VerticalScrollbarImpl(
        scrollState = scrollState,
        modifier = modifier,
        reverseLayout = reverseLayout,
        style = style,
        interactionSource = interactionSource
    )

    @Composable
    protected abstract fun VerticalScrollbarImpl(
        scrollState: LazyListState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    )

}

/**
 * The old scrollbar implementation.
 */
@Suppress("DEPRECATION")
private object OldScrollbar: ScrollbarImpl() {

    // Our old implementation of the old scrollbar adapter interface
    private class OldScrollableScrollbarAdapter(
        private val scrollState: ScrollState
    ) : ScrollbarAdapter {
        override val scrollOffset: Float get() = scrollState.value.toFloat()

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            scrollState.scrollTo(scrollOffset.roundToInt())
        }

        override fun maxScrollOffset(containerSize: Int) =
            scrollState.maxValue.toFloat()
    }

    @Composable
    override fun VerticalScrollbarImpl(
        scrollState: ScrollState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        val oldAdapter = remember(scrollState){
            OldScrollableScrollbarAdapter(scrollState)
        }
        VerticalScrollbar(
            adapter = oldAdapter,
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

    // Our old implementation of the old scrollbar adapter interface
    private class OldLazyScrollbarAdapter(
        private val scrollState: LazyListState
    ) : ScrollbarAdapter {

        override val scrollOffset: Float
            get() = scrollState.firstVisibleItemIndex * averageItemSize +
                scrollState.firstVisibleItemScrollOffset

        override suspend fun scrollTo(containerSize: Int, scrollOffset: Float) {
            val distance = scrollOffset - this@OldLazyScrollbarAdapter.scrollOffset
            if (abs(distance) <= containerSize) {
                scrollState.scrollBy(distance)
            } else {
                snapTo(containerSize, scrollOffset)
            }
        }

        private suspend fun snapTo(containerSize: Int, scrollOffset: Float) {
            val maximumValue = maxScrollOffset(containerSize).toDouble()
            val scrollOffsetCoerced = scrollOffset.toDouble().coerceIn(0.0, maximumValue)
            val averageItemSize = averageItemSize.toDouble()

            val index = (scrollOffsetCoerced / averageItemSize)
                .toInt()
                .coerceAtLeast(0)
                .coerceAtMost(itemCount - 1)

            val offset = (scrollOffsetCoerced - index * averageItemSize)
                .toInt()
                .coerceAtLeast(0)

            scrollState.scrollToItem(index = index, scrollOffset = offset)
        }

        override fun maxScrollOffset(containerSize: Int) =
            (averageItemSize * itemCount
                + scrollState.layoutInfo.beforeContentPadding
                + scrollState.layoutInfo.afterContentPadding
                - containerSize
                ).coerceAtLeast(0f)

        private val itemCount get() = scrollState.layoutInfo.totalItemsCount

        private val averageItemSize by derivedStateOf {
            scrollState
                .layoutInfo
                .visibleItemsInfo
                .asSequence()
                .map { it.size }
                .average()
                .toFloat()
        }

    }

    @Composable
    override fun VerticalScrollbarImpl(
        scrollState: LazyListState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        val oldAdapter = remember(scrollState){
            OldLazyScrollbarAdapter(scrollState)
        }
        VerticalScrollbar(
            adapter = oldAdapter,
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

}

/**
 * The new scrollbar implementation
 */
private object NewScrollbar: ScrollbarImpl() {

    @Composable
    override fun VerticalScrollbarImpl(
        scrollState: ScrollState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

    @Composable
    override fun VerticalScrollbarImpl(
        scrollState: LazyListState,
        modifier: Modifier,
        reverseLayout: Boolean,
        style: ScrollbarStyle,
        interactionSource: MutableInteractionSource
    ) {
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = modifier,
            reverseLayout = reverseLayout,
            style = style,
            interactionSource = interactionSource
        )
    }

}

private val ScrollbarImplLocal = compositionLocalOf<ScrollbarImpl>{ NewScrollbar }

private fun ComposeContentTestRule.setContent(scrollbarImpl: ScrollbarImpl, composable: @Composable () -> Unit){
    setContent {
        CompositionLocalProvider(ScrollbarImplLocal provides scrollbarImpl){
            composable()
        }
    }
}

internal object TestConfig : ScrollConfig {
    // the formula was determined experimentally based on MacOS Finder behaviour
    // MacOS driver will send events with accelerating delta
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        return -event.totalScrollDelta * 10.dp.toPx()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private val PointerEvent.totalScrollDelta
    get() = this.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta }