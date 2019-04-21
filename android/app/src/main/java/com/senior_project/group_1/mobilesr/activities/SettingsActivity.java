package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfiguration;
import com.senior_project.group_1.mobilesr.configurations.SRModelConfigurationManager;


public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //Get the view From pick_photo_activity
        setContentView(R.layout.settings_activity);

        String[] conf_list = SRModelConfigurationManager.getConfigurationMapKeys();
        String[] nnapi_str = new String[] {"True", "False"};
        String[] batch_str = new String[] {"2", "7", "30"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, conf_list);
        Spinner spinner = (Spinner) findViewById(R.id.modelNameSpinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SRModelConfigurationManager.getConfiguration(conf_list[position]);
                fill_table();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });
        // nnapi
        spinner = (Spinner) findViewById(R.id.nnapi_spin);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, nnapi_str);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SRModelConfigurationManager.setConfiguration("nnapi", nnapi_str[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

        // num parallel batch
        spinner = (Spinner) findViewById(R.id.num_par_bat_spin);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, batch_str);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SRModelConfigurationManager.setConfiguration("batch", batch_str[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }

        });

    }

    private void fill_table()
    {
        SRModelConfiguration mconf  = SRModelConfigurationManager.getCurrentConfiguration();

        TextView model_height = (TextView) findViewById(R.id.tv_imageh);
        TextView model_width  = (TextView) findViewById(R.id.tv_imagew);
        TextView rescaling_f  = (TextView) findViewById(R.id.tv_rscl_f);
        TextView m_rescales   = (TextView) findViewById(R.id.tv_rescales);
        Spinner use_nnapi    = (Spinner) findViewById(R.id.nnapi_spin);
        Spinner num_par_bat  = (Spinner) findViewById(R.id.num_par_bat_spin);

        int im_height = mconf.getInputImageHeight();
        int im_width  = mconf.getInputImageHeight();
        int rescale_f = mconf.getRescalingFactor();
        boolean nnapi = mconf.getNNAPISetting();
        boolean rescl = mconf.getModelRescales();
        int num_bat   = mconf.getNumParallelBatch();


        model_height.setText(Integer.toString(im_height));
        model_width.setText(Integer.toString(im_width));
        rescaling_f.setText(Integer.toString(rescale_f));
        m_rescales.setText(Boolean.toString(rescl));
        use_nnapi.setSelection(((ArrayAdapter)(use_nnapi.getAdapter())).getPosition(nnapi?"True":"False"));
        num_par_bat.setSelection(((ArrayAdapter)(num_par_bat.getAdapter())).getPosition(Integer.toString(num_bat)));
    }
}
