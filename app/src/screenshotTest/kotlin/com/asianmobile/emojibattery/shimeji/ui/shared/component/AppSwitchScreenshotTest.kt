package com.asianmobile.emojibattery.shimeji.ui.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest

/**
 * The switch is used by every toggle in the app and had no coverage, which is how it drifted from
 * the design unnoticed. Both states are pinned here.
 */
@PreviewTest
@Preview(widthDp = 120, heightDp = 40)
@Composable
fun AppSwitchStatesScreenshotTest() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppSwitch(checked = false, onCheckedChange = {})
        AppSwitch(checked = true, onCheckedChange = {})
    }
}
