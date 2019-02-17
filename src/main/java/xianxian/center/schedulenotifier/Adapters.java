package xianxian.center.schedulenotifier;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import butterknife.BindView;
import butterknife.ButterKnife;
import xianxian.center.main.IAdapter;
//import xianxian.center.main.MainAdapter;

public class Adapters {
    public static class ScheduleAdapter extends BaseAdapter implements Observer, IAdapter {
        private List<Schedule> schedules;
        private Context mContext;
        private boolean withEmpty;

        private Observable observable = Schedules.schedulesObservable;

        public ScheduleAdapter(Context mContext) {
            //Emmm...
            //克隆一个新List
            this.schedules = new ArrayList<>(Schedules.getSchedules());
            this.mContext = mContext;
            observable.addObserver(this);
        }

        @Override
        public void notifyDataSetChanged() {
            this.schedules = new ArrayList<>(Schedules.getSchedules());
            if (withEmpty)
                schedules.add(0, Schedule.EMPTY);
            super.notifyDataSetChanged();
        }

        public ScheduleAdapter withEmpty() {
            schedules.add(0, Schedule.EMPTY);
            withEmpty = true;
            return this;
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return schedules.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Schedule getItem(int position) {
            return schedules.get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            Schedule schedule = getItem(position);
            //当schedule是空的且列表不应该有空
            if (schedule == Schedule.EMPTY && !withEmpty) {
                schedules.remove(schedule);
                return new View(mContext);
            }
            convertView = layoutInflater.inflate(R.layout.layout_schedule, null);

            ViewHolder vh = new ViewHolder(convertView);
            vh.textViewScheduleName.setText(schedule.getName());
            return convertView;
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
            if (o == observable)
                notifyDataSetChanged();
        }

        @Override
        public void onDestroy() {
            observable.deleteObserver(this);
        }

        static class ViewHolder {
            @BindView(R2.id.textViewScheduleName)
            TextView textViewScheduleName;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }
    }

    public static class NotificationAdapter extends BaseAdapter implements IAdapter, Observer {
        private Schedule schedule;
        private Observable scheduleObservable;
        private Context context;

        public NotificationAdapter(Schedule schedule, Context context) {
            this.schedule = schedule;
            this.scheduleObservable = schedule.scheduleObservable;
            this.scheduleObservable.addObserver(this);
            this.context = context;
        }

        public Schedule getSchedule() {
            return schedule;
        }

        public void setSchedule(Schedule schedule) {
            this.schedule = schedule;
            if (this.scheduleObservable != null)
                this.scheduleObservable.deleteObserver(this);
            this.scheduleObservable = schedule.scheduleObservable;
            this.scheduleObservable.addObserver(this);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return schedule.getScheduleItems().size();
        }

        @Override
        public ScheduleItem getItem(int position) {
            return schedule.getScheduleItems().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.layout_schedule_item, null);

            if (convertView != null) {
                ViewHolder viewHolder = new ViewHolder(convertView);
                ScheduleItem scheduleItem = getItem(position);
                viewHolder.scheduleItem.setText(scheduleItem.getDesc());
            }
            return convertView;
        }

        /**
         * 当Adapter要被销毁时调用
         */
        @Override
        public void onDestroy() {
            if (this.scheduleObservable != null)
                this.scheduleObservable.deleteObserver(this);
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
            if (o == scheduleObservable)
                notifyDataSetChanged();
        }

        class ViewHolder {
            @BindView(R2.id.scheduleItem)
            TextView scheduleItem;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }
    }

    public static class ScheduleItemAdapter extends BaseAdapter implements Observer, IAdapter {
        private Context context;
        private Schedule schedule;
        private Observable scheduleObservable;
        private Observable onScheduleOfTodayChanged = NotifyService.onScheduleOfTodayChanged;
        private Observable onDoingScheduleItemChanged = NotifyService.onDoingScheduleItemChanged;

        public ScheduleItemAdapter(Context context) {
            this.context = context;
            schedule = Schedules.getScheduleToday();
            scheduleObservable = schedule.scheduleObservable;
            scheduleObservable.addObserver(this);
            onScheduleOfTodayChanged.addObserver(this);
            onDoingScheduleItemChanged.addObserver(this);
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return schedule.getScheduleItems().size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public ScheduleItem getItem(int position) {
            return schedule.getScheduleItems().get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.listitem_sn_schedule_item, null);
            ViewHolder vh = new ViewHolder(convertView);
            ScheduleItem scheduleItem = getItem(position);
            vh.textViewScheduleItemName.setText(scheduleItem.getDesc());
            try {
                //获取正在做的计划，如果无，抛出异常
                ScheduleItem doingScheduleItem = schedule.getDoingScheduleItem();
                if (doingScheduleItem != null) {
                    //如果该计划与进行中的计划ID相同
                    if (doingScheduleItem.getId() == scheduleItem.getId()) {
                        vh.checkBoxDid.setChecked(false);
                        vh.textViewScheduleItemName.getPaint().setFakeBoldText(true);
                    } else if (doingScheduleItem.after(scheduleItem)) {
                        vh.checkBoxDid.setChecked(true);
                    }

                }

            } catch (Schedule.NoScheduleItemException e) {
                Date now = null;
                try {
                    now = Schedule.SCHEDULE_TIME_FORMAT.parse(Schedule.SCHEDULE_TIME_FORMAT.format(new Date()));
                } catch (ParseException e1) {
                    e1.printStackTrace();
                }
                if (scheduleItem.getEndTimeDate().before(now)) {
                    vh.checkBoxDid.setChecked(true);
                }
            }
            return convertView;
        }

        /**
         * 当Adapter要被销毁时调用
         */
        @Override
        public void onDestroy() {
            scheduleObservable.deleteObserver(this);
            onDoingScheduleItemChanged.deleteObserver(this);
            onScheduleOfTodayChanged.deleteObserver(this);
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
            if (o == scheduleObservable) {
                schedule = Schedules.getScheduleToday();
                notifyDataSetChanged();
            } else if (o == onDoingScheduleItemChanged) {
                notifyDataSetChanged();
            } else if (o == onScheduleOfTodayChanged) {
                schedule = Schedules.getScheduleToday();
                notifyDataSetChanged();
            }
        }

        static class ViewHolder {
            @BindView(R2.id.checkBoxDid)
            CheckBox checkBoxDid;
            @BindView(R2.id.textViewScheduleItemName)
            TextView textViewScheduleItemName;

            ViewHolder(View view) {
                ButterKnife.bind(this, view);
            }
        }
    }

    public static class SettingsAdapter extends BaseAdapter {

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return 0;
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return null;
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         * Get a View that displays the data at the specified position in the data set. You can either
         * create a View manually or inflate it from an XML layout file. When the View is inflated, the
         * parent View (GridView, ListView...) will apply default layout parameters unless you use
         * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
         * to specify a root view and to prevent attachment to the root.
         *
         * @param position    The position of the item within the adapter's data set of the item whose view
         *                    we want.
         * @param convertView The old view to reuse, if possible. Note: You should check that this view
         *                    is non-null and of an appropriate type before using. If it is not possible to convert
         *                    this view to display the correct data, this method can create a new view.
         *                    Heterogeneous lists can specify their number of view types, so that this View is
         *                    always of the right type (see {@link #getViewTypeCount()} and
         *                    {@link #getItemViewType(int)}).
         * @param parent      The parent that this view will eventually be attached to
         * @return A View corresponding to the data at the specified position.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = new TextView(null);
            ((TextView) convertView).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            return null;
        }
    }
}
