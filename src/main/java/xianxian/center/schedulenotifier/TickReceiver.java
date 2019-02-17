package xianxian.center.schedulenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import xianxian.center.main.Callback;

public class TickReceiver extends BroadcastReceiver {
    Callback callback;

    public TickReceiver(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // TODO: an Intent broadcast.
        if (!NotifyService.isNotifyServiceRunning(context))
            context.startService(new Intent(context, NotifyService.class));
        //当Tick时
        if (callback != null)
            callback.Do();
    }
}
