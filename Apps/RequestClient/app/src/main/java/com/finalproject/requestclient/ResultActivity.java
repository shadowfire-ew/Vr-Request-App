package com.finalproject.requestclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        tv = findViewById(R.id.result);

        Intent i = getIntent();

        String result = i.getStringExtra(RequestChangeChecker.TAG);

        tv.setText("Your status is now: "+result);
    }
}