package com.asianmobile.emojibattery.shimeji.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.asianmobile.emojibattery.shimeji.R

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

@Composable
fun appTypography(): Typography {
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.withRoboto(),
        displayMedium = defaults.displayMedium.withRoboto(),
        displaySmall = defaults.displaySmall.withRoboto(),
        headlineLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = ssp(22)
        ),
        headlineMedium = defaults.headlineMedium.withRoboto(),
        headlineSmall = defaults.headlineSmall.withRoboto(),
        titleLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = ssp(17)
        ),
        titleMedium = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = ssp(14)
        ),
        titleSmall = defaults.titleSmall.withRoboto(),
        bodyLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = ssp(12)
        ),
        bodyMedium = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = ssp(11)
        ),
        bodySmall = defaults.bodySmall.withRoboto(),
        labelLarge = defaults.labelLarge.withRoboto(),
        labelMedium = defaults.labelMedium.withRoboto(),
        labelSmall = TextStyle(
            fontFamily = RobotoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = ssp(10)
        )
    )
}

private fun TextStyle.withRoboto(): TextStyle = copy(fontFamily = RobotoFontFamily)

@Composable
private fun ssp(value: Int): TextUnit {
    val res = when (value) {
        9 -> com.intuit.ssp.R.dimen._9ssp
        10 -> com.intuit.ssp.R.dimen._10ssp
        11 -> com.intuit.ssp.R.dimen._11ssp
        12 -> com.intuit.ssp.R.dimen._12ssp
        13 -> com.intuit.ssp.R.dimen._13ssp
        14 -> com.intuit.ssp.R.dimen._14ssp
        17 -> com.intuit.ssp.R.dimen._17ssp
        22 -> com.intuit.ssp.R.dimen._22ssp
        else -> {
            val context = LocalContext.current
            context.resources.getIdentifier("_${value}ssp", "dimen", "com.intuit.ssp")
        }
    }
    return dimensionResource(res).value.sp
}
