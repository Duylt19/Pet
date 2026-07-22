package com.asianmobile.privatebrower.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/25/2026
 *
 * Thành phần hiển thị văn bản với hiệu ứng nhảy từng chữ một.
 * Đã được tối ưu hiệu năng bằng cách sử dụng GraphicsLayer và Animatable,
 * tránh Recomposition cực đoan của AnimatedVisibility giúp animation mượt mà hơn.
 */
@Composable
fun JumpAnimatedText(
    modifier: Modifier = Modifier,
    state: AnimatedTextState? = null,
    text: String,
    style: TextStyle? = null,
    textAlign: TextAlign = TextAlign.Start,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
    intermediateDuration: Duration = 80.milliseconds,
    animateOnMount: Boolean = true
) {
    val currentStyle = style ?: LocalTextStyle.current
    val currentState = state ?: rememberAnimatedTextState()

    // Lưu trữ thông tin tọa độ và chiều cao dòng để render chính xác
    val charOffsets =
        remember(text) { mutableStateListOf<Offset>().apply { repeat(text.length) { add(Offset.Zero) } } }
    val lineHeightList =
        remember(text) { mutableStateListOf<Float>().apply { repeat(text.length) { add(0f) } } }
    val animatables = remember(text) { List(text.length) { Animatable(0f) } }

    // Chỉ số các ký tự đang thực hiện animation (để tránh lặp toàn bộ chuỗi trong composition)
    val activeIndices = remember { mutableStateListOf<Int>() }

    // Luồng điều khiển animation chính
    LaunchedEffect(text, currentState) {
        if (animateOnMount) {
            currentState.start()
        }

        snapshotFlow { currentState.isPaused.value to currentState.isStopped.value }
            .collectLatest { (isPaused, isStopped) ->
                if (!isPaused && !isStopped) {
                    currentState.layoutDeferred.await()

                    text.forEachIndexed { index, _ ->
                        if (index <= currentState.settledIndex) {
                            animatables[index].snapTo(1f)
                        } else {
                            launch {
                                activeIndices.add(index)
                                animatables[index].animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio, stiffness)
                                )
                                activeIndices.remove(index)
                                currentState.settledIndex = index
                            }
                            delay(intermediateDuration)
                        }
                    }
                }
            }
    }

    Box(modifier = modifier.clipToBounds()) {
        // 1. Layer Layout (Ẩn): Dùng để xác định layout chuẩn và lấy thông số lineHeight
        Text(
            text = text,
            style = currentStyle.copy(color = Color.Transparent),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = { layoutResult ->
                for (i in text.indices) {
                    val line = layoutResult.getLineForOffset(i)
                    lineHeightList[i] = layoutResult.multiParagraph.getLineHeight(line)
                    charOffsets[i] = layoutResult.getBoundingBox(i).topLeft
                }
                if (!currentState.layoutDeferred.isCompleted) {
                    currentState.layoutDeferred.complete(Unit)
                }
            }
        )

        // 2. Layer Ổn Định: Hiển thị các ký tự đã hoàn thành animation
        val settledTextProgression by remember(text) {
            derivedStateOf {
                if (currentState.settledIndex < 0) ""
                else text.take(currentState.settledIndex + 1)
            }
        }

        if (settledTextProgression.isNotEmpty()) {
            Text(
                text = buildAnnotatedString {
                    append(settledTextProgression)
                    // Giữ layout ổn định bằng cách add phần còn lại nhưng trong suốt
                    val remainingCount = text.length - settledTextProgression.length
                    if (remainingCount > 0) {
                        pushStyle(currentStyle.copy(color = Color.Transparent).toSpanStyle())
                        append(text.substring(settledTextProgression.length))
                        pop()
                    }
                },
                style = currentStyle,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 3. Layer Động: Chỉ render các ký tự đang thực hiện quá trình nhảy
        // 3. Layer Động: Chỉ render các ký tự đang thực hiện quá trình nhảy
        // Tối ưu hóa: Chỉ lặp qua các index đang active để giảm số lượng Composable
        activeIndices.forEach { index ->
            val char = text[index]
            Text(
                text = char.toString(),
                style = currentStyle,
                modifier = Modifier
                    .graphicsLayer {
                        // Cực kỳ quan quan trọng: Đọc giá trị animation bên trong graphicsLayer
                        // giúp tránh Recomposition cho mỗi frame hình.
                        val progress = animatables[index].value
                        translationX = charOffsets[index].x
                        translationY =
                            charOffsets[index].y + (1f - progress) * (lineHeightList[index] * 0.4f)
                        alpha = progress
                    }
            )
        }
    }
}


