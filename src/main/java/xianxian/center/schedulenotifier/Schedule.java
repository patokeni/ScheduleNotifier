package xianxian.center.schedulenotifier;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;

import xianxian.center.Constants;
import xianxian.center.utils.FileUtils;

public class Schedule implements Serializable {
    public static final SimpleDateFormat SCHEDULE_TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.CHINA);
    //一个空计划表
    public static final Schedule EMPTY = new Schedule("无");
    //用于监听数据变化
    public final Observable scheduleObservable = new DataObservable();
    private List<ScheduleItem> scheduleItems = new ArrayList<>();
    private String name;
    private File storageFile;

    private Schedule(String name) {
        this.name = name;
    }

    public Schedule(String name, File storageFile) {
        this.storageFile = storageFile;
        this.name = name;

        try {
            loadScheduleItemsFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Schedule(String name, List<ScheduleItem> scheduleItems) throws IOException {
        this.scheduleItems = scheduleItems;
        this.name = name;

        this.storageFile = new File(Constants.SCHEDULE_XML_DIR.getPath() + File.separator + name + ".xml");
        FileUtils.checkFileExistOrCreate(storageFile);
        saveScheduleItems();
    }

    public static Schedule newSchedule(String name) {
        File scheduleItemsConfig = new File(Constants.SCHEDULE_XML_DIR.getPath() + File.separator + name + ".xml");
        try {
            FileUtils.checkFileExistOrCreate(scheduleItemsConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Schedule(name, scheduleItemsConfig);
    }

    /**
     * 对scheduleItems排序，并储存到文件
     */
    public void sort() {
        ScheduleItem[] scheduleItems = new ScheduleItem[this.scheduleItems.size()];
        this.scheduleItems.toArray(scheduleItems);

        Arrays.sort(scheduleItems, (o1, o2) -> (int) (o1.getStartTimeDate().getTime() - o2.getStartTimeDate().getTime()));

        this.scheduleItems.clear();
        this.scheduleItems.addAll(Arrays.asList(scheduleItems));

        for (ScheduleItem scheduleItem :
                scheduleItems) {
            int index = this.scheduleItems.indexOf(scheduleItem);
            scheduleItem.setId(index);
        }
    }

    public List<ScheduleItem> getScheduleItems() {
        return scheduleItems;
    }

    public void saveScheduleItems() throws IOException {
        saveScheduleItems(storageFile);
    }

    public void saveScheduleItems(File file) throws IOException {
        this.sort();
        OutputStream os = null;

        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(os, "utf-8");
        xmlSerializer.startDocument("utf-8", true);
        xmlSerializer.startTag(null, "Schedule");
        for (ScheduleItem si : scheduleItems) {
            xmlSerializer.startTag(null, "ScheduleItem");
            xmlSerializer.attribute(null, "id", String.valueOf(si.getId() != 0 ? si.getId() : String.valueOf(scheduleItems.indexOf(si) + 1)));

            xmlSerializer.startTag(null, "startTime");
            xmlSerializer.text(si.getStartTime());
            xmlSerializer.endTag(null, "startTime");

            xmlSerializer.startTag(null, "endTime");
            xmlSerializer.text(si.getEndTime());
            xmlSerializer.endTag(null, "endTime");

            xmlSerializer.startTag(null, "Type");
            xmlSerializer.text(si.getType());
            xmlSerializer.endTag(null, "Type");

            xmlSerializer.startTag(null, "isNeedNotify");
            xmlSerializer.text(String.valueOf(si.isNeedNotify()));
            xmlSerializer.endTag(null, "isNeedNotify");

            xmlSerializer.startTag(null, "customMessage");
            xmlSerializer.attribute(null, "isCustomMessage", String.valueOf(si.isCustomMessage()));
            if (si.isCustomMessage())
                xmlSerializer.text(si.getMessage());
            xmlSerializer.endTag(null, "customMessage");

            xmlSerializer.endTag(null, "ScheduleItem");
        }
        xmlSerializer.endTag(null, "Schedule");
        xmlSerializer.endDocument();
        os.close();
    }

    public ScheduleItem getDoingScheduleItem() throws NoScheduleItemException {
        Date now = new Date();
        try {
            now = SCHEDULE_TIME_FORMAT.parse(SCHEDULE_TIME_FORMAT.format(new Date()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        for (ScheduleItem scheduleItem : scheduleItems) {
            if (scheduleItem != null
                    && scheduleItem.getStartTimeDate() != null
                    && scheduleItem.getEndTimeDate() != null) {
                if ((now.after(scheduleItem.getStartTimeDate()) || now.getTime() == scheduleItem.getStartTimeDate().getTime())
                        && now.before(scheduleItem.getEndTimeDate())) {
                    return scheduleItem;
                }
            }
        }
        throw new NoScheduleItemException();
    }

    public void loadScheduleItemsFromFile() throws IOException {
        if (!storageFile.exists()) {
            FileUtils.checkFileExistOrCreate(storageFile);
        }

        try (InputStream is = new FileInputStream(storageFile)) {

            XmlPullParser xmlPullParser = Xml.newPullParser();

            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();

            int id = 0;
            String startTime = "";
            String endTime = "";
            String type = "";
            boolean isNeedNotify = false;
            boolean isCustomMessage = false;
            String customMessage = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = xmlPullParser.getName();
                Log.d("Center/SN", "Tag: " + tag);
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Schedule".equals(tag)) ;
                        else if ("ScheduleItem".equals(tag))
                            id = Integer.valueOf(xmlPullParser.getAttributeValue(null, "id"));
                        else if ("startTime".equals(tag))
                            startTime = xmlPullParser.nextText();
                        else if ("endTime".equals(tag))
                            endTime = xmlPullParser.nextText();
                        else if ("Type".equals(tag))
                            type = xmlPullParser.nextText();
                        else if ("isNeedNotify".equals(tag))
                            isNeedNotify = Boolean.valueOf(xmlPullParser.nextText());
                        else if ("customMessage".equals(tag)) {
                            isCustomMessage = Boolean.valueOf(xmlPullParser.getAttributeValue(null, "isCustomMessage"));
                            if (isCustomMessage)
                                customMessage = xmlPullParser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("ScheduleItem".equals(tag)) {
                            ScheduleItem scheduleItem = new ScheduleItem(id, startTime, endTime, type, isNeedNotify);
                            scheduleItem.parent = this;
                            scheduleObservable.notifyObservers(scheduleItem);
                            if (isCustomMessage)
                                scheduleItem.setCustomMessage(customMessage);
                            scheduleItems.add(scheduleItem);
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
            this.sort();
        } catch (IOException e) {
            Log.e("Center/SN", e.getLocalizedMessage(), e);
        } catch (XmlPullParserException e) {
            Log.e("Center/SN", e.getLocalizedMessage(), e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getStorageFile() {
        return storageFile;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "name='" + name + '\'' +
                ", storageFile=" + storageFile +
                '}';
    }

    public void add(ScheduleItem scheduleItem) {
        scheduleItems.add(scheduleItem);
        this.sort();
        scheduleObservable.notifyObservers();
        try {
            saveScheduleItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(int id) {
        scheduleItems.remove(id);
        this.sort();
        scheduleObservable.notifyObservers();
        try {
            saveScheduleItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void remove(ScheduleItem scheduleItem) {
        scheduleItems.remove(scheduleItem);
        this.sort();
        scheduleObservable.notifyObservers();
        try {
            saveScheduleItems();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class NoScheduleItemException extends Exception {
        @Override
        public String getMessage() {
            return "There is no schedule item";
        }
    }

    public static class NoScheduleException extends Exception {
        @Override
        public String getMessage() {
            return "There is no schedule";
        }
    }
}