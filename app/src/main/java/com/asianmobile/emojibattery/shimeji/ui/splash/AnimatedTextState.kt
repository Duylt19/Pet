package com.asianmobile.emojibattery.shimeji.ui.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/25/2026
 *
 * Quản lý trạng thái cho hiệu ứng chữ nhảy (Jump Animation).
 * Tối ưu hiệu năng bằng cách loại bỏ GlobalScope và giảm thiểu recomposition.
 */
class AnimatedTextState {
    /** Vị trí ký tự đã ổn định cuối cùng (đã hoàn thành animation) */
    var settledIndex by mutableIntStateOf(-1)
        internal set

    /** Đánh dấu khi text layout đã được tính toán xong */
    internal val layoutDeferred = CompletableDeferred<Unit>()

    private val _isStopped = MutableStateFlow(true)
    val isStopped: StateFlow<Boolean> = _isStopped.asStateFlow()

    private val _isPaused = MutableStateFlow(true)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    /** Bắt đầu animation từ đầu */
    fun start() {
        settledIndex = -1
        _isStopped.update { false }
        _isPaused.update { false }
    }

    /** Dừng hẳn animation */
    fun stop() {
        _isStopped.update { true }
        _isPaused.update { true }
    }

    /** Tạm dừng animation tại vị trí hiện tại */
    fun pause() {
        _isPaused.update { true }
    }

    /** Tiếp tục animation từ vị trí tạm dừng */
    fun resume() {
        _isPaused.update { false }
    }

    /** Cleanup khi không sử dụng nữa */
    fun release() {
        stop()
    }
}

/**
 * Tạo một [AnimatedTextState] được ghi nhớ qua các lần recomposition.
 */
@Composable
fun rememberAnimatedTextState() = remember { AnimatedTextState() }


