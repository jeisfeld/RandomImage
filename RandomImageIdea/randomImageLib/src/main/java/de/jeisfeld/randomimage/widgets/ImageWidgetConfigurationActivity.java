package de.jeisfeld.randomimage.widgets;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class ImageWidgetConfigurationActivity extends GenericImageWidgetConfigurationActivity {
	@Override
	protected final void configure(final int appWidgetId, final String listName) {
		ImageWidget.configure(appWidgetId, listName, GenericWidget.UpdateType.NEW_LIST);
	}

	@Override
	protected final GenericImageWidgetConfigurationFragment createFragment() {
		return new ImageWidgetConfigurationFragment();
	}
}
