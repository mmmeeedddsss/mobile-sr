package com.senior_project.group_1.mobilesr.activities;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.senior_project.group_1.mobilesr.R;

import exampleclient.ExampleServerActivity;

public class ExampleServerPage extends  Activity{
    private Button send;
    private EditText ipPrompt;
    public static String serverIp;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tcpserver_xml);

        send = (Button)findViewById(R.id.enterIP);
        ipPrompt = (EditText) findViewById(R.id.ipPrompt);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serverIp =  ipPrompt.getText().toString();
                if ( !serverIp.matches("[" + .0123456789 + "]+") || serverIp.contains(" ") || serverIp.isEmpty() || serverIp.equals(null) || serverIp == null ) {
                    ipPrompt.setText("");
                    Toast.makeText(ExampleServerPage.this, "Invalid IP Address!", Toast.LENGTH_LONG).show();
                }
                else {
                    Intent i = new Intent( view.getContext(), ExampleServerActivity.class);
                    startActivity(i);
                }
            }
        });
    }

    public static String getServerIp() {
        return serverIp;
    }
}
