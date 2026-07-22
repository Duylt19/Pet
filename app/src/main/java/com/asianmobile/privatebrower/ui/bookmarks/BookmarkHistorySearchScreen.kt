package com.asianmobile.privatebrower.ui.bookmarks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.asianmobile.privatebrower.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import kotlinx.coroutines.delay

@Composable
internal fun BookmarkHistorySearchScreen(
    query: String,
    onQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    @StringRes voicePromptRes: Int,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let(onQueryChanged)
        }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun dismissSearch() {
        keyboardController?.hide()
        onBack()
    }

    BackHandler(onBack = ::dismissSearch)

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.colors_161718))
            .statusBarsPadding()
    ) {
        BookmarkHistorySearchHeader(
            query = query,
            onQueryChanged = onQueryChanged,
            onBack = ::dismissSearch,
            onMicClick = {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.bookmarks_voice_search_unavailable),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@BookmarkHistorySearchHeader
                }

                keyboardController?.hide()
                runCatching {
                    voiceSearchLauncher.launch(
                        createBookmarkHistoryVoiceSearchIntent(
                            context = context,
                            prompt = context.getString(voicePromptRes)
                        )
                    )
                }.onFailure {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                    Toast.makeText(
                        context,
                        context.getString(R.string.bookmarks_voice_search_unavailable),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            focusRequester = focusRequester
        )

        Text(
            text = if (query.isBlank()) {
                stringResource(R.string.bookmarks_search_frequently_visited)
            } else {
                stringResource(R.string.bookmarks_search_results)
            },
            color = colorResource(R.color.colors_9B9C9E),
            fontFamily = FontFamily(Font(R.font.inter_medium)),
            fontWeight = FontWeight.Medium,
            fontSize = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._11ssp).toSp()
            },
            lineHeight = with(LocalDensity.current) {
                dimensionResource(SspR.dimen._15ssp).toSp()
            },
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._6sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._6sdp)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }
    }
}

@Composable
private fun BookmarkHistorySearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onMicClick: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._46sdp))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._12sdp),
                vertical = dimensionResource(SdpR.dimen._6sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = colorResource(R.color.colors_FFFFFF),
                modifier = Modifier.size(dimensionResource(SdpR.dimen._22sdp))
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._12sdp)))

        SearchInput(
            query = query,
            onQueryChanged = onQueryChanged,
            onMicClick = onMicClick,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChanged: (String) -> Unit,
    onMicClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }

    LaunchedEffect(query) {
        if (fieldValue.text != query) {
            fieldValue = TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    Row(
        modifier = modifier
            .height(dimensionResource(SdpR.dimen._34sdp))
            .border(
                width = dimensionResource(SdpR.dimen._1sdp),
                color = if (query.isBlank()) {
                    colorResource(R.color.colors_333538)
                } else {
                    colorResource(R.color.colors_FFFFFF)
                },
                shape = CircleShape
            )
            .background(
                color = colorResource(R.color.colors_212327),
                shape = CircleShape
            )
            .padding(horizontal = dimensionResource(SdpR.dimen._9sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (fieldValue.text.isEmpty()) {
                Text(
                    text = stringResource(R.string.bookmarks_search_field_placeholder),
                    color = colorResource(R.color.colors_6F7073),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._15ssp).toSp()
                    },
                    letterSpacing = 0.sp
                )
            }

            BasicTextField(
                value = fieldValue,
                onValueChange = { value ->
                    fieldValue = value
                    onQueryChanged(value.text)
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colorResource(R.color.colors_FFFFFF),
                    fontFamily = FontFamily(Font(R.font.inter_regular)),
                    fontSize = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._11ssp).toSp()
                    },
                    lineHeight = with(LocalDensity.current) {
                        dimensionResource(SspR.dimen._15ssp).toSp()
                    },
                    letterSpacing = 0.sp
                ),
                cursorBrush = SolidColor(colorResource(R.color.colors_3369FD)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))

        Box(
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._18sdp))
                .clickable {
                    if (query.isBlank()) {
                        onMicClick()
                    } else {
                        fieldValue = TextFieldValue(
                            text = "",
                            selection = TextRange.Zero
                        )
                        onQueryChanged("")
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (query.isBlank()) {
                        R.drawable.ic_mic
                    } else {
                        R.drawable.ic_search_clear
                    }
                ),
                contentDescription = stringResource(
                    if (query.isBlank()) {
                        R.string.search_voice_content_description
                    } else {
                        R.string.search_clear_content_description
                    }
                ),
                tint = Color.Unspecified,
                modifier = Modifier.size(
                    dimensionResource(
                        if (query.isBlank()) {
                            SdpR.dimen._18sdp
                        } else {
                            SdpR.dimen._15sdp
                        }
                    )
                )
            )
        }
    }
}

private fun createBookmarkHistoryVoiceSearchIntent(
    context: Context,
    prompt: String
): Intent {
    val languagePreferences = context.getSharedPreferences(
        "language_cache",
        Context.MODE_PRIVATE
    )
    val language = languagePreferences.getString("key_language", "en") ?: "en"
    val country = languagePreferences.getString("country_language", "US") ?: "US"
    val localeTag = "$language-$country"

    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
}
