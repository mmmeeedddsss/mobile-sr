package com.senior_project.group_1.mobilesr.activities;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.configurations.ApplicationConstants;
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
                setModel(conf_list[position]);
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

        EditText ip = (EditText) findViewById( R.id.ip );
        ip.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SRModelConfigurationManager.getCurrentConfiguration().setIPAddress( s.toString() );
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        EditText port = (EditText) findViewById( R.id.port );
        port.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    SRModelConfigurationManager.getCurrentConfiguration().setPort(Integer.parseInt(s.toString()));
                }
                catch (Exception ex)
                {}
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // set the default/stored model value on the spinner
        spinner = findViewById(R.id.modelNameSpinner);
        String model = PreferenceManager.getDefaultSharedPreferences(this).getString("model", ApplicationConstants.DEFAULT_MODEL);
        for(int i = 0; i < conf_list.length; i++) {
            if (model.equals(conf_list[i])) {
                spinner.setSelection(i);
                break;
            }
        }
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
        EditText ip = (EditText) findViewById( R.id.ip );
        EditText port = (EditText) findViewById( R.id.port );

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
        ip.setText( mconf.getIPAddress() );
        port.setText( mconf.getPort() == 0 ?"": ""+mconf.getPort());
    }

    private void setModel(String model) {
        // NOTE: this getConfig also sets the current config huh? Weird!
        SRModelConfigurationManager.switchConfiguration(model);
        fill_table();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("model", model).apply();
    }
}
