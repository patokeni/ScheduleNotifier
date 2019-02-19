package xianxian.center.schedulenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String SCHEDULE_ITEM_START_ACTION = "xianxian.center.schedulenotifier.alarm.ScheduleItemStartAction";
    public static final String NEXT_DAY_ACTION = "xianxian.center.schedulenotifier.alarm.nextDayAction";
    public static final String SCHEDULE_ITEM_END_ACTION = "xianxian.center.schedulenotifier.alarm.scheduleItemEndAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("SN/AlarmReceiver", intent.getAction() + " received");
        context.startService(new Intent(context, NotifyService.class));
    }
}
