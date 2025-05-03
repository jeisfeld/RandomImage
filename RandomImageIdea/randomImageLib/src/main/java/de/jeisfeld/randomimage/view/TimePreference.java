package de.jeisfeld.randomimage.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Locale;

public class TimePreference extends DialogPreference {
	private int lastHour = 0;
	private int lastMinute = 0;
	private TimePicker picker;

	public TimePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setPositiveButtonText("Set");
		setNegativeButtonText("Cancel");
	}

	@Override
	protected View onCreateDialogView() {
		picker = new TimePicker(getContext());
		picker.setIs24HourView(true);
		if (Build.VERSION.SDK_INT >= 23) {
			picker.setHour(lastHour);
			picker.setMinute(lastMinute);
		}
		else {
			picker.setCurrentHour(lastHour);
			picker.setCurrentMinute(lastMinute);
		}
		return picker;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		if (Build.VERSION.SDK_INT >= 23) {
			picker.setHour(lastHour);
			picker.setMinute(lastMinute);
		}
		else {
			picker.setCurrentHour(lastHour);
			picker.setCurrentMinute(lastMinute);
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			if (Build.VERSION.SDK_INT >= 23) {
				lastHour = picker.getHour();
				lastMinute = picker.getMinute();
			}
			else {
				lastHour = picker.getCurrentHour();
				lastMinute = picker.getCurrentMinute();
			}
			String time = String.format(Locale.getDefault(), "%02d:%02d", lastHour, lastMinute);
			if (callChangeListener(time)) {
				persistString(time);
				setSummary(time);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		String time = restorePersistedValue ? getPersistedString("00:00") : (String) defaultValue;
		lastHour = Integer.parseInt(time.split(":")[0]);
		lastMinute = Integer.parseInt(time.split(":")[1]);
		setSummary(time);
	}
}
