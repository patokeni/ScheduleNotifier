package xianxian.center.schedulenotifier;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceScreen;

import xianxian.center.main.IFragment;

public class SettingsFragment extends android.support.v7.preference.PreferenceFragmentCompat implements IFragment {
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R2.xml.sn_preference);

    }

    /**
     * Called during {@link #onCreate(Bundle)} to supply the preferences for this fragment.
     * Subclasses are expected to call {@link #setPreferenceScreen(PreferenceScreen)} either
     * directly or via helper methods such as {@link #addPreferencesFromResource(int)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     * @param rootKey            If non-null, this preference fragment should be rooted at the
     *                           {@link PreferenceScreen} with this key.
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //setPreferencesFromResource(R,null);
    }

    @Override
    public String tag() {
        return "sn_set";
    }

    @Override
    public int menuID() {
        return R.id.nav_sn_settings;
    }
}
