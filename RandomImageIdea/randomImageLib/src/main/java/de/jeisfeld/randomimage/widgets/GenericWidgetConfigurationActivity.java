package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.jeisfeld.randomimage.ConfigureImageListActivity;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.ListFiltering;
import de.jeisfeld.randomimagelib.R;

/**
 * Generic activity for the configuration of widgets. First select the image list, then continue initialization.
 */
public abstract class GenericWidgetConfigurationActivity extends Activity {
	/**
	 * Resource name for the flag to indicate if the widget should just be reconfigured.
	 */
	public static final String EXTRA_RECONFIGURE_WIDGET = "de.jeisfeld.randomimagelib.RECONFIGURE_WIDGET";
	/**
	 * The fragment tag.
	 */
	private static final String FRAGMENT_TAG = "FRAGMENT_TAG";

	/**
	 * The Intent used as result.
	 */
	private Intent mResultValue;

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		// Retrieve the widget id.
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			finish();
			return;
		}
		final int appWidgetId =
				extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		// If they gave us an intent without the widget id, just bail.
		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}
		mResultValue = new Intent();
		mResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(false);

		final boolean reconfigureWidget = extras.getBoolean(EXTRA_RECONFIGURE_WIDGET, false);

		ArrayList<String> listNames = getImageListNames();

		if (reconfigureWidget) {
			startConfigurationPage(appWidgetId, true);
		}
		else {
			List<String> imageListNames = ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);

			if (imageListNames.size() == 0) {
				// On first startup need to create default list.
				ImageRegistry.getCurrentImageListRefreshed(true);
				String listName = ImageRegistry.getCurrentListName();

				initialize(savedInstanceState, appWidgetId, listName);
				ConfigureImageListActivity.startActivity(this, listName);
			}
			else if (imageListNames.size() == 1) {
				initialize(savedInstanceState, appWidgetId, imageListNames.get(0));
			}
			else {
				DialogUtil.displayListSelectionDialog(this, new SelectFromListDialogListener() {
					@Override
					public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
						initialize(savedInstanceState, appWidgetId, text);
					}

					@Override
					public void onDialogNegativeClick(final DialogFragment dialog) {
						finish();
					}
				}, R.string.title_dialog_select_list_name, listNames, R.string.dialog_select_list_for_widget);
			}
		}

	}

	/**
	 * Get the list of image list names allowed for this widget.
	 *
	 * @return The list of image list names.
	 */
	// OVERRIDABLE
	protected ArrayList<String> getImageListNames() {
		return ImageRegistry.getImageListNames(ListFiltering.HIDE_BY_REGEXP);
	}

	/**
	 * Set the result of the activity.
	 *
	 * @param success true if widget successfully created.
	 */
	protected final void setResult(final boolean success) {
		setResult(success ? RESULT_OK : RESULT_CANCELED, mResultValue);
	}

	/**
	 * Initialize the configuration view after retrieving the name of the image list associated to the widget. Does the
	 * same things as the usual onCreate() method.
	 *
	 * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the
	 *                           data it most recently supplied in onSaveInstanceState
	 * @param appWidgetId        the widgetId of the widget to be configured.
	 * @param listName           The selected image list name.
	 */
	private void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		configure(appWidgetId, listName);
		setResult(true);

		startConfigurationPage(appWidgetId, false);
	}

	/**
	 * Start the configuration page of the widget.
	 *
	 * @param appWidgetId       the widgetId of the widget to be configured.
	 * @param reconfigureWidget Flag indicating if the widget is already configured.
	 */
	private void startConfigurationPage(final int appWidgetId, final boolean reconfigureWidget) {
		PreferenceFragment fragment = (PreferenceFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
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
	protected abstract PreferenceFragment createFragment();
}
