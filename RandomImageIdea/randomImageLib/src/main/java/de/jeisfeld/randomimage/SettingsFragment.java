package de.jeisfeld.randomimage;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.android.vending.billing.Purchase;
import com.android.vending.billing.PurchasedSku;
import com.android.vending.billing.SkuDetails;

import de.jeisfeld.randomimage.util.AuthorizationHelper;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.GoogleBillingHelper;
import de.jeisfeld.randomimage.util.GoogleBillingHelper.OnInventoryFinishedListener;
import de.jeisfeld.randomimage.util.GoogleBillingHelper.OnPurchaseSuccessListener;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Fragment for displaying the settings.
 */
public class SettingsFragment extends PreferenceFragment {
	/**
	 * A prefix put before the productId to define the according preference key.
	 */
	private static final String SKU_KEY_PREFIX = "sku_";

	/**
	 * Field holding the value of the language preference, in order to detect a real change.
	 */
	private String mLanguageString;

	/**
	 * Field holding the value of the hidden lists pattern, in order to detect a real change.
	 */
	private String mHiddenListsPattern;

	/**
	 * The preference screen handling donations.
	 */
	private PreferenceCategory mPrefCategoryPremium;

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

		bindPreferenceSummaryToValue(R.string.key_pref_language);
		bindPreferenceSummaryToValue(R.string.key_pref_folder_selection_mechanism);
		bindPreferenceSummaryToValue(R.string.key_pref_hidden_lists_pattern);

		addHintButtonListener(R.string.key_pref_show_info, false);
		addHintButtonListener(R.string.key_pref_hide_info, true);
		addHelpPageListener();
		addDonationListener();
		addDeveloperContactListener();
		addProAppButtonListener();

		mPrefCategoryPremium = (PreferenceCategory) findPreference(getString(R.string.key_pref_category_premium));

		if (AuthorizationHelper.requiresGoogleBilling()) {
			GoogleBillingHelper.initialize(getActivity(), mOnInventoryFinishedListener);
		}
		else {
			getPreferenceScreen().removePreference(findPreference(getString(R.string.key_pref_category_premium)));
		}
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
	 * Add an entry for variable donation.
	 */
	private void addDonationListener() {
		Preference variableDonationPreference = findPreference(getString(R.string.key_pref_donation));

		variableDonationPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent browserIntent =
						new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.menu_target_donation)));
				startActivity(browserIntent);
				return true;
			}
		});
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
		mOnPreferenceChangeListener.setSummary(preference, PreferenceManager
				.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
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
	 * A listener handling the response after reading the in-add purchase inventory.
	 */
	private OnInventoryFinishedListener mOnInventoryFinishedListener = new OnInventoryFinishedListener() {
		@Override
		public void handleProducts(final List<PurchasedSku> purchases, final List<SkuDetails> availableProducts,
								   final boolean isPremium) {
			if (isPremium != PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_has_premium)) {
				// Update premium status, also in ConfigureImageListActivity.
				PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_has_premium, isPremium);
				((SettingsActivity) getActivity()).returnResult(true);
			}

			// List inventory items.
			for (PurchasedSku purchase : purchases) {
				Preference purchasePreference = new Preference(getActivity());
				String title = getString(R.string.button_purchased_item, purchase.getSkuDetails().getDisplayTitle(getActivity()));
				purchasePreference.setTitle(title);
				purchasePreference.setSummary(purchase.getSkuDetails().getDescription());
				purchasePreference.setEnabled(false);
				mPrefCategoryPremium.addPreference(purchasePreference);
			}
			for (SkuDetails skuDetails : availableProducts) {
				Preference skuPreference = new Preference(getActivity());
				skuPreference.setTitle(skuDetails.getDisplayTitle(getActivity()));
				skuPreference.setKey(SKU_KEY_PREFIX + skuDetails.getSku());
				skuPreference.setSummary(skuDetails.getDescription());
				skuPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(final Preference preference) {
						String productId = preference.getKey().substring(SKU_KEY_PREFIX.length());
						GoogleBillingHelper.launchPurchaseFlow(productId, mOnPurchaseSuccessListener);
						return false;
					}
				});
				mPrefCategoryPremium.addPreference(skuPreference);
			}
		}
	};

	/**
	 * A listener handling the response after purchasing a product.
	 */
	private OnPurchaseSuccessListener mOnPurchaseSuccessListener = new OnPurchaseSuccessListener() {
		@Override
		public void handlePurchase(final Purchase purchase, final boolean addedPremiumProduct) {
			PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_has_premium, true);
			MessageDialogListener listener = new MessageDialogListener() {
				@Override
				public void onDialogFinished() {
					((SettingsActivity) getActivity()).returnResult(true);
					getActivity().finish();
				}
			};

			DialogUtil.displayInfo(getActivity(), listener, 0, R.string.dialog_info_premium_thanks);
		}
	};

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
					Application.setLanguage();
					PreferenceUtil.setSharedPreferenceString(R.string.key_pref_language, stringValue);

					Application.startApplication(getActivity());
					System.exit(0);
				}
			}

			// In case of switch of hidden lists pattern, refresh
			if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_hidden_lists_pattern))) {
				if (mHiddenListsPattern == null || !mHiddenListsPattern.equals(value)) {
					PreferenceUtil.setSharedPreferenceString(R.string.key_pref_hidden_lists_pattern, (String) value);
					ImageRegistry.parseConfigFiles();
				}
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
			else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(value);
			}
		}
	}

}
