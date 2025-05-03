package de.jeisfeld.randomimage;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Base class of activities within the app.
 */
public abstract class BasePreferenceActivity extends PreferenceActivity {
	@Override
	protected final void attachBaseContext(final Context newBase) {
		super.attachBaseContext(Application.createContextWrapperForLocale(newBase));
	}

	// OVERRIDABLE
	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View root = getWindow().getDecorView().findViewById(android.R.id.content);
		root.setOnApplyWindowInsetsListener((v, insets) -> {
			v.setPadding(insets.getSystemWindowInsetLeft(),
					insets.getSystemWindowInsetTop(),
					insets.getSystemWindowInsetRight(),
					insets.getSystemWindowInsetBottom());
			return insets.consumeSystemWindowInsets();
		});
		root.setFitsSystemWindows(true);

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
