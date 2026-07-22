package com.asianmobile.privatebrower.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import kotlinx.coroutines.delay

/**
 * Reusable dark-theme search bar — synced with Figma design (11009:61).
 * Specs: bg #101010, border 1px #333333, pill shape (rounded-full), padding 12dp
 * Leading icon: 28×28 (Google logo / search engine icon)
 * Text: Inter regular, 14sp → 11ssp, white
 * Placeholder: #808080
 * Clear button: 20dp X icon, appears when text is not empty
 *
 * Note: BasicTextField is NOT focusable during initial composition (300ms delay).
 * This prevents auto-focus on low-API devices (API 27) when:
 * - The tab is pre-composed via HorizontalPager's beyondViewportPageCount
 * - Navigating from another screen that had a focused text field
 * User must tap to focus after the delay.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_placeholder),
    leadingIconRes: Int = R.drawable.ic_tab_search,
    isLightTheme: Boolean = false,
    hasBorder: Boolean = false,
    showMic: Boolean = false,
    onMicClick: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    var canFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }

    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        canFocus = true
    }

    val backgroundColor = if (isLightTheme) Color.White else colorResource(R.color.colors_212327)
    val textColor = if (isLightTheme) colorResource(R.color.black_0D0D0D) else Color.White
    val placeholderColor = if (isLightTheme) colorResource(R.color.colors_B3B3B3) else Color.White
    val cornerShape = if (isLightTheme) {
        RoundedCornerShape(dimensionResource(com.intuit.sdp.R.dimen._100sdp))
    } else {
        RoundedCornerShape(percent = 50) // Pill shape
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(com.intuit.sdp.R.dimen._43sdp))
            .then(
                if (isLightTheme) {
                    Modifier.shadow(
                        elevation = 24.dp,
                        shape = cornerShape,
                        ambientColor = Color(0x40666666),
                        spotColor = Color(0x40666666)
                    )
                } else Modifier
            )
            .clip(cornerShape)
            .background(backgroundColor)
            .then(
                if (!isLightTheme || hasBorder) {
                    val borderColor = if (isLightTheme) {
                        colorResource(R.color.colors_E6E6E6)
                    } else if (isFocused || query.isNotEmpty()) {
                        colorResource(R.color.colors_C8C8C9)
                    } else {
                        colorResource(R.color.colors_333538)
                    }
                    Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = cornerShape
                    )
                } else Modifier
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (canFocus) focusRequester.requestFocus()
            }
            .padding(horizontal = dimensionResource(com.intuit.sdp.R.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(com.intuit.sdp.R.dimen._9sdp))
    ) {
        // Leading Icon (No tint if it's a multi-color search engine icon)
        Icon(
            painter = painterResource(leadingIconRes),
            contentDescription = null,
            tint = if (leadingIconRes == R.drawable.ic_tab_search) {
                if (isLightTheme) colorResource(R.color.colors_005DFD) else placeholderColor
            } else Color.Unspecified,
            modifier = Modifier.size(dimensionResource(com.intuit.sdp.R.dimen._22sdp))
        )

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    color = placeholderColor,
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(com.intuit.ssp.R.dimen._15ssp).value.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { value ->
                    textFieldValue = value
                    onQueryChanged(value.text)
                },
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = dimensionResource(com.intuit.ssp.R.dimen._11ssp).value.sp,
                    lineHeight = dimensionResource(com.intuit.ssp.R.dimen._15ssp).value.sp,
                    fontFamily = FontFamily(Font(R.font.inter_regular))
                ),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusProperties { this.canFocus = canFocus }
                    .onFocusChanged { focusState -> isFocused = focusState.isFocused }
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.ic_search_clear),
                contentDescription = stringResource(R.string.search_clear_content_description),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(dimensionResource(com.intuit.sdp.R.dimen._15sdp))
                    .clickable {
                        textFieldValue = TextFieldValue(
                            text = "",
                            selection = TextRange.Zero
                        )
                        onQueryChanged("")
                    }
            )
        } else if (showMic) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.search_voice_content_description),
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(dimensionResource(com.intuit.sdp.R.dimen._18sdp))
                    .clip(CircleShape)
                    .clickable(onClick = onMicClick)
            )
        }
    }
}
