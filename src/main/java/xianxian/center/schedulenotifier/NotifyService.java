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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import xianxian.center.MainLogger;
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
    private PendingIntent nextAlarmIntent;
    private AlarmManager.AlarmClockInfo nextAlarm;
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
        MainLogger.i(TAG, "Notify Service is running");
        //先获取今天的计划表
        Schedule schedule = Schedules.getScheduleToday();

        //如果今天的计划不是null
        if (schedule != null) {
            //如果缓存的计划表不是null或今天的计划表不等于缓存的计划表
            if (scheduleToday == null || schedule != scheduleToday)
                //发布修改
                onScheduleOfTodayChanged.notifyObservers(schedule);
        }


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
        MainLogger.i(TAG, "Checking Notify start");
        checkNotify0();
        MainLogger.i(TAG, "Checking Notify end");
    }

    //TODO: 考虑增加一个Activity来显示目前提醒
    private void checkNotify0() {
        //防止未提醒就进入休眠
        wakeLock.acquire(1000 * 30);

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
                    ContextUtils.vibrateRepeat(new long[]{500,1000,500,1000,500,1000,500,1000},-1);
                    //获得模块对应的TTS引擎，并播放语音
                    TTSFactory.getEngine("sn").textToSpeech(nowScheduleItem.getMessage(), Locale.CHINESE);
                    MainLogger.i(TAG,"Vibrate and Text to speech for "+nowScheduleItem);
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
        updateNotification(nowScheduleItem);
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

    private void setAlarm(AlarmManager.AlarmClockInfo clockInfo, PendingIntent intent) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setAlarmClock(clockInfo,intent);
    }

    private void setStartAlarm(ScheduleItem scheduleItem) {
        cancelNextAlarm();
        Calendar nextCalendar = Calendar.getInstance();
        nextCalendar.setTime(scheduleItem.getStartTimeDate());

        Calendar nextAlarmTime = Calendar.getInstance();
        nextAlarmTime.set(Calendar.HOUR_OF_DAY, nextCalendar.get(Calendar.HOUR_OF_DAY));
        nextAlarmTime.set(Calendar.MINUTE, nextCalendar.get(Calendar.MINUTE));
        nextAlarmTime.set(Calendar.SECOND, 0);
        nextAlarmTime.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(AlarmReceiver.SCHEDULE_ITEM_START_ACTION);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(getPackageName());
        intent.putExtra("Alarm",nextAlarmTime);
        nextAlarmIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager.AlarmClockInfo nextDayAlarm = new AlarmManager.AlarmClockInfo(nextAlarmTime.getTimeInMillis(),null);
        setAlarm(nextDayAlarm, nextAlarmIntent);
        this.nextAlarm = nextDayAlarm;
        MainLogger.i(TAG, "Next Schedule Item Alarm has scheduled at " +
                String.format(Locale.getDefault(),"%d-%d-%d|%d:%d",
                        nextAlarmTime.get(Calendar.YEAR),
                        nextAlarmTime.get(Calendar.MONTH) + 1,
                        nextAlarmTime.get(Calendar.DAY_OF_MONTH),
                        nextAlarmTime.get(Calendar.HOUR_OF_DAY),
                        nextAlarmTime.get(Calendar.MINUTE))
        );

    }

    private void setNextDayAlarm() {
        cancelNextAlarm();
        Calendar nextDayAlarmTime = Calendar.getInstance();
        nextDayAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
        nextDayAlarmTime.set(Calendar.HOUR_OF_DAY, 0);
        nextDayAlarmTime.set(Calendar.MINUTE, 0);
        nextDayAlarmTime.set(Calendar.SECOND, 0);
        nextDayAlarmTime.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(AlarmReceiver.NEXT_DAY_ACTION);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(getPackageName());
        intent.putExtra("Alarm",nextDayAlarmTime);
        nextAlarmIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager.AlarmClockInfo nextAlarm = new AlarmManager.AlarmClockInfo(nextDayAlarmTime.getTimeInMillis(),null);
        setAlarm(nextAlarm, nextAlarmIntent);
        this.nextAlarm = nextAlarm;
        MainLogger.i(TAG, "Next day alarm has scheduled at " +
                String.format(Locale.getDefault(),"%d-%d-%d|%d:%d",
                        nextDayAlarmTime.get(Calendar.YEAR),
                        nextDayAlarmTime.get(Calendar.MONTH) + 1,
                        nextDayAlarmTime.get(Calendar.DAY_OF_MONTH),
                        nextDayAlarmTime.get(Calendar.HOUR_OF_DAY),
                        nextDayAlarmTime.get(Calendar.MINUTE))
        );
    }

    private void setEndAlarm(ScheduleItem scheduleItem) {
        cancelNextAlarm();
        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(scheduleItem.getEndTimeDate());
        Calendar endAlarmTime = Calendar.getInstance();
        endAlarmTime.set(Calendar.HOUR_OF_DAY, endCalendar.get(Calendar.HOUR_OF_DAY));
        endAlarmTime.set(Calendar.MINUTE, endCalendar.get(Calendar.MINUTE));
        endAlarmTime.set(Calendar.SECOND, 0);
        endAlarmTime.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(AlarmReceiver.SCHEDULE_ITEM_END_ACTION);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setPackage(getPackageName());
        intent.putExtra("Alarm",endAlarmTime);
        nextAlarmIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager.AlarmClockInfo endAlarm = new AlarmManager.AlarmClockInfo(endAlarmTime.getTimeInMillis(),null);
        setAlarm(endAlarm, nextAlarmIntent);
        this.nextAlarm = endAlarm;

        MainLogger.i(TAG, "The Schedule Item end with " + String.format(Locale.getDefault(),"%d-%d-%d|%d:%d",
                endAlarmTime.get(Calendar.YEAR),
                endAlarmTime.get(Calendar.MONTH) + 1,
                endAlarmTime.get(Calendar.DAY_OF_MONTH),
                endAlarmTime.get(Calendar.HOUR_OF_DAY),
                endAlarmTime.get(Calendar.MINUTE))
        );
    }

    private void cancelNextAlarm() {
        if (nextAlarmIntent != null) {
            ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(nextAlarmIntent);
            nextAlarmIntent = null;
            Calendar nextAlarmTime = Calendar.getInstance();
            nextAlarmTime.setTimeInMillis(this.nextAlarm.getTriggerTime());
            MainLogger.i(TAG, "Alarm " + String.format(Locale.getDefault(),"%d-%d-%d|%d:%d",
                    nextAlarmTime.get(Calendar.YEAR),
                    nextAlarmTime.get(Calendar.MONTH),
                    nextAlarmTime.get(Calendar.DAY_OF_MONTH),
                    nextAlarmTime.get(Calendar.HOUR_OF_DAY),
                    nextAlarmTime.get(Calendar.MINUTE)) + " canceled");
            this.nextAlarm = null;
        }
    }

    private void updateNotification(ScheduleItem scheduleItem) {
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

        String scheduleItemInfo = scheduleItem != null ? "正在" + scheduleItem.getType().getTypeName() : "当前无计划";

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
}
