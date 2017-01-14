package com.shalzz.attendance.util;

import com.shalzz.attendance.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = DefaultConfig.EMULATE_SDK)
public final class PermissionsTest {

    private static final String[] EXPECTED_PERMISSIONS = {
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_SYNC_SETTINGS",
            "android.permission.WRITE_SYNC_SETTINGS",
            "android.permission.AUTHENTICATE_ACCOUNTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
    };

    private static final String MERGED_MANIFEST =
        "build/intermediates/manifests/full/" + BuildConfig.FLAVOR +
                "/" + BuildConfig.BUILD_TYPE + "/AndroidManifest.xml";

    @Test
    public void shouldMatchPermissions() {
        AndroidManifest manifest = new AndroidManifest(
                Fs.fileFromPath(MERGED_MANIFEST),
                null,
                null
        );

        assertEquals(manifest.getUsedPermissions().toArray(), EXPECTED_PERMISSIONS);
    }
}
