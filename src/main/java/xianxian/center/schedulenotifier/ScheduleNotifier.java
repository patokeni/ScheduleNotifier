package xianxian.center.schedulenotifier;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v4.app.Fragment;
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
        //ObserverDebug.load(Schedules.dailySchedulesObservable, Schedules.schedulesObservable, Schedules.specificDaysObservable, NotifyService.onDoingScheduleItemChanged, NotifyService.onScheduleOfTodayChanged);

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
                    Fragment sn_dbFragment = fragmentManager.findFragmentByTag("sn_db");

                    fragmentManager.beginTransaction().replace(R.id.container, sn_dbFragment == null ? DashboardFragment.newInstance() : sn_dbFragment, "sn_db").addToBackStack("sn_db").commit();
                    Main.showingFragment = sn_dbFragment;
                } else if (item.getItemId() == R.id.nav_sn_notifications) {
                    Fragment sn_ncFragment = fragmentManager.findFragmentByTag("sn_nc");

                    fragmentManager.beginTransaction().replace(R.id.container, sn_ncFragment == null ? NotificationsFragment.newInstance() : sn_ncFragment, "sn_nc").addToBackStack("sn_nc").commit();
                    Main.showingFragment = sn_ncFragment;
                } else if (item.getItemId() == R.id.nav_sn_settings) {
                    Fragment sn_setFragment = fragmentManager.findFragmentByTag("sn_set");

                    fragmentManager.beginTransaction().replace(R.id.container, sn_setFragment == null ? SettingsFragment.newInstance() : sn_setFragment, "sn_set").addToBackStack("sn_set").commit();
                    Main.showingFragment = sn_setFragment;
                }
                return false;
            }
        };
    }

}
