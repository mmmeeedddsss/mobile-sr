package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
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

    Preference rescalingFactorText;
    Preference modelRescalesText;
    Preference inputHeightText;
    Preference inputWidthText;

    int[] possibleBatchNumbers;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_general, rootKey);
        PreferenceManager prefManager = getPreferenceManager();
        modelNameList = (ListPreference) prefManager.findPreference("model_name_list");
        nnapiSwitch = (SwitchPreference) prefManager.findPreference("use_nnapi_switch");
        parallelBatchList = (ListPreference) prefManager.findPreference("parallel_batch_number");
        rescalingFactorText = prefManager.findPreference("rescaling_factor");
        modelRescalesText = prefManager.findPreference("model_rescales");
        inputHeightText = prefManager.findPreference("input_height");
        inputWidthText = prefManager.findPreference("input_width");

        // first of all, the hard-coded model list in the XML is super lame! We need to get
        // it from the model configuration file instead.
        String[] fullModelList = SRModelConfigurationManager.getConfigurationMapKeys();
        modelNameList.setEntries(fullModelList);
        modelNameList.setEntryValues(fullModelList);


        initBatchValues(); // possible batch values to ensure configurations

        // before setting listeners, load the previously stored config
        CharSequence storedModel = modelNameList.getValue();
        if(storedModel != null)
            SRModelConfigurationManager.switchConfiguration(storedModel.toString());
        else
            modelNameList.setValue(SRModelConfigurationManager.getCurrentConfiguration().getModelName());
        loadCurrentModelPreferences();

        modelNameList.setOnPreferenceChangeListener((preference, value) -> {
            String model = (String) value;
            SRModelConfigurationManager.switchConfiguration(model);
            loadCurrentModelPreferences();
            return true;
        });

        nnapiSwitch.setOnPreferenceChangeListener((preference, value) -> {
            // the value gotten is a Boolean in this case, no issues here
            SRModelConfigurationManager.setNNAPI((Boolean) value);
            return true;
        });

        // NOTE: I could not find any way to properly get a number using support.v7! It sucks.
        // Instead I decided to use a drop-down list for the batch size, since they should
        // ideally be powers of two anyway
        parallelBatchList.setOnPreferenceChangeListener((preference, value) -> {
            SRModelConfigurationManager.setBatch(Integer.parseInt((String) value));
            String formatted = String.format(getString(R.string.summary_format_parallel_batch_number), value);
            parallelBatchList.setSummary(formatted);
            return true;
        });
    }

    // a wrapper to ensure config values are near a power of two provided in the selections
    // if it is not, the config value is replaced with the closest option
    private void checkAndFixCurrentBatchNumber() {
        SRModelConfiguration conf = SRModelConfigurationManager.getCurrentConfiguration();
        int number = conf.getNumParallelBatch();
        int index = Arrays.binarySearch(possibleBatchNumbers, number);
        if(index < 0) {
            // decide on the closest
            int insertionPoint = -index - 1; // see the java docs for Arrays.binarysearch
            int ldiff = Integer.MAX_VALUE, rdiff = Integer.MAX_VALUE;
            if (insertionPoint > 0)
                ldiff = Math.abs(number - possibleBatchNumbers[insertionPoint - 1]);
            if (insertionPoint < possibleBatchNumbers.length)
                rdiff = Math.abs(number - possibleBatchNumbers[insertionPoint]);
            int closest = ldiff < rdiff ? possibleBatchNumbers[insertionPoint - 1] : possibleBatchNumbers[insertionPoint];
            SRModelConfigurationManager.setBatch(closest);
            Log.w("Preferences", String.format("Batch number %d read from config is not a proper batch value. Replaced with closest value %d.", number, closest));
        }
    }

    private void initBatchValues() {
        CharSequence[] stringVals = parallelBatchList.getEntryValues();
        possibleBatchNumbers = new int[stringVals.length];
        for(int i = 0; i < stringVals.length; i++)
            possibleBatchNumbers[i] = Integer.parseUnsignedInt(stringVals[i].toString());
    }

    // Since the preferences depend on the model, the existing
    // values should be set when the model is changed
    private void loadCurrentModelPreferences() {
        SRModelConfiguration conf = SRModelConfigurationManager.getCurrentConfiguration();
        checkAndFixCurrentBatchNumber();
        String batchNum = Integer.toString(conf.getNumParallelBatch());
        String formatted = String.format(getString(R.string.summary_format_model_name), conf.getModelName());
        modelNameList.setSummary(formatted);
        nnapiSwitch.setChecked(conf.getNNAPISetting());
        nnapiSwitch.setEnabled(conf.supportsNNAPI());
        parallelBatchList.setValue(batchNum);
        formatted = String.format(getString(R.string.summary_format_parallel_batch_number), batchNum);
        parallelBatchList.setSummary(formatted);
        rescalingFactorText.setSummary(Integer.toString(conf.getRescalingFactor()));
        modelRescalesText.setSummary(conf.getModelRescales() ? "No" : "Yes");
        inputHeightText.setSummary(Integer.toString(conf.getInputImageHeight()));
        inputWidthText.setSummary(Integer.toString(conf.getInputImageWidth()));
    }
}
