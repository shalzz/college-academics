/*
 * Copyright (c) 2013-2015 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
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

package com.shalzz.attendance.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.CircularIndeterminate;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.wrapper.MyVolley;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class CaptchaDialogFragment extends DialogFragment{

	@InjectView(R.id.ivCapImg) ImageView ivCapImg;
    @InjectView(R.id.progressBar1) CircularIndeterminate pbar;
    @InjectView(R.id.etCapTxt)
	TextInputLayout Captxt;
    @InjectView(R.id.bRefresh) ImageButton bRefreshCaptcha;
	private Context mContext;
	private String mTag = "Captcha Dialog";

	// Use this instance of the interface to deliver action events
	CaptchaDialogListener mListener;

	/** The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks.
	 * Each method passes the DialogFragment in case the host needs to query it. 
	 **/
	public interface CaptchaDialogListener {
		public void onDialogPositiveClick(DialogFragment dialog);
	}

	// Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
	@Override
	public void onAttach(Activity activity) {
		mContext = activity;
		super.onAttach(activity);
		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the CaptchaDialogListener so we can send events to the host
			mListener = (CaptchaDialogListener) activity;
		} catch (ClassCastException e) {
			// The activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement CaptchaDialogListener");
		}
	}
	
	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Context context = getActivity();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.dialog_captcha, null);
        ButterKnife.inject(this,mView);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(context)
                .positiveText(R.string.log_in)
                .negativeText(android.R.string.cancel)
                .customView(mView, false)
                .autoDismiss(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        mListener.onDialogPositiveClick(CaptchaDialogFragment.this);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Miscellaneous.showKeyboard(context, Captxt.getEditText());
                    }
                });

		return builder.build();
	}

	/**
	 * Called when the DialogView is started. Used to setup the onClick listeners.
	 */
	@Override
	public void onStart() {
		super.onStart();

		Tracker t = ((MyVolley) getActivity().getApplication()).getTracker(
				MyVolley.TrackerName.APP_TRACKER);

		t.send(new HitBuilders.ScreenViewBuilder().build());
		
		AlertDialog alertDialog = (AlertDialog) getDialog();
		final Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		
		// Get the Captcha Image
		getImg();

		// OnClickListener event for the Reload captcha Button
		bRefreshCaptcha.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				getImg();
				Captxt.getEditText().setText("");
			}
		});

		// logs in when user press done on keyboard.
		Captxt.getEditText().setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {   
					positiveButton.performClick(); 
					return true;
				}
				return false;
			}});
	}

	/**
	 * Gets the captcha image.
	 */
	private void getImg() 
	{
		ImageLoader imageLoader = MyVolley.getInstance().getImageLoader();
		imageLoader.setBatchedResponseDelay(0);
		imageLoader.get(getString(R.string.URL_captcha), new ImageLoader.ImageListener() {

			final ImageView view = ivCapImg;
			final int errorImageResId = R.drawable.ic_menu_report_image;
			@Override
			public void onErrorResponse(VolleyError error) {
                if(pbar!=null)
                    pbar.setVisibility(View.INVISIBLE);
                view.setVisibility(View.VISIBLE);
                view.setScaleType(ImageView.ScaleType.CENTER);
                view.setImageResource(errorImageResId);
                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                Log.e(mTag, msg);
            }

			@Override
			public void onResponse(ImageContainer response, boolean isImmediate) {
				if (response!=null && response.getBitmap() != null) {
                    if(pbar!=null)
					    pbar.setVisibility(View.INVISIBLE);
					view.setVisibility(View.VISIBLE);
					view.setImageBitmap(response.getBitmap());
					view.setScaleType(ImageView.ScaleType.FIT_XY);
				} else {
                    if(pbar!=null)
					    pbar.setVisibility(ProgressBar.VISIBLE);
					view.setVisibility(View.INVISIBLE);
				}
			}
		});
	}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
