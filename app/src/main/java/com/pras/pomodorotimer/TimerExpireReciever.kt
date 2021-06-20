package com.pras.pomodorotimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pras.pomodorotimer.util.PrefUtil

class TimerExpireReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //TODO: Show Notification

        PrefUtil.setTimerState(MainActivity.TimerState.Stopped, context)
        PrefUtil.setAlarmSetTime(0, context)
    }
}