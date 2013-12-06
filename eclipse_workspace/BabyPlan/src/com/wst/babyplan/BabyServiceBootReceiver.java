package com.wst.babyplan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BabyServiceBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	String action = "android.intent.action.MAIN";
        String category = "android.intent.category.LAUNCHER";
 
        // start activity ActivityTest，开机启动的activity
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.setAction(action);
        mIntent.addCategory(category);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(mIntent);
 
        /*
        // start service ServiceTest，开机启动的service
        mIntent = new Intent(context, ServiceTest.class);
        context.startService(mIntent);
        */
    }

} 
