package com.finalproject.requestclient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
    public static String TAG = "status_change_result";
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
                        if(!open){
                            doNotif();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("in work response",error.getMessage());
                    }
                });
        queue.add(getStatus);
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d("in the worker","work stopped");
    }

    private void doNotif(){
        Log.d("not open","starting process");
        // update the local status
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(MainActivity.REQUEST_KEY,false).apply();

        // make the notification manager
        NotificationManagerCompat NM = NotificationManagerCompat.from(context);

        // make the notification builder
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(getApplicationContext(),MainActivity.CHANNEL_ID);

        // creating an intent to go to main page when approved
        Intent intent = new Intent(context,MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(TAG,statusChanged);
        PendingIntent pIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        // build the notification
        Notification notify = notifyBuilder
                .setContentText("Status changed to "+statusChanged)
                .setContentTitle("Request status changed!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // for backwards compat
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

        // displaying the notification
        NM.notify(0,notify);

        //canceling the work
        WorkManager.getInstance(context).cancelWorkById(this.getId());
    }
}
