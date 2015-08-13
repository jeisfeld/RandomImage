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

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.GoogleBillingHelper;
import de.jeisfeld.randomimage.util.GoogleBillingHelper.OnInventoryFinishedListener;
import de.jeisfeld.randomimage.util.GoogleBillingHelper.OnPurchaseSuccessListener;
import de.jeisfeld.randomimage.util.PreferenceUtil;

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
	private String languageString;

	/**
	 * The preference screen handling donations.
	 */
	private PreferenceCategory prefCategoryPremium;

	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.pref_general);

		// Fill variables in order to detect changed values.
		languageString = PreferenceUtil.getSharedPreferenceString(R.string.key_pref_language);

		bindPreferenceSummaryToValue(R.string.key_pref_language);

		addHintButtonListener(R.string.key_pref_show_info, false);
		addHintButtonListener(R.string.key_pref_hide_info, true);

		prefCategoryPremium = (PreferenceCategory) findPreference(getString(R.string.key_pref_category_premium));

		GoogleBillingHelper.initialize(getActivity(), onInventoryFinishedListener);
	}

	/**
	 * Add the listener for a "hints" button.
	 *
	 * @param preferenceId
	 *            The id of the button.
	 * @param hintPreferenceValue
	 *            The value to be set to all the hints preferences.
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
	private void addVariableDonation() {
		Preference variableDonationPreference = new Preference(getActivity());
		variableDonationPreference.setTitle(getString(R.string.menu_title_variable_donation));
		variableDonationPreference.setSummary(getString(R.string.menu_detail_variable_donation));

		variableDonationPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent browserIntent =
						new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.menu_target_variable_donation)));
				startActivity(browserIntent);
				return true;
			}
		});

		prefCategoryPremium.addItemFromInflater(variableDonationPreference);
	}

	/**
	 * Add an entry for developer contact.
	 */
	private void addDeveloperContact() {
		Preference contactPreference = new Preference(getActivity());
		contactPreference.setTitle(getString(R.string.menu_title_contact_developer));
		contactPreference.setSummary(getString(R.string.menu_detail_contact_developer));

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

		prefCategoryPremium.addItemFromInflater(contactPreference);
	}

	/**
	 * Binds a preference's summary to its value. More specifically, when the preference's value is changed, its summary
	 * (line of text below the preference title) is updated to reflect the value. The summary is also immediately
	 * updated upon calling this method. The exact display format is dependent on the type of preference.
	 *
	 * @param preference
	 *            The preference to be bound.
	 */
	private void bindPreferenceSummaryToValue(final Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's current value.
		bindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
				.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	/**
	 * Helper method for easier call of {@link #bindPreferenceSummaryToValue(android.preference.Preference)}.
	 *
	 * @param preferenceKey
	 *            The key of the preference.
	 */
	private void bindPreferenceSummaryToValue(final int preferenceKey) {
		bindPreferenceSummaryToValue(findPreference(getString(preferenceKey)));
	}

	/**
	 * A preference value change listener that updates the preference's summary to reflect its new value.
	 */
	private OnPreferenceChangeListener bindPreferenceSummaryToValueListener =
			new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object value) {
					String stringValue = value.toString();

					// Apply change of language
					if (preference.getKey().equals(preference.getContext().getString(R.string.key_pref_language))) {
						if (!languageString.equals(value)) {
							Application.setLanguage();
							PreferenceUtil.setSharedPreferenceString(R.string.key_pref_language, (String) value);

							System.exit(0);
						}
					}

					// set summary
					if (preference.getClass().equals(ListPreference.class)) {
						// For list preferences (except customized ones), look up the correct display value in
						// the preference's 'entries' list.
						ListPreference listPreference = (ListPreference) preference;
						int index = listPreference.findIndexOfValue(stringValue);

						preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
					}
					else {
						// For all other preferences, set the summary to the value's
						// simple string representation.
						preference.setSummary(stringValue);
					}

					return true;
				}

			};

	/**
	 * A listener handling the response after reading the in-add purchase inventory.
	 */
	private OnInventoryFinishedListener onInventoryFinishedListener = new OnInventoryFinishedListener() {
		@Override
		public void handleProducts(final List<PurchasedSku> purchases, final List<SkuDetails> availableProducts,
				final boolean isPremium) {
			PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_has_premium, isPremium);

			// List inventory items.
			for (PurchasedSku purchase : purchases) {
				Preference purchasePreference = new Preference(getActivity());
				String title =
						String.format(getString(R.string.button_purchased_item), purchase.getSkuDetails()
								.getDisplayTitle(getActivity()));
				purchasePreference.setTitle(title);
				purchasePreference.setSummary(purchase.getSkuDetails().getDescription());
				purchasePreference.setEnabled(false);
				prefCategoryPremium.addPreference(purchasePreference);
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
						GoogleBillingHelper.launchPurchaseFlow(productId, onPurchaseSuccessListener);
						return false;
					}
				});
				prefCategoryPremium.addPreference(skuPreference);
			}

			addVariableDonation();
			addDeveloperContact();
		}
	};

	/**
	 * A listener handling the response after purchasing a product.
	 */
	private OnPurchaseSuccessListener onPurchaseSuccessListener = new OnPurchaseSuccessListener() {
		@Override
		public void handlePurchase(final Purchase purchase, final boolean addedPremiumProduct) {
			PreferenceUtil.setSharedPreferenceBoolean(R.string.key_pref_has_premium, true);
			MessageDialogListener listener = new MessageDialogListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void onDialogFinished() {
					((SettingsActivity) getActivity()).returnResult(true);
				}
			};

			DialogUtil.displayInfo(getActivity(), listener, 0, R.string.dialog_info_premium_thanks);
		}
	};

}
