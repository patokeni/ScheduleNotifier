package xianxian.center.schedulenotifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import xianxian.center.Constants;
import xianxian.center.utils.FileUtils;

public class Settings {
    //
    public static final String SETTINGS_KEY_NOTIFY_TEMPLE = "settingsNotifyTemple";
    public static String SETTINGS_DEFAULT_VALUE_NOTIFY_TEMPLE = "注意，注意，现在是%s时间，请立即开始";
    private static Properties properties = new Properties();

    static {
        try {
            FileUtils.checkFileExistOrCreate(Constants.SCHEDULE_SETTINGS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        load();
        if (properties.isEmpty()) {
            properties.setProperty(SETTINGS_KEY_NOTIFY_TEMPLE, SETTINGS_DEFAULT_VALUE_NOTIFY_TEMPLE);
        }
    }

    public static String getProp(String propKey) {
        return properties.getProperty(propKey);
    }

    public static void setProp(String propKey, String value) {
        properties.setProperty(propKey, value);
        store();
    }

    public static void load() {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(Constants.SCHEDULE_SETTINGS);
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void store() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(Constants.SCHEDULE_SETTINGS);
            properties.store(fos, "");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
