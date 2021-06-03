package com.finalproject.requestclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class RequestChangeChecker extends Worker {
    private boolean waiting = true;
    private boolean open = true;
    private String statusChanged;
    private Context context;
    public RequestChangeChecker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context=context;
        Log.d("in worker","worker created");
    }

    @Override
    public Result doWork() {
        RequestQueue queue = Volley.newRequestQueue(context);
        // our waiting for response from server controll bool
        waiting = true;
        // loading shared preferences and url
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        String url = context.getString(R.string.url)+"check_status/"+preferences.getString(MainActivity.RID_KEY,null);
        // the string request
        StringRequest getStatus = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("worker listener",response);
                        waiting = false;
                        open =response.equals("open");
                        Log.d("in the work's response",response);
                        statusChanged = response;
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("in work response",error.getMessage());
                    }
                });
        queue.add(getStatus);
        while(waiting){
            // do nothing
        }
        if(!open){
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean(MainActivity.REQUEST_KEY,false);
            NotificationManager NM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notify = new Notification.Builder(getApplicationContext()).setContentText("Status changed to "+statusChanged)
                    .setContentTitle("Request status changed!").setSmallIcon(R.drawable.ic_launcher_background).build();
            NM.notify(0,notify);
            WorkManager.getInstance(context).cancelWorkById(this.getId());
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d("in the worker","work stopped");
    }
}
