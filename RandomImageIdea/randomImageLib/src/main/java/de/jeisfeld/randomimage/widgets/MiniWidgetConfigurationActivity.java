package de.jeisfeld.randomimage.widgets;

import android.preference.PreferenceFragment;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class MiniWidgetConfigurationActivity extends GenericWidgetConfigurationActivity {
	@Override
	protected final void configure(final int appWidgetId, final String listName) {
		MiniWidget.configure(appWidgetId, listName);
	}

	@Override
	protected final PreferenceFragment createFragment() {
		return new MiniWidgetConfigurationFragment();
	}
}
