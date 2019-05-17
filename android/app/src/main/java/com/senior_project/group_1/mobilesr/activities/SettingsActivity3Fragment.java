package com.senior_project.group_1.mobilesr.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;

public class SettingsActivity3Fragment extends PreferenceFragmentCompat {

    // Nice! No need to get and edit the preferences, this guy does those automatically,
    // the results are persistent by default, as far as I understand.

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, rootKey);
        PreferenceManager prefManager = getPreferenceManager();

        prefManager.findPreference("model_name_list").setOnPreferenceChangeListener((preference, value) -> {
            SRModelConfigurationManager.getConfiguration((String) value);
            Log.i("PREF", "hi");
            return true;
        });

        prefManager.findPreference("use_nnapi_switch").setOnPreferenceChangeListener((preference, value) -> {
            // the value returned is a Boolean in this case, no issues here
            SRModelConfigurationManager.setNNAPI((Boolean) value);
            return true;
        });

        // NOTE: I could not find any way to properly get a number using support.v7! It sucks.
        // Instead I decided to use a drop-down list for the batch size, since they should
        // ideally be powers of two anyway
        prefManager.findPreference("parallel_batch_number").setOnPreferenceChangeListener((preference, value) -> {
            SRModelConfigurationManager.setBatch(Integer.parseInt((String) value));
            return true;
        });
    }
}
