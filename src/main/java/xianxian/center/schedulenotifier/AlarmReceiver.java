package xianxian.center.schedulenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.Locale;

import xianxian.center.MainLogger;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String SCHEDULE_ITEM_START_ACTION = "xianxian.center.schedulenotifier.alarm.ScheduleItemStartAction";
    public static final String NEXT_DAY_ACTION = "xianxian.center.schedulenotifier.alarm.nextDayAction";
    public static final String SCHEDULE_ITEM_END_ACTION = "xianxian.center.schedulenotifier.alarm.scheduleItemEndAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        Calendar alarm = (Calendar) intent.getSerializableExtra("Alarm");
        Calendar now = Calendar.getInstance();

        if (alarm != null)
            MainLogger.i("SN/AlarmReceiver", intent.getAction() + " received " +
                    "excepted time " + String.format(Locale.getDefault(), "%d-%d-%d|%d:%d",
                    alarm.get(Calendar.YEAR),
                    alarm.get(Calendar.MONTH) + 1,
                    alarm.get(Calendar.DAY_OF_MONTH),
                    alarm.get(Calendar.HOUR_OF_DAY),
                    alarm.get(Calendar.MINUTE)) + " " +
                    "now time " + String.format(Locale.getDefault(), "%d-%d-%d|%d:%d",
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE)));
        else
            MainLogger.i("SN/AlarmReceiver", intent.getAction() + " received " +
                    "at " + String.format(Locale.getDefault(), "%d-%d-%d|%d:%d",
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE)));
        context.startService(new Intent(context, NotifyService.class));

    }
}
