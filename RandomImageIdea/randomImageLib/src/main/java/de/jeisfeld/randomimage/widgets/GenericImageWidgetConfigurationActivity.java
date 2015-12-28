package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.os.Bundle;

/**
 * Activity for the configuration of an image widget.
 */
public abstract class GenericImageWidgetConfigurationActivity extends GenericWidgetConfigurationActivity {
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		configure(appWidgetId, listName);
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
		GenericImageWidgetConfigurationFragment fragment =
				(GenericImageWidgetConfigurationFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = createFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			bundle.putBoolean(EXTRA_RECONFIGURE_WIDGET, reconfigureWidget);
			fragment.setArguments(bundle);

			getFragmentManager().beginTransaction().replace(android.R.id.content, fragment, FRAGMENT_TAG).commit();
			getFragmentManager().executePendingTransactions();
		}
	}

	/**
	 * Configure an instance of the widget.
	 *
	 * @param appWidgetId The widget id.
	 * @param listName    The list name to be used by the widget.
	 */
	protected abstract void configure(final int appWidgetId, final String listName);

	/**
	 * Create an instance of the configuration fragment.
	 *
	 * @return An instance of the configuration fragment.
	 */
	protected abstract GenericImageWidgetConfigurationFragment createFragment();

}
