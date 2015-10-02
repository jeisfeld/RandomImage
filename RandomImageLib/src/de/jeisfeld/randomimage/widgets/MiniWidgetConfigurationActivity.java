package de.jeisfeld.randomimage.widgets;

import android.os.Bundle;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class MiniWidgetConfigurationActivity extends WidgetConfigurationActivity {
	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		MiniWidget.configure(appWidgetId, listName);
		setResult(true);
		finish();
	}
}
