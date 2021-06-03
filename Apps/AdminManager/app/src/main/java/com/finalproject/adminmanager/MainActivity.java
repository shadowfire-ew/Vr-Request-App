package com.finalproject.adminmanager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // arrays used in temporary
    private String[] usernameList;
    private String[] uidList;
    private int[] timeList;

    private ArrayList<UserRequest> userRequests = new ArrayList<>();

    private ListView theListView;

    private Context context = this;

    public static String UID_TAG = "com.finalproject.adminmanager.uid";
    public static String USERNAME_TAG = "com.finalproject.adminmanager.username";
    public static String TIME_TAG = "com.finalproject.adminmanager.time";

    private boolean usePlaceholders = false;
    private boolean updatingList;

    private RequestQueue queue;

    private UserRequestAdapter uAdapter;
    private ArrayAdapter<String> adapter;

    public static final int ACTIVITY_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(context);

        updatingList = false; // getting a new list
        requestNames();

        theListView =findViewById(R.id.user_list_view);



        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // preparing to go to the details activity
                Intent i = new Intent(context,Details.class);
                // this seems kind of counter intuitive, but it will save on network requests
                UserRequest creq = userRequests.get(position);
                i.putExtra(UID_TAG, creq.getUid());
                i.putExtra(TIME_TAG, creq.getTimeString());
                i.putExtra(USERNAME_TAG,creq.getUsername());

                startActivityForResult(i,ACTIVITY_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ACTIVITY_CODE){
            if(resultCode== Activity.RESULT_OK){
                Log.d("on activity result","result ok, changing list");
                updatingList = true;
                requestNames();
            }
            else if(resultCode==Activity.RESULT_CANCELED){
                Log.d("on activity result","activity cancelled");
            }
            else{
                Log.d("on activity result","how did we get here?");
            }
        }
    }

    private void requestNames(){
        // function which will eventually get list of usernames from webserver
        if(usePlaceholders){
            uidList = getResources().getStringArray(R.array.placeholder_uids);
            usernameList = getResources().getStringArray(R.array.placeholder_usernames);
            timeList = new int[] {15,10,40};
            for(int i = 0;i<3;i++){
                userRequests.add(new UserRequest(uidList[i],usernameList[i],timeList[i]));
            }
            adapter = new ArrayAdapter<String>(this, R.layout.user_list_item_layout,usernameList);
            // might change this to use custom userRequest class and adapter
            theListView.setAdapter(adapter);
        }
        else{
            // getting the requests from server
            // will probably redo how the intent and adapter are handled when i get to this
            String url = getString(R.string.url)+"get_open_requests";
            JsonArrayRequest getOpenRequests = new JsonArrayRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            userRequests.clear();
                            for(int i = 0; i<response.length(); i++){
                                try {
                                    JSONObject temp = response.getJSONObject(i);
                                    userRequests.add(new UserRequest(temp));
                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                            whenJsonReturns();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
            queue.add(getOpenRequests);
        }
    }

    private void whenJsonReturns(){
        if(updatingList){
            uAdapter.notifyDataSetChanged();
        }
        else{
            // instance adapter
            uAdapter = new UserRequestAdapter(context,userRequests);
            // attach adapter
            theListView.setAdapter(uAdapter);
        }
    }
}