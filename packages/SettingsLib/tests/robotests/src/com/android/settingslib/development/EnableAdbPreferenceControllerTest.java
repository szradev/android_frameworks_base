/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settingslib.SettingsLibRobolectricTestRunner;
import com.android.settingslib.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsLibRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EnableAdbPreferenceControllerTest {
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private SwitchPreference mPreference;
    private ConcreteEnableAdbPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = spy(shadowContext.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mPreference = new SwitchPreference(mContext);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new ConcreteEnableAdbPreferenceController(mContext);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void displayPreference_isNotAdmin_shouldRemovePreference() {
        when(mUserManager.isAdminUser()).thenReturn(false);
        when(mScreen.getPreferenceCount()).thenReturn(1);
        when(mScreen.getPreference(0)).thenReturn(mPreference);

        mController.displayPreference(mScreen);

        verify(mScreen).removePreference(any(Preference.class));
    }

    @Test
    public void displayPreference_isAdmin_shouldNotRemovePreference() {
        when(mUserManager.isAdminUser()).thenReturn(true);

        mController.displayPreference(mScreen);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }


    @Test
    public void resetPreference_shouldUncheck() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        mController.displayPreference(mScreen);
        mPreference.setChecked(true);

        mController.resetPreference();

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_shouldUpdateSettings() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 1);
        mPreference.setChecked(true);
        mController.displayPreference(mScreen);

        mController.handlePreferenceTreeClick(mPreference);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0)).isEqualTo(0);
    }

    @Test
    public void updateState_settingsOn_shouldCheck() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 1);
        mPreference.setChecked(false);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingsOff_shouldUncheck() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0);
        mPreference.setChecked(true);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    class ConcreteEnableAdbPreferenceController extends AbstractEnableAdbPreferenceController {
        public ConcreteEnableAdbPreferenceController(Context context) {
            super(context);
        }

        @Override
        public void showConfirmationDialog(Preference preference) {
            // Don't show a dialog, just set setting.
            writeAdbSetting(true);
        }

        @Override
        public boolean isConfirmationDialogShowing() {
            return false;
        }

        @Override
        public void dismissConfirmationDialog() {}
    }
}
