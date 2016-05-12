package de.jeisfeld.randomimage;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * An activity used for starting the app.
 *
 * <p>Here, potential version upgrade activities are done, and required app permissions are checked for Android 6.
 */
public abstract class StartActivity extends Activity {
	/**
	 * The request code used to query for permission.
	 */
	private static final int REQUEST_CODE_PERMISSION = 3;

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO: remove in the next version (This is used only while deprecating Google Billing)
		if (PreferenceUtil.getSharedPreferenceBoolean(R.string.key_pref_has_premium)) {
			DialogUtil.displayInfo(this, R.string.dialog_info_premium_deprecated);
			PreferenceUtil.removeSharedPreference(R.string.key_pref_has_premium);
		}

		int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
			DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
				@Override
				public void onDialogPositiveClick(final DialogFragment dialog) {
					ActivityCompat.requestPermissions(StartActivity.this,
							new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
							REQUEST_CODE_PERMISSION);
				}

				@Override
				public void onDialogNegativeClick(final DialogFragment dialog) {
					finish();
				}
			}, R.string.title_dialog_request_permission, R.string.button_continue, R.string.dialog_confirmation_need_read_permission);
		}
	}

	@Override
	public final void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		if (requestCode == REQUEST_CODE_PERMISSION) {
			// If request is cancelled, the result arrays are empty.
			if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				finish();
			}
		}
	}
}
