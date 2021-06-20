package com.pras.pomodorotimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.view.MotionEventCompat
import androidx.lifecycle.lifecycleScope
import com.pras.pomodorotimer.databinding.ActivityMainBinding
import com.pras.pomodorotimer.util.PrefUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaing: Long): Long {
            val wakeUpTime = (nowSeconds + secondsRemaing) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpireReciever::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context) {
            val intent = Intent(context, TimerExpireReciever::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState {
        Stopped, Paused, Running
    }

    enum class PomodoroState {
        Focus, Break, LongBreak
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped
    private var secondsRemaining = 0L
    private var pomodoroState = PomodoroState.Focus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("TimerStateStart", timerState.toString())

        binding.btnStart.setOnClickListener { v ->
            when (timerState) {
                TimerState.Stopped -> {
                    startTimer()
                    timerState = TimerState.Running
                }
                TimerState.Paused -> {
                    startTimer()
                    timerState = TimerState.Running
                }
                TimerState.Running -> {
                    timer.cancel()
                    timerState = TimerState.Paused
                }
            }
            Log.d("TimerStateClick", timerState.toString())
        }

        binding.btnBreak.setOnClickListener {
            if (timerState == TimerState.Running) {
                timer.cancel()
            }

            toBreakState()

            setNewTimerLength()
            PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
            secondsRemaining = timerLengthSeconds
            updateCountdownUI()

            Log.d("PomodoroState", pomodoroState.toString())
        }

        binding.btnFocus.setOnClickListener {
            if (timerState == TimerState.Running) {
                timer.cancel()
            }

            toFocusState()

            setNewTimerLength()
            PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
            secondsRemaining = timerLengthSeconds
            updateCountdownUI()
        }

        binding.btnLongbreak.setOnClickListener {
            if (timerState == TimerState.Running) {
                timer.cancel()
            }

            toLongBreakState()

            setNewTimerLength()
            PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
            secondsRemaining = timerLengthSeconds
            updateCountdownUI()
        }
    }

    private fun toFocusState() {
        timerState = TimerState.Stopped

        var transitionDrawable: TransitionDrawable? = null

        when (pomodoroState) {
            PomodoroState.Focus -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.focus)),
                    ColorDrawable(getColor(R.color.focus))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.Break -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.breakk)),
                    ColorDrawable(getColor(R.color.focus))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.LongBreak -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.longBreak)),
                    ColorDrawable(getColor(R.color.focus))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
        }

        binding.btnBreak.setBackgroundResource(R.drawable.btn_transparent)
        binding.btnFocus.setBackgroundResource(R.drawable.btn_active)
        binding.btnLongbreak.setBackgroundResource(R.drawable.btn_transparent)
        binding.screen.background = transitionDrawable
        transitionDrawable?.startTransition(200)
        pomodoroState = PomodoroState.Focus
    }

    private fun toBreakState() {
        timerState = TimerState.Stopped

        var transitionDrawable: TransitionDrawable? = null

        when (pomodoroState) {
            PomodoroState.Focus -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.focus)),
                    ColorDrawable(getColor(R.color.breakk))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.Break -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.breakk)),
                    ColorDrawable(getColor(R.color.breakk))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.LongBreak -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.longBreak)),
                    ColorDrawable(getColor(R.color.breakk))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
        }

        binding.btnBreak.setBackgroundResource(R.drawable.btn_active)
        binding.btnFocus.setBackgroundResource(R.drawable.btn_transparent)
        binding.btnLongbreak.setBackgroundResource(R.drawable.btn_transparent)
        binding.screen.background = transitionDrawable
        transitionDrawable?.startTransition(200)
        pomodoroState = PomodoroState.Break
    }

    private fun toLongBreakState() {
        timerState = TimerState.Stopped

        var transitionDrawable: TransitionDrawable? = null

        when (pomodoroState) {
            PomodoroState.Focus -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.focus)),
                    ColorDrawable(getColor(R.color.longBreak))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.Break -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.breakk)),
                    ColorDrawable(getColor(R.color.longBreak))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
            PomodoroState.LongBreak -> {
                val colorTransition = arrayOf(
                    ColorDrawable(getColor(R.color.longBreak)),
                    ColorDrawable(getColor(R.color.longBreak))
                )
                transitionDrawable = TransitionDrawable(colorTransition)
            }
        }

        binding.btnBreak.setBackgroundResource(R.drawable.btn_transparent)
        binding.btnFocus.setBackgroundResource(R.drawable.btn_transparent)
        binding.btnLongbreak.setBackgroundResource(R.drawable.btn_active)
        binding.screen.background = transitionDrawable
        transitionDrawable?.startTransition(200)
        pomodoroState = PomodoroState.LongBreak
    }

    override fun onResume() {
        super.onResume()
        initTimer()

        removeAlarm(this)
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
        } else if (timerState == TimerState.Paused) {

        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    fun initTimer() {
        timerState = PrefUtil.getTimerState(this)

        if (timerState == TimerState.Stopped)
            setNewTimerLength()
        else
            setPreviousTimerLength()

        secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        if (alarmSetTime > 0)
            secondsRemaining -= nowSeconds - alarmSetTime

        if (secondsRemaining <= 0)
            onTimerFinished()
        else if (timerState == TimerState.Running)
            startTimer()

        updateCountdownUI()
    }

    private fun onTimerFinished() {
        timerState = TimerState.Stopped
        when (pomodoroState) {
            PomodoroState.Focus -> {
                toBreakState()
            }

            PomodoroState.Break -> {
                toFocusState()
            }

            PomodoroState.LongBreak -> {
                toFocusState()
            }
        }
        setNewTimerLength()

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds
        Log.d("TimerState", timerState.toString())
        Log.d("PomodoroState", pomodoroState.toString())
        updateCountdownUI()
    }

    private fun startTimer() {
        timerState = TimerState.Running
        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }
        }.start()
    }

    private fun setNewTimerLength() {
        when (pomodoroState) {
            PomodoroState.Focus -> {
                val lengthInMinutes = PrefUtil.getPomodoroLenght(this)
                timerLengthSeconds = (lengthInMinutes * 60L)
            }
            PomodoroState.Break -> {
                val lengthInMinutes = PrefUtil.getBreakLength(this)
                timerLengthSeconds = (lengthInMinutes * 60L)
            }
            PomodoroState.LongBreak -> {
                val lengthInMinutes = PrefUtil.getLongBreakLength(this)
                timerLengthSeconds = (lengthInMinutes * 60L)
            }
        }
    }

    private fun setPreviousTimerLength() {
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
    }

    private fun updateCountdownUI() {
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr = secondsInMinuteUntilFinished.toString()
        binding.tvCountdown.text = "$minutesUntilFinished:${
            if (secondsStr.length == 2) secondsStr
            else "0" + secondsStr
        }"
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

}