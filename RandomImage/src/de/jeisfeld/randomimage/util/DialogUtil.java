package de.jeisfeld.randomimage.util;

import java.io.Serializable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;

/**
 * Helper class to show standard dialogs.
 */
public final class DialogUtil {
	/**
	 * Parameter to pass the title to the DialogFragment.
	 */
	private static final String PARAM_TITLE = "title";
	/**
	 * Parameter to pass the message to the DialogFragment.
	 */
	private static final String PARAM_MESSAGE = "message";
	/**
	 * Parameter to pass the skip preference to the DialogFragment.
	 */
	private static final String PARAM_SKIPPREFERENCE = "preference";
	/**
	 * Parameter to pass the icon to the DialogFragment.
	 */
	private static final String PARAM_ICON = "icon";
	/**
	 * Parameter to pass the callback listener to the ConfirmDialogFragment.
	 */
	private static final String PARAM_LISTENER = "listener";

	/**
	 * Hide default constructor.
	 */
	private DialogUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Display an information message and go back to the current activity.
	 *
	 * @param activity
	 *            the current activity
	 * @param listener
	 *            an optional listener waiting for the dialog response. If a listener is given, then the dialog will not
	 *            be automatically recreated on orientation change!
	 * @param messageResource
	 *            the message resource
	 * @param skipDialogResource
	 *            the preference indicating if the info should be skipped
	 * @param args
	 *            arguments for the error message
	 */
	public static void displayInfo(final Activity activity,
			final MessageDialogListener listener,
			final int messageResource,
			final int skipDialogResource,
			final Object... args) {
		Bundle bundle = new Bundle();

		if (skipDialogResource != 0) {
			boolean skipDialog = PreferenceUtil.getSharedPreferenceBoolean(skipDialogResource);
			if (skipDialog) {
				if (listener != null) {
					listener.onDialogFinished();
				}
				return;
			}
			bundle.putInt(PARAM_SKIPPREFERENCE, skipDialogResource);
		}

		String message = String.format(activity.getString(messageResource), args);
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putString(PARAM_TITLE, activity.getString(R.string.title_dialog_info));
		bundle.putInt(PARAM_ICON, R.drawable.ic_title_info);
		if (listener != null) {
			bundle.putSerializable(PARAM_LISTENER, listener);
		}
		DialogFragment fragment = new DisplayMessageDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a toast.
	 *
	 * @param context
	 *            the current activity or context
	 * @param resource
	 *            the message resource
	 * @param args
	 *            arguments for the error message
	 */
	public static void displayToast(final Context context, final int resource, final Object... args) {
		String message = String.format(context.getString(resource), args);
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Fragment to display an error and go back to the current activity.
	 */
	public static class DisplayMessageDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private MessageDialogListener listener = null;

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			final CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			final String title = getArguments().getString(PARAM_TITLE);
			final int iconResource = getArguments().getInt(PARAM_ICON);
			final int skipPreference = getArguments().getInt(PARAM_SKIPPREFERENCE); // STORE_PROPERTY

			listener = (MessageDialogListener) getArguments().getSerializable(
					PARAM_LISTENER);
			getArguments().putSerializable(PARAM_LISTENER, null);

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean("preventRecreation");
			}
			if (preventRecreation) {
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(title) //
					.setIcon(iconResource) //
					.setMessage(message) //
					.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							if (listener != null) {
								listener.onDialogFinished();
							}
							dialog.dismiss();
						}
					});

			if (skipPreference != 0) {
				builder.setNegativeButton(R.string.button_dont_show, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
						PreferenceUtil.setSharedPreferenceBoolean(skipPreference, true);
						if (listener != null) {
							listener.onDialogFinished();
						}
						dialog.dismiss();
					}
				});
			}

			Dialog dialog = builder.create();
			return dialog;
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (listener != null) {
				listener.onDialogFinished();
			}
			super.onCancel(dialogInterface);
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (listener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				listener = null;
				outState.putBoolean("preventRecreation", true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * The activity that creates an instance of this dialog must implement this interface in order to receive event
		 * callbacks.
		 */
		public interface MessageDialogListener extends Serializable {
			/**
			 * Callback method called after finishing the dialog.
			 */
			void onDialogFinished();
		}
	}

}
