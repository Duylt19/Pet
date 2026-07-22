/*
 * Copyright © 2023 Asian Mobile Co.,Ltd
 * Created by am_tuannq on 10/3/23, 2:39 PM
 */

package com.asianmobile.privatebrower.ads.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.asianmobile.privatebrower.ads.R

@SuppressLint("CustomViewStyleable")
class BorderImageView(context: Context, attributeSet: AttributeSet) : AppCompatImageView(context, attributeSet) {

    private var radius = -1F
    private var leftTopRadius = dpToPx(0F)
    private var leftBottomRadius = dpToPx(0F)
    private var topRightRadius = dpToPx(0F)
    private var bottomRightRadius = dpToPx(0F)
    private var clipPath = Path()
    private var tempRect = RectF()

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CustomBorderImageView)
        try {
            val radiusValue = typedArray.getDimension(R.styleable.CustomBorderImageView_cbivRadius, -1F)
            if (radiusValue < 0) {
                leftTopRadius = (typedArray.getDimension(R.styleable.CustomBorderImageView_cbivLeftTopRadius, 0F))
                leftBottomRadius = (typedArray.getDimension(R.styleable.CustomBorderImageView_cbivLeftBottomRadius, 0F))
                topRightRadius = (typedArray.getDimension(R.styleable.CustomBorderImageView_cbivTopRightRadius, 0F))
                bottomRightRadius =
                    (typedArray.getDimension(R.styleable.CustomBorderImageView_cbivBottomRightRadius, 0F))
            } else {
                radius = (radiusValue)
            }
        } finally {
            typedArray.recycle()
        }
        postInvalidate()
    }

    private fun dpToPx(dp: Float) = dp * context.resources.displayMetrics.density

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        clipPath.reset()
        tempRect.apply {
            left = 0F
            top = 0F
            right = w.toFloat()
            bottom = h.toFloat()
        }
        if (radius < 0) {
            clipPath.addRoundRect(
                tempRect, floatArrayOf(
                    leftTopRadius,
                    leftTopRadius,
                    topRightRadius,
                    topRightRadius,
                    bottomRightRadius,
                    bottomRightRadius,
                    leftBottomRadius,
                    leftBottomRadius
                ), Path.Direction.CW
            )
        } else {
            clipPath.addRoundRect(tempRect, radius, radius, Path.Direction.CW)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
    }
}


