package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.util.ImageRegistry;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class StackedImageWidgetConfigurationActivity extends GenericWidgetConfigurationActivity {
	@Override
	protected final void configure(final int appWidgetId, final String listName) {
		StackedImageWidget.configure(appWidgetId, listName, GenericWidget.UpdateType.NEW_LIST);
	}

	@Override
	protected final PreferenceFragment createFragment() {
		return new StackedImageWidgetConfigurationFragment();
	}

	@Override
	protected final ArrayList<String> getImageListNames() {
		return ImageRegistry.getStandardImageListNames();
	}
}
