package com.asianmobile.emojibattery.shimeji.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.asianmobile.emojibattery.shimeji.R

/**
 * The badge on locked catalog artwork: a translucent 24 unit yellow disc with the 18 unit crown
 * inside. Shared by Pet Store, Search, and Battery so Premium state stays visually identical.
 */
@Composable
fun PetPremiumBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(colorResource(R.color.colors_FFEA89).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.img_pet_premium_badge),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // Figma: 18 of the 24 unit disc, so the crown keeps its ring of yellow at any size.
            modifier = Modifier.fillMaxSize(BADGE_ART_FRACTION)
        )
    }
}

private const val BADGE_ART_FRACTION = 18f / 24f
