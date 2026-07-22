package com.asianmobile.privatebrower.ui.language

import android.content.Context
import androidx.compose.runtime.Immutable
import com.asianmobile.privatebrower.R
import com.asianmobile.privatebrower.constant.AF
import com.asianmobile.privatebrower.constant.AR
import com.asianmobile.privatebrower.constant.DE
import com.asianmobile.privatebrower.constant.EN
import com.asianmobile.privatebrower.constant.EN_REGION
import com.asianmobile.privatebrower.constant.ES
import com.asianmobile.privatebrower.constant.FR
import com.asianmobile.privatebrower.constant.HA
import com.asianmobile.privatebrower.constant.HA_REGION
import com.asianmobile.privatebrower.constant.HI
import com.asianmobile.privatebrower.constant.HI_REGION
import com.asianmobile.privatebrower.constant.PT
import com.asianmobile.privatebrower.constant.VI
import com.asianmobile.privatebrower.constant.VI_REGION
import com.asianmobile.privatebrower.constant.ZH

/**
 * Copyright © 2026 Asian Mobile Co.,Ltd
 * Created by am_viennv on 3/9/2026
 */
@Immutable
data class Language(
    val id: Int,
    val flag: Int,
    val name: String,
    val key: String,
    val country: String
)

fun Context.mockData() =
    listOf(
        Language(1, R.drawable.ic_flag_en, getString(R.string.language_en), EN, EN_REGION),
        Language(3, R.drawable.ic_flag_es, getString(R.string.language_es), ES, ES),
        Language(7, R.drawable.ic_flag_vi, getString(R.string.language_vi), VI, VI_REGION),
        Language(8, R.drawable.ic_flag_fr, getString(R.string.language_fr), FR, FR),
        Language(2, R.drawable.ic_flag_hi, getString(R.string.language_hi), HI, HI_REGION),
        Language(5, R.drawable.ic_flag_de, getString(R.string.language_de), DE, DE),
        Language(4, R.drawable.ic_flag_pt, getString(R.string.language_pt), PT, PT),
        Language(6, R.drawable.ic_flag_ar, getString(R.string.language_ar), AR, AR),
        Language(9, R.drawable.ic_flag_ha, getString(R.string.language_ha), HA, HA_REGION),
        Language(10, R.drawable.ic_flag_af, getString(R.string.language_af), AF, AF),
        Language(11, R.drawable.ic_flag_zh, getString(R.string.language_zh), ZH, ZH),
    )
