/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.platform.SkiaRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.MainTestClockImpl
import androidx.compose.ui.test.junit4.UncaughtExceptionHandler
import androidx.compose.ui.test.junit4.isOnUiThread
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.currentNanoTime

@ExperimentalTestApi
actual fun runComposeUiTest(block: ComposeUiTest.() -> Unit) {
    SkikoComposeUiTest().runTest(block)
}

@ExperimentalTestApi
fun runSkikoComposeUiTest(
    size: Size = Size(1024.0f, 768.0f),
    block: SkikoComposeUiTest.() -> Unit
) {
    SkikoComposeUiTest(
        width = size.width.roundToInt(),
        height = size.height.roundToInt()
    ).runTest(block)
}

@ExperimentalTestApi
@OptIn(ExperimentalCoroutinesApi::class, InternalTestApi::class)
class SkikoComposeUiTest(
    width: Int = 1024,
    height: Int = 768
) : ComposeUiTest {

    override val density = Density(1f, 1f)

    private val textInputService = object : PlatformTextInputService {
        var onEditCommand: ((List<EditCommand>) -> Unit)? = null
        var onImeActionPerformed: ((ImeAction) -> Unit)? = null

        override fun startInput(
            value: TextFieldValue,
            imeOptions: ImeOptions,
            onEditCommand: (List<EditCommand>) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ) {
            this.onEditCommand = onEditCommand
            this.onImeActionPerformed = onImeActionPerformed
        }

        override fun stopInput() {
            this.onEditCommand = null
            this.onImeActionPerformed = null
        }

        override fun showSoftwareKeyboard() = Unit
        override fun hideSoftwareKeyboard() = Unit
        override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = Unit
    }

    private val coroutineDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(coroutineDispatcher)
    override val mainClock: MainTestClock =
        MainTestClockImpl(coroutineDispatcher.scheduler, frameDelayMillis = 16L)
    private val uncaughtExceptionHandler = UncaughtExceptionHandler()
    private val infiniteAnimationPolicy = object : InfiniteAnimationPolicy {
        override suspend fun <R> onInfiniteOperation(block: suspend () -> R): R {
            if (mainClock.autoAdvance) {
                throw CancellationException("Infinite animations are disabled on tests")
            }
            return block()
        }
    }
    private val coroutineContext =
        coroutineDispatcher + uncaughtExceptionHandler + infiniteAnimationPolicy
    private val surface = Surface.makeRasterN32Premul(width, height)

    lateinit var scene: ComposeScene
        internal set

    private val testOwner = DesktopTestOwner()
    private val testContext = createTestContext(testOwner)

    fun <R> runTest(block: SkikoComposeUiTest.() -> R): R {
        scene = runOnUiThread(::createUi)
        try {
            return block()
        } finally {
            // call runTest instead of deprecated cleanupTestCoroutines()
            testScope.runTest { }
            runOnUiThread(scene::close)
            uncaughtExceptionHandler.throwUncaught()
        }
    }

    private fun renderNextFrame() = runOnUiThread {
        scene.render(
            surface.canvas,
            mainClock.currentTime * 1_000_000
        )
        if (mainClock.autoAdvance) {
            mainClock.advanceTimeByFrame()
        }
    }

    private fun createUi() = ComposeScene(
        textInputService = textInputService,
        density = density,
        coroutineContext = coroutineContext,
        invalidate = { }
    ).apply {
        constraints = Constraints(maxWidth = surface.width, maxHeight = surface.height)
    }

    private fun shouldPumpTime(): Boolean {
        return mainClock.autoAdvance &&
            (Snapshot.current.hasPendingChanges() || scene.hasInvalidations())
    }

    @OptIn(InternalComposeUiApi::class)
    private fun isIdle(): Boolean {
        var i = 0
        while (i < 100 && shouldPumpTime()) {
            mainClock.advanceTimeByFrame()
            ++i
        }

        val hasPendingMeasureOrLayout = testOwner.getRoots(true).any {
            (it as SkiaRootForTest).hasPendingMeasureOrLayout
        }

        return !shouldPumpTime() && !hasPendingMeasureOrLayout
    }

    override fun waitForIdle() {
        // TODO: consider adding a timeout to avoid an infinite loop?
        do {
            // always check even if we are idle
            uncaughtExceptionHandler.throwUncaught()
            renderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
        } while (!isIdle())
    }

    override suspend fun awaitIdle() {
        // always check even if we are idle
        uncaughtExceptionHandler.throwUncaught()
        while (!isIdle()) {
            renderNextFrame()
            uncaughtExceptionHandler.throwUncaught()
            yield()
        }
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return androidx.compose.ui.test.junit4.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        // We are waiting for idle before and AFTER `action` to guarantee that changes introduced
        // in `action` are propagated to components. In Android's version, it's executed in the
        // Main thread which has similar effects.
        waitForIdle()
        return action().also { waitForIdle() }
    }

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = currentNanoTime()
        val timeoutNanos = timeoutMillis * NanoSecondsPerMilliSecond
        while (!condition()) {
            renderNextFrame()
            if (currentNanoTime() - startTime > timeoutNanos) {
                throw ComposeTimeoutException(
                    "Condition still not satisfied after $timeoutMillis ms"
                )
            }
        }
    }

    override fun registerIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun unregisterIdlingResource(idlingResource: IdlingResource) {
        // TODO: implement
    }

    override fun setContent(composable: @Composable () -> Unit) {
        if (isOnUiThread()) {
            scene.setContent(content = composable)
        } else {
            runOnUiThread {
                scene.setContent(content = composable)
            }

            // Only wait for idleness if not on the UI thread. If we are on the UI thread, the
            // caller clearly wants to keep tight control over execution order, so don't go
            // executing future tasks on the main thread.
            waitForIdle()
        }
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
    }

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
    }

    fun captureToImage(): ImageBitmap {
        waitForIdle()
        return surface.makeImageSnapshot().toComposeImageBitmap()
    }

    fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
        waitForIdle()
        val rect = fetchSemanticsNode().boundsInWindow
        val iRect = IRect.makeLTRB(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
        val image = surface.makeImageSnapshot(iRect)
        return image!!.toComposeImageBitmap()
    }

    private inner class DesktopTestOwner : TestOwner {
        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditCommand>) {
            runOnIdle {
                val onEditCommand = textInputService.onEditCommand
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onEditCommand(command)
            }
        }

        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            runOnIdle {
                val onImeActionPerformed = textInputService.onImeActionPerformed
                    ?: throw IllegalStateException("No input session started. Missing a focus?")
                onImeActionPerformed.invoke(actionSpecified)
            }
        }

        override fun <T> runOnUiThread(action: () -> T): T {
            return this@SkikoComposeUiTest.runOnUiThread(action)
        }

        override fun getRoots(atLeastOneRootExpected: Boolean): Set<RootForTest> {
            return this@SkikoComposeUiTest.scene.roots
        }

        override val mainClock get() =
            this@SkikoComposeUiTest.mainClock
    }
}

@ExperimentalTestApi
actual sealed interface ComposeUiTest : SemanticsNodeInteractionsProvider {
    actual val density: Density
    actual val mainClock: MainTestClock
    actual fun <T> runOnUiThread(action: () -> T): T
    actual fun <T> runOnIdle(action: () -> T): T
    actual fun waitForIdle()
    actual suspend fun awaitIdle()
    actual fun waitUntil(timeoutMillis: Long, condition: () -> Boolean)
    actual fun registerIdlingResource(idlingResource: IdlingResource)
    actual fun unregisterIdlingResource(idlingResource: IdlingResource)
    actual fun setContent(composable: @Composable () -> Unit)
}
