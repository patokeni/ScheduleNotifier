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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;

import xianxian.center.Constants;
import xianxian.center.utils.FileUtils;

/**
 * Created by xiaoyixian on 18-3-22. <br/>
 * 有关增删的操作，大部分完成后都会储存到文件
 */

public class Schedules {
    //用于监听数据变化(我猜RxJava不是这个用处吧)
    public static final Observable dailySchedulesObservable = new DataObservable();
    public static final Observable specificDaysObservable = new DataObservable();
    public static final Observable schedulesObservable = new DataObservable();
    //日常的计划
    private final static Map<Integer, Schedule> dailySchedules = new LinkedHashMap<>(7);
    //特殊日子的计划
    private final static Map<String, Schedule> specificDays = new LinkedHashMap<>();
    //缓存所有的计划表
    private static final List<Schedule> schedules = new ArrayList<>();
    public static SimpleDateFormat SPECIFIC_DAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static void load() {
        //解析计划表配置文件
        parseSchedulesStorageFile();
        //解析日常计划表
        parseDailyScheduleConfig();
        //解析特殊日子计划表
        parseSpecificDaysConfig();

    }

    public static void remove(String scheduleName) {
        Iterator<Schedule> iterator = schedules.iterator();
        while (iterator.hasNext())
            if (iterator.next().getName().equals(scheduleName)) {
                iterator.remove();
                schedulesObservable.notifyObservers();
            }
        try {
            saveSchedules();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void remove(Schedule schedule) {
        schedules.remove(schedule);
        schedulesObservable.notifyObservers();
        try {
            saveSchedules();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Schedule removeSpecificDay(String dateString) {
        Schedule old = specificDays.remove(dateString);
        specificDaysObservable.notifyObservers();
        try {
            saveSpecificDays();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return old;
    }

    public static void removeSpecificDay(Date date) {
        removeSpecificDay(SPECIFIC_DAY_FORMAT.format(date));
    }

    /**
     * 添加一个新的空计划表到schedules
     *
     * @param name
     */
    public static void addNewSchedule(String name) {
        //初始化一个空计划表
        Schedule schedule = Schedule.newSchedule(name);
        //往schedules添加计划表
        schedules.add(schedule);
        schedulesObservable.notifyObservers(schedule);
        try {
            //尝试保存
            saveSchedules();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addSchedule(Schedule schedule) {
        //往schedules添加计划表
        schedules.add(schedule);
        schedulesObservable.notifyObservers(schedule);
        try {
            //尝试保存
            saveSchedules();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addDailySchedule(int dayOfWeek, Schedule schedule) {
        Map.Entry<Integer, Schedule> dailyScheduleEntry = new LinkedHashMap.SimpleEntry<>(dayOfWeek, schedule);
        dailySchedules.put(dailyScheduleEntry.getKey(), dailyScheduleEntry.getValue());
        dailySchedulesObservable.notifyObservers(dailyScheduleEntry);

        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == today && !isInSpecificDay(new Date()))
            NotifyService.onScheduleOfTodayChanged.notifyObservers(dailyScheduleEntry.getValue());
        try {
            saveDailySchedule();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addSpecificDay(String dateString, Schedule schedule) {
        Map.Entry<String, Schedule> specificDayEntry = new LinkedHashMap.SimpleEntry<>(dateString, schedule);
        specificDays.put(dateString, schedule);
        specificDaysObservable.notifyObservers(specificDayEntry);
        String nowDateString = SPECIFIC_DAY_FORMAT.format(new Date());
        if (specificDayEntry.getKey().equals(nowDateString))
            NotifyService.onScheduleOfTodayChanged.notifyObservers(specificDayEntry.getValue());
        try {
            saveSpecificDays();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 默认解析{@link Constants#SCHEDULES_CONFIG_XML}
     *
     * @return 解析后的结果
     */
    public static List<Schedule> parseSchedulesStorageFile() {
        return parseSchedulesStorageFile(Constants.SCHEDULES_CONFIG_XML);
    }

    /**
     * @param file 解析此文件并储存至{@link Schedules#schedules}
     * @return 解析后的结果
     */
    public static List<Schedule> parseSchedulesStorageFile(File file) {

        InputStream is;

        if (FileUtils.isFileEmpty(file))
            return schedules;

        try {
            is = new FileInputStream(file);

            XmlPullParser xmlPullParser = Xml.newPullParser();

            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();

            String name = "";
            String path = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = xmlPullParser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Schedules".equals(tag)) ;
                        else if ("schedule".equals(tag)) {
                            name = xmlPullParser.getAttributeValue(null, "name");
                            path = xmlPullParser.nextText();
                            Schedule schedule = new Schedule(name, new File(path));
                            schedules.add(schedule);
                            schedulesObservable.notifyObservers(schedule);
                        }
                        break;
                }

                eventType = xmlPullParser.next();
            }
        } catch (IOException e) {
            Log.e("Center/SN", e.getLocalizedMessage(), e);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return schedules;
    }

    /**
     * 默认储存 {@link Schedules#schedules} 到{@link Constants#SCHEDULES_CONFIG_XML}
     *
     * @throws IOException
     */
    public static void saveSchedules() throws IOException {
        saveSchedules(schedules, Constants.SCHEDULES_CONFIG_XML);
    }

    public static void saveSchedules(List<Schedule> schedules, File file) throws IOException {
        OutputStream os = null;

        FileUtils.checkFileExistOrCreate(file);

        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(os, "utf-8");
        xmlSerializer.startDocument("utf-8", true);
        xmlSerializer.startTag(null, "Schedules");
        for (Schedule schedule : schedules) {
            if (schedule == Schedule.EMPTY)
                break;
            xmlSerializer.startTag(null, "schedule");
            xmlSerializer.attribute(null, "name", schedule.getName());
            xmlSerializer.text(schedule.getStorageFile().getPath());
            xmlSerializer.endTag(null, "schedule");
        }
        xmlSerializer.endTag(null, "Schedules");
        xmlSerializer.endDocument();
        os.close();
    }

    /**
     * 必须在解析完schedules之后再调用
     */
    public static Map<Integer, Schedule> parseDailyScheduleConfig() {
        InputStream is = null;

        if (FileUtils.isFileEmpty(Constants.SCHEDULE_DAILY_SCHEDULE)) {
            for (int i = 0; i < 7; i++)
                dailySchedules.put(i, Schedule.EMPTY);
            return dailySchedules;
        }


        try {
            is = new FileInputStream(Constants.SCHEDULE_DAILY_SCHEDULE);

            XmlPullParser xmlPullParser = Xml.newPullParser();

            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();

            Integer dayOfWeek = -1;
            String scheduleName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = xmlPullParser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Days".equals(tag)) ;
                        else if ("day".equals(tag)) {
                            dayOfWeek = Integer.valueOf(xmlPullParser.getAttributeValue(null, "dayOfWeek"));
                            scheduleName = String.valueOf(xmlPullParser.nextText());
                            Map.Entry<Integer, Schedule> dailyScheduleEntry = new LinkedHashMap.SimpleEntry<>(dayOfWeek, scheduleName.equals(Schedule.EMPTY.getName()) ? Schedule.EMPTY : getScheduleByName(scheduleName));
                            dailySchedules.put(dailyScheduleEntry.getKey(), dailyScheduleEntry.getValue());
                            dailySchedulesObservable.notifyObservers(dailyScheduleEntry);
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (IOException e) {
            Log.e("Center/SN", e.getLocalizedMessage(), e);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dailySchedules;
    }

    public static void saveDailySchedule() throws IOException {
        OutputStream os = null;

        FileUtils.checkFileExistOrCreate(Constants.SCHEDULE_DAILY_SCHEDULE);

        try {
            os = new FileOutputStream(Constants.SCHEDULE_DAILY_SCHEDULE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(os, "utf-8");
        xmlSerializer.startDocument("utf-8", true);
        xmlSerializer.startTag(null, "Days");
        for (Integer dayOfWeek : dailySchedules.keySet()) {
            Schedule schedule = dailySchedules.get(dayOfWeek);
            xmlSerializer.startTag(null, "day");
            xmlSerializer.attribute(null, "dayOfWeek", String.valueOf(dayOfWeek));
            xmlSerializer.text(schedule.getName());
            xmlSerializer.endTag(null, "day");
        }
        xmlSerializer.endTag(null, "Days");
        xmlSerializer.endDocument();
        os.close();
    }

    public static Map<String, Schedule> parseSpecificDaysConfig() {
        InputStream is = null;
        if (FileUtils.isFileEmpty(Constants.SCHEDULE_SPECIFIC_SCHEDULE))
            return specificDays;

        try {
            is = new FileInputStream(Constants.SCHEDULE_SPECIFIC_SCHEDULE);

            XmlPullParser xmlPullParser = Xml.newPullParser();

            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();

            String date = "";
            String scheduleName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = xmlPullParser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Days".equals(tag)) ;
                        else if ("day".equals(tag)) {
                            date = xmlPullParser.getAttributeValue(null, "date");
                            scheduleName = String.valueOf(xmlPullParser.nextText());
                            Map.Entry<String, Schedule> specificDaySchedule = new LinkedHashMap.SimpleEntry<>(date, scheduleName.equals("null") ? null : getScheduleByName(scheduleName));
                            specificDays.put(specificDaySchedule.getKey(), specificDaySchedule.getValue());
                            specificDaysObservable.notifyObservers(specificDaySchedule);
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (IOException e) {
            Log.e("Center/SN", e.getLocalizedMessage(), e);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return specificDays;
    }

    public static void saveSpecificDays() throws IOException {
        OutputStream os = null;

        FileUtils.checkFileExistOrCreate(Constants.SCHEDULE_SPECIFIC_SCHEDULE);

        try {
            os = new FileOutputStream(Constants.SCHEDULE_SPECIFIC_SCHEDULE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(os, "utf-8");
        xmlSerializer.startDocument("utf-8", true);
        xmlSerializer.startTag(null, "Days");
        for (String date : specificDays.keySet()) {
            xmlSerializer.startTag(null, "day");
            xmlSerializer.attribute(null, "date", date);
            xmlSerializer.text(specificDays.get(date) != null ? specificDays.get(date).getName() : "null");
            xmlSerializer.endTag(null, "day");
        }
        xmlSerializer.endTag(null, "Days");
        xmlSerializer.endDocument();
        os.close();
    }

    public static Schedule getDailyScheduleByDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        //NOTE: 因为程序中范围为(0~6)，Calendar范围为(1~7)
        return dailySchedules.get(day - 1) == null ? Schedule.EMPTY : dailySchedules.get(day - 1);
    }

    public static Schedule getDailyScheduleByDate(String dateString) {
        try {
            return getDailyScheduleByDate((SPECIFIC_DAY_FORMAT.parse(dateString)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return Schedule.EMPTY;
    }

    /**
     * @param date
     * @return 可能为null
     */
    public static Schedule getScheduleByDate(Date date) {
        //将date格式化
        String dateString = Schedules.SPECIFIC_DAY_FORMAT.format(date);
        //如果在特殊日子里就加载特殊日子计划表
        if (specificDays.containsKey(dateString))
            return specificDays.get(dateString);
        //不然就加载普通计划表
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        //NOTE: 因为程序中范围为(0~6)，Calendar范围为(1~7)
        return dailySchedules.get(day - 1) == null ? Schedule.EMPTY : dailySchedules.get(day - 1);
    }

    public static Schedule getScheduleByDate(String dateString) throws ParseException {
        return getScheduleByDate(SPECIFIC_DAY_FORMAT.parse(dateString));
    }

    public static Schedule getSpecificDay(String dateString) {
        return specificDays.get(dateString);
    }

    public static Schedule getScheduleByName(String name) {
        for (Schedule schedule :
                schedules) {
            if (schedule.getName().equals(name))
                return schedule;
        }
        return null;
    }

    public static List<Schedule> getSchedules() {
        return schedules;
    }

    public static Map<Integer, Schedule> getDailySchedules() {
        return dailySchedules;
    }

    public static Map<String, Schedule> getSpecificDays() {
        return specificDays;
    }

    public static ScheduleItem getDoingScheduleItem() throws Schedule.NoScheduleItemException, Schedule.NoScheduleException {
        Schedule nowSchedule = getScheduleToday();
        if (nowSchedule == null)
            throw new Schedule.NoScheduleException();
        return nowSchedule.getDoingScheduleItem();
    }

    public static Schedule getScheduleToday() {
        Schedule scheduleToday = getScheduleByDate(new Date());
        if (scheduleToday != null)
            return scheduleToday;
        else
            throw new RuntimeException("Internal Error");
        //无论如何，不可能有null，只有Schedule.EMPTY
    }

    public static boolean isInSpecificDay(Date date) {
        String dateString = SPECIFIC_DAY_FORMAT.format(date);
        return specificDays.containsKey(dateString);
    }
}

