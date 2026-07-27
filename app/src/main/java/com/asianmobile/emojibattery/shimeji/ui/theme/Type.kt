package com.asianmobile.emojibattery.shimeji.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun appTypography() = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = ssp(22)
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = ssp(17)
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = ssp(14)
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = ssp(12)
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = ssp(11)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = ssp(10)
    )
)

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


