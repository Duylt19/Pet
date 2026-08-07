package com.asianmobile.emojibattery.shimeji.ui.component

/**
 * How much of a catalog card its preview artwork takes up.
 *
 * Figma sizes the artwork at almost the whole card, but its placeholders carry a transparent
 * margin that the real catalog thumbnails do not: those are cropped tight. Copying the design
 * number therefore fills the card edge to edge. Discover settled on this fraction, and every
 * grid that shows a catalog thumbnail uses it so the same asset is never two sizes in two places.
 */
const val CATALOG_ITEM_PREVIEW_FRACTION = 0.65f
