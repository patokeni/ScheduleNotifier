package xianxian.center.schedulenotifier;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import xianxian.center.Constants;
import xianxian.center.utils.FileUtils;

public class Types {
    public static final Observable typesObservable = new DataObservable("SN/Types");
    private static Map<String, Type> types = new HashMap<>();

    public static void load() {
        ObserverDebug.debug(typesObservable);
        //解析类型
        parseTypesConfig();

    }

    public static Map<String, Type> parseTypesConfig() {
        if (FileUtils.isFileEmpty(Constants.SCHEDULE_TYPES))
            return types;

        try (InputStream is = new FileInputStream(Constants.SCHEDULE_TYPES)){
            XmlPullParser xmlPullParser = Xml.newPullParser();

            xmlPullParser.setInput(is, "utf-8");

            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag = xmlPullParser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Types".equals(tag)) ;
                        else if ("type".equals(tag)) {
                            String typeName = xmlPullParser.getAttributeValue(null, "typeName");
                            boolean isCustomMessage = Boolean.parseBoolean(xmlPullParser.getAttributeValue(null, "isCustomMessage"));
                            Type type = new Type(typeName);
                            if (isCustomMessage)
                                type.setCustomMessage(xmlPullParser.nextText());
                            types.put(typeName, type);
                        }
                        break;
                }
                eventType = xmlPullParser.next();
            }
        } catch (IOException |XmlPullParserException e) {
            //no-op
        }
        return types;
    }

    public static void saveTypes() throws IOException {
        OutputStream os = null;

        FileUtils.checkFileExistOrCreate(Constants.SCHEDULE_TYPES);

        try {
            os = new FileOutputStream(Constants.SCHEDULE_TYPES);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(os, "utf-8");
        xmlSerializer.startDocument("utf-8", true);
        xmlSerializer.startTag(null, "Types");
        for (String typeName : types.keySet()) {
            Type type = types.get(typeName);
            xmlSerializer.startTag(null, "type");
            xmlSerializer.attribute(null, "typeName", typeName);
            xmlSerializer.attribute(null, "isCustomMessage", String.valueOf(type.isCustomMessage()));
            xmlSerializer.text(type.isCustomMessage() ? type.getMessage() : "null");
            xmlSerializer.endTag(null, "type");
        }
        xmlSerializer.endTag(null, "Types");
        xmlSerializer.endDocument();
        os.close();
    }

    public static Type getOrCreate(String typeString) {
        if (!types.containsKey(typeString))
            addType(typeString);
        return types.get(typeString);
    }

    public static Map<String, Type> getTypes() {
        return types;
    }

    public static void addType(Type type) {
        types.put(type.getTypeName(), type);
        typesObservable.notifyObservers();
        try {
            saveTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addType(String typeString) {
        types.put(typeString, new Type(typeString));
        typesObservable.notifyObservers();
        try {
            saveTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addTypeWithCustomMessage(String typeName, String customMessage) {
        types.put(typeName, new Type(typeName, customMessage));
        typesObservable.notifyObservers();
        try {
            saveTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeType(Type type) {
        if (!type.hasUser()) {
            types.remove(type);
        } else {
            type.setCustomMessage(null);
        }
        typesObservable.notifyObservers();
        try {
            saveTypes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
