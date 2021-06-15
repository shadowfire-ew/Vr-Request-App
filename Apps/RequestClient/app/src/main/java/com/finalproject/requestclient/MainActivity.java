package com.finalproject.requestclient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private Button sendRequest, changeName;
    private EditText time;
    private TextView username;

    SharedPreferences preferences;

    private PeriodicWorkRequest statusChecker;

    private Context context = this;

    private boolean openRequest;
    public static final String REQUEST_KEY = "requestKey";
    public static final String RID_KEY = "ridKey";

    private RequestQueue queue;

    public static String CHANNEL_ID = "vr_client_notify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        preferences = getSharedPreferences(SettingsActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);

        statusChecker = new PeriodicWorkRequest.Builder(RequestChangeChecker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS,
                PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
                .build();

        sendRequest = findViewById(R.id.requestButton);
        changeName = findViewById(R.id.settingsButton);
        time = findViewById(R.id.editTimeNumber);
        username = findViewById(R.id.usernameView);

        username.setText(preferences.getString(SettingsActivity.USERNAME_KEY,getString(R.string.placeholder_name)));
        openRequest = preferences.getBoolean(REQUEST_KEY,false);

        queue = Volley.newRequestQueue(context);

        updateRequestButton();

        changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context,SettingsActivity.class);
                startActivity(i);
            }
        });

        sendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getString(R.string.url);
                SharedPreferences.Editor edit = preferences.edit();
                if(openRequest){
                    // if the user has an open request
                    url +="cancel/"+preferences.getString(SettingsActivity.UID_KEY,null);
                    // log event for debug
                    Log.d("request button",url);
                    StringRequest cancel = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d("cancel response",response);
                                    WorkManager.getInstance(context).cancelUniqueWork("listenForChanges");
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    error.printStackTrace();
                                }
                            });
                    queue.add(cancel);
                    openRequest = false;
                }
                else{
                    // when the user doesn't have an open request
                    url += "request";
                    // going to do something with jsons and attaching them to the request
                    Log.d("request button",url);
                    // send request along volley
                    String timeText =time.getText().toString();
                    int rtime = -1;
                    try{
                        rtime = Integer.parseInt(timeText);
                    } catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(context,"please enter a time",Toast.LENGTH_SHORT).show();
                    }
                    if(rtime>=0) {
                        // prepping the json to be send to webserver
                        JSONObject json = new JSONObject();
                        try {
                            json.put("uid", preferences.getString(SettingsActivity.UID_KEY, null));
                            json.put("time", rtime);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // the json request itself
                        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            Log.d("request response", "rid: " + response.getString("rid")
                                                    + " status: " + response.getString("status"));
                                            SharedPreferences.Editor edit = preferences.edit();
                                            edit.putString(RID_KEY,response.getString("rid"));
                                            edit.apply();

                                            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                                    "listenForChanges",
                                                    ExistingPeriodicWorkPolicy.KEEP,
                                                    statusChecker);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        error.printStackTrace();
                                    }
                                });
                        queue.add(request);
                        openRequest = true;
                    }
                }
                edit.putBoolean(REQUEST_KEY,openRequest).apply();
                // update button
                updateRequestButton();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        username.setText(preferences.getString(SettingsActivity.USERNAME_KEY,getString(R.string.placeholder_name)));
        openRequest = preferences.getBoolean(REQUEST_KEY,false);
        updateRequestButton();
    }

    private void updateRequestButton(){
        if(openRequest){
            sendRequest.setText(R.string.cancel_button_label);
        }
        else{
            sendRequest.setText(R.string.request_button_label);
        }
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notify_channel_name);
            String description = getString(R.string.notify_channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,name,importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}