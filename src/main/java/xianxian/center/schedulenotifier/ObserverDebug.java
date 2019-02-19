package xianxian.center.schedulenotifier;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ObserverDebug implements Observer {
    public static ObserverDebug INSTANCE = new ObserverDebug();
    private static List<Observable> observables = new ArrayList<>();

    private ObserverDebug() {
    }

    public static void debug(@NonNull Observable... observable) {
        for (Observable targetObservable : observable) {
            targetObservable.addObserver(INSTANCE);
            observables.add(targetObservable);
        }
        //observables.addAll(Arrays.asList(observable));
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
        if (BuildConfig.DEBUG)
            Log.i("ObserverDebug", o + " " + arg);
    }
}
