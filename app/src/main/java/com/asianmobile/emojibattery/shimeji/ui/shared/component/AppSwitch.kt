package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import com.asianmobile.emojibattery.shimeji.R
import com.intuit.sdp.R as SdpR

/**
 * The app's switch, from Figma nodes `8080:7307` (on) and `8080:7343` (off): a 44x24 track with a
 * 20x20 knob inset by 2. The knob is white in both states; only the track changes colour.
 *
 * Every toggle in the app uses this one, so a switch never looks like two different controls.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true
) {
    val trackWidth = dimensionResource(SdpR.dimen._34sdp)
    val trackHeight = dimensionResource(SdpR.dimen._18sdp)
    val knobSize = dimensionResource(SdpR.dimen._15sdp)
    // Derived, never a resource of its own: sdp rounds every dimension into its own bucket, so a
    // hardcoded inset drifts away from (track - knob) / 2 and leaves the knob off-centre.
    val knobInset = (trackHeight - knobSize) / 2
    val knobOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - knobInset - knobSize else knobInset,
        label = "appSwitchKnob"
    )
    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(
                colorResource(
                    if (checked) R.color.colors_FB3675 else R.color.colors_C8C8C9
                )
            )
            .then(
                if (interactive) {
                    Modifier.toggleable(
                        value = checked,
                        role = Role.Switch,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onValueChange = { onCheckedChange() }
                    )
                } else {
                    Modifier
                }
            ),
        // Centred rather than offset vertically, so the knob cannot drift off the track's axis.
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.roundToPx(), 0) }
                .size(knobSize)
                .clip(CircleShape)
                .background(colorResource(R.color.colors_FFFFFF))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun AppSwitchPreview() {
    Box { AppSwitch(checked = true, onCheckedChange = {}) }
}
