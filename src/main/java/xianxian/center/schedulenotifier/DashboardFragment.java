package xianxian.center.schedulenotifier;

import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.CalendarView;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.IOException;
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

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DashboardFragment.
     */
    public static DashboardFragment newInstance() {
        return new DashboardFragment();
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
        listViewScheduleOfDay.setAdapter(notificationAdapter = new Adapters.ScheduleItemAdapter(getContext()));
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

    private void onItemClicked(AdapterView<?> adapterView, View view, int pos, long id) {
        //显示一个对话框来设置一天的计划表
        final Spinner spinner = new Spinner(this.getContext());
        //初始化计划表适配器
        Adapters.ScheduleAdapter adapter = new Adapters.ScheduleAdapter(this.getContext());
        //设置适配器
        spinner.setAdapter(adapter);
        new AlertDialog.Builder(this.getContext())
                .setTitle("设置这天的计划表")
                .setView(spinner)
                .setCancelable(true)
                .setPositiveButton("确认", ((dialog, which) -> {
                    ((Adapters.ScheduleAdapter) spinner.getAdapter()).onDestroy();
                    Schedule schedule = adapter.getItem(spinner.getSelectedItemPosition());
                    if (pos < 7) {
                        Schedules.addDailySchedule(pos, schedule);
                        try {
                            Schedules.saveDailySchedule();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        String key = (String) Schedules.getSpecificDays().keySet().toArray()[pos - 7];
                        Schedules.addSpecificDay(key, schedule);
                        try {
                            Schedules.saveSpecificDays();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }))
                .setNegativeButton("取消", ((dialog, which) -> {
                    ((Adapters.ScheduleAdapter) spinner.getAdapter()).onDestroy();
                    dialog.dismiss();
                }))
                .show();
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
        unbinder.unbind();
    }

    @Override
    public String tag() {
        return "SN_DB";
    }

    @Override
    public int menuID() {
        return R.id.nav_sn_dashboard;
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
