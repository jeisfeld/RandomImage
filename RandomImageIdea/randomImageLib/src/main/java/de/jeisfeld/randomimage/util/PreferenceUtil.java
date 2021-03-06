package de.jeisfeld.randomimage.util;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.Nullable;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimagelib.R;

/**
 * Utility class for handling the shared preferences.
 */
public final class PreferenceUtil {
	/**
	 * The list of preferences used for switching on and off hints.
	 */
	private static final Integer[] HINT_PREFERENCES = {
			R.string.key_hint_first_use,
			R.string.key_hint_add_images,
			R.string.key_hint_select_folder,
			R.string.key_hint_display_image,
			R.string.key_hint_main_configuration};

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
	private static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(Application.getAppContext());
	}

	/**
	 * Retrieve a String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId) {
		return getSharedPreferences().getString(Application.getAppContext().getString(preferenceId), null);
	}

	/**
	 * Retrieve a String shared preference, setting a default value if the preference is not set.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId    the String key of the default value.
	 * @return the corresponding preference value.
	 */
	public static String getSharedPreferenceString(final int preferenceId, final int defaultId) {
		String result = getSharedPreferenceString(preferenceId);
		if (result == null) {
			result = Application.getAppContext().getString(defaultId);
			setSharedPreferenceString(preferenceId, result);
		}
		return result;
	}

	/**
	 * Set a String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param s            the target value of the preference.
	 */
	public static void setSharedPreferenceString(final int preferenceId, final String s) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(Application.getAppContext().getString(preferenceId), s);
		editor.apply();
	}

	/**
	 * Retrieve a boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static boolean getSharedPreferenceBoolean(final int preferenceId) {
		return getSharedPreferences().getBoolean(Application.getAppContext().getString(preferenceId), false);
	}

	/**
	 * Set a Boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param b            the target value of the preference.
	 */
	public static void setSharedPreferenceBoolean(final int preferenceId, final boolean b) {
		Editor editor = getSharedPreferences().edit();
		editor.putBoolean(Application.getAppContext().getString(preferenceId), b);
		editor.apply();
	}

	/**
	 * Retrieve an integer shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getSharedPreferenceInt(final int preferenceId, final int defaultValue) {
		return getSharedPreferences().getInt(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set an integer shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i            the target value of the preference.
	 */
	public static void setSharedPreferenceInt(final int preferenceId, final int i) {
		Editor editor = getSharedPreferences().edit();
		editor.putInt(Application.getAppContext().getString(preferenceId), i);
		editor.apply();
	}

	/**
	 * Increment a counter shared preference, and return the new value.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the new value.
	 */
	public static int incrementCounter(final int preferenceId) {
		int newValue = getSharedPreferenceInt(preferenceId, 0) + 1;
		setSharedPreferenceInt(preferenceId, newValue);
		return newValue;
	}

	/**
	 * Retrieve an integer from a shared preference string.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId    the String key of the default value. If not existing, value -1 is returned.
	 * @return the corresponding preference value.
	 */
	public static int getSharedPreferenceIntString(final int preferenceId, @Nullable final Integer defaultId) {
		String resultString;

		if (defaultId == null) {
			resultString = getSharedPreferenceString(preferenceId);
		}
		else {
			resultString = getSharedPreferenceString(preferenceId, defaultId);
		}
		if (resultString == null || resultString.length() == 0) {
			return -1;
		}
		try {
			return Integer.parseInt(resultString);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Set a string shared preference from an integer.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i            the target value of the preference.
	 */
	public static void setSharedPreferenceIntString(final int preferenceId, final int i) {
		setSharedPreferenceString(preferenceId, Integer.toString(i));
	}

	/**
	 * Retrieve a long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getSharedPreferenceLong(final int preferenceId, final long defaultValue) {
		return getSharedPreferences().getLong(Application.getAppContext().getString(preferenceId), defaultValue);
	}

	/**
	 * Set a long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i            the target value of the preference.
	 */
	public static void setSharedPreferenceLong(final int preferenceId, final long i) {
		Editor editor = getSharedPreferences().edit();
		editor.putLong(Application.getAppContext().getString(preferenceId), i);
		editor.apply();
	}

	/**
	 * Get a String set shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static Set<String> getSharedPreferenceStringSet(final int preferenceId) {
		return getSharedPreferences().getStringSet(Application.getAppContext().getString(preferenceId), new HashSet<String>());
	}


	/**
	 * Retrieve a long from a shared preference string.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param defaultId    the String key of the default value. If not existing, value -1 is returned.
	 * @return the corresponding preference value.
	 */
	public static long getSharedPreferenceLongString(final int preferenceId, @Nullable final Integer defaultId) {
		String resultString;

		if (defaultId == null) {
			resultString = getSharedPreferenceString(preferenceId);
		}
		else {
			resultString = getSharedPreferenceString(preferenceId, defaultId);
		}
		if (resultString == null || resultString.length() == 0) {
			return -1;
		}
		try {
			return Long.parseLong(resultString);
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Set a string shared preference from a long.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param i            the target value of the preference.
	 */
	public static void setSharedPreferenceLongString(final int preferenceId, final long i) {
		setSharedPreferenceString(preferenceId, Long.toString(i));
	}

	/**
	 * Retrieve a String List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static ArrayList<String> getSharedPreferenceStringList(final int preferenceId) {
		String restoreString = getSharedPreferenceString(preferenceId);
		if (restoreString == null || restoreString.length() == 0) {
			return new ArrayList<>();
		}

		String[] folderArray = restoreString.split("\\r?\\n");
		return new ArrayList<>(Arrays.asList(folderArray));
	}

	/**
	 * Set a String List shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param stringList   the target value of the preference.
	 */
	public static void setSharedPreferenceStringList(final int preferenceId, final List<String> stringList) {
		if (stringList == null || stringList.size() == 0) {
			PreferenceUtil.removeSharedPreference(preferenceId);
		}
		else {
			StringBuilder saveStringBuffer = new StringBuilder();
			for (String string : stringList) {
				if (saveStringBuffer.length() > 0) {
					saveStringBuffer.append("\n");
				}
				saveStringBuffer.append(string);
			}
			PreferenceUtil.setSharedPreferenceString(preferenceId, saveStringBuffer.toString());
		}
	}

	/**
	 * Retrieve an Uri shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
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
	 * @param preferenceId the id of the shared preference.
	 * @param uri          the target value of the preference.
	 */
	public static void setSharedPreferenceUri(final int preferenceId, @Nullable final Uri uri) {
		Editor editor = getSharedPreferences().edit();
		if (uri == null) {
			editor.putString(Application.getAppContext().getString(preferenceId), null);
		}
		else {
			editor.putString(Application.getAppContext().getString(preferenceId), uri.toString());
		}
		editor.apply();
	}

	/**
	 * Remove a shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 */
	public static void removeSharedPreference(final int preferenceId) {
		Editor editor = getSharedPreferences().edit();
		editor.remove(Application.getAppContext().getString(preferenceId));
		editor.apply();
	}

	/**
	 * Get an indexed preference key that allows to store a shared preference with index.
	 *
	 * @param preferenceId The base preference id
	 * @param index        The index
	 * @return The indexed preference key.
	 */
	private static String getIndexedPreferenceKey(final int preferenceId, final Object index) {
		return Application.getAppContext().getString(preferenceId) + "[" + index + "]";
	}

	/**
	 * Retrieve an indexed String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @return the corresponding preference value.
	 */
	public static String getIndexedSharedPreferenceString(final int preferenceId, final Object index) {
		return getSharedPreferences().getString(getIndexedPreferenceKey(preferenceId, index), null);
	}

	/**
	 * Set an indexed String shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param s            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceString(final int preferenceId, final Object index, final String s) {
		Editor editor = getSharedPreferences().edit();
		editor.putString(getIndexedPreferenceKey(preferenceId, index), s);
		editor.apply();
	}

	/**
	 * Retrieve an indexed boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static boolean getIndexedSharedPreferenceBoolean(final int preferenceId, final Object index, final boolean defaultValue) {
		return getSharedPreferences().getBoolean(getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed boolean shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param b            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceBoolean(final int preferenceId, final Object index, final boolean b) {
		Editor editor = getSharedPreferences().edit();
		editor.putBoolean(getIndexedPreferenceKey(preferenceId, index), b);
		editor.apply();
	}

	/**
	 * Retrieve an indexed int shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static int getIndexedSharedPreferenceInt(final int preferenceId, final Object index, final int defaultValue) {
		return getSharedPreferences().getInt(getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed int shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param i            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceInt(final int preferenceId, final Object index, final int i) {
		Editor editor = getSharedPreferences().edit();
		editor.putInt(getIndexedPreferenceKey(preferenceId, index), i);
		editor.apply();
	}

	/**
	 * Retrieve an indexed long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param defaultValue the default value of the shared preference.
	 * @return the corresponding preference value.
	 */
	public static long getIndexedSharedPreferenceLong(final int preferenceId, final Object index, final long defaultValue) {
		return getSharedPreferences().getLong(getIndexedPreferenceKey(preferenceId, index), defaultValue);
	}

	/**
	 * Set an indexed long shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param i            the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceLong(final int preferenceId, final Object index, final long i) {
		Editor editor = getSharedPreferences().edit();
		editor.putLong(getIndexedPreferenceKey(preferenceId, index), i);
		editor.apply();
	}

	/**
	 * Retrieve a String List indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @return the corresponding preference value.
	 */
	public static ArrayList<String> getIndexedSharedPreferenceStringList(final int preferenceId, final Object index) {
		String restoreString = getIndexedSharedPreferenceString(preferenceId, index);
		if (restoreString == null || restoreString.length() == 0) {
			return new ArrayList<>();
		}

		String[] folderArray = restoreString.split("\\r?\\n");
		return new ArrayList<>(Arrays.asList(folderArray));
	}

	/**
	 * Set a String List indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @param stringList   the target value of the preference.
	 */
	public static void setIndexedSharedPreferenceStringList(final int preferenceId, final Object index, final List<String> stringList) {
		if (stringList == null || stringList.size() == 0) {
			PreferenceUtil.removeIndexedSharedPreference(preferenceId, index);
		}
		else {
			StringBuilder saveStringBuffer = new StringBuilder();
			for (String string : stringList) {
				if (saveStringBuffer.length() > 0) {
					saveStringBuffer.append("\n");
				}
				saveStringBuffer.append(string);
			}
			PreferenceUtil.setIndexedSharedPreferenceString(preferenceId, index, saveStringBuffer.toString());
		}
	}

	/**
	 * Remove an indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 */
	public static void removeIndexedSharedPreference(final int preferenceId, final Object index) {
		Editor editor = getSharedPreferences().edit();
		editor.remove(getIndexedPreferenceKey(preferenceId, index));
		editor.apply();
	}

	/**
	 * Check the existence of an indexed shared preference.
	 *
	 * @param preferenceId the id of the shared preference.
	 * @param index        The index
	 * @return True if the preference exists.
	 */
	public static boolean hasIndexedSharedPreference(final int preferenceId, final Object index) {
		return getSharedPreferences().contains(getIndexedPreferenceKey(preferenceId, index));
	}

	/**
	 * Set all hint preferences to the given value.
	 *
	 * @param value The value.
	 */
	public static void setAllHints(final boolean value) {
		for (int preferenceId : HINT_PREFERENCES) {
			setSharedPreferenceBoolean(preferenceId, value);
		}
	}

}
