package de.jeisfeld.randomimage.util;

import java.util.Arrays;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;

/**
 * Utility class for handling the shared preferences.
 */
public final class PreferenceUtil {
	/**
	 * The list of preferences used for switching on and off hints.
	 */
	private static final Integer[] INFO_PREFERENCES = {
			R.string.key_info_add_images,
			R.string.key_info_backup,
			R.string.key_info_delete_images,
			R.string.key_info_display_image,
			R.string.key_info_new_list };

	/**
	 * Hide default constructor.
	 */
	private PreferenceUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve the default shared preferences of the application.
	 *
	 * @return the default shared preferences.
	 */
	public static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(Application.getAppContext());
	}

	/**
	 * Retrieve a String shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId) {
		return getSharedPreferences().getString(Application.getAppContext().getString(preferenceId), "");
	}

	/**
	 * Retrieve a String shared preference, setting a default value if the preference is not set.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param defaultId
	 *            the String key of the default value.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId, final int defaultId) {
		String result = getSharedPreferences().getString(Application.getAppContext().getString(preferenceId), null);
		if (result == null) {
			result = Application.getAppContext().getString(defaultId);
			setSharedPreferenceString(preferenceId, result);
		}
		return result;
	}

	/**
	 * Set a String shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param s
	 *            the target value of the preference.
	 */
	public static void setSharedPreferenceString(final int preferenceId, final String s) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(Application.getAppContext().getString(preferenceId), s);
		editor.commit();
	}

	/**
	 * Retrieve an Uri shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static Uri getSharedPreferenceUri(final int preferenceId) {
		String uriString = getSharedPreferences().getString(Application.getAppContext().getString(preferenceId), null);

		if (uriString == null) {
			return null;
		}
		else {
			return Uri.parse(uriString);
		}
	}

	/**
	 * Set a shared preference for an Uri.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param uri
	 *            the target value of the preference.
	 */
	public static void setSharedPreferenceUri(final int preferenceId, final Uri uri) {
		Editor editor = getSharedPreferences().edit();
		if (uri == null) {
			editor.putString(Application.getAppContext().getString(preferenceId), null);
		}
		else {
			editor.putString(Application.getAppContext().getString(preferenceId), uri.toString());
		}
		editor.commit();
	}

	/**
	 * Retrieve a boolean shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static boolean getSharedPreferenceBoolean(final int preferenceId) {
		return getSharedPreferences().getBoolean(Application.getAppContext().getString(preferenceId), false);
	}

	/**
	 * Set a Boolean shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param b
	 *            the target value of the preference.
	 */
	public static void setSharedPreferenceBoolean(final int preferenceId, final boolean b) {
		Editor editor = getSharedPreferences().edit();
		editor.putBoolean(Application.getAppContext().getString(preferenceId), b);
		editor.commit();
	}

	/**
	 * Retrieve an integer shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param defaultValue
	 *            the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getSharedPreferenceInt(final int preferenceId, final int defaultValue) {
		return getSharedPreferences().getInt(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set an integer shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param i
	 *            the target value of the preference.
	 */
	public static void setSharedPreferenceInt(final int preferenceId, final int i) {
		Editor editor = getSharedPreferences().edit();
		editor.putInt(Application.getAppContext().getString(preferenceId), i);
		editor.commit();
	}

	/**
	 * Increment a counter shared preference, and return the new value.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @return the new value.
	 */
	public static int incrementCounter(final int preferenceId) {
		int newValue = getSharedPreferenceInt(preferenceId, 0) + 1;
		setSharedPreferenceInt(preferenceId, newValue);
		return newValue;
	}

	/**
	 * Retrieve a long shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param defaultValue
	 *            the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getSharedPreferenceLong(final int preferenceId, final long defaultValue) {
		return getSharedPreferences().getLong(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set a long shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param i
	 *            the target value of the preference.
	 */
	public static void setSharedPreferenceLong(final int preferenceId, final long i) {
		Editor editor = getSharedPreferences().edit();
		editor.putLong(Application.getAppContext().getString(preferenceId), i);
		editor.commit();
	}

	/**
	 * Remove a shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 */
	public static void removeSharedPreference(final int preferenceId) {
		Editor editor = getSharedPreferences().edit();
		editor.remove(Application.getAppContext().getString(preferenceId));
		editor.commit();
	}

	/**
	 * Get an indexed preference key that allows to store a shared preference with index.
	 *
	 * @param preferenceId
	 *            The base preference id
	 * @param index
	 *            The index
	 * @return The indexed preference key.
	 */
	private static String getIndexedPreferenceKey(final int preferenceId, final int index) {
		return Application.getAppContext().getString(preferenceId) + "[" + index + "]";
	}

	/**
	 * Retrieve an indexed String shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @return the corresponding preference value.
	 */
	public static String getIndexedSharedPreferenceString(final int preferenceId, final int index) {
		return getSharedPreferences().getString(getIndexedPreferenceKey(preferenceId, index), null);
	}

	/**
	 * Set an indexed String shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @param s
	 *            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceString(final int preferenceId, final int index, final String s) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(getIndexedPreferenceKey(preferenceId, index), s);
		editor.commit();
	}

	/**
	 * Retrieve an indexed int shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @param defaultValue
	 *            the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getIndexedSharedPreferenceInt(final int preferenceId, final int index, final int defaultValue) {
		return getSharedPreferences().getInt(getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed int shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @param i
	 *            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceInt(final int preferenceId, final int index, final int i) {
		Editor editor = getSharedPreferences().edit();
		editor.putInt(getIndexedPreferenceKey(preferenceId, index), i);
		editor.commit();
	}

	/**
	 * Retrieve an indexed long shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @param defaultValue
	 *            the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getIndexedSharedPreferenceLong(final int preferenceId, final int index, final long defaultValue) {
		return getSharedPreferences().getLong(getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed long shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 * @param i
	 *            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceLong(final int preferenceId, final int index, final long i) {
		Editor editor = getSharedPreferences().edit();
		editor.putLong(getIndexedPreferenceKey(preferenceId, index), i);
		editor.commit();
	}

	/**
	 * Remove an indexed shared preference.
	 *
	 * @param preferenceId
	 *            the id of the shared preference.
	 * @param index
	 *            The index
	 */
	public static void removeIndexedSharedPreference(final int preferenceId, final int index) {
		Editor editor = getSharedPreferences().edit();
		editor.remove(getIndexedPreferenceKey(preferenceId, index));
		editor.commit();
	}

	/**
	 * Set all hint preferences to the given value.
	 *
	 * @param value
	 *            The value.
	 */
	public static void setAllHints(final boolean value) {
		for (int preferenceId : Arrays.asList(INFO_PREFERENCES)) {
			setSharedPreferenceBoolean(preferenceId, value);
		}
	}

}
