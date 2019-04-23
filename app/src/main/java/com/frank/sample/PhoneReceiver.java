package com.frank.sample;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, String.format("call onReceive(): context = [%s], intent = [%s]", context, intent));
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {//去电
            System.out.println("去电");

            Log.d(TAG, "去电:" + getResultData());
        } else {//来电(存在以下三种情况)
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            switch (tm.getCallState()) {
                case TelephonyManager.CALL_STATE_IDLE:
                    System.out.println("挂断");
                    Log.d(TAG, "挂断:" + getResultData());
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    System.out.println("接听");
                    Log.d(TAG, "接听:" + getResultData());
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    System.out.println("响铃");
                    Log.d(TAG, "响铃:" + getResultData());
                    break;
            }
        }
    }
}