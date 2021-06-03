package com.finalproject.adminmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class Details extends AppCompatActivity {

    private TextView username;
    private TextView uid;
    private TextView time;

    private Button approve;
    private Button cancel;

    private String responseURL;

    private Context context = this;

    public static final String CHANGED = "com.finalproject.adminmanager.result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        username = findViewById(R.id.userName);
        time = findViewById(R.id.timeAmount);
        uid = findViewById(R.id.uidView);

        approve = findViewById(R.id.approveButton);
        cancel = findViewById(R.id.cancelButton);

        Intent i = getIntent();
        uid.setText(i.getStringExtra(MainActivity.UID_TAG));
        username.setText(i.getStringExtra(MainActivity.USERNAME_TAG));
        time.setText(i.getStringExtra(MainActivity.TIME_TAG));

        approve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                responseURL = context.getString(R.string.url)+"approve/";
                sendResponse();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                responseURL = context.getString(R.string.url)+"deny/";
                sendResponse();
            }
        });
    }

    private void sendResponse(){
        // this function will be called by both on click listeners
        // send response with url
        responseURL = responseURL+uid.getText();
        Log.d("in sendResponse",responseURL);
        // sending the request
        RequestQueue queue = Volley.newRequestQueue(context);
        StringRequest update = new StringRequest(Request.Method.POST, responseURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("response to change",response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("response to change",error.getMessage());
                    }
                });
        queue.add(update);

        // the return intents
        Intent returnIntent = new Intent();
        returnIntent.putExtra(CHANGED,true);
        setResult(Activity.RESULT_OK,returnIntent);
        this.finish();
    }
}