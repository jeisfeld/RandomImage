package de.jeisfeld.randomimage;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import androidx.annotation.RequiresApi;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.util.SystemUtil.ApplicationInfo;
import de.jeisfeld.randomimage.view.DynamicMultiSelectListPreference;
import de.jeisfeld.randomimage.view.DynamicMultiSelectListPreference.DynamicListPreferenceOnClickListener;
import de.jeisfeld.randomimage.view.MultiDirectoryChooserDialogFragment;
import de.jeisfeld.randomimage.view.MultiDirectoryChooserDialogFragment.ChosenDirectoriesListener;
import de.jeisfeld.randomimage.view.TimeSelectorPreference;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings.
 */
public class SettingsFragment extends PreferenceFragment {
	/**
	 * Field holding the value of the language preference, in order to detect a real change.
	 */
	private String mLanguageString;

	/**
	 * Field holding the value of the hidden lists pattern, in order to detect a real change.
	 */
	private String mHiddenListsPattern;

	/**
	 * Field holding the value of the hidden folders pattern, in order to detect a real change.
	 */
	private String mHiddenFoldersPattern;

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private CustomOnPreferenceChangeListener mOnPreferenceChangeListener = new CustomOnPreferenceChangeListener();

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.pref_general);

		// Fill variables in order to detect changed values.
		mLanguageString = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_language);
		mHiddenListsPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_lists_pattern);
		mHiddenFoldersPattern = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_hidden_folders_pattern);

		bindPreferenceSummaryToValue(R.string.key_pref_language);
		bindPreferenceSummaryToValue(R.string.key_pref_folder_selection_mechanism);
		bindPreferenceSummaryToValue(R.string.key_pref_show_list_notification);
		bindPreferenceSummaryToValue(R.string.key_pref_use_regex_filter);
		bindPreferenceSummaryToValue(R.string.key_pref_hidden_folders_pattern);
		bindPreferenceSummaryToValue(R.string.key_pref_hidden_lists_pattern);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_scale_type);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_background);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_flip_behavior);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_change_timeout);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_change_with_tap);
		bindPreferenceSummaryToValue(R.string.key_pref_detail_prevent_screen_timeout);

		addHintButtonListener(R.string.key_pref_show_info, false);
		addHintButtonListener(R.string.key_pref_hide_info, true);
		addResetBackupFolderListener();
		addHelpPageListener();
		addDeveloperContactListener();
		addProAppButtonListener();
		addRestrictPopupNotificationsListener();
		updateRegexpPreferences(getActivity());
		addSearchImageFoldersListener();

		if (Boolean.parseBoolean(Application.getResourceString(R.string.has_premium))) {
			getPreferenceScreen().removePreference(findPreference(getString(R.string.key_pref_category_premium)));
		}
		if (SystemUtil.findImagesViaMediaStore()) {
			bindPreferenceSummaryToValue(R.string.key_pref_preferred_image_folders);
			addPreferredImageFolderListener();
			PreferenceGroup groupOther = (PreferenceGroup) findPreference(getString(R.string.key_pref_category_other));
			groupOther.removePreference(findPreference(getString(R.string.key_pref_show_hidden_folders)));
		}
		else {
			bindPreferenceSummaryToValue(R.string.key_pref_show_hidden_folders);
			updateShowHiddenFoldersPreference(getActivity());
			PreferenceGroup groupOther = (PreferenceGroup) findPreference(getString(R.string.key_pref_category_other));
			groupOther.removePreference(findPreference(getString(R.string.key_pref_preferred_image_folders)));
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = super.onCreateView(inflater, container, savedInstanceState);

		if (root != null) {
			root.setOnApplyWindowInsetsListener((v, insets) -> {
				v.setPadding(insets.getSystemWindowInsetLeft(),
						insets.getSystemWindowInsetTop(),
						insets.getSystemWindowInsetRight(),
						insets.getSystemWindowInsetBottom());
				return insets.consumeSystemWindowInsets();
			});
			root.setFitsSystemWindows(true);
		}

		return root;
	}

	@Override
	public final void onResume() {
		super.onResume();
	}

	/**
	 * Add the listener for a "hints" button.
	 *
	 * @param preferenceId        The id of the button.
	 * @param hintPreferenceValue The value to be set to all the hints preferences.
	 */
	private void addHintButtonListener(final int preferenceId, final boolean hintPreferenceValue) {
		Preference showPreference = findPreference(getString(preferenceId));
		showPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				PreferenceUtil.setAllHints(hintPreferenceValue);
				return true;
			}
		});
	}

	/**
	 * Add the listener for the "Search image folders" button.
	 */
	private void addSearchImageFoldersListener() {
		Preference searchPreference = findPreference(getString(R.string.key_pref_search_image_folders));
		searchPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				if (SystemUtil.findImagesViaMediaStore()) {
					final long startTime = System.currentTimeMillis();
					MediaStoreUtil.triggerMediaScan(getContext(), new OnScanCompletedListener() {
						@Override
						public void onScanCompleted(final String path, final Uri uri) {
							PreferenceUtil.setSharedPreferenceLong(R.string.key_last_media_scanning_time, System.currentTimeMillis() - startTime);
						}
					});
					final long lastDuration = PreferenceUtil.getSharedPreferenceLong(R.string.key_last_media_scanning_time, -1);
					DialogUtil.displayToast(getContext(), R.string.toast_triggered_media_scan, getDurationString(lastDuration));
				}
				else {
					DialogUtil.displaySearchForImageFoldersIfRequired(getActivity(), true);
				}
				return true;
			}
		});
	}

	/**
	 * Convert a duration in milliseconds into a message String.
	 *
	 * @param durationMillis The duration in millis
	 * @return The message String
	 */
	private String getDurationString(final long durationMillis) {
		if (durationMillis > 99900) { // MAGIC_NUMBER
			return getString(R.string.message_value_minutes, durationMillis / 60000.0); // MAGIC_NUMBER
		}
		else if (durationMillis > 0) {
			return getString(R.string.message_value_seconds, durationMillis / 1000.0); // MAGIC_NUMBER
		}
		else {
			return getString(R.string.message_value_unknown);
		}
	}

	/**
	 * Add the listener for selecting the preferred image folder.
	 */
	@RequiresApi(api = VERSION_CODES.O)
	private void addPreferredImageFolderListener() {
		final Preference preferredFolderPreference = findPreference(getString(R.string.key_pref_preferred_image_folders));
		preferredFolderPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				List<String> dirs = PreferenceUtil.getSharedPreferenceStringList(R.string.key_pref_preferred_image_folders);

				MultiDirectoryChooserDialogFragment.displayMultiDirectoryChooserDialog(getActivity(), R.string.dialog_select_preferred_image_folders,
						new ChosenDirectoriesListener() {
							/**
							 * The default serial version UID.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onChosenDirs(final List<String> chosenDirs) {
								if (chosenDirs == null || chosenDirs.size() == 0) {
									PreferenceUtil.removeSharedPreference(R.string.key_pref_preferred_image_folders);
									mOnPreferenceChangeListener.onPreferenceChange(preferredFolderPreference, "");
								}
								else {
									PreferenceUtil.setSharedPreferenceStringList(R.string.key_pref_preferred_image_folders, chosenDirs);
									mOnPreferenceChangeListener.onPreferenceChange(preferredFolderPreference, String.join("\n", chosenDirs));
								}
							}

							@Override
							public void onCancelled() {

							}
						}, dirs);

				return true;
			}
		});
	}


	/**
	 * Add the listener for the "Reset Backup Folder" button.
	 */
	private void addResetBackupFolderListener() {
		final Preference resetBackupPreference = findPreference(getString(R.string.key_pref_reset_backup_folder));
		if (SystemUtil.isAtLeastVersion(VERSION_CODES.Q)) {
			Uri uri = PreferenceUtil.getSharedPreferenceUri(R.string.key_backup_folder_uri);
			if (uri != null) {
				resetBackupPreference.setSummary(FileUtil.getFullPathFromTreeUri(uri));
			}

			resetBackupPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(final Preference preference) {
					Uri backupUri = PreferenceUtil.getSharedPreferenceUri(R.string.key_backup_folder_uri);
					if (backupUri != null) {
						Application.getAppContext().getContentResolver().releasePersistableUriPermission(backupUri,
								Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					}
					PreferenceUtil.removeSharedPreference(R.string.key_backup_folder_uri);
					resetBackupPreference.setSummary(getString(R.string.menu_reset_backup_folder_unset));
					return true;
				}
			});
		}
		else {
			((PreferenceCategory) findPreference(getString(R.string.key_pref_category_other))).removePreference(resetBackupPreference);
		}
	}

	/**
	 * Add an entry for variable donation.
	 */
	private void addHelpPageListener() {
		Preference variableDonationPreference = findPreference(getString(R.string.key_pref_help_page));

		variableDonationPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent browserIntent =
						new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.menu_target_help_page)));
				startActivity(browserIntent);
				return true;
			}
		});
	}

	/**
	 * Add an entry for developer contact.
	 */
	private void addDeveloperContactListener() {
		Preference contactPreference = findPreference(getString(R.string.key_pref_contact_developer));

		contactPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
						"mailto", getString(R.string.menu_email_contact_developer), null));
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.menu_subject_contact_developer));

				startActivity(intent);
				return true;
			}
		});
	}

	/**
	 * Add the listener for pro app.
	 */
	private void addProAppButtonListener() {
		Preference unlockPreference = findPreference(getString(R.string.key_pref_pro_app));
		unlockPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
				googlePlayIntent.setData(Uri.parse("market://details?id=de.jeisfeld.randomimagepro"));
				try {
					startActivity(googlePlayIntent);
				}
				catch (Exception e) {
					DialogUtil.displayToast(getActivity(), R.string.toast_failed_to_open_google_play, false);
				}
				return true;
			}
		});
		unlockPreference.setEnabled(!SystemUtil.isAppInstalled("de.jeisfeld.randomimagepro"));
	}

	/**
	 * Add an entry for variable donation.
	 */
	private void addRestrictPopupNotificationsListener() {
		final DynamicMultiSelectListPreference restrictPopupNotificationsPreference =
				(DynamicMultiSelectListPreference) findPreference(getString(R.string.key_pref_apps_without_popup_notifications));
		if (android.os.Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
			((PreferenceCategory) findPreference(getString(R.string.key_pref_category_other))).removePreference(restrictPopupNotificationsPreference);
			return;
		}

		restrictPopupNotificationsPreference.setOnClickListener(new DynamicListPreferenceOnClickListener() {
			@Override
			public boolean onClick(final DynamicMultiSelectListPreference preference) {
				if (!SystemUtil.isUsageStatsAvailable()) {
					restrictPopupNotificationsPreference.setEntries(new String[0]);
					restrictPopupNotificationsPreference.setEntryValues(new String[0]);

					DialogUtil.displayConfirmationMessage(getActivity(), new ConfirmDialogListener() {
						@RequiresApi(VERSION_CODES.LOLLIPOP)
						@Override
						public void onDialogPositiveClick(final DialogFragment dialog) {
							Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
							intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivityForResult(intent, 0);
						}

						@Override
						public void onDialogNegativeClick(final DialogFragment dialog) {
						}
					}, R.string.title_dialog_request_permission, R.string.button_continue, R.string.dialog_confirmation_need_usage_access);
					return false;
				}

				List<ApplicationInfo> applicationInfoList = SystemUtil.getInstalledApplications();
				String[] entries = new String[applicationInfoList.size()];
				String[] entryValues = new String[applicationInfoList.size()];
				int i = 0;

				for (ApplicationInfo applicationInfo : applicationInfoList) {
					entries[i] = applicationInfo.getLabelName();
					entryValues[i] = applicationInfo.getPackageName();
					i++;
				}

				restrictPopupNotificationsPreference.setEntries(entries);
				restrictPopupNotificationsPreference.setEntryValues(entryValues);
				return true;
			}
		});
	}

	/**
	 * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary
	 * (line of text below the preference title) is updated to reflect the value. The summary is also immediately
	 * updated upon calling this method. The exact display format is dependent on the type of preference.
	 *
	 * @param preference The preference to be bound.
	 */
	private void bindPreferenceSummaryToValue(final Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);

		// Trigger the listener immediately with the preference's current value.
		if (preference.getKey().equals(getDurationString(R.string.dialog_select_preferred_image_folders))) {
			if (VERSION.SDK_INT >= VERSION_CODES.O) {
				List<String> values = PreferenceUtil.getSharedPreferenceStringList(R.string.dialog_select_preferred_image_folders);
				mOnPreferenceChangeListener.setSummary(preference, String.join("\n", values));
			}
		}
		if (!(preference instanceof CheckBoxPreference)) {
			mOnPreferenceChangeListener.setSummary(preference, PreferenceManager
					.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
		}
	}

	/**
	 * Helper method for easier call of {@link #bindPreferenceSummaryToValue(android.preference.Preference)}.
	 *
	 * @param preferenceKey The key of the preference.
	 */
	private void bindPreferenceSummaryToValue(final int preferenceKey) {
		bindPreferenceSummaryToValue(findPreference(getString(preferenceKey)));
	}

	/**
	 * Update the enabling of the regexp preferences.
	 *
	 * @param context The context.
	 */
	private void updateRegexpPreferences(final Context context) {
		boolean booleanValue = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_use_regex_filter);
		findPreference(context.getString(R.string.key_pref_hidden_folders_pattern)).setEnabled(booleanValue);
		findPreference(context.getString(R.string.key_pref_hidden_lists_pattern)).setEnabled(booleanValue);
	}

	/**
	 * Update the enabling of the "Show Hidden Folders" preference.
	 *
	 * @param context The context.
	 */
	private void updateShowHiddenFoldersPreference(final Context context) {
		int mechanism = PreferenceUtil.getSharedPreferenceIntString(R.string.key_pref_folder_selection_mechanism, -1);
		Preference preference = findPreference(context.getString(R.string.key_pref_show_hidden_folders));
		if (preference != null) {
			preference.setEnabled(mechanism == 0);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private class CustomOnPreferenceChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(final Preference preference, final Object value) {
			String stringValue = value.toString();

			// Apply change of language
			if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_language))) {

				if (mLanguageString == null || !mLanguageString.equals(value)) {
					PreferenceUtil.setSharedPreferenceString(R.string.key_pref_language, stringValue);

					Application.startApplication(getActivity());
					if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
						System.exit(0);
					}
				}
			}
			// show/hide regex preferences in dependence of main setting
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_use_regex_filter))) {
				PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_use_regex_filter, (Boolean) value);
				updateRegexpPreferences(preference.getContext());
				ImageRegistry.parseConfigFiles();
			}
			// In case of switch of hidden lists pattern, refresh
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_hidden_lists_pattern))) {
				if (mHiddenListsPattern == null || !mHiddenListsPattern.equals(stringValue)) {
					try {
						Pattern.compile(stringValue);
					}
					catch (PatternSyntaxException e) {
						DialogUtil.displayInfo(getActivity(), R.string.dialog_info_invalid_regexp);
						return false;
					}
					PreferenceUtil.setSharedPreferenceString(R.string.key_pref_hidden_lists_pattern, (String) value);
					ImageRegistry.parseConfigFiles();
				}
			}
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_hidden_folders_pattern))) {
				if (mHiddenFoldersPattern == null || !mHiddenFoldersPattern.equals(stringValue)) {
					try {
						Pattern.compile(stringValue);
					}
					catch (PatternSyntaxException e) {
						DialogUtil.displayInfo(getActivity(), R.string.dialog_info_invalid_regexp);
						return false;
					}
				}
			}
			// Update view depending on folder selection mechanism.
			else if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_folder_selection_mechanism))) {
				PreferenceUtil.setSharedPreferenceString(R.string.key_pref_folder_selection_mechanism, stringValue);
				updateShowHiddenFoldersPreference(preference.getContext());
			}

			setSummary(preference, stringValue);

			return true;
		}

		/**
		 * Set the summary of the preference.
		 *
		 * @param preference The preference.
		 * @param value      The value of the preference.
		 */
		public void setSummary(final Preference preference, final String value) {
			// set summary
			if (preference.getClass().equals(ListPreference.class)) {
				// For list preferences (except customized ones), look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(value);

				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			}
			else if (preference instanceof TimeSelectorPreference) {
				preference.setSummary(((TimeSelectorPreference) preference).getSummaryFromValue(value));
			}
			else if (!(preference instanceof CheckBoxPreference)) {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(value);
			}
		}
	}
}
