package xianxian.center.schedulenotifier;

import java.text.ParseException;
import java.util.Date;

import static xianxian.center.schedulenotifier.Schedule.SCHEDULE_TIME_FORMAT;

/**
 * Created by xiaoyixian on 18-3-24.
 */

public class ScheduleItem {
    public transient Schedule parent;
    public transient ScheduleItem prev;
    public transient ScheduleItem next;
    private int id = -1;
    private Date startTime = null;
    private Date endTime = null;
    private Type type;
    private boolean isNeedNotify = false;
    private boolean isCustomMessage = false;
    private String customMessage;

    ScheduleItem() {
    }

    public ScheduleItem(int id, Date startTime, Date endTime, Type type, boolean isNeedNotify) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.isNeedNotify = isNeedNotify;
    }

    public ScheduleItem(int id, String startTime, String endTime, String type, boolean isNeedNotify) {
        setId(id);
        setStartTime(startTime);
        setEndTime(endTime);
        setType(type);
        setNeedNotify(isNeedNotify);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartTime() {
        return SCHEDULE_TIME_FORMAT.format(startTime);
    }

    /**
     * @param source
     */
    public void setStartTime(String source) {
        try {
            startTime = SCHEDULE_TIME_FORMAT.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getStartTimeDate() {
        return startTime;
    }

    public String getEndTime() {
        return SCHEDULE_TIME_FORMAT.format(endTime);
    }

    /**
     * @param source
     */
    public void setEndTime(String source) {
        try {
            this.endTime = SCHEDULE_TIME_FORMAT.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Date getEndTimeDate() {
        return endTime;
    }

    public Type getType() {
        return type;
    }

    public void setType(String type) {
        this.type = Types.getOrCreate(type);
        this.type.addUser(this);
    }

    public boolean isNeedNotify() {
        return isNeedNotify;
    }

    public void setNeedNotify(boolean needNotify) {
        isNeedNotify = needNotify;
    }

    public boolean isCustomMessage() {
        return isCustomMessage;
    }

    public void setCustomMessage(String customMessage) {
        if (customMessage != null) {
            this.isCustomMessage = true;
            this.customMessage = customMessage;
        } else {
            this.isCustomMessage = false;
            this.customMessage = null;
        }
    }

    public String getMessage() {
        return isCustomMessage ? customMessage : type.getMessage();
    }

    public boolean before(ScheduleItem s1) {
        return getStartTimeDate().before(s1.getStartTimeDate());
    }

    public boolean after(ScheduleItem s1) {
        return getStartTimeDate().after(s1.getStartTimeDate());
    }

    @Override
    public String toString() {
        return String.format("{ScheduleItem ID=%s,StartTime=%s,EndTime=%s,type=%s,isNotify=%b}", getId(), getStartTime(), getEndTime(), type, isNeedNotify);
    }

    public String getDesc() {
        return String.format("%s~%s %s", getStartTime(), getEndTime(), type);
    }
}
