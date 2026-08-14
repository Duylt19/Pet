package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

/** Visual state for an asynchronously-backed content region. */
enum class AsyncContentState {
    LOADING,
    LOAD_FAILED,
    EMPTY,
    CONTENT
}

/**
 * Existing or cached content always wins over a refresh failure. This keeps a transient network
 * issue from replacing data that is still usable on screen.
 */
fun resolveAsyncContentState(
    isLoading: Boolean,
    hasError: Boolean,
    isEmpty: Boolean
): AsyncContentState = when {
    !isEmpty -> AsyncContentState.CONTENT
    isLoading -> AsyncContentState.LOADING
    hasError -> AsyncContentState.LOAD_FAILED
    else -> AsyncContentState.EMPTY
}

private val AsyncStateFont = FontFamily(Font(R.font.roboto_medium))

@Composable
fun AsyncContentStatePanel(
    state: AsyncContentState,
    modifier: Modifier = Modifier,
    minHeight: Dp = dimensionResource(SdpR.dimen._115sdp),
    @StringRes emptyMessageRes: Int = R.string.data_empty_message,
    onRetry: (() -> Unit)? = null
) {
    if (state == AsyncContentState.CONTENT) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(horizontal = dimensionResource(SdpR.dimen._18sdp)),
        contentAlignment = Alignment.Center
    ) {
        if (state == AsyncContentState.LOADING) {
            CircularProgressIndicator(
                color = colorResource(R.color.colors_FB3675),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._24sdp))
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(
                        if (state == AsyncContentState.LOAD_FAILED) {
                            R.string.data_load_failed_title
                        } else {
                            emptyMessageRes
                        }
                    ),
                    color = colorResource(R.color.colors_212327),
                    fontFamily = AsyncStateFont,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    textAlign = TextAlign.Center
                )
                if (state == AsyncContentState.LOAD_FAILED) {
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._3sdp)))
                    Text(
                        text = stringResource(R.string.data_load_failed_message),
                        color = colorResource(R.color.colors_6F7073),
                        fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                        textAlign = TextAlign.Center
                    )
                    onRetry?.let { retry ->
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
                        Text(
                            text = stringResource(R.string.data_retry),
                            color = colorResource(R.color.colors_FB3675),
                            fontFamily = AsyncStateFont,
                            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                                .clickable(onClick = retry)
                                .padding(
                                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                                    vertical = dimensionResource(SdpR.dimen._4sdp)
                                )
                        )
                    }
                }
            }
        }
    }
}
