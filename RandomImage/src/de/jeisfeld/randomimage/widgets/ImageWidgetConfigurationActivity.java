package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import android.app.DialogFragment;
import android.os.Bundle;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;

/**
 * Activity for the configuration of the MiniWidget.
 */
public class ImageWidgetConfigurationActivity extends WidgetConfigurationActivity {
	@Override
	protected final void initialize(final Bundle savedInstanceState, final int appWidgetId, final String listName) {
		final String[] durationNames = getResources().getStringArray(R.array.timer_duration_names);
		final String[] durationValueStrings = getResources().getStringArray(R.array.timer_durations);

		final long[] durationValues = new long[durationValueStrings.length];
		for (int i = 0; i < durationValueStrings.length; i++) {
			durationValues[i] = Long.parseLong(durationValueStrings[i]);
		}
		final ArrayList<String> durationNameList = new ArrayList<String>();
		for (int i = 0; i < durationNames.length; i++) {
			durationNameList.add(durationNames[i]);
		}

		DialogUtil.displayListSelectionDialog(this, new SelectFromListDialogListener() {
			/**
			 * The serial version id.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text) {
				ImageWidget.configure(appWidgetId, listName, durationValues[position]);
				setResult(true);
				finish();
			}

			@Override
			public void onDialogNegativeClick(final DialogFragment dialog) {
				setResult(false);
				finish();
			}
		}, R.string.title_dialog_select_change_frequency, durationNameList,
				R.string.dialog_select_image_change_frequency);

	}
}
