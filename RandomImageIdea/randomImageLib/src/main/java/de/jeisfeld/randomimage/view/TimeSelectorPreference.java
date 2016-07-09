package de.jeisfeld.randomimage.view;

import java.util.Arrays;
import java.util.Locale;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimagelib.R;

/**
 * Preference used to select a time span for timers.
 */
public class TimeSelectorPreference extends DialogPreference {
	/**
	 * The EditText for the number of seconds/minutes etc.
	 */
	private EditText mEditText;
	/**
	 * The spinner for the seletion of units (seconds, minutes etc.).
	 */
	private Spinner mSpinner;
	/**
	 * The entries of the Spinner.
	 */
	private static final SpinnerEntry[] SPINNER_ENTRIES;

	static {
		SPINNER_ENTRIES = SpinnerEntry.getSpinnerEntries(Application.getAppContext());
	}

	/**
	 * Standard constructor.
	 *
	 * @param context The Context this is associated with.
	 * @param attrs   (from Preference) The attributes of the XML tag that is inflating the preference.
	 */
	public TimeSelectorPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.dialog_time_selector);
	}

	/**
	 * Standard constructor.
	 *
	 * @param context The Context this is associated with.
	 */
	public TimeSelectorPreference(final Context context) {
		this(context, null);
	}

	@Override
	protected void onBindDialogView(final View view) {
		super.onBindDialogView(view);

		mEditText = (EditText) view.findViewById(R.id.editTextUnits);
		mSpinner = (Spinner) view.findViewById(R.id.spinnerUnits);

		ArrayAdapter<SpinnerEntry> dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, SPINNER_ENTRIES);
		mSpinner.setAdapter(dataAdapter);

		String oldValue = getSharedPreferences().getString(getKey(), null);

		if (oldValue != null) {
			try {
				DialogData dialogData = DialogData.fromValue(Long.parseLong(oldValue));
				mEditText.setText(String.format(Locale.getDefault(), "%d", dialogData.mUnitsEntry));
				mSpinner.setSelection(dialogData.getSpinnerPosition());
			}
			catch (NumberFormatException e) {
				// do nothing
			}
		}
	}


	/**
	 * Fill the value after closing the dialog.
	 *
	 * @param positiveResult (from DialogPreference) positiveResult Whether the positive button was clicked (true), or the negative
	 *                       button was clicked or the dialog was canceled (false).
	 */
	@Override
	protected final void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			DialogData dialogData = DialogData.fromDialog(mEditText, mSpinner);
			String value = Long.toString(dialogData.getValue());

			if (callChangeListener(value)) {
				persistString(value);
			}
		}
	}

	/**
	 * Get the summary text from the value.
	 *
	 * @param value The value.
	 * @return The summary text.
	 */
	public static String getSummaryFromValue(final String value) {
		return DialogData.fromValue(Long.parseLong(value)).getDisplayText();
	}

	/**
	 * Bean class for a Spinner entry.
	 */
	private static final class SpinnerEntry {
		/**
		 * The display String.
		 */
		private String mDisplayString;

		public String getDisplayString() {
			return mDisplayString;
		}

		/**
		 * The value.
		 */
		private long mValue;

		public long getValue() {
			return mValue;
		}

		/**
		 * Get all entries of the spinner.
		 *
		 * @param context The context.
		 * @return The array of spinner entries.
		 */
		public static SpinnerEntry[] getSpinnerEntries(final Context context) {
			String[] names = context.getResources().getStringArray(R.array.timer_unit_names);
			String[] values = context.getResources().getStringArray(R.array.timer_unit_values);

			SpinnerEntry[] result = new SpinnerEntry[values.length];
			for (int i = 0; i < values.length; i++) {
				result[i] = new SpinnerEntry(names[i], values[i]);
			}

			return result;
		}

		/**
		 * Create a spinner entry.
		 *
		 * @param displayString The display String.
		 * @param value         Thevlue.
		 */
		private SpinnerEntry(final String displayString, final String value) {
			mDisplayString = displayString;
			mValue = Long.parseLong(value);
		}

		@Override
		public String toString() {
			return getDisplayString();
		}
	}

	/**
	 * Data holder for the dialog entry.
	 */
	private static class DialogData {
		/**
		 * The selected number of units.
		 */
		private long mUnitsEntry;
		/**
		 * The spinner entry.
		 */
		private SpinnerEntry mSpinnerEntry;

		/**
		 * Fill from the layout values.
		 *
		 * @param editText The EditText for the units.
		 * @param spinner  The spinner for the unit type.
		 * @return The dialog data.
		 */
		public static DialogData fromDialog(final EditText editText, final Spinner spinner) {
			DialogData result = new DialogData();
			try {
				result.mUnitsEntry = Long.parseLong(editText.getText().toString());
			}
			catch (NumberFormatException e) {
				result.mUnitsEntry = 1;
			}

			if (result.mUnitsEntry == 0) {
				result.mUnitsEntry = 1;
				spinner.setSelection(SPINNER_ENTRIES.length - 1);
			}

			result.mSpinnerEntry = (SpinnerEntry) spinner.getSelectedItem();
			return result;
		}

		/**
		 * Get the dialog data from the value (in seconds).
		 *
		 * @param value The value in seconds.
		 * @return The dialog data.
		 */
		public static DialogData fromValue(final long value) {
			DialogData result = new DialogData();
			if (value == 0) {
				result.mSpinnerEntry = SPINNER_ENTRIES[SPINNER_ENTRIES.length - 1];
				result.mUnitsEntry = 0;
				return result;
			}

			for (int i = SPINNER_ENTRIES.length - 2; i >= 0; i--) {
				Long spinnerValue = SPINNER_ENTRIES[i].getValue();
				if (value % spinnerValue == 0) {
					result.mSpinnerEntry = SPINNER_ENTRIES[i];
					result.mUnitsEntry = value / spinnerValue;
					return result;
				}
			}
			Log.e(Application.TAG, "TimeSelectorPreference: Missing Spinner value 1?");
			return result;
		}

		/**
		 * Get the value in seconds.
		 *
		 * @return The value.
		 */
		public long getValue() {
			return mUnitsEntry * mSpinnerEntry.getValue();
		}

		/**
		 * Get the position of the Spinner entry.
		 *
		 * @return The position of the Spinner entry.
		 */
		public int getSpinnerPosition() {
			return Arrays.asList(SPINNER_ENTRIES).indexOf(mSpinnerEntry);
		}

		/**
		 * Get the text to be displayed in the preference summary.
		 *
		 * @return The text to be displayed.
		 */
		public String getDisplayText() {
			String originalUnitName = mSpinnerEntry.getDisplayString();
			String unitName = originalUnitName;
			if (mUnitsEntry == 0) {
				return unitName;
			}
			else if (mUnitsEntry == 1) {
				int parenthesisIndex = originalUnitName.indexOf('(');
				if (parenthesisIndex > 0) {
					unitName = originalUnitName.substring(0, parenthesisIndex);
				}
			}
			else {
				unitName = originalUnitName.replaceAll("[\\(\\)]", "");
			}
			return Long.toString(mUnitsEntry) + " " + unitName;
		}
	}
}
