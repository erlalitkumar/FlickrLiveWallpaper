package novoda.wallpaper.flickr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		getPreferenceManager().setSharedPreferencesName(Constants.Prefs.NAME);
		addPreferencesFromResource(R.xml.flickr_settings);
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
	}
}