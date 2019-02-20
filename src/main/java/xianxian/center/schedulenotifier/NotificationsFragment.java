package xianxian.center.schedulenotifier;

import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.main.IFragment;
import xianxian.center.main.Main;
import xianxian.center.utils.DateUtils;

/**
 * Created by xiaoyixian on 18-6-5.
 */

public class NotificationsFragment extends Fragment implements IFragment {

    public static final int SCHEDULE_LIST = 0x01;
    public static final int NOTIFICATIONS_LIST = 0x02;

    public static int statusType;

    @BindView(R2.id.listView)
    ListView listView;
    @BindView(R2.id.floatingActionButtonAdd)
    FloatingActionButton fab;
    Unbinder unbinder;

    EditText editTextScheduleName;

    Adapters.ScheduleAdapter scheduleAdapter = null;
    Adapters.NotificationAdapter notificationAdapter = null;

    Schedule selectedSchedule;
    @BindView(R2.id.floatingActionButtonBack)
    FloatingActionButton floatingActionButtonBack;
    private Callback onContextMenuSelected;
    private int contextMenuRes;
    private String contextMenuTitle;

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_sn_notificantions, container, false);
        unbinder = ButterKnife.bind(this, view);
        //初始化适配器
        scheduleAdapter = new Adapters.ScheduleAdapter(getContext());
        listView.setAdapter(scheduleAdapter);
        //不在浏览计划表内容模式，将返回键关闭
        floatingActionButtonBack.setVisibility(View.INVISIBLE);
        //floatingActionButtonBack.setOnClickListener(this);
        //将状态设置为浏览计划表模式
        statusType = SCHEDULE_LIST;
        //fab.setOnClickListener(this);
        //设置点击监听
        listView.setOnItemClickListener(this::onItemClick);

        listView.setOnItemLongClickListener(this::onItemLongClick);

        registerForContextMenu(listView);
        listView.setDivider(null);
        return view;
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
        scheduleAdapter.onDestroy();
        unbinder.unbind();
    }

    @OnClick({R2.id.floatingActionButtonBack})
    public void onButtonBackClicked() {
        switch (statusType) {
            //当浏览计划表内容模式时
            case NOTIFICATIONS_LIST:
                notificationAdapter.onDestroy();
                //将ListView的内容设置为计划表
                listView.setAdapter(scheduleAdapter);
                //返回按键不可视
                floatingActionButtonBack.setVisibility(View.INVISIBLE);
                //每次返回都重新设置监听
                listView.setOnItemClickListener(this::onItemClick);
                //将状态改为浏览计划表模式
                statusType = SCHEDULE_LIST;
                break;
        }
    }

    @OnClick(R2.id.floatingActionButtonAdd)
    public void onButtonAddClicked() {
        //添加按钮
        switch (statusType) {
            //当浏览计划表模式时
            case SCHEDULE_LIST:
                //显示一个对话框来添加新的计划表
                editTextScheduleName = new EditText(this.getContext());
                editTextScheduleName.setHint("名称");
                new AddScheduleDialog().show(this.getContext(), new Callback() {
                    @Override
                    public void onComplete(Object... objects) {
                        scheduleAdapter.notifyDataSetChanged();
                    }
                });
                break;
            //当浏览计划表内容时
            case NOTIFICATIONS_LIST:
                //显示一个对话框来添加计划
                new AddScheduleItemDialog().show(this.getContext(), new Callback() {
                    @Override
                    public void onComplete(Object... objects) {
                        ScheduleItem scheduleItem = (ScheduleItem) Objects.requireNonNull(objects[0]);
                        scheduleItem.parent = notificationAdapter.getSchedule();
                        //添加到该适配器对应的计划表
                        notificationAdapter.getSchedule().add(scheduleItem);
                        //排序计划表
                        notificationAdapter.getSchedule().sort();
                        //更新适配器
                        notificationAdapter.notifyDataSetChanged();

                    }
                });
                break;
        }

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

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent The AdapterView where the click happened.
     * @param view   The view within the AdapterView that was clicked (this
     *               will be a view provided by the adapter)
     * @param pos    The position of the view in the adapter.
     * @param id     The row id of the item that was clicked.
     */
    //比较特殊，不用ButterKnife
    private void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        switch (statusType) {
            //当浏览计划表模式时
            case SCHEDULE_LIST:
                //获取选择了的计划表
                Schedule schedule = scheduleAdapter.getItem(pos);
                //进入浏览计划表内容模式
                notificationAdapter = new Adapters.NotificationAdapter(schedule, getContext());
                listView.setAdapter(notificationAdapter);
                //设置没有分界线
                listView.setDivider(null);
                //储存已选择的计划表
                this.selectedSchedule = schedule;
                //设置状态为浏览计划表内容模式
                statusType = NOTIFICATIONS_LIST;
                //返回按钮可见
                floatingActionButtonBack.setVisibility(View.VISIBLE);
                break;
            case NOTIFICATIONS_LIST:

                break;
        }
    }

    private boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
        switch (statusType) {
            case SCHEDULE_LIST:
                showContextMenu(adapterView, "操作", R.menu.menu_sn_schedule_operation, new Callback() {
                    @Override
                    public boolean Do(Object... objects) {
                        MenuItem menuItem = (MenuItem) objects[0];

                        if (menuItem.getItemId() == R.id.menu_sn_schedule_oper_set_as) {

                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, DateUtils.DAYS_OF_WEEK);
                            View dialogView = getLayoutInflater().inflate(R.layout.dialog_sn_schedule_set_as, null);
                            if (dialogView.getParent() != null)
                                ((ViewGroup) dialogView.getParent()).removeView(dialogView);
                            Spinner spinner = dialogView.findViewById(R.id.spinner);
                            spinner.setAdapter(arrayAdapter);

                            new AlertDialog.Builder(getContext())
                                    .setTitle("设置为...的计划")
                                    .setCancelable(true)
                                    //我真傻，IllegalStateException是因为这里报出来的
                                    //留着当笑话看
                                    //.setView(spinner)
                                    .setView(dialogView)
                                    .setPositiveButton("确认", ((dialog, which) -> Schedules.addDailySchedule((int) spinner.getSelectedItemId(), (Schedule) adapterView.getItemAtPosition(pos))))
                                    .setNegativeButton("取消", ((dialog, which) -> dialog.dismiss()))
                                    //FIXED:这里显示时不知为何会报IllegalStateException
                                    .show();

                        } else if (menuItem.getItemId() == R.id.menu_sn_schedule_oper_rename) {
                            Schedule schedule = (Schedule) adapterView.getItemAtPosition(pos);

                            EditText editText = new EditText(getContext());
                            editText.setHint("输入新名称");
                            editText.setText(schedule.getName());
                            new AlertDialog.Builder(getContext())
                                    .setTitle("请输入新名称")
                                    .setView(editText)
                                    .setPositiveButton("确定", (dialog, which) -> {
                                        schedule.setName(editText.getText().toString());
                                        try {
                                            Schedules.saveSchedules();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        dialog.cancel();
                                    })
                                    .setNegativeButton("取消", (dialog, whick) -> {
                                        dialog.cancel();
                                    })
                                    .show();

                        } else if (menuItem.getItemId() == R.id.menu_sn_schedule_oper_delete) {
                            Schedule schedule = (Schedule) adapterView.getItemAtPosition(pos);

                            new AlertDialog.Builder(getContext())
                                    .setTitle("删除")
                                    .setCancelable(true)
                                    //我真傻，IllegalStateException是因为这里报出来的
                                    //留着当笑话看
                                    //.setView(spinner)
                                    .setMessage(String.format("你确认要删除%s吗", schedule.getName()))
                                    .setPositiveButton("删除", ((dialog, which) -> {
                                        Schedules.remove(schedule);
                                        Main.getMainActivity().prepareSnackbar("已删除", Snackbar.LENGTH_LONG)
                                                .setAction("撤销", (view) -> {
                                                    Schedules.addSchedule(schedule);
                                                })
                                                .show();
                                    }))
                                    .setNegativeButton("取消", ((dialog, which) -> dialog.cancel()))
                                    //FIXED:这里显示时不知为何会报IllegalStateException
                                    .show();
                        }
                        return false;
                    }
                });
                break;
            case NOTIFICATIONS_LIST:
                ScheduleItem scheduleItem = notificationAdapter.getItem(pos);

                showContextMenu(listView, scheduleItem.getDesc(), R.menu.menu_sn_schedule_item_operation, new Callback() {
                    @Override
                    public boolean Do(Object... objects) {
                        MenuItem menuItem = (MenuItem) objects[0];
                        if (menuItem.getItemId() == R.id.menu_sn_schedule_item_oper_edit) {
                            new AddScheduleItemDialog().edit(getContext(), new Callback() {
                                @Override
                                public boolean Do(Object... objects) {
                                    return false;
                                }
                            }, scheduleItem);
                        } else if (menuItem.getItemId() == R.id.menu_sn_schedule_item_oper_set_custom_message) {
                            EditText editText = new EditText(getContext());
                            editText.setHint("输入自定义提醒");
                            editText.setText(scheduleItem.getMessage());
                            new AlertDialog.Builder(getContext())
                                    .setTitle("设置自定义提醒")
                                    .setView(editText)
                                    .setPositiveButton("确定", (dialog, which) -> {
                                        scheduleItem.setCustomMessage(editText.getText().toString());
                                        dialog.cancel();
                                    })
                                    .setNegativeButton("取消", (dialog, whick) -> {
                                        dialog.cancel();
                                    })
                                    .show();
                        } else if (menuItem.getItemId() == R.id.menu_sn_schedule_item_oper_delete) {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("确定？")
                                    .setMessage("你确定要删除此计划项目")
                                    .setPositiveButton("删除", (dialog, which) -> {
                                        boolean isCustomMessage = scheduleItem.isCustomMessage();
                                        String customMessage = scheduleItem.getMessage();
                                        scheduleItem.parent.remove(scheduleItem);
                                    })
                                    .setNegativeButton("取消", ((dialog, which) -> {
                                        dialog.cancel();
                                    }))
                                    .show();
                        }

                        return false;
                    }
                });
                break;
        }
        return true;
    }

    @Override
    public boolean onBackPressed() {
        boolean flag = (statusType == SCHEDULE_LIST);
        if (!flag) {
            notificationAdapter.onDestroy();
            //将ListView的内容设置为计划表
            listView.setAdapter(scheduleAdapter);
            //返回按键不可视
            floatingActionButtonBack.setVisibility(View.INVISIBLE);
            //每次返回都重新设置监听
            listView.setOnItemClickListener(this::onItemClick);
            //将状态改为浏览计划表模式
            statusType = SCHEDULE_LIST;
        }
        return flag;
    }
}
