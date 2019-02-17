package xianxian.center.schedulenotifier;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import xianxian.center.main.Callback;
import xianxian.center.utils.ToastUtil;

public class AddSpecificDayDialog {
    @BindView(R2.id.editTextDate)
    EditText editTextDate;
    @BindView(R2.id.buttonSelectDate)
    Button buttonSelectDate;
    @BindView(R2.id.spinnerSchedule)
    Spinner spinnerSchedule;
    private Unbinder unbinder;
    private Context context;

    public void show(Context context, Callback callback) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = inflater.inflate(R.layout.dialog_sn_add_specific_day, null);
        if (view == null)
            return;
        unbinder = ButterKnife.bind(this, view);
        spinnerSchedule.setAdapter(new Adapters.ScheduleAdapter(context));
        builder.setView(view)
                .setTitle("添加特殊的日子")
                .setPositiveButton("添加", (dialog, which) -> {
                    dialog.dismiss();
                    ((Adapters.ScheduleAdapter) spinnerSchedule.getAdapter()).onDestroy();
                    if (editTextDate.getText().length() == 0) {
                        ToastUtil.showToast(context, "您输入的数据不合法", Toast.LENGTH_LONG);
                    }
                    try {
                        Schedules.addSpecificDay(editTextDate.getText().toString(), (Schedule) spinnerSchedule.getSelectedItem());
                        Schedules.saveSpecificDays();
                        callback.onComplete();
                    } catch (IOException e) {
                        ToastUtil.showToast(context, "您输入的数据不合法", Toast.LENGTH_LONG);
                    }
                    unbinder.unbind();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    ((Adapters.ScheduleAdapter) spinnerSchedule.getAdapter()).onDestroy();
                    dialog.dismiss();
                    unbinder.unbind();
                });
        builder.create().show();
    }

    @OnClick({R2.id.buttonSelectDate})
    public void onClick(View view) {
        if (view.getId() == R.id.buttonSelectDate) {
            Calendar calendar = Calendar.getInstance();
            int defaultYear = calendar.get(Calendar.YEAR);
            int defaultMonth = calendar.get(Calendar.MONTH);
            int defaultDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(context,
                    (view1, year, month, dayOfMonth) -> {
                        //NOTE:因为DatePickerDialog中month的范围为(0~11)
                        int actuallyMonth = month + 1;
                        editTextDate.setText(String.format(Locale.getDefault(), "%d-%s-%s",
                                year,
                                (actuallyMonth < 10 ? String.format(Locale.getDefault(), "0%d", actuallyMonth) : actuallyMonth).toString(),
                                (dayOfMonth < 10 ? String.format(Locale.getDefault(), "0%d", dayOfMonth) : dayOfMonth).toString()
                        ));
                    }, defaultYear, defaultMonth, defaultDayOfMonth).show();
        }
    }
}
