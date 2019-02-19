package xianxian.center.schedulenotifier;

import android.util.Log;

import java.util.Observable;
import java.util.Observer;

/**
 *
 */
public class DataObservable extends Observable {
    private final String name;

    public DataObservable(String name) {
        this.name = name;
    }

    @Override
    public synchronized void notifyObservers(Object arg) {
        //既然我会notify，那我就有Change
        setChanged();
        super.notifyObservers(arg);
    }

    @Override
    public synchronized void addObserver(Observer o) {
        Log.i(name, o + " has added to " + name);
        super.addObserver(o);
    }

    @Override
    public synchronized void deleteObserver(Observer o) {
        Log.i(name, o + " has deleted");
        super.deleteObserver(o);
    }

    @Override
    public String toString() {
        return name;
    }
}
