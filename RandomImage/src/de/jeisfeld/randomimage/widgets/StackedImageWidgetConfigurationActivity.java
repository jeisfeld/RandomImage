package de.jeisfeld.randomimage.widgets;

import android.os.Bundle;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class StackedImageWidgetConfigurationActivity extends WidgetConfigurationActivity {
	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		StackedImageWidget.configure(appWidgetId, listName);
		finish();
	}
}
