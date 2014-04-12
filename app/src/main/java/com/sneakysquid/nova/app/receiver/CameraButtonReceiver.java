package com.sneakysquid.nova.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by luke on 3/25/14.
 */
public class CameraButtonReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        //start activity
        Intent i = new Intent();
        i.setClassName("com.sneakysquid.nova.app.activity", "com.sneakysquid.nova.app.activity.NovaCamera");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
