package com.finalproject.adminmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class UserRequestAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<UserRequest> userRequests;
    private TextView username;

    public UserRequestAdapter(Context context, ArrayList<UserRequest> userRequests){
        this.context = context;
        this.userRequests = userRequests;
    }

    @Override
    public int getCount() {
        return userRequests.size();
    }

    @Override
    public Object getItem(int position) {
        return userRequests.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.user_list_item_layout,
                parent, false);
        username = convertView.findViewById(R.id.textView);
        username.setText(userRequests.get(position).getUsername());
        return  convertView;
    }
}
