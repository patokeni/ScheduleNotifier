package xianxian.center.schedulenotifier;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Spinner;

import java.text.ParseException;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.main.IFragment;
import xianxian.center.main.Main;
import xianxian.center.utils.DateUtils;

public class DashboardFragment extends Fragment implements Observer, IFragment {
    private final Observable dailySchedulesObservable = Schedules.dailySchedulesObservable;
    private final Observable specificDaysObservable = Schedules.specificDaysObservable;
    @BindView(R2.id.listViewScheduleOfDay)
    ListView listViewScheduleOfDay;
    @BindView(R2.id.calendarView)
    CalendarView calendarView;
    Adapters.ScheduleItemAdapter notificationAdapter;
    private Unbinder unbinder;
    private Callback onContextMenuSelected;
    private int contextMenuRes;
    private String contextMenuTitle;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.content_sn_dashboard, container, false);
        unbinder = ButterKnife.bind(this, view);
        calendarView.setOnDateChangeListener(this::onCalendarViewDateChange);
        registerForContextMenu(calendarView);
        notificationAdapter = new Adapters.ScheduleItemAdapter(getContext());
        listViewScheduleOfDay.setAdapter(notificationAdapter);
        return view;
    }

    private void onCalendarViewDateChange(CalendarView view, int year, int month, int dayOfMonth) {
        //NOTE:因为month范围(0~11)
        int actuallyMonth = month + 1;

        Schedule schedule = null;
        try {
            schedule = Schedules.getScheduleByDate(DateUtils.format(year, actuallyMonth, dayOfMonth));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //TTSFactory.getEngine("sn").textToSpeech(DateUtils.format(year, actuallyMonth, dayOfMonth),Locale.CHINESE);
        this.getContext().startService(new Intent(this.getContext(), NotifyService.class));
        showContextMenu(view, "这天的计划表为:" + schedule.getName(), R.menu.menu_sn_calendar_operation, new Callback() {
            @Override
            public boolean Do(Object... objects) {
                MenuItem item = (MenuItem) objects[0];

                if (item.getItemId() == R.id.menu_sn_calendar_oper_set_schedule) {
                    View dialogView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_sn_calendar_set_schedule, null);
                    Spinner spinner = dialogView.findViewById(R.id.spinner);
                    spinner.setAdapter(new Adapters.ScheduleAdapter(getContext()).withEmpty());
                    new AlertDialog.Builder(getContext())
                            .setTitle("更改这天的计划表")
                            .setCancelable(true)
                            .setView(dialogView)
                            .setPositiveButton("确认", ((dialog, which) -> {
                                Schedule selectedSchedule = (Schedule) spinner.getSelectedItem();
                                Schedules.addSpecificDay(DateUtils.format(year, actuallyMonth, dayOfMonth), selectedSchedule);
                                Main.getMainActivity().prepareSnackbar(String.format(Locale.getDefault(), "已将%d年%d月%d日的计划设置为%s", year, actuallyMonth, dayOfMonth, selectedSchedule.getName()), Snackbar.LENGTH_INDEFINITE)
                                        .setAction("撤销", (View v) -> {
                                            Schedules.removeSpecificDay(DateUtils.format(year, actuallyMonth, dayOfMonth));
                                        })
                                        .show();
                                ((Adapters.ScheduleAdapter) spinner.getAdapter()).onDestroy();
                            }))
                            .setNegativeButton("取消", ((DialogInterface dialog, int which) -> {
                                ((Adapters.ScheduleAdapter) spinner.getAdapter()).onDestroy();
                                dialog.dismiss();
                            }))
                            .show();
                } else if (item.getItemId() == R.id.menu_sn_calendar_oper_set_default) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("设置默认")
                            .setMessage(String.format(Locale.getDefault(), "你确认要将%d年%d月%d日设置默认(%s)吗？", year, actuallyMonth, dayOfMonth, Schedules.getDailyScheduleByDate(DateUtils.format(year, actuallyMonth, dayOfMonth))))
                            .setCancelable(true)
                            .setPositiveButton("确认", (DialogInterface dialog, int which) -> {
                                Schedule old = Schedules.removeSpecificDay(DateUtils.format(year, actuallyMonth, dayOfMonth));
                                if (old != null) {
                                    Main.getMainActivity().prepareSnackbar(String.format(Locale.getDefault(), "已将%d年%d月%d日的计划设置为%s", year, actuallyMonth, dayOfMonth, Schedules.getDailyScheduleByDate(DateUtils.format(year, actuallyMonth, dayOfMonth))), Snackbar.LENGTH_INDEFINITE)
                                            .setAction("撤销", (View v) -> Schedules.addSpecificDay(DateUtils.format(year, actuallyMonth, dayOfMonth), old))
                                            .show();
                                }
                            })
                            .setNegativeButton("取消", (DialogInterface dialog, int which) -> {
                            })
                            .show();
                }
                return false;
            }
        });
    }

    private void showContextMenu(View view, String title, @MenuRes int menuRes, Callback onContextMenuSelected) {
        this.onContextMenuSelected = onContextMenuSelected;
        this.contextMenuTitle = title;
        this.contextMenuRes = menuRes;
        view.showContextMenu();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (onContextMenuSelected != null)
            onContextMenuSelected.Do(item);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(contextMenuRes, menu);
        menu.setHeaderTitle(contextMenuTitle);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        notificationAdapter.onDestroy();
        dailySchedulesObservable.deleteObserver(this);
        specificDaysObservable.deleteObserver(this);
        onContextMenuSelected = null;
        notificationAdapter = null;
        unbinder.unbind();
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
        if (o == dailySchedulesObservable || o == specificDaysObservable) {
            this.notificationAdapter.notifyDataSetChanged();
        }
    }
}
