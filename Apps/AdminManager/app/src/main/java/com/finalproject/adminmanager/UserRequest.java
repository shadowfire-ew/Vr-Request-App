package com.finalproject.adminmanager;

import org.json.JSONException;
import org.json.JSONObject;

public class UserRequest {
    private String username, uid;
    private int time;
    public UserRequest(String uid, String username, int time){
        this.time = time;
        this.uid = uid;
        this.username = username;
    }
    public UserRequest(JSONObject json){
        try{
            this.time = json.getInt("time");
            this.uid = json.getJSONObject("user").getString("_id");
            this.username = json.getJSONObject("user").getString("username");
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getTime() {
        return time;
    }

    public String getTimeString(){
        return String.valueOf(time)+" min";
    }

    public void setTime(int time) {
        this.time = time;
    }
}
