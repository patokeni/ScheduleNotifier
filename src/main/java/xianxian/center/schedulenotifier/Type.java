package xianxian.center.schedulenotifier;

import java.util.ArrayList;
import java.util.List;

public class Type implements Cloneable {
    private final List<ScheduleItem> users = new ArrayList<>();
    private String typeName;
    private String customMessage;
    private boolean isCustomMessage;

    public Type(String typeName) {
        this.typeName = typeName;
        this.isCustomMessage = false;
    }

    public Type(String typeName, String customMessage) {
        this.typeName = typeName;
        this.customMessage = customMessage;
        this.isCustomMessage = true;
    }

    public String getTypeName() {
        return typeName;
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
        return !isCustomMessage ? String.format(Settings.getProp(Settings.SETTINGS_KEY_NOTIFY_TEMPLE), typeName) : customMessage;
    }

    public void addUser(ScheduleItem user) {
        this.users.add(user);
    }

    public void removeUser(ScheduleItem user) {
        this.users.remove(user);
    }

    public boolean hasUser() {
        return this.users.isEmpty();
    }

    public List<ScheduleItem> getUsers() {
        return this.users;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
