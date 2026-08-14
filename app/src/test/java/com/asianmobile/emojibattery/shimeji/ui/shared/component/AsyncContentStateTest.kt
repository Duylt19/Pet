package com.asianmobile.emojibattery.shimeji.ui.shared.component

import org.junit.Assert.assertEquals
import org.junit.Test

class AsyncContentStateTest {
    @Test
    fun `cached content stays visible while refresh fails`() {
        assertEquals(
            AsyncContentState.CONTENT,
            resolveAsyncContentState(isLoading = false, hasError = true, isEmpty = false)
        )
    }

    @Test
    fun `empty region shows loading before an error`() {
        assertEquals(
            AsyncContentState.LOADING,
            resolveAsyncContentState(isLoading = true, hasError = true, isEmpty = true)
        )
    }

    @Test
    fun `empty failed region exposes retry state`() {
        assertEquals(
            AsyncContentState.LOAD_FAILED,
            resolveAsyncContentState(isLoading = false, hasError = true, isEmpty = true)
        )
    }

    @Test
    fun `successful empty region is not reported as network failure`() {
        assertEquals(
            AsyncContentState.EMPTY,
            resolveAsyncContentState(isLoading = false, hasError = false, isEmpty = true)
        )
    }
}
