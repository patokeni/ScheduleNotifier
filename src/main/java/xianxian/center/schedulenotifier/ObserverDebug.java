package xianxian.center.schedulenotifier;

import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ObserverDebug implements Observer {
    public static ObserverDebug INSTANCE = new ObserverDebug();
    private static List<Observable> observables;

    private ObserverDebug() {
    }

    public static void load(Observable... observable) {
        observables.addAll(Arrays.asList(observable));
    }

    public static void dispose() {
        for (Observable o :
                observables) {
            o.deleteObserver(INSTANCE);
        }
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
        Log.i("ObserverDebug", o + " " + arg);
    }
}
