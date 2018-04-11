package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Locale;

import de.jeisfeld.randomimage.notifications.NotificationAlarmReceiver;
import de.jeisfeld.randomimage.util.MigrationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class to retrieve base application resources.
 */
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
		justification = "Intentionally using same name as superclass")
public class Application extends android.app.Application {
	/**
	 * A utility field to store a context statically.
	 */
	private static Context mContext;
	/**
	 * The default tag for logging.
	 */
	public static final String TAG = "Randomimage.JE";

	@Override
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
			justification = "Make some context visible statically (no matter which one)")
	public final void onCreate() {
		super.onCreate();
		Application.mContext = getApplicationContext();

		MigrationUtil.migrateAppVersion();
		setLanguage();
		setExceptionHandler();

		// Set statistics
		int initialVersion = PreferenceUtil.getSharedPreferenceInt(R.string.key_statistics_initialversion, -1);
		if (initialVersion == -1) {
			PreferenceUtil.setSharedPreferenceInt(R.string.key_statistics_initialversion, getVersion());
		}

		long firstStartTime = PreferenceUtil.getSharedPreferenceLong(R.string.key_statistics_firststarttime, -1);
		if (firstStartTime == -1) {
			PreferenceUtil.setSharedPreferenceLong(R.string.key_statistics_firststarttime, System.currentTimeMillis());
		}

		PreferenceUtil.incrementCounter(R.string.key_statistics_countstarts);
	}

	/**
	 * Define custom ExceptionHandler which ensures that notification alarms are not lost in case of error.
	 */
	private void setExceptionHandler() {
		final UncaughtExceptionHandler defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

		UncaughtExceptionHandler customExceptionHandler =
				new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(final Thread thread, final Throwable ex) {
						try {
							NotificationAlarmReceiver.createAllNotificationAlarms();
						}
						catch (Exception e) {
							Log.e(TAG, "Failed to trigger notifications in exception case ", e);
						}

						// re-throw critical exception further to the os
						defaultExceptionHandler.uncaughtException(thread, ex);
					}
				};

		Thread.setDefaultUncaughtExceptionHandler(customExceptionHandler);
	}

	/**
	 * Retrieve the application context.
	 *
	 * @return The (statically stored) application context
	 */
	public static Context getAppContext() {
		return Application.mContext;
	}

	/**
	 * Get a resource string.
	 *
	 * @param resourceId the id of the resource.
	 * @param args       arguments for the formatting
	 * @return the value of the String resource.
	 */
	public static String getResourceString(final int resourceId, final Object... args) {
		return getAppContext().getResources().getString(resourceId, args);
	}

	/**
	 * Retrieve the version number of the app.
	 *
	 * @return the app version.
	 */
	public static int getVersion() {
		PackageInfo pInfo;
		try {
			pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			return pInfo.versionCode;
		}
		catch (NameNotFoundException e) {
			Log.e(TAG, "Did not find application version", e);
			return 0;
		}
	}

	/**
	 * Retrieve the version String of the app.
	 *
	 * @return the app version String.
	 */
	public static String getVersionString() {
		PackageInfo pInfo;
		try {
			pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			return pInfo.versionName;
		}
		catch (NameNotFoundException e) {
			Log.e(TAG, "Did not find application version", e);
			return null;
		}
	}

	/**
	 * Set the language.
	 */
	public static void setLanguage() {
		String languageString = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_language);
		if (languageString == null || languageString.length() == 0) {
			PreferenceUtil.setSharedPreferenceString(R.string.key_pref_language, "0");
			return;
		}

		int languageSetting = Integer.parseInt(languageString);

		if (languageSetting != 0) {
			switch (languageSetting) {
			case 1:
				setLocale(Locale.ENGLISH);
				break;
			case 2:
				setLocale(Locale.GERMAN);
				break;
			case 3: // MAGIC_NUMBER
				setLocale(new Locale("es"));
				break;
			default:
			}
		}
	}

	/**
	 * Set the locale.
	 *
	 * @param locale The locale to be set.
	 */
	private static void setLocale(final Locale locale) {
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
			setLocale17(locale);
		}
		else {
			setLocale16(locale);
		}
	}

	/**
	 * Set the locale for Android version above Jelly Bean.
	 *
	 * @param locale The locale to be set.
	 */
	@RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR1)
	private static void setLocale17(final Locale locale) {
		Resources res = getAppContext().getResources();
		Configuration conf = res.getConfiguration();
		conf.setLocale(locale);
		res.updateConfiguration(conf, res.getDisplayMetrics());
	}

	/**
	 * Set the locale for Android version below Jelly Bean.
	 *
	 * @param locale The locale to be set.
	 */
	@SuppressWarnings("deprecation")
	private static void setLocale16(final Locale locale) {
		Resources res = getAppContext().getResources();
		Configuration conf = res.getConfiguration();
		conf.locale = locale;
		res.updateConfiguration(conf, res.getDisplayMetrics());
	}

	/**
	 * Start the app programmatically.
	 *
	 * @param triggeringActivity triggeringActivity the triggering activity.
	 */
	public static void startApplication(final Activity triggeringActivity) {
		Intent intent = new Intent(triggeringActivity, MainConfigurationActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		triggeringActivity.startActivity(intent);
	}
}
