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
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.shalzz.attendance.R
import com.shalzz.attendance.data.remote.DataAPI
import com.shalzz.attendance.utils.Miscellaneous
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.captcha_dialog.*
import kotlinx.android.synthetic.main.captcha_dialog.view.*
import timber.log.Timber

class CaptchaDialogFragment(listener: CaptchaDialogListener, dataAPI: DataAPI) : DialogFragment() {

    private lateinit var mContext: Context
    // Use this instance of the interface to deliver action events
    private var mListener: CaptchaDialogListener = listener
    private var mDataAPI = dataAPI

    private var mDisposable: Disposable? = null

    /** The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    interface CaptchaDialogListener {
        fun onDialogPositiveClick(captcha: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mContext = context!!
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

    override fun onDestroy() {
        super.onDestroy()
        RxUtil.dispose(mDisposable)
    }

    private fun loadImg(clg: String) {
        val materialDialog = dialog as MaterialDialog
        val imageView = materialDialog.ivCapImg

        val circularProgressDrawable = CircularProgressDrawable(mContext)
        circularProgressDrawable.strokeWidth = 8f
        circularProgressDrawable.centerRadius = 30f
        val color = mContext.resources.getColor(R.color.accent)
        circularProgressDrawable.setColorSchemeColors(color)
        circularProgressDrawable.start()
        imageView.setImageDrawable(circularProgressDrawable)

        RxUtil.dispose(mDisposable)
        mDisposable = mDataAPI.getCaptcha(clg)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {response ->
                    val cookie = response.headers().get("x-cookie")
                    Timber.d("Cookie: %s", cookie)
                    if (response.isSuccessful) {
                        val bmp = BitmapFactory.decodeStream(response.body()!!.byteStream())
                        imageView.scaleType = ImageView.ScaleType.FIT_XY
                        imageView.setImageBitmap(bmp)
                    }
                }, {error ->
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageView.setImageResource(R.drawable.ic_error_black)
                    Timber.e(error, "Unable to load captcha image")
                })
    }
}