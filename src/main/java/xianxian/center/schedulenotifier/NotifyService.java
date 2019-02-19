package xianxian.center.schedulenotifier;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import xianxian.center.ttsengine.TTSFactory;
import xianxian.center.utils.ContextUtils;

/**
 * Created by xiaoyixian on 18-3-27.
 */

public class NotifyService extends Service implements Observer {
    public static final String TAG = "SN/NotifyService";

    public final static Observable onDoingScheduleItemChanged = new DataObservable(TAG + "/OnDoingScheduleItemChanged");
    public final static Observable onScheduleOfTodayChanged = new DataObservable(TAG + "/OnScheduleOfTodayChanged");
    public static final int NOTIFICATION_ID = 1;

    private static Schedule scheduleToday = null;
    private static ScheduleItem doingScheduleItem = null;
    private static Observable scheduleObservable;
    
    private Notification notification;
    private PendingIntent scheduleStartIntent;
    private PendingIntent scheduleEndIntent;
    private Calendar nextAlarm;
    private Calendar endAlarm;
    private PowerManager.WakeLock wakeLock;
    private AlarmReceiver alarmReceiver = new AlarmReceiver();

    {
        ObserverDebug.debug(onDoingScheduleItemChanged, onScheduleOfTodayChanged);
    }

    public static boolean isNotifyServiceRunning(final Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> list = activityManager.getRunningServices(100);
        for (ActivityManager.RunningServiceInfo runningService :
                list) {
            if (runningService.service.getClassName().equals(NotifyService.class.getName()))
                return true;
        }
        return false;
    }

    public static Schedule getScheduleToday() {
        return scheduleToday;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScheduleNotifier:NotifyWakeLock");
        onScheduleOfTodayChanged.addObserver(this);
        Schedules.dailySchedulesObservable.addObserver(this);
        Schedules.specificDaysObservable.addObserver(this);
        Log.i(TAG, "Service is running");
        //先获取今天的计划表
        Schedule schedule = Schedules.getScheduleToday();

        //如果今天的计划不是null
        if (schedule != null) {
            //如果缓存的计划表不是null或今天的计划表不等于缓存的计划表
            if (scheduleToday == null || schedule != scheduleToday)
                //发布修改
                onScheduleOfTodayChanged.notifyObservers(schedule);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmReceiver.SCHEDULE_ITEM_START_ACTION);
        filter.addAction(AlarmReceiver.NEXT_DAY_ACTION);
        filter.addAction(AlarmReceiver.SCHEDULE_ITEM_END_ACTION);
        registerReceiver(alarmReceiver, filter);
    }

    public void onTick() {
        checkNotify();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //wakeLock.release();
        wakeLock = null;
        if (scheduleObservable != null)
            scheduleObservable.deleteObserver(this);
        onDoingScheduleItemChanged.deleteObservers();
        onScheduleOfTodayChanged.deleteObservers();
        //注销广播接收器
        unregisterReceiver(alarmReceiver);
        //停止前台服务
        stopForeground(true);
        //取消所有的Notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        //AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        //alarmManager.cancel(scheduleStartIntent);
    }

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkNotify();
        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    private void checkNotify() {
        Log.i(TAG, "Checking Notify start");
        checkNotify0();
        Log.i(TAG, "Checking Notify end");
    }

    private void checkNotify0() {
        //防止未提醒就进入休眠
        wakeLock.acquire(1000 * 60 * 2);

        ScheduleItem nowScheduleItem = null;
        try {
            //获得现在的计划
            nowScheduleItem = Schedules.getDoingScheduleItem();
        } catch (Schedule.NoScheduleItemException | Schedule.NoScheduleException e) {
            //no-op
        }
        //如果现在的计划不是null
        if (nowScheduleItem != null) {
            //如果之前的计划不是null，且现在计划的ID不等于之前计划的ID,且两个项目在同一个计划表里面
            if (doingScheduleItem == null || (nowScheduleItem.getId() != doingScheduleItem.getId() && doingScheduleItem.parent == nowScheduleItem.parent)) {
                //那么将之前的计划设置为现在的计划
                doingScheduleItem = nowScheduleItem;
                onDoingScheduleItemChanged.notifyObservers(doingScheduleItem);
                //如果需要提醒
                if (nowScheduleItem.isNeedNotify()) {
                    ContextUtils.vibrate(2000);
                    //获得模块对应的TTS引擎，并播放语音
                    TTSFactory.getEngine("sn").textToSpeech(nowScheduleItem.getMessage(), Locale.CHINESE);
                }
                ScheduleItem nextScheduleItem = scheduleToday.getNextScheduleItem();
                if (nextScheduleItem == scheduleToday.tail || nextScheduleItem == null) {
                    setNextDayAlarm();
                } else {
                    setStartAlarm(nextScheduleItem);
                }
                setEndAlarm(nowScheduleItem);
            }
        } else {
            ScheduleItem nextScheduleItem = scheduleToday.getNextScheduleItem();
            if (nextScheduleItem != scheduleToday.tail && nextScheduleItem != null) {
                setStartAlarm(nextScheduleItem);
            } else {
                setNextDayAlarm();
            }
        }

        //更新提醒
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //当点击提醒时，跳出MainActivity
        Intent intent = null;
        try {
            //暂时不想解决这个问题
            intent = new Intent(this.getApplicationContext(), Class.forName("xianxian.center.main.MainActivity"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder;

        String scheduleItemInfo = doingScheduleItem != null ? "正在" + doingScheduleItem.getType() : "当前无计划";

        //尝试兼容Android O(但也许不工作，我没有8.0的机子，没法测试)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("channel_1", "ScheduleNotifier", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(mChannel);
            builder = new NotificationCompat.Builder(this.getApplicationContext(), mChannel.getId())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("计划通知")
                    .setContentText(scheduleItemInfo + " " + "今天的计划表为" + scheduleToday.getName())
                    .setContentIntent(pendingIntent);
        } else {
            builder = new NotificationCompat.Builder(this.getApplicationContext())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("计划通知")
                    .setContentText(scheduleItemInfo + " " + "今天的计划表为" + scheduleToday.getName())
                    .setContentIntent(pendingIntent);
        }
        notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o == onScheduleOfTodayChanged) {
            if (scheduleToday != null)
                scheduleObservable.deleteObserver(this);
            scheduleToday = (Schedule) arg;
            doingScheduleItem = null;
            scheduleObservable = scheduleToday.scheduleObservable;
            scheduleObservable.addObserver(this);
            checkNotify();
        } else if (o == scheduleObservable) {
            checkNotify();
        } else if (o == Schedules.dailySchedulesObservable) {
            checkNotify();
        } else if (o == Schedules.specificDaysObservable) {
            checkNotify();
        }
    }

    private void setAlarm(long time, PendingIntent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent);
        } else {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, intent);
        }
    }

    private void setStartAlarm(ScheduleItem scheduleItem) {
        cancelStartAlarm();
        scheduleStartIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, new Intent(AlarmReceiver.SCHEDULE_ITEM_START_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar nextCalendar = Calendar.getInstance();
        nextCalendar.setTime(scheduleItem.getStartTimeDate());

        Calendar nextAlarmTime = Calendar.getInstance();
        nextAlarmTime.set(Calendar.HOUR_OF_DAY, nextCalendar.get(Calendar.HOUR_OF_DAY));
        nextAlarmTime.set(Calendar.MINUTE, nextCalendar.get(Calendar.MINUTE));
        nextAlarmTime.set(Calendar.SECOND, 0);
        nextAlarmTime.set(Calendar.MILLISECOND, 0);
        setAlarm(nextAlarmTime.getTimeInMillis(), scheduleStartIntent);
        nextAlarm = nextAlarmTime;

        Log.i(TAG, "Next Schedule Item Alarm has scheduled " + nextAlarmTime.toString());

    }

    private void setNextDayAlarm() {
        cancelStartAlarm();
        scheduleStartIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, new Intent(AlarmReceiver.NEXT_DAY_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setAlarm(calendar.getTimeInMillis(), scheduleStartIntent);
        nextAlarm = calendar;
        Log.i(TAG, "Next day alarm has scheduled " + calendar.toString());
    }

    private void setEndAlarm(ScheduleItem scheduleItem) {
        cancelEndAlarm();
        scheduleEndIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, new Intent(AlarmReceiver.SCHEDULE_ITEM_END_ACTION), PendingIntent.FLAG_CANCEL_CURRENT);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(scheduleItem.getEndTimeDate());
        Calendar endAlarmTime = Calendar.getInstance();
        endAlarmTime.set(Calendar.HOUR_OF_DAY, endCalendar.get(Calendar.HOUR_OF_DAY));
        endAlarmTime.set(Calendar.MINUTE, endCalendar.get(Calendar.MINUTE));
        endAlarmTime.set(Calendar.SECOND, 0);
        endAlarmTime.set(Calendar.MILLISECOND, 0);
        setAlarm(endAlarmTime.getTimeInMillis(), scheduleEndIntent);
        endAlarm = endAlarmTime;
        Log.i(TAG, "The Schedule Item end with " + endAlarmTime.toString());
    }

    private void cancelStartAlarm() {
        if (scheduleStartIntent != null) {
            ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(scheduleStartIntent);
            scheduleStartIntent = null;
            Log.i(TAG, "Alarm " + nextAlarm + " canceled");
        }
    }

    private void cancelEndAlarm() {
        if (scheduleEndIntent != null) {
            ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(scheduleEndIntent);
            scheduleEndIntent = null;
            Log.i(TAG, "Alarm " + endAlarm + " canceled");
        }
    }
}
