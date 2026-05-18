package de.jeisfeld.randomimage;

import android.Manifest;
import android.Manifest.permission;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.NotificationPermissionUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * An activity used for starting the app.
 * Here, required app permissions are checked for Android 6.
 */
public abstract class StartActivity extends BaseActivity {
	/**
	 * The request code used to query for permission.
	 */
	protected static final int REQUEST_CODE_PERMISSION = 3;

	/**
	 * The request code used to query for notification permission without blocking the app startup.
	 */
	private static final int REQUEST_CODE_STARTUP_NOTIFICATION_PERMISSION = 4;

	// OVERRIDABLE
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final boolean imagePermissionMissing = isImagePermissionMissing();
		final boolean notificationPermissionMissing = !NotificationPermissionUtil.hasNotificationPermission(this);
		final boolean mustHaveNotificationPermission = isNotificationPermissionRequiredForActivity() && notificationPermissionMissing;

		if (imagePermissionMissing || mustHaveNotificationPermission) {
			DialogUtil.displayConfirmationMessage(this, new ConfirmDialogListener() {
				@Override
				public void onDialogPositiveClick(final DialogFragment dialog) {
					ActivityCompat.requestPermissions(StartActivity.this, createPermissionRequest(mustHaveNotificationPermission), REQUEST_CODE_PERMISSION);
				}

				@Override
				public void onDialogNegativeClick(final DialogFragment dialog) {
					finish();
				}
			}, R.string.title_dialog_request_permission, R.string.button_continue, SystemUtil.isAtLeastVersion(VERSION_CODES.TIRAMISU)
					? R.string.dialog_confirmation_need_image_permission : R.string.dialog_confirmation_need_read_permission);
		}
		else if (shouldRequestNotificationPermissionOnStartup() && NotificationPermissionUtil.shouldRequestStartupNotificationPermission(this)) {
			NotificationPermissionUtil.requestStartupNotificationPermission(this, REQUEST_CODE_STARTUP_NOTIFICATION_PERMISSION);
		}
	}

	/**
	 * Check if this activity requires notification permission to continue.
	 *
	 * @return true if notification permission is required.
	 */
	protected boolean isNotificationPermissionRequiredForActivity() {
		return false;
	}

	/**
	 * Check if startup may request notification permission opportunistically.
	 *
	 * @return true if startup may request notification permission.
	 */
	protected boolean shouldRequestNotificationPermissionOnStartup() {
		return true;
	}

	/**
	 * Check if image access permissions are missing.
	 *
	 * @return true if an image access permission is missing.
	 */
	private boolean isImagePermissionMissing() {
		int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		int locationPermission = PackageManager.PERMISSION_GRANTED;
		int mediaPermission = PackageManager.PERMISSION_GRANTED;
		if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
			locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION);
		}
		if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
			mediaPermission = ContextCompat.checkSelfPermission(this, permission.READ_MEDIA_IMAGES);
		}

		return (!SystemUtil.isAtLeastVersion(VERSION_CODES.TIRAMISU) && readPermission != PackageManager.PERMISSION_GRANTED) // BOOLEAN_EXPRESSION_COMPLEXITY
				|| (!SystemUtil.isAtLeastVersion(VERSION_CODES.Q) && writePermission != PackageManager.PERMISSION_GRANTED)
				|| (SystemUtil.isAtLeastVersion(VERSION_CODES.R) && locationPermission != PackageManager.PERMISSION_GRANTED)
				|| (SystemUtil.isAtLeastVersion(VERSION_CODES.TIRAMISU) && mediaPermission != PackageManager.PERMISSION_GRANTED);
	}

	/**
	 * Create the list of permissions to request.
	 *
	 * @param requireNotificationPermission true if notification permission is required for continuing.
	 * @return The permissions to request.
	 */
	private String[] createPermissionRequest(final boolean requireNotificationPermission) {
		List<String> permissions = new ArrayList<>();
		if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
			permissions.add(permission.ACCESS_MEDIA_LOCATION);
			permissions.add(permission.READ_MEDIA_IMAGES);
			if (requireNotificationPermission
					|| (shouldRequestNotificationPermissionOnStartup() && NotificationPermissionUtil.shouldRequestStartupNotificationPermission(this))) {
				permissions.add(permission.POST_NOTIFICATIONS);
				NotificationPermissionUtil.markNotificationPermissionRequested();
			}
		}
		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			permissions.add(permission.READ_EXTERNAL_STORAGE);
			permissions.add(permission.WRITE_EXTERNAL_STORAGE);
			permissions.add(permission.ACCESS_MEDIA_LOCATION);
		}
		else {
			permissions.add(permission.READ_EXTERNAL_STORAGE);
			permissions.add(permission.WRITE_EXTERNAL_STORAGE);
		}
		return permissions.toArray(new String[0]);
	}

	// OVERRIDABLE
	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		if (requestCode == REQUEST_CODE_PERMISSION) {
			if (!areRequiredPermissionsGranted()) {
				finish();
				return;
			}
			ImageUtil.init();
			DialogUtil.displaySearchForImageFoldersIfRequired(this, false);
			if (SystemUtil.findImagesViaMediaStore()) {
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		}
	}

	/**
	 * Check whether all permissions required to continue are granted.
	 *
	 * @return true if all required permissions are granted.
	 */
	private boolean areRequiredPermissionsGranted() {
		return !isImagePermissionMissing()
				&& (!isNotificationPermissionRequiredForActivity() || NotificationPermissionUtil.hasNotificationPermission(this));
	}

	/**
	 * Activities to be done after the first image list has been automatically created.
	 */
	public void updateAfterFirstImageListCreated() {

	}

}
