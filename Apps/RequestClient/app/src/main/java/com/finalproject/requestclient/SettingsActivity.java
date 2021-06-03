package com.finalproject.requestclient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.lang.reflect.Array;
import java.util.Random;

public class SettingsActivity extends AppCompatActivity {

    public static final String USERNAME_KEY = "usernameKey";
    public static final String UID_KEY = "uidKey";
    public static final String PREFERENCES_KEY = "myPrefs";

    SharedPreferences preferences;

    // used when user is new, allows us to put to the user collection
    private boolean newUser;

    private EditText name;
    private Button submit;

    private Context context = this;

    private String[] placeholderUIDs;

    private boolean usingPlaceholders = false;
    private boolean inDemo = false; // this bool will let me keep multiple open requests on one device

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        name = findViewById(R.id.editName);
        submit = findViewById(R.id.submitButton);

        preferences = getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE);

        newUser = !preferences.contains(USERNAME_KEY);

        name.setText(preferences.getString(USERNAME_KEY,getString(R.string.placeholder_name)));

        placeholderUIDs = getResources().getStringArray(R.array.placeholder_uids);

        queue = Volley.newRequestQueue(context);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick","in here");
                SharedPreferences.Editor edit = preferences.edit();
                String username = name.getText().toString();
                if(newUser){
                    // put a new user to the webserver / db
                    // get back the user id and add it to the shared preferences
                    String uid = addNewUser();
                    Log.d("on click","new uid: "+uid);
                    newUser=false;
                }
                else{
                    // update the users name in the database
                    String uid = preferences.getString(UID_KEY,null);
                    // putting the uid and new name into a json object to be sent
                    JSONObject json = new JSONObject();
                    try{
                        json.put("username",username);
                        json.put("uid",uid);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                    String url = getString(R.string.url)+"update_user";
                    JsonObjectRequest update = new JsonObjectRequest(Request.Method.PUT, url, json,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        Log.d("updated user status", response.getString("response"));
                                    } catch (JSONException e){
                                        e.printStackTrace();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.d("updated user status",error.getMessage());
                                }
                            });
                    queue.add(update);
                }
                // once that is done, save the name to the preferences
                edit.putString(USERNAME_KEY,username);
                // then apply
                edit.apply();
                // then pop up toast which shows that user has changed name
                Toast.makeText(context,"Name Updated",Toast.LENGTH_SHORT).show();
            }
        });
        submit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("on loncg click","hello");
                // going to use this to request new user id
                // will cancel current request and set new user tag
                boolean openRequest = preferences.getBoolean(MainActivity.REQUEST_KEY,false);
                String text = "acquiring new uid";
                if(openRequest){
                    // cancel the open request here
                    preferences.edit().putBoolean(MainActivity.REQUEST_KEY,false).apply();
                    // edit text to show that current request was cancelled
                    if(!inDemo){
                        // when i am in the demo, i can send multiple requests from the same phone
                        // otherwise, requesting a new uid will cancel your open request
                        text += " and cancelled old request";
                        // cancel request with uid of old uid
                        String url = getString(R.string.url)+"cancel/"+preferences.getString(UID_KEY,null);
                        StringRequest cancel = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("long click response",response);
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.d("long clock respopnse",error.getMessage());
                                    }
                                });
                        queue.add(cancel);
                    }
                }
                // do the rest of the updating here
                newUser = true;
                Toast.makeText(context,text,Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    String addNewUser(){
        // this function will do the heavy lifting of doing the string request
        if(usingPlaceholders){
            Random random = new Random();
            return placeholderUIDs[Math.abs(random.nextInt())%4];
        }
        else{
            String url = getString(R.string.url) + "add_new_user";
            JSONObject json = new JSONObject();
            try{
                json.put("username",name.getText().toString());
                json.put("device","testing");
            } catch (JSONException e){
                e.printStackTrace();
            }
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            SharedPreferences.Editor edit = preferences.edit();
                            try {
                                String rvalHolder = response.getString("uid");
                                edit.putString(UID_KEY,rvalHolder);
                            } catch (JSONException e){
                                e.printStackTrace();
                            }
                            edit.apply();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("new user erre",error.getMessage());
                        }
                    });
            queue.add(request);
            return "waiting";
        }
    }
}