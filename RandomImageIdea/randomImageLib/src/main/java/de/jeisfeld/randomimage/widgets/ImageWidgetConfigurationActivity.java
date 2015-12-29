package de.jeisfeld.randomimage.widgets;

import android.preference.PreferenceFragment;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class ImageWidgetConfigurationActivity extends GenericWidgetConfigurationActivity {
	@Override
	protected final void configure(final int appWidgetId, final String listName) {
		ImageWidget.configure(appWidgetId, listName, GenericWidget.UpdateType.NEW_LIST);
	}

	@Override
	protected final PreferenceFragment createFragment() {
		return new ImageWidgetConfigurationFragment();
	}
}
