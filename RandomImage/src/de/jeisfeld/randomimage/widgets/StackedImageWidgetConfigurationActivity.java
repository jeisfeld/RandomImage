package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class StackedImageWidgetConfigurationActivity extends WidgetConfigurationActivity {
	/**
	 * The minimum allowed timer duration for this widget.
	 */
	private static final long MIN_DURATION = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		final String[] durationNames = getResources().getStringArray(R.array.timer_duration_names);
		final String[] durationValueStrings = getResources().getStringArray(R.array.timer_durations);

		final ArrayList<Long> durationValueList = new ArrayList<Long>();
		final ArrayList<String> durationNameList = new ArrayList<String>();
		for (int i = 0; i < durationNames.length; i++) {
			long duration = Long.parseLong(durationValueStrings[i]);
			if (duration >= MIN_DURATION) {
				durationValueList.add(duration);
				durationNameList.add(durationNames[i]);
			}

		}

		DialogUtil.displayListSelectionDialog(this, new SelectFromListDialogListener() {
			/**
			 * The serial version id.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
				StackedImageWidget.configure(appWidgetId, listName, durationValueList.get(position));
				finish();
			}

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				StackedImageWidget.configure(appWidgetId, listName, 0);
				finish();
			}
		}, R.string.title_dialog_select_change_frequency, durationNameList,
				R.string.dialog_select_reshuffle_frequency);

	}
}
