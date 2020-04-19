package com.roman;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.Menu;

import com.roman.pawelm.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);


        ListPreference appRolePreference = (ListPreference) findPreference("app_role");


        if (appRolePreference != null) {

            if (appRolePreference.getValue().equals(getString(R.string.app_role_camera_key))) {
                appRolePreference.setSummary(R.string.app_role_camera_label);
            } else {
                appRolePreference.setSummary(R.string.app_role_display_label);
            }

            appRolePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object new_app_role) {

                    String app_role = (String) new_app_role;

                    if (app_role.equals(getString(R.string.app_role_camera_key))) {
                        preference.setSummary(R.string.app_role_camera_label);
                    } else {
                        preference.setSummary(R.string.app_role_display_label);
                    }
                    return true;
                }
            });


        }


        Preference launchLoginActivity = findPreference("launch_login");
        launchLoginActivity.setSummary("roman.dmowski@outlook.com");



//        EditTextPreference usernamePreference = (EditTextPreference) findPreference("username");
//
//
//        if (usernamePreference != null) {
//
//            String useremail = usernamePreference.getText();
//            usernamePreference.setSummary(useremail);
//
//
//            usernamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object new_app_role) {
//
//                    String _username = (String) new_app_role;
//
//                    preference.setSummary(_username);
//
//                    return true;
//                }
//            });
//
//
//        }





    }

}
