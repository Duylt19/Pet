package com.asianmobile.emojibattery.shimeji.ui.battery.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.asianmobile.emojibattery.shimeji.R
import com.asianmobile.emojibattery.shimeji.data.model.BUNDLED_BATTERY_EMOTION_GROUPS
import com.asianmobile.emojibattery.shimeji.data.model.BatteryDecorationEntry
import com.asianmobile.emojibattery.shimeji.data.model.BatteryEmotionGroup
import com.asianmobile.emojibattery.shimeji.data.model.BatteryStatusConfig
import com.asianmobile.emojibattery.shimeji.ui.shared.component.HomePremiumButton
import com.asianmobile.emojibattery.shimeji.ui.shared.theme.RobotoFontFamily
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR

@Composable
internal fun BatteryEmotionFigmaScreen(
    state: BatteryEditorUiState,
    groupKey: String?,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onConfig: (BatteryStatusConfig) -> Unit,
    onApply: () -> Unit
) {
    val selectedGroup = BUNDLED_BATTERY_EMOTION_GROUPS.firstOrNull { it.key == groupKey }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.img_home_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            EmotionTopBar(
                title = selectedGroup?.title() ?: stringResource(R.string.battery_component_emotion),
                checked = state.config.showEmotion,
                showSwitch = selectedGroup != null,
                onBack = onBack,
                onPremium = onPremium,
                onCheckedChange = { onConfig(state.config.copy(showEmotion = it)) }
            )
            BatteryPreview(
                state = state,
                page = BatteryEditorPage.EMOJI,
                modifier = Modifier.padding(
                    start = dimensionResource(SdpR.dimen._12sdp),
                    end = dimensionResource(SdpR.dimen._12sdp),
                    top = dimensionResource(SdpR.dimen._9sdp)
                )
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
            if (selectedGroup == null) {
                EmotionGroupList(
                    state = state,
                    onOpenGroup = onOpenGroup,
                    modifier = Modifier.weight(1f)
                )
            } else {
                EmotionDetailContent(
                    state = state,
                    group = selectedGroup,
                    onConfig = onConfig,
                    modifier = Modifier.weight(1f)
                )
                EmotionApplyPanel(onClick = onApply)
            }
        }
    }
}

@Composable
private fun EmotionTopBar(
    title: String,
    checked: Boolean,
    showSwitch: Boolean,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(SdpR.dimen._43sdp))
            .padding(horizontal = dimensionResource(SdpR.dimen._12sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_favorite_recent_back),
            contentDescription = stringResource(R.string.back),
            modifier = Modifier
                .size(dimensionResource(SdpR.dimen._22sdp))
                .clip(CircleShape)
                .clickable(onClick = onBack)
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._9sdp)))
        Text(
            text = title,
            color = colorResource(R.color.colors_212327),
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = dimensionResource(SspR.dimen._15ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._22ssp).value.sp
        )
        Spacer(Modifier.weight(1f))
        if (showSwitch) {
            EmotionSwitch(checked = checked, onCheckedChange = onCheckedChange)
        } else {
            HomePremiumButton(onClick = onPremium)
        }
    }
}

@Composable
private fun EmotionSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackWidth = dimensionResource(SdpR.dimen._40sdp)
    val trackHeight = dimensionResource(SdpR.dimen._22sdp)
    val thumbSize = dimensionResource(SdpR.dimen._17sdp)
    val inset = (trackHeight - thumbSize) / 2
    Box(
        modifier = Modifier
            .size(trackWidth, trackHeight)
            .clip(CircleShape)
            .background(colorResource(if (checked) R.color.colors_FB3675 else R.color.colors_F1E0FF))
            .semantics { role = Role.Switch }
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(horizontal = inset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(colorResource(if (checked) R.color.colors_FFFFFF else R.color.colors_B06EFF))
        )
    }
}

@Composable
private fun EmotionGroupList(
    state: BatteryEditorUiState,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = dimensionResource(SdpR.dimen._12sdp),
            end = dimensionResource(SdpR.dimen._12sdp),
            bottom = dimensionResource(SdpR.dimen._12sdp)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._12sdp))
    ) {
        items(BUNDLED_BATTERY_EMOTION_GROUPS, key = BatteryEmotionGroup::key) { group ->
            EmotionGroupCard(
                group = group,
                emotions = group.items(state.emotions),
                onOpenGroup = onOpenGroup
            )
        }
    }
}

@Composable
private fun EmotionGroupCard(
    group: BatteryEmotionGroup,
    emotions: List<BatteryDecorationEntry>,
    onOpenGroup: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(SdpR.dimen._4sdp)))
                .clickable { onOpenGroup(group.key) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._4sdp))
        ) {
            emotions.firstOrNull()?.let { emotion ->
                AsyncImage(
                    model = emotion.assetPath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._15sdp))
                )
            }
            Text(
                text = group.title(),
                color = colorResource(R.color.colors_000000),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp
            )
        }
        val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._12sdp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._104sdp))
                .shadow(dimensionResource(SdpR.dimen._9sdp), shape)
                .clip(shape)
                .background(colorResource(R.color.colors_FFFFFF))
                .clickable { onOpenGroup(group.key) }
        ) {
            if (group.key != "emoji") {
                AsyncImage(
                    model = "file:///android_asset/battery_emotions/backgrounds/${group.key}.jpg",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = if (group.key == "cony") 0.2f else 1f,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensionResource(SdpR.dimen._11sdp),
                        vertical = dimensionResource(SdpR.dimen._8sdp)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._13sdp))
            ) {
                emotions.chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { emotion ->
                            AsyncImage(
                                model = emotion.assetPath,
                                contentDescription = emotion.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(dimensionResource(SdpR.dimen._37sdp))
                                    .clickable { onOpenGroup(group.key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmotionDetailContent(
    state: BatteryEditorUiState,
    group: BatteryEmotionGroup,
    onConfig: (BatteryStatusConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DesignSlider(
            label = stringResource(R.string.battery_editor_size_label),
            value = state.config.emojiSizeDp,
            range = 12f..36f,
            onValueChange = { onConfig(state.config.copy(emojiSizeDp = it)) },
            modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        Text(
            text = stringResource(R.string.battery_emotion_style_title),
            color = colorResource(R.color.colors_212327),
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
            lineHeight = dimensionResource(SspR.dimen._18ssp).value.sp,
            modifier = Modifier.padding(horizontal = dimensionResource(SdpR.dimen._12sdp))
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        val emotions = group.items(state.emotions)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensionResource(SdpR.dimen._12sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            ),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._9sdp))
        ) {
            items(emotions, key = BatteryDecorationEntry::id) { emotion ->
                val active = state.config.emotionDecorationId == emotion.id
                val shape = RoundedCornerShape(dimensionResource(SdpR.dimen._9sdp))
                Box(
                    modifier = Modifier
                        .aspectRatio(101.333f / 102f)
                        .clip(shape)
                        .background(colorResource(if (active) R.color.colors_FFEBF1 else R.color.colors_FFFFFF))
                        .border(
                            dimensionResource(SdpR.dimen._1sdp),
                            colorResource(if (active) R.color.colors_FB3675 else R.color.colors_DEDEDF),
                            shape
                        )
                        .semantics {
                            contentDescription = emotion.name
                            selected = active
                        }
                        .clickable {
                            onConfig(
                                state.config.copy(
                                    showEmotion = true,
                                    emotionDecorationId = emotion.id
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = emotion.assetPath,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(dimensionResource(SdpR.dimen._43sdp))
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionApplyPanel(onClick: () -> Unit) {
    val shape = RoundedCornerShape(
        topStart = dimensionResource(SdpR.dimen._18sdp),
        topEnd = dimensionResource(SdpR.dimen._18sdp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(dimensionResource(SdpR.dimen._4sdp), shape)
            .clip(shape)
            .background(colorResource(R.color.colors_FFFFFF))
            .padding(
                start = dimensionResource(SdpR.dimen._12sdp),
                top = dimensionResource(SdpR.dimen._18sdp),
                end = dimensionResource(SdpR.dimen._12sdp),
                bottom = dimensionResource(SdpR.dimen._9sdp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(SdpR.dimen._37sdp))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colorResource(R.color.colors_C95DFF),
                            colorResource(R.color.colors_FB54BB)
                        )
                    )
                )
                .semantics { role = Role.Button }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.battery_apply),
                color = colorResource(R.color.colors_FFFFFF),
                fontFamily = RobotoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = dimensionResource(SspR.dimen._14ssp).value.sp,
                lineHeight = dimensionResource(SspR.dimen._20ssp).value.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun BatteryEmotionGroup.items(
    emotions: List<BatteryDecorationEntry>
): List<BatteryDecorationEntry> = emotionIds.mapNotNull { id ->
    emotions.firstOrNull { it.id == id }
}

@Composable
private fun BatteryEmotionGroup.title(): String = stringResource(
    when (key) {
        "emoji" -> R.string.battery_emotion_group_emoji
        "cony" -> R.string.battery_emotion_group_cony
        "kiiroitori" -> R.string.battery_emotion_group_kiiroitori
        "molang" -> R.string.battery_emotion_group_molang
        "mochi" -> R.string.battery_emotion_group_mochi
        "tobi" -> R.string.battery_emotion_group_tobi
        "keroppi" -> R.string.battery_emotion_group_keroppi
        "pochacco" -> R.string.battery_emotion_group_pochacco
        else -> R.string.battery_component_emotion
    }
)
