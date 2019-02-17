package xianxian.center.schedulenotifier;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.utils.ToastUtil;

public class AddScheduleDialog {

    @BindView(R2.id.editTextScheduleName)
    EditText editTextScheduleName;
    @BindView(R2.id.spinnerSchedule)
    Spinner spinnerSchedule;
    private Unbinder unbinder;
    private Context context;

    public void show(Context context, Callback callback) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = inflater.inflate(R.layout.dialog_sn_add_schedule, null);
        if (view == null)
            return;
        unbinder = ButterKnife.bind(this, view);
        spinnerSchedule.setAdapter(new Adapters.ScheduleAdapter(context).withEmpty());
        builder.setView(view)
                .setTitle("添加计划表")
                .setPositiveButton("添加", ((dialog, which) -> {
                    ((Adapters.ScheduleAdapter) spinnerSchedule.getAdapter()).onDestroy();
                    if (editTextScheduleName.getText().length() == 0) {
                        ToastUtil.showToast(context, "您输入的数据不合法", Toast.LENGTH_LONG);
                        return;
                    }
                    if (Schedules.getScheduleByName(editTextScheduleName.getText().toString()) != null) {
                        ToastUtil.showToast(context, "已有同名计划表", Toast.LENGTH_SHORT);
                        return;
                    }
                    if (spinnerSchedule.getSelectedItem() == Schedule.EMPTY)
                        Schedules.addNewSchedule(editTextScheduleName.getText().toString());
                    else {
                        try {
                            Schedules.addSchedule(new Schedule(editTextScheduleName.getText().toString(), ((Schedule) spinnerSchedule.getSelectedItem()).getScheduleItems()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    callback.onComplete();
                    unbinder.unbind();
                }))
                .setNegativeButton("取消", ((dialog, which) -> {
                    ((Adapters.ScheduleAdapter) spinnerSchedule.getAdapter()).onDestroy();
                    dialog.dismiss();
                    unbinder.unbind();
                }));
        builder.create().show();
    }
}
