package com.senior_project.group_1.mobilesr;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button pickPhotoButton, takePhotoButton, settingsButton, tutorialButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pick photo button activity
        pickPhotoButton = (Button) findViewById(R.id.buttonPickPhoto);

        // Capture Pick Photo clicks
        pickPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhotoIntent = new Intent(MainActivity.this, PickPhotoActivity.class);
                startActivity(pickPhotoIntent);
            }
        });

        // Take photo button activity
        takePhotoButton = (Button) findViewById(R.id.buttonTakePhoto);

        // Take Photo clicks
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePhotoIntent = new Intent(MainActivity.this, TakePhotoActivity.class);
                startActivity(takePhotoIntent);
            }
        });

        // Settings button activity
        settingsButton = (Button) findViewById(R.id.buttonSetttings);

        // Settings button clicks
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        // Tutorial button activity
        tutorialButton = (Button) findViewById(R.id.buttonTutorial);

        // Tutorial button clicks
        tutorialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, TutorialActivity.class);
                startActivity(settingsIntent);
            }
        });

    }
}
