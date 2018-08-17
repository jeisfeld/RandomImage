package de.jeisfeld.randomimage.widgets;

import android.preference.PreferenceFragment;

import java.util.ArrayList;

import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;

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
		return ImageRegistry.getStandardImageListNames(ListFiltering.HIDE_BY_REGEXP);
	}
}
