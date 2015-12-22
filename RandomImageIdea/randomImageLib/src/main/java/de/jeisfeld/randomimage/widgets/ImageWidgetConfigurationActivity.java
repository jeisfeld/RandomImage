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

	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		ImageWidget.configure(appWidgetId, listName);
		setResult(true);

		update(savedInstanceState, appWidgetId);
	}

	@Override
	protected final void update(final Bundle savedInstanceState, final int appWidgetId) {
		ImageWidgetConfigurationFragment fragment = (ImageWidgetConfigurationFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
		if (fragment == null) {
			fragment = new ImageWidgetConfigurationFragment();
			Bundle bundle = new Bundle();
			bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			fragment.setArguments(bundle);

			getFragmentManager().beginTransaction().replace(android.R.id.content, fragment, FRAGMENT_TAG).commit();
			getFragmentManager().executePendingTransactions();
		}
	}
}
