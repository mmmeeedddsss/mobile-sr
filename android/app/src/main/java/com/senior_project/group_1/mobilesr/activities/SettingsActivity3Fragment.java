package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;

import java.util.Arrays;

public class SettingsActivity3Fragment extends PreferenceFragmentCompat {

    // Nice! No need to get and edit the preferences, this guy does those automatically,
    // the results are persistent by default, as far as I understand.

    ListPreference modelNameList;
    SwitchPreference nnapiSwitch;
    ListPreference parallelBatchList;

    int[] possibleBatchNumbers;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, rootKey);
        PreferenceManager prefManager = getPreferenceManager();
        modelNameList = (ListPreference) prefManager.findPreference("model_name_list");
        nnapiSwitch = (SwitchPreference) prefManager.findPreference("use_nnapi_switch");
        parallelBatchList = (ListPreference) prefManager.findPreference("parallel_batch_number");

        initBatchValues(); // possible batch values to ensure configurations

        modelNameList.setOnPreferenceChangeListener((preference, value) -> {
            loadModelPreferences((String) value);
            return true;
        });

        nnapiSwitch.setOnPreferenceChangeListener((preference, value) -> {
            // the value returned is a Boolean in this case, no issues here
            SRModelConfigurationManager.setNNAPI((Boolean) value);
            return true;
        });

        // NOTE: I could not find any way to properly get a number using support.v7! It sucks.
        // Instead I decided to use a drop-down list for the batch size, since they should
        // ideally be powers of two anyway
        parallelBatchList.setOnPreferenceChangeListener((preference, value) -> {
            SRModelConfigurationManager.setBatch(Integer.parseInt((String) value));
            return true;
        });
    }

    // a wrapper to ensure config values are near a power of two provided in the selections
    private int fixBatchNumber(int number) {
        int index = Arrays.binarySearch(possibleBatchNumbers, number);
        if(index >= 0)
            return number;
        // decide on the closest
        int insertionPoint = -index - 1;
        int ldiff = Integer.MAX_VALUE, rdiff = Integer.MAX_VALUE;
        if(insertionPoint > 0)
            ldiff = Math.abs(number - possibleBatchNumbers[insertionPoint - 1]);
        if(insertionPoint < possibleBatchNumbers.length)
            rdiff = Math.abs(number - possibleBatchNumbers[insertionPoint]);
        int closest = ldiff < rdiff ? possibleBatchNumbers[insertionPoint - 1] : possibleBatchNumbers[insertionPoint];
        Log.w("Preferences", String.format("Batch number %d read from config is not a proper batch value. Replaced with closest value %d.", number, closest));
        return closest;
    }

    private void initBatchValues() {
        CharSequence[] stringVals = parallelBatchList.getEntryValues();
        possibleBatchNumbers = new int[stringVals.length];
        for(int i = 0; i < stringVals.length; i++)
            possibleBatchNumbers[i] = Integer.parseUnsignedInt(stringVals[i].toString());
    }

    private void loadModelPreferences(String modelName) {
        // Since the preferences depend on the model, the existing
        // values should be set when the model is changed
        SRModelConfiguration conf = SRModelConfigurationManager.switchConfiguration(modelName);
        Log.i("REF", "" + conf.getNNAPISetting());
        Log.i("REF", "" + conf.getNumParallelBatch());
        String batchNum = Integer.toString(fixBatchNumber(conf.getNumParallelBatch()));
        nnapiSwitch.setChecked(conf.getNNAPISetting());
        parallelBatchList.setValue(batchNum);
    }
}
