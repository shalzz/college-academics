/*
 * Copyright (c) 2013-2019 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.ui.login

import android.app.Dialog
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.shalzz.attendance.R
import com.shalzz.attendance.utils.Miscellaneous
import kotlinx.android.synthetic.main.captcha_dialog.*
import kotlinx.android.synthetic.main.captcha_dialog.view.*
import timber.log.Timber


class CaptchaDialogFragment(listener: CaptchaDialogListener) : DialogFragment() {

    // Use this instance of the interface to deliver action events
    private var mListener: CaptchaDialogListener = listener

    /** The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    interface CaptchaDialogListener {
        fun onDialogPositiveClick(captcha: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity!!.layoutInflater
        val mView = inflater.inflate(R.layout.captcha_dialog, null)

        return MaterialDialog.Builder(context!!)
                .positiveText(R.string.login_button)
                .negativeText(android.R.string.cancel)
                .customView(mView, false)
                .autoDismiss(false)
                .onPositive { _, _ ->
                    mListener.onDialogPositiveClick(mView.captchaEditText.text.toString())
                }
                .onNegative { dialog, _ ->
                    dialog.dismiss() }
                .showListener { Miscellaneous.showKeyboard(context, mView.captchaEditText) }
                .build()
    }

    override fun onStart() {
        val materialDialog = dialog as MaterialDialog
        val positiveButton = materialDialog.getActionButton(DialogAction.POSITIVE)

        // Get the Captcha Image
        loadImg("mecs")

        // OnClickListener event for the Reload captcha Button
        materialDialog.refreshButton.setOnClickListener {
            loadImg("mecs")
            materialDialog.captchaEditText.setText("")
        }

        // logs in when user press done on keyboard.
        materialDialog.captchaEditText.setOnEditorActionListener(
                TextView.OnEditorActionListener { view, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        positiveButton.performClick()
                        return@OnEditorActionListener true
                    }
                    false
                })

        super.onStart()
    }

    private fun loadImg(clg: String) {
        val materialDialog = dialog as MaterialDialog

        val glideUrl = GlideUrl("https://academics.8bitlabs.tech/v4/dev/me/captcha",
                LazyHeaders.Builder()
                .addHeader("x-clg-id", clg)
                .build())
        Timber.d("Loading Image: %s", glideUrl.toURL())
        Timber.d("ImageView %s", materialDialog.ivCapImg)

        Glide.with(this)
                .load(glideUrl)
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .skipMemoryCache(true)
                .into(materialDialog.ivCapImg)
    }
}