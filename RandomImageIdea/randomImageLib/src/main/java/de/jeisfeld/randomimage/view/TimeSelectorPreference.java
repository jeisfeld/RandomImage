package de.jeisfeld.randomimage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Arrays;
import java.util.Locale;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimagelib.R;

/**
 * Preference used to select a time span for timers.
 */
public class TimeSelectorPreference extends DialogPreference {
	/**
	 * The standard entries of the Spinner.
	 */
	private static final SpinnerEntry[] DEFAULT_SPINNER_ENTRIES;
	/**
	 * The current entries of the Spinner.
	 */
	private SpinnerEntry[] mSpinnerEntries;
	/**
	 * The EditText for the number of seconds/minutes etc.
	 */
	private EditText mEditText;
	/**
	 * The spinner for the seletion of units (seconds, minutes etc.).
	 */
	private Spinner mSpinner;

	static {
		DEFAULT_SPINNER_ENTRIES = SpinnerEntry.getDefaultSpinnerEntries(Application.getAppContext());
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
		// VARIABLE_DISTANCE:OFF
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimeSelectorPreference);
		String indefiniteValue = a.getString(R.styleable.TimeSelectorPreference_indefiniteValue);
		boolean allowSeconds = a.getBoolean(R.styleable.TimeSelectorPreference_allowSeconds, true);
		boolean allowLongDuration = a.getBoolean(R.styleable.TimeSelectorPreference_allowLongDuration, true);
		a.recycle();
		// VARIABLE_DISTANCE:ON

		mSpinnerEntries = SpinnerEntry.setIndefiniteValue(SpinnerEntry.getDefaultSpinnerEntries(context), indefiniteValue);
		if (!allowSeconds) {
			mSpinnerEntries = SpinnerEntry.hideSeconds(mSpinnerEntries);
		}
		if (!allowLongDuration) {
			mSpinnerEntries = SpinnerEntry.hideLongDurations(mSpinnerEntries);
		}
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
	protected final void onBindDialogView(final View view) {
		super.onBindDialogView(view);

		mEditText = view.findViewById(R.id.editTextUnits);
		mSpinner = view.findViewById(R.id.spinnerUnits);

		ArrayAdapter<SpinnerEntry> dataAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, mSpinnerEntries);
		mSpinner.setAdapter(dataAdapter);

		String oldValue = getSharedPreferences().getString(getKey(), null);

		if (oldValue != null) {
			try {
				DialogData dialogData = DialogData.fromValue(mSpinnerEntries, Long.parseLong(oldValue));
				mEditText.setText(String.format(Locale.getDefault(), "%d", dialogData.mUnitsEntry));
				mSpinner.setSelection(Arrays.asList(mSpinnerEntries).indexOf(dialogData.mSpinnerEntry));
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
	public final String getSummaryFromValue(final String value) {
		return DialogData.fromValue(mSpinnerEntries, Long.parseLong(value)).getDisplayText();
	}

	/**
	 * Get the summary text from the value.
	 *
	 * @param context the context.
	 * @param value   The value.
	 * @return The summary text.
	 */
	public static String getDefaultSummaryFromValue(final Context context, final String value) {
		DialogData dialogData = DialogData.fromValue(DEFAULT_SPINNER_ENTRIES, Long.parseLong(value));
		return dialogData.getValue() == 0 ? null : dialogData.getDisplayText();
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
		public static SpinnerEntry[] getDefaultSpinnerEntries(final Context context) {
			String[] names = context.getResources().getStringArray(R.array.timer_unit_names);
			String[] values = context.getResources().getStringArray(R.array.timer_unit_values);

			SpinnerEntry[] result = new SpinnerEntry[values.length];
			for (int i = 0; i < values.length; i++) {
				result[i] = new SpinnerEntry(names[i], values[i]);
			}

			return result;
		}

		/**
		 * Clone the list of spinner entries, setting the indefinite display value.
		 *
		 * @param spinnerEntries  The list of spinner entries.
		 * @param indefiniteValue The indefinite display value.
		 * @return The cloned list.
		 */
		public static SpinnerEntry[] setIndefiniteValue(final SpinnerEntry[] spinnerEntries, final String indefiniteValue) {
			SpinnerEntry[] result = new SpinnerEntry[spinnerEntries.length];
			for (int i = 0; i < spinnerEntries.length; i++) {
				result[i] = new SpinnerEntry(spinnerEntries[i]);
				if (result[i].getValue() == 0) {
					result[i].mDisplayString = indefiniteValue;
				}
			}
			return result;
		}

		/**
		 * Remove the "Seconds" entry from the spinner.
		 *
		 * @param spinnerEntries The list of spinner entries.
		 * @return The cloned list without seconds.
		 */
		private static SpinnerEntry[] hideSeconds(final SpinnerEntry[] spinnerEntries) {
			return Arrays.copyOfRange(spinnerEntries, 1, spinnerEntries.length);
		}

		/**
		 * Remove the "Days", "Weeks", "Months", "Years" entries from the spinner.
		 *
		 * @param spinnerEntries The list of spinner entries.
		 * @return The cloned list without seconds.
		 */
		private static SpinnerEntry[] hideLongDurations(final SpinnerEntry[] spinnerEntries) {
			SpinnerEntry[] result = new SpinnerEntry[spinnerEntries.length - 4]; // MAGIC_NUMBER
			System.arraycopy(spinnerEntries, 0, result, 0, result.length - 1);
			result[result.length - 1] = spinnerEntries[spinnerEntries.length - 1];
			return result;
		}

		/**
		 * Create a spinner entry.
		 *
		 * @param displayString The display String.
		 * @param value         The value.
		 */
		private SpinnerEntry(final String displayString, final String value) {
			mDisplayString = displayString;
			mValue = Long.parseLong(value);
		}

		/**
		 * Clone a spinnerEntry.
		 *
		 * @param spinnerEntry The spinnerEntry to be cloned.
		 */
		private SpinnerEntry(final SpinnerEntry spinnerEntry) {
			this.mDisplayString = spinnerEntry.mDisplayString;
			this.mValue = spinnerEntry.mValue;
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
				spinner.setSelection(DEFAULT_SPINNER_ENTRIES.length - 1);
			}

			try {
				result.mSpinnerEntry = (SpinnerEntry) spinner.getSelectedItem();
			}
			catch (ArrayIndexOutOfBoundsException e) {
				// ignore
			}
			return result;
		}

		/**
		 * Get the dialog data from the value (in seconds).
		 *
		 * @param spinnerEntries The list of spinner entries.
		 * @param value          The value in seconds.
		 * @return The dialog data.
		 */
		public static DialogData fromValue(final SpinnerEntry[] spinnerEntries, final long value) {
			DialogData result = new DialogData();
			if (value == 0) {
				result.mSpinnerEntry = spinnerEntries[spinnerEntries.length - 1];
				result.mUnitsEntry = 0;
				return result;
			}

			for (int i = spinnerEntries.length - 1; i >= 0; i--) {
				long spinnerValue = spinnerEntries[i].getValue();

				if (spinnerValue != 0 && value % spinnerValue == 0) {
					result.mSpinnerEntry = spinnerEntries[i];
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
			return mSpinnerEntry == null ? 0 : mUnitsEntry * mSpinnerEntry.getValue();
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
				unitName = originalUnitName.replaceAll("[()]", "");
			}
			return mUnitsEntry + " " + unitName;
		}
	}
}
