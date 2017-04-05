package de.jeisfeld.randomimage.view;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

/**
 * A MultiSelectListPreference that allows to update entries when clicking.
 */
public class DynamicMultiSelectListPreference extends MultiSelectListPreference {

	/**
	 * The listener that runs before opening the preference.
	 */
	private DynamicListPreferenceOnClickListener mListener;

	/**
	 * Standard constructor.
	 *
	 * @param context The Context this is associated with.
	 * @param attrs   (from Preference) The attributes of the XML tag that is inflating the preference.
	 */
	public DynamicMultiSelectListPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Standard constructor.
	 *
	 * @param context The Context this is associated with.
	 */
	public DynamicMultiSelectListPreference(final Context context) {
		this(context, null);
	}

	@Override
	protected final void onClick() {
		boolean onClickResult = true;
		if (mListener != null) {
			onClickResult = mListener.onClick(this);
		}
		if (onClickResult) {
			super.onClick();
		}
	}

	/**
	 * Set the listener that should be running before opening the preference.
	 *
	 * @param listener The listener.
	 */
	public void setOnClickListener(final DynamicListPreferenceOnClickListener listener) {
		mListener = listener;
	}

	/**
	 * A Listener called when clicking on the preference before opening the preference.
	 */
	public interface DynamicListPreferenceOnClickListener {
		/**
		 * An listener executed when clicking on the preference, before evaluating the entries.
		 *
		 * @param preference The preference.
		 * @return true if successful. In case of false, the opening of the preference will be cancelled.
		 */
		boolean onClick(DynamicMultiSelectListPreference preference);
	}
}
