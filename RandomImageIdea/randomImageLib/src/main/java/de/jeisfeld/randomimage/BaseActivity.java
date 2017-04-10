package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Base class of activities within the app.
 */
public abstract class BaseActivity extends Activity {
	@Override
	protected final void attachBaseContext(final Context newBase) {
		super.attachBaseContext(Application.getAppContext());
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Update title
		try {
			ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
			setTitle(activityInfo.labelRes);
		}
		catch (NameNotFoundException ex) {
			Log.e(Application.TAG, "Error while getting activity info. " + ex.getMessage(), ex);
		}
	}
}
