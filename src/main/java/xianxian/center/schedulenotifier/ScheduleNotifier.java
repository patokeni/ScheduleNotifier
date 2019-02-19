package xianxian.center.schedulenotifier;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import xianxian.center.main.Callback;
import xianxian.center.main.IModule;
import xianxian.center.main.Main;
import xianxian.center.ttsengine.TTSFactory;

public class ScheduleNotifier implements IModule {
    private Context context;

    @Override
    public void init(Context context) {
        this.context = context;
        Schedules.load();

        TTSFactory.createTTSEngine("sn", context).setAudioStream(AudioManager.STREAM_NOTIFICATION);

        //本来我准备延时启动Service，后来改为阻塞
//        new Thread(()->{
//            try {
//                Thread.sleep(1000 * 15);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            Intent intent = new Intent(context, NotifyService.class);
//            context.startService(intent);
//        }).start();
        Intent intent = new Intent(context, NotifyService.class);
        context.startService(intent);
    }

    @Override
    public int menuRes() {
        return R.menu.navi_schedulenotifier;
    }

    @Override
    public Callback getOnNavigationItemSelectedListener() {
        return new Callback() {
            @Override
            public boolean Do(Object... objects) {
                FragmentManager fragmentManager = (FragmentManager) objects[0];
                MenuItem item = (MenuItem) objects[1];
                if (item.getItemId() == R.id.nav_sn_dashboard) {
                    Main.getMainActivity().setShowingFragment("sn_db", DashboardFragment.class);
                } else if (item.getItemId() == R.id.nav_sn_notifications) {
                    Main.getMainActivity().setShowingFragment("sn_nc", NotificationsFragment.class);
                } else if (item.getItemId() == R.id.nav_sn_types) {
                    Main.getMainActivity().setShowingFragment("sn_types", TypesFragment.class);
                } else if (item.getItemId() == R.id.nav_sn_settings) {
                    Main.getMainActivity().setShowingFragment("sn_set", SettingsFragment.class);
                }
                return false;
            }
        };
    }

}
