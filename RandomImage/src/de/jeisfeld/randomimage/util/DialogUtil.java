package de.jeisfeld.randomimage.util;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;

/**
 * Helper class to show standard dialogs.
 */
public final class DialogUtil {
	/**
	 * Parameter to pass the title text to the DialogFragment.
	 */
	private static final String PARAM_TITLE = "title";
	/**
	 * Parameter to pass the title resource to the DialogFragment.
	 */
	private static final String PARAM_TITLE_RESOURCE = "titleResource";
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
	 * Parameter to pass the text resource for the confirmation button to the ConfirmDialogFragment.
	 */
	private static final String PARAM_BUTTON_RESOURCE = "buttonResource";
	/**
	 * Parameter to pass the text value of the input field.
	 */
	private static final String PARAM_TEXT_VALUE = "textValue";
	/**
	 * Parameter to pass a list of items.
	 */
	private static final String PARAM_LIST_ITEMS = "listItems";

	/**
	 * Instance state flag indicating if a dialog should not be recreated after orientation change.
	 */
	private static final String PREVENT_RECREATION = "preventRecreation";

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
	 * @param skipDialogResource
	 *            the preference indicating if the info should be skipped. Value 0 should be used if the dialog is
	 *            non-skippable.
	 * @param messageResource
	 *            the message resource
	 * @param args
	 *            arguments for the error message
	 */
	public static void displayInfo(final Activity activity,
			final MessageDialogListener listener,
			final int skipDialogResource,
			final int messageResource,
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
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity
	 *            the current activity
	 * @param listener
	 *            The listener waiting for the response
	 * @param buttonResource
	 *            the display on the positive button
	 * @param messageResource
	 *            the confirmation message
	 * @param args
	 *            arguments for the confirmation message
	 */
	public static void displayConfirmationMessage(final Activity activity,
			final ConfirmDialogListener listener, final int buttonResource,
			final int messageResource, final Object... args) {
		String message = String.format(activity.getString(messageResource), args);
		Bundle bundle = new Bundle();
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_BUTTON_RESOURCE, buttonResource);
		bundle.putSerializable(PARAM_LISTENER, listener);
		ConfirmDialogFragment fragment = new ConfirmDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity
	 *            the current activity
	 * @param listener
	 *            The listener waiting for the response
	 * @param titleResource
	 *            the resource with the title string
	 * @param buttonResource
	 *            the display on the positive button
	 * @param textValue
	 *            the text to be displayed in the input field
	 * @param messageResource
	 *            the confirmation message
	 * @param args
	 *            arguments for the confirmation message
	 */
	public static void displayInputDialog(final Activity activity,
			final RequestInputDialogListener listener, final int titleResource, final int buttonResource,
			final String textValue, final int messageResource, final Object... args) {
		String message = String.format(activity.getString(messageResource), args);
		Bundle bundle = new Bundle();
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource);
		bundle.putInt(PARAM_BUTTON_RESOURCE, buttonResource);
		bundle.putString(PARAM_TEXT_VALUE, textValue);
		bundle.putSerializable(PARAM_LISTENER, listener);
		RequestInputDialogFragment fragment = new RequestInputDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity
	 *            the current activity
	 * @param listener
	 *            The listener waiting for the response
	 * @param titleResource
	 *            the resource with the title string
	 * @param listValues
	 *            the array of values from which to be selected.
	 * @param messageResource
	 *            the confirmation message
	 * @param args
	 *            arguments for the confirmation message
	 */
	public static void displayListSelectionDialog(final Activity activity,
			final SelectFromListDialogListener listener, final int titleResource, final ArrayList<String> listValues,
			final int messageResource, final Object... args) {
		String message = String.format(activity.getString(messageResource), args);
		Bundle bundle = new Bundle();
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource);
		bundle.putStringArrayList(PARAM_LIST_ITEMS, listValues);
		bundle.putSerializable(PARAM_LISTENER, listener);
		SelectFromListDialogFragment fragment = new SelectFromListDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
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
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
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
				outState.putBoolean(PREVENT_RECREATION, true);
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

	/**
	 * Fragment to display a confirmation message.
	 */
	public static class ConfirmDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private ConfirmDialogListener listener = null;

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int confirmButtonResource = getArguments().getInt(PARAM_BUTTON_RESOURCE);
			listener = (ConfirmDialogListener) getArguments().getSerializable(PARAM_LISTENER);

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_dialog_confirmation) //
					.setIcon(R.drawable.ic_title_warning) //
					.setMessage(message) //
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the positive button event back to the host activity
							listener.onDialogNegativeClick(ConfirmDialogFragment.this);
						}
					}) //
					.setPositiveButton(confirmButtonResource, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the negative button event back to the host activity
							listener.onDialogPositiveClick(ConfirmDialogFragment.this);
						}
					});
			return builder.create();
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (listener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				listener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * The activity that creates an instance of this dialog listFoldersFragment must implement this interface in
		 * order to receive event callbacks. Each method passes the DialogFragment in case the host needs to query it.
		 */
		public interface ConfirmDialogListener extends Serializable {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 */
			void onDialogPositiveClick(final DialogFragment dialog);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(final DialogFragment dialog);
		}
	}

	/**
	 * Fragment to request an input.
	 */
	public static class RequestInputDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private RequestInputDialogListener listener = null;

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int confirmButtonResource = getArguments().getInt(PARAM_BUTTON_RESOURCE);
			int titleResource = getArguments().getInt(PARAM_TITLE_RESOURCE);
			listener = (RequestInputDialogListener) getArguments().getSerializable(PARAM_LISTENER);

			final EditText input = new EditText(getActivity());
			input.setText(getArguments().getString(PARAM_TEXT_VALUE));

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(titleResource) //
					.setMessage(message) //
					.setView(input) //
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the positive button event back to the host activity
							listener.onDialogNegativeClick(RequestInputDialogFragment.this);
						}
					}) //
					.setPositiveButton(confirmButtonResource, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the negative button event back to the host activity
							listener.onDialogPositiveClick(RequestInputDialogFragment.this, input.getText().toString());
						}
					});
			return builder.create();
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (listener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				listener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * The activity that creates an instance of this dialog listFoldersFragment must implement this interface in
		 * order to receive event callbacks. Each method passes the DialogFragment in case the host needs to query it.
		 */
		public interface RequestInputDialogListener extends Serializable {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 * @param text
			 *            the text returned from the input.
			 */
			void onDialogPositiveClick(final DialogFragment dialog, final String text);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(final DialogFragment dialog);
		}
	}

	/**
	 * Fragment to request an selection from a set of choices.
	 */
	public static class SelectFromListDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private SelectFromListDialogListener listener = null;

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int titleResource = getArguments().getInt(PARAM_TITLE_RESOURCE);
			listener = (SelectFromListDialogListener) getArguments().getSerializable(PARAM_LISTENER);
			final String[] items = getArguments().getStringArrayList(PARAM_LIST_ITEMS).toArray(new String[0]);

			// setMessage is not combinable with setItems, therefore using setView for the list of names.
			ListView listView = new ListView(getActivity());
			ArrayAdapter<String> listViewAdapter =
					new ArrayAdapter<String>(getActivity(), R.layout.adapter_list_names, items);
			listView.setAdapter(listViewAdapter);
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void
						onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
					listener.onDialogPositiveClick(SelectFromListDialogFragment.this, position, items[position]);
					dismiss();
				}
			});

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(titleResource) //
					.setMessage(message)
					.setView(listView)
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the positive button event back to the host activity
							listener.onDialogNegativeClick(SelectFromListDialogFragment.this);
						}
					});
			return builder.create();
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (listener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				listener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * The activity that creates an instance of this dialog listFoldersFragment must implement this interface in
		 * order to receive event callbacks. Each method passes the DialogFragment in case the host needs to query it.
		 */
		public interface SelectFromListDialogListener extends Serializable {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 * @param the
			 *            position of the array that was selected.
			 * @param text
			 *            the text returned from the input.
			 */
			void onDialogPositiveClick(final DialogFragment dialog, final int position, final String text);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog
			 *            the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(final DialogFragment dialog);
		}
	}
}
