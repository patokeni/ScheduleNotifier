package xianxian.center.schedulenotifier;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.utils.ToastUtil;

/**
 * Created by xiaoyixian on 18-6-2.
 */

public class AddScheduleItemDialog {
    @BindView(R2.id.editTextType)
    EditText editTextType;
    @BindView(R2.id.checkBoxNeedNotify)
    CheckBox checkBoxNeedNotify;
    @BindView(R2.id.buttonStartTime)
    Button buttonStartTime;
    @BindView(R2.id.buttonEndTime)
    Button buttonEndTime;
    private Callback callback;
    private Unbinder unbinder;
    private Context context;

    public void show(Context context, Callback callback) {
        this.callback = callback;
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = inflater.inflate(R.layout.dialog_sn_add_schedule_item, null);
        if (view == null)
            return;
        unbinder = ButterKnife.bind(this, view);

        builder.setView(view)
                .setTitle("添加计划项目")
                .setPositiveButton("添加", (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null) {
                        //笑话二：Integer.valueOf(editTextStart.getText().toString()) > 24 || Integer.valueOf(editTextEnd.getText().toString()) > 59
                        if (editTextType.getText().length() == 0) {
                            ToastUtil.showToast(context, "您输入的数据不合法", Toast.LENGTH_LONG);
                        }
                        String startTime = buttonStartTime.getText().toString();
                        String endTime = buttonEndTime.getText().toString();

                        try {
                            if ((Schedule.SCHEDULE_TIME_FORMAT.parse(startTime).after(Schedule.SCHEDULE_TIME_FORMAT.parse(endTime)))) {
                                ToastUtil.showToast(context, "开始时间不能小于结束时间", Toast.LENGTH_LONG);
                                return;
                            }

                        } catch (ParseException e) {
                            ToastUtil.showToast(context, "您输入的数据不合法", Toast.LENGTH_LONG);
                        }
                        callback.onComplete(new ScheduleItem(-1, startTime, endTime, editTextType.getText().toString(), checkBoxNeedNotify.isChecked()));

                    }
                    unbinder.unbind();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    unbinder.unbind();
                });
        builder.create().show();
    }

    @OnClick(R2.id.buttonStartTime)
    public void onButtonStartTimeClicked() {
        new TimePickerDialog(context,
                (view1, hourOfDay, minute) -> buttonStartTime.setText(String.format("%s:%s",
                        (hourOfDay < 10 ? String.format("0%s", String.valueOf(hourOfDay)) : hourOfDay).toString(),
                        (minute < 10 ? String.format("0%s", String.valueOf(minute)) : minute).toString()
                )), 0, 0, false).show();

    }

    @OnClick(R2.id.buttonEndTime)
    public void onButtonEndTimeClicked() {
        new TimePickerDialog(context,
                (view1, hourOfDay, minute) -> buttonEndTime.setText(String.format("%s:%s",
                        (hourOfDay < 10 ? String.format("0%s", String.valueOf(hourOfDay)) : hourOfDay).toString(),
                        (minute < 10 ? String.format("0%s", String.valueOf(minute)) : minute).toString()
                )), 0, 0, false).show();

    }
}
