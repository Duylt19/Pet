package com.asianmobile.emojibattery.shimeji.ui.home.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.pet.speech.PetMessageListPolicy
import com.asianmobile.emojibattery.shimeji.ui.component.DismissibleDialogBackdrop
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
internal fun PetMessageEditorDialog(
    initialMessages: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initialMessages) {
        mutableStateOf(initialMessages.joinToString("\n"))
    }
    val draftMessages = draft.lineSequence().filter(String::isNotBlank).toList()
    val isWithinCountLimit = draftMessages.size <= PetMessageListPolicy.MAX_CUSTOM_MESSAGES
    val isWithinLengthLimit = draftMessages.all { message ->
        message.codePointCount(0, message.length) <=
            PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS
    }
    val longestMessageLength = draftMessages.maxOfOrNull { message ->
        message.codePointCount(0, message.length)
    } ?: 0
    val isValid = isWithinCountLimit && isWithinLengthLimit
    val fontRegular = FontFamily(Font(R.font.inter_regular))
    val fontMedium = FontFamily(Font(R.font.inter_medium))
    val fontSemiBold = FontFamily(Font(R.font.inter_semibold))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWidth = LocalConfiguration.current.screenWidthDp * 0.9f
        DismissibleDialogBackdrop(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .width(dialogWidth.dp)
                    .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._16sdp)))
                    .background(colorResource(R.color.colors_FFFFFB))
                    .padding(dimensionResource(SdpR.dimen._14sdp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_pet_message_editor_title),
                        color = colorResource(R.color.colors_2F2440),
                        fontFamily = fontSemiBold,
                        fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                        modifier = Modifier.weight(1f)
                    )
                    MessageEditorAction(
                        text = stringResource(R.string.settings_pet_message_editor_save),
                        fontFamily = fontMedium,
                        enabled = isValid,
                        isPrimary = true,
                        onClick = { onSave(draft.lines()) }
                    )
                    Spacer(Modifier.width(dimensionResource(SdpR.dimen._8sdp)))
                    Icon(
                        painter = painterResource(R.drawable.ic_close_x),
                        contentDescription = stringResource(
                            R.string.settings_pet_message_editor_close
                        ),
                        tint = colorResource(R.color.colors_2F2440),
                        modifier = Modifier
                            .size(dimensionResource(SdpR.dimen._20sdp))
                            .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._6sdp)))
                            .clickable(onClick = onDismiss)
                            .padding(dimensionResource(SdpR.dimen._3sdp))
                    )
                }
                Text(
                    text = stringResource(
                        R.string.settings_pet_message_editor_description,
                        PetMessageListPolicy.MAX_CUSTOM_MESSAGES,
                        PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS
                    ),
                    color = colorResource(R.color.colors_776D84),
                    fontFamily = fontRegular,
                    fontSize = dimensionResource(SspR.dimen._9ssp).value.sp,
                    modifier = Modifier.padding(
                        top = dimensionResource(SdpR.dimen._5sdp),
                        bottom = dimensionResource(SdpR.dimen._10sdp)
                    )
                )

                val editorShape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(
                        color = colorResource(R.color.colors_2F2440),
                        fontFamily = fontRegular,
                        fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                    ),
                    cursorBrush = SolidColor(colorResource(R.color.colors_7B61FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = dimensionResource(SdpR.dimen._150sdp),
                            max = dimensionResource(SdpR.dimen._260sdp)
                        ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = dimensionResource(SdpR.dimen._150sdp),
                                    max = dimensionResource(SdpR.dimen._260sdp)
                                )
                                .clip(editorShape)
                                .background(colorResource(R.color.colors_F7F0FF))
                                .border(
                                    width = dimensionResource(SdpR.dimen._1sdp),
                                    color = colorResource(
                                        if (isValid) {
                                            R.color.colors_E9DFEF
                                        } else {
                                            R.color.colors_E45D6A
                                        }
                                    ),
                                    shape = editorShape
                                )
                                .padding(dimensionResource(SdpR.dimen._10sdp))
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.settings_pet_message_editor_placeholder
                                    ),
                                    color = colorResource(R.color.colors_776D84),
                                    fontFamily = fontRegular,
                                    fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Text(
                    text = if (isValid) {
                        stringResource(
                            R.string.settings_pet_message_editor_counter,
                            draftMessages.size,
                            PetMessageListPolicy.MAX_CUSTOM_MESSAGES,
                            longestMessageLength,
                            PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS
                        )
                    } else {
                        stringResource(
                            R.string.settings_pet_message_editor_limit_error,
                            PetMessageListPolicy.MAX_CUSTOM_MESSAGES,
                            PetMessageListPolicy.MAX_MESSAGE_CODE_POINTS
                        )
                    },
                    color = colorResource(
                        if (isValid) R.color.colors_776D84 else R.color.colors_E45D6A
                    ),
                    fontFamily = fontRegular,
                    fontSize = dimensionResource(SspR.dimen._8ssp).value.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = dimensionResource(SdpR.dimen._4sdp))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(SdpR.dimen._12sdp)),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageEditorAction(
                        text = stringResource(R.string.settings_pet_message_editor_use_builtin),
                        fontFamily = fontMedium,
                        onClick = { onSave(emptyList()) }
                    )
                    Spacer(Modifier.weight(1f))
                    MessageEditorAction(
                        text = stringResource(R.string.common_cancel_label),
                        fontFamily = fontMedium,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageEditorAction(
    text: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (isPrimary && enabled) {
                    colorResource(R.color.colors_7B61FF)
                } else {
                    colorResource(R.color.colors_EDE4FF)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(SdpR.dimen._10sdp),
                vertical = dimensionResource(SdpR.dimen._7sdp)
            )
    ) {
        Text(
            text = text,
            color = colorResource(
                if (isPrimary && enabled) {
                    R.color.colors_FFFFFF
                } else if (enabled) {
                    R.color.colors_5D46D7
                } else {
                    R.color.colors_776D84
                }
            ),
            fontFamily = fontFamily,
            fontSize = dimensionResource(SspR.dimen._9ssp).value.sp
        )
    }
}
