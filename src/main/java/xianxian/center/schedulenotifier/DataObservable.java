package xianxian.center.schedulenotifier;

import java.util.Observable;

/**
 *
 */
public class DataObservable extends Observable {

    @Override
    public void notifyObservers(Object arg) {
        //既然我会notify，那我就有Change
        setChanged();
        super.notifyObservers(arg);
    }
}
