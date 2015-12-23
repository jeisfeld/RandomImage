package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class ImageWidgetConfigurationActivity extends WidgetConfigurationActivity {
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";
	/**
	 * Intent flag to indicate if this is the initial configuration.
	 */
	protected static final String EXTRA_FIRST_START = "de.jeisfeld.randomimage.FIRST_START";

	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		ImageWidget.configure(appWidgetId, listName, GenericWidget.UpdateType.NEW_LIST);
		setResult(true);

		startConfigurationPage(appWidgetId, false);
	}

	@Override
	protected final void update(final Bundle savedInstanceState, final int appWidgetId) {
		startConfigurationPage(appWidgetId, true);
	}

	/**
	 * Start the configuration page of the widget.
	 *
	 * @param appWidgetId       the widgetId of the widget to be configured.
	 * @param reconfigureWidget Flag indicating if the widget is already configured.
	 */
	private void startConfigurationPage(final int appWidgetId, final boolean reconfigureWidget) {
		ImageWidgetConfigurationFragment fragment = (ImageWidgetConfigurationFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = new ImageWidgetConfigurationFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			bundle.putBoolean(EXTRA_RECONFIGURE_WIDGET, reconfigureWidget);
			fragment.setArguments(bundle);

			getFragmentManager().beginTransaction().replace(android.R.id.content, fragment, FRAGMENT_TAG).commit();
			getFragmentManager().executePendingTransactions();
		}
	}
}
