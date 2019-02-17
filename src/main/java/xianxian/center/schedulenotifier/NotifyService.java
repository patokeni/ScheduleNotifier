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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import xianxian.center.main.Callback;
import xianxian.center.main.Main;
import xianxian.center.ttsengine.TTSFactory;

/**
 * Created by xiaoyixian on 18-3-27.
 */

public class NotifyService extends Service implements Observer {
    public final static Observable onDoingScheduleItemChanged = new DataObservable();
    public final static Observable onScheduleOfTodayChanged = new DataObservable();
    public static final int NOTIFICATION_ID = 1;
    private static Schedule scheduleToday = null;
    private static ScheduleItem doingScheduleItem = null;
    private static Observable scheduleObservable;
    private Notification notification;
    private TickReceiver receiver = new TickReceiver(new Callback() {
        @Override
        public boolean Do(Object... objects) {
            onTick();
            return false;
        }
    });
    private PendingIntent pendingIntent;

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

        onScheduleOfTodayChanged.addObserver(this);
        //NOTE: 因为TIME_TICK必须使用动态注册
        //注册广播接收器
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        pendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, new Intent(this.getApplicationContext(), NotifyService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 2, pendingIntent);
    }

    public void onTick() {
        checkNotify();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduleObservable.deleteObserver(this);
        onDoingScheduleItemChanged.deleteObservers();
        onScheduleOfTodayChanged.deleteObservers();
        //注销广播接收器
        unregisterReceiver(receiver);
        //停止前台服务
        stopForeground(true);
        //取消所有的Notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
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
        //先获取今天的计划表
        Schedule schedule = Schedules.getScheduleToday();

        //如果今天的计划不是null
        if (schedule != null) {
            //如果缓存的计划表不是null或今天的计划表不等于缓存的计划表
            if (scheduleToday == null || schedule != scheduleToday)
                //发布修改
                onScheduleOfTodayChanged.notifyObservers(schedule);
        }

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
                    Main.getMainActivity().vibrate(2000);
                    //获得模块对应的TTS引擎，并播放语音
                    TTSFactory.getEngine("sn").textToSpeech(nowScheduleItem.getMessage(), Locale.CHINESE);
                }
            }
        }

        //更新提醒
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //当点击提醒时，跳出MainActivity
        Intent intent = new Intent(this.getApplicationContext(), Main.getMainActivity().getClass());
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
        }
    }
}
