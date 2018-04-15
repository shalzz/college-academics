package com.shalzz.attendance.wrapper;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.shalzz.attendance.R;

import timber.log.Timber;

/**
 * @author shalzz
 */
public class ProModeListPreference extends ListPreference {

    Context mContext;

    public ProModeListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
    }

    public ProModeListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ProModeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProModeListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        if (false) {
            super.onClick();
        } else {
            Timber.d("Clicked!!! ");
        }
    }
}
