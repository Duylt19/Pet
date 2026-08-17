package com.asianmobile.emojibattery.shimeji.ads.ui.dialog

import android.app.Dialog
import android.content.res.ColorStateList
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.asianmobile.emojibattery.shimeji.ads.R
import com.asianmobile.emojibattery.shimeji.ads.config.TIME_LOADING_ADS

/**
 * Copyright © 2025 Asian Mobile Co.,Ltd
 * Created by am_Huyhn on 20/3/25
 */
class LoadingDialogFragment : DialogFragment() {
    private var dismissCallback: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable { dismissCallback?.invoke() }

    companion object {
        private const val TAG = "LoadingDialog"

        fun showDialog(activity: FragmentActivity, callback: () -> Unit) {
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }

            activity.runOnUiThread {
                val existingFragment = activity.supportFragmentManager.findFragmentByTag(TAG) as? LoadingDialogFragment
                if (existingFragment != null) {
                    existingFragment.updateCallback(callback)
                    return@runOnUiThread
                }

                val dialogFragment = LoadingDialogFragment().apply {
                    dismissCallback = callback
                }

                dialogFragment.show(activity.supportFragmentManager, TAG)
            }
        }

        fun dismissDialog(activity: FragmentActivity) {
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }

            activity.runOnUiThread {
                val fragment = activity.supportFragmentManager.findFragmentByTag(TAG) as? LoadingDialogFragment
                if (fragment != null && fragment.isShowing()) {
                    fragment.dismissAllowingStateLoss()
                }
            }
        }
    }

    fun updateCallback(callback: () -> Unit) {
        dismissCallback = callback
    }

    internal fun isShowing() = dialog?.isShowing == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity()).apply {
            val backgroundColor = ContextCompat.getColor(context, R.color.colors_161718)
            val accentColor = ContextCompat.getColor(context, R.color.colors_5FADFF)
            val container = android.widget.LinearLayout(requireActivity()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(backgroundColor)
                addView(android.widget.ProgressBar(requireActivity()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(120, 120)
                    indeterminateTintList = ColorStateList.valueOf(accentColor)
                })
            }
            setContentView(container)

            window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawable(ColorDrawable(backgroundColor))
                setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                )
            }

            setCancelable(false)
            setCanceledOnTouchOutside(false)

            handler.postDelayed(dismissRunnable, TIME_LOADING_ADS)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        handler.removeCallbacks(dismissRunnable)
        handler.removeCallbacksAndMessages(null)
        dismissCallback = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissRunnable)
        handler.removeCallbacksAndMessages(null)
        dismissCallback = null
    }
}
