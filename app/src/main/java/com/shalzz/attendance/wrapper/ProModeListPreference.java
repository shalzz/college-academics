package com.shalzz.attendance.wrapper;

import android.content.Context;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import android.util.AttributeSet;

/**
 * @author shalzz
 */
public class ProModeListPreference extends ListPreference {

    private Context mContext;
    private OnProModeListPreferenceClickListener mCallback;

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

    public void showDialog() {
        super.onClick();
    }

    public void setProModeListPreferenceClickListener(OnProModeListPreferenceClickListener listener) {
        mCallback = listener;
    }

    @Override
    protected void onClick() {
        if (mCallback != null)
            mCallback.onPreferenceClick(this);
    }

    public interface OnProModeListPreferenceClickListener {
        boolean onPreferenceClick(ProModeListPreference preference);
    }
}
