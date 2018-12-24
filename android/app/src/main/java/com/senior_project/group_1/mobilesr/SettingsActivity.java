package com.senior_project.group_1.mobilesr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;


public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        //Get the view From pick_photo_activity
        setContentView(R.layout.settings_activity);

        String[] conf_list = SRModelConfigurationManager.getConfigurationMapKeys();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_dropdown_item, conf_list);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
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
    }

    private void fill_table()
    {
        SRModelConfiguration mconf  = SRModelConfigurationManager.getCurrentConfiguration();

        TextView model_height = (TextView) findViewById(R.id.model_height);
        TextView model_width  = (TextView) findViewById(R.id.model_width);
        TextView rescaling_f  = (TextView) findViewById(R.id.rescaling_factor);
        TextView use_nnapi    = (TextView) findViewById(R.id.use_nnapi);
        TextView m_rescales   = (TextView) findViewById(R.id.model_rescales);

        int im_height = mconf.getInputImageHeight();
        int im_width  = mconf.getInputImageHeight();
        int rescale_f = mconf.getRescalingFactor();
        boolean nnapi = mconf.getNNAPISetting();
        boolean rescl = mconf.getModelRescales();

        model_height.setText(Integer.toString(im_height));
        model_width.setText(Integer.toString(im_width));
        rescaling_f.setText(Integer.toString(rescale_f));
        use_nnapi.setText(Boolean.toString(nnapi));
        m_rescales.setText(Boolean.toString(rescl));
    }
}
