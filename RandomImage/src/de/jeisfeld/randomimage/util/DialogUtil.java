package de.jeisfeld.randomimage.util;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

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
import de.jeisfeld.randomimage.Application;
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

		String message = capitalizeFirst(String.format(activity.getString(messageResource), args));
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
		try {
			String message = capitalizeFirst(String.format(context.getString(resource), args));
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		}
		catch (Exception e) {
			// Do nothing - just prevent runtime exception.
		}
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
		String message = capitalizeFirst(String.format(activity.getString(messageResource), args));
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
		String message = capitalizeFirst(String.format(activity.getString(messageResource), args));
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
	 * @param iconId
	 *            The icon to be used in the dialog.
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
			final SelectFromListDialogListener listener, final int iconId, final int titleResource,
			final ArrayList<String> listValues,
			final int messageResource, final Object... args) {
		String message = capitalizeFirst(String.format(activity.getString(messageResource), args));
		Bundle bundle = new Bundle();
		if (iconId != 0) {
			bundle.putInt(PARAM_ICON, iconId);
		}
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource);
		bundle.putStringArrayList(PARAM_LIST_ITEMS, listValues);
		bundle.putSerializable(PARAM_LISTENER, listener);
		SelectFromListDialogFragment fragment = new SelectFromListDialogFragment();
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
		displayListSelectionDialog(activity, listener, 0, titleResource, listValues, messageResource, args);
	}

	/**
	 * Create a message sub-string telling about the number and names of folders and or files.
	 *
	 * @param nestedImageLists
	 *            The list of nested image lists.
	 * @param folderList
	 *            The list of folders.
	 * @param imageList
	 *            The list of files.
	 * @return The message sub-string.
	 */
	public static String createFileFolderMessageString(final ArrayList<String> nestedImageLists,
			final ArrayList<String> folderList,
			final ArrayList<String> imageList) {
		int nestedListCount = nestedImageLists == null ? 0 : nestedImageLists.size();
		int folderCount = folderList == null ? 0 : folderList.size();
		int imageCount = imageList == null ? 0 : imageList.size();

		if (nestedListCount == 0 && folderCount == 0 && imageCount == 0) {
			return Application.getResourceString(R.string.partial_none);
		}

		String imageString = imageList != null && imageCount == 1
				? getFormattedString(R.string.partial_image_single, new File(imageList.get(0)).getName())
				: getFormattedString(R.string.partial_image_multiple, imageCount);

		String nestedListString = nestedImageLists != null && nestedListCount == 1
				? getFormattedString(R.string.partial_nested_list_single, nestedImageLists.get(0))
				: getFormattedString(R.string.partial_nested_list_multiple, nestedListCount);

		String folderString = null;
		if (folderList != null && folderCount > 0) {
			switch (folderCount) {
			case 1:
				folderString =
						getFormattedString(R.string.partial_folder_single, new File(folderList.get(0)).getName());
				break;
			case 2:
				folderString = getFormattedString(R.string.partial_folder_two, new File(folderList.get(0)).getName(),
						new File(folderList.get(1)).getName());
				break;
			default:
				folderString = getFormattedString(R.string.partial_folder_multiple, folderCount);
				break;
			}
		}

		String result = null;

		if (nestedListCount > 0) {
			result = nestedListString;
		}

		if (folderCount > 0) {
			if (result == null) {
				result = folderString;
			}
			else {
				result = getFormattedString(R.string.partial_and, result, folderString);
			}
		}

		if (imageCount > 0) {
			if (result == null) {
				result = imageString;
			}
			else {
				result = getFormattedString(R.string.partial_and, result, imageString);
			}
		}

		return result;
	}

	/**
	 * Get a formatted String out of a message resource and parameter strings.
	 *
	 * @param resource
	 *            The message resource.
	 * @param args
	 *            The parameter strings.
	 * @return The formatted string.
	 */
	private static String getFormattedString(final int resourceId, final Object... args) {
		return String.format(Application.getResourceString(resourceId), args);
	}

	/**
	 * Capitalize the first letter of a String.
	 *
	 * @param input
	 *            The input String
	 * @return The same string with the first letter capitalized.
	 */
	private static String capitalizeFirst(final String input) {
		if (input == null || input.length() == 0) {
			return input;
		}
		else {
			return input.substring(0, 1).toUpperCase(Locale.getDefault()) + input.substring(1);
		}
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
		public final void onCancel(final DialogInterface dialogInterface) {
			if (listener != null) {
				listener.onDialogNegativeClick(ConfirmDialogFragment.this);
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
		public final void onCancel(final DialogInterface dialogInterface) {
			if (listener != null) {
				listener.onDialogNegativeClick(RequestInputDialogFragment.this);
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
			final int iconId = getArguments().getInt(PARAM_ICON);

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

			if (iconId != 0) {
				builder.setIcon(iconId);
			}
			return builder.create();
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (listener != null) {
				listener.onDialogNegativeClick(SelectFromListDialogFragment.this);
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
