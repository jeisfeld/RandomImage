package de.jeisfeld.randomimage.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.RequiresApi;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.StartActivity;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.DisplayMessageDialogFragment.MessageDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.RequestInputDialogFragment.RequestInputDialogListener;
import de.jeisfeld.randomimage.util.DialogUtil.SelectFromListDialogFragment.SelectFromListDialogListener;
import de.jeisfeld.randomimagelib.R;

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
	 * Parameter to pass the view resource to the DialogFragment.
	 */
	private static final String PARAM_VIEW = "view";
	/**
	 * Parameter to pass the skip preference to the DialogFragment.
	 */
	private static final String PARAM_SKIPPREFERENCE = "preference";
	/**
	 * Parameter to pass the icon to the DialogFragment.
	 */
	private static final String PARAM_ICON = "icon";
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
	 * @param activity        the current activity
	 * @param messageResource the message resource
	 * @param args            arguments for the error message
	 */
	public static void displayInfo(final Activity activity,
								   final int messageResource,
								   final Object... args) {
		displayInfo(activity, null, 0, messageResource, args);
	}

	/**
	 * Display an information message and go back to the current activity.
	 *
	 * @param activity           the current activity
	 * @param listener           an optional listener waiting for the dialog response. If a listener is given, then the dialog will not
	 *                           be automatically recreated on orientation change!
	 * @param skipDialogResource the preference indicating if the info should be skipped. Value 0 should be used if the dialog is
	 *                           non-skippable.
	 * @param messageResource    the message resource
	 * @param args               arguments for the error message
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

		String message = capitalizeFirst(activity.getString(messageResource, args));
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putString(PARAM_TITLE, activity.getString(R.string.title_dialog_info));
		bundle.putInt(PARAM_ICON, R.drawable.ic_title_info);
		DisplayMessageDialogFragment fragment = new DisplayMessageDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a toast.
	 *
	 * @param context  the current activity or context
	 * @param resource the message resource
	 * @param args     arguments for the error message
	 */
	public static void displayToast(final Context context, final int resource, final Object... args) {
		try {
			final String message = capitalizeFirst(context.getString(resource, args));
			if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
			else {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(context, message, Toast.LENGTH_LONG).show();
					}
				});
			}
		}
		catch (Exception e) {
			Log.e(Application.TAG, "Error displaying toast.", e);
		}
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity        the current activity
	 * @param listener        The listener waiting for the response
	 * @param titleResource   the title of the confirmation dialog
	 * @param buttonResource  the display on the positive button
	 * @param messageResource the confirmation message
	 * @param args            arguments for the confirmation message
	 */
	public static void displayConfirmationMessage(final Activity activity, final ConfirmDialogListener listener,
												  final Integer titleResource, final int buttonResource,
												  final int messageResource, final Object... args) {
		String message = capitalizeFirst(activity.getString(messageResource, args));
		Bundle bundle = new Bundle();
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_BUTTON_RESOURCE, buttonResource);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource == null ? R.string.title_dialog_confirmation : titleResource);
		ConfirmDialogFragment fragment = new ConfirmDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity        the current activity
	 * @param listener        The listener waiting for the response
	 * @param titleResource   the resource with the title string
	 * @param buttonResource  the display on the positive button
	 * @param textValue       the text to be displayed in the input field
	 * @param messageResource the confirmation message
	 * @param args            arguments for the confirmation message
	 */
	public static void displayInputDialog(final Activity activity,
										  final RequestInputDialogListener listener, final int titleResource, final int buttonResource,
										  final String textValue, final int messageResource, final Object... args) {
		String message = capitalizeFirst(activity.getString(messageResource, args));
		Bundle bundle = new Bundle();
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource);
		bundle.putInt(PARAM_BUTTON_RESOURCE, buttonResource);
		bundle.putString(PARAM_TEXT_VALUE, textValue);
		RequestInputDialogFragment fragment = new RequestInputDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity        the current activity
	 * @param listener        The listener waiting for the response
	 * @param iconId          The icon to be used in the dialog.
	 * @param titleResource   the resource with the title string
	 * @param listValues      the array of values from which to be selected.
	 * @param messageResource the confirmation message
	 * @param args            arguments for the confirmation message
	 */
	public static void displayListSelectionDialog(final Activity activity,
												  final SelectFromListDialogListener listener, final int iconId, final int titleResource,
												  final ArrayList<String> listValues,
												  final int messageResource, final Object... args) {
		String message = capitalizeFirst(activity.getString(messageResource, args));
		Bundle bundle = new Bundle();
		if (iconId != 0) {
			bundle.putInt(PARAM_ICON, iconId);
		}
		bundle.putCharSequence(PARAM_MESSAGE, message);
		bundle.putInt(PARAM_TITLE_RESOURCE, titleResource);
		bundle.putStringArrayList(PARAM_LIST_ITEMS, listValues);
		SelectFromListDialogFragment fragment = new SelectFromListDialogFragment();
		fragment.setListener(listener);
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
	}

	/**
	 * Display a confirmation message asking for cancel or ok.
	 *
	 * @param activity        the current activity
	 * @param listener        The listener waiting for the response
	 * @param titleResource   the resource with the title string
	 * @param listValues      the array of values from which to be selected.
	 * @param messageResource the confirmation message
	 * @param args            arguments for the confirmation message
	 */
	public static void displayListSelectionDialog(final Activity activity,
												  final SelectFromListDialogListener listener,
												  final int titleResource,
												  final ArrayList<String> listValues,
												  final int messageResource,
												  final Object... args) {
		displayListSelectionDialog(activity, listener, 0, titleResource, listValues, messageResource, args);
	}

	/**
	 * Create a message sub-string telling about the number and names of folders and or files.
	 *
	 * @param nestedImageLists The list of nested image lists.
	 * @param folderList       The list of folders.
	 * @param imageList        The list of files.
	 * @return The message sub-string.
	 */
	public static String createFileFolderMessageString(final List<String> nestedImageLists,
													   final List<String> folderList,
													   final List<String> imageList) {
		int nestedListCount = nestedImageLists == null ? 0 : nestedImageLists.size();
		int folderCount = folderList == null ? 0 : folderList.size();
		int imageCount = imageList == null ? 0 : imageList.size();

		if (nestedListCount == 0 && folderCount == 0 && imageCount == 0) {
			return Application.getResourceString(R.string.partial_none);
		}

		String imageString = imageList != null && imageCount == 1
				? getFormattedString(R.string.partial_image_single, new File(imageList.get(0)).getName())
				: getFormattedString(R.string.partial_image_multiple, imageCount);

		String nestedListString = nestedImageLists == null ? "" : getFormattedString(
				nestedImageLists.size() == 1 ? R.string.partial_nested_list_single : R.string.partial_nested_list_multiple,
				createListNameString(nestedImageLists));

		String folderString = null;
		if (folderList != null && folderCount > 0) {
			switch (folderCount) {
			case 1:
				folderString =
						getFormattedString(R.string.partial_folder_single, ImageUtil.getImageFolderShortName(folderList.get(0)));
				break;
			case 2:
				folderString = getFormattedString(R.string.partial_folder_two, ImageUtil.getImageFolderShortName(folderList.get(0)),
						ImageUtil.getImageFolderShortName(folderList.get(1)));
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
	 * Create a concatenated String for multiple list names.
	 *
	 * @param listNames The list names.
	 * @return The concatenated String.
	 */
	public static String createListNameString(final List<String> listNames) {
		if (listNames.size() == 0) {
			return "";
		}
		else if (listNames.size() == 1) {
			return Application.getResourceString(R.string.partial_quoted_string, listNames.get(0));
		}
		else {
			String lastTwoNames = Application.getResourceString(R.string.partial_and,
					Application.getResourceString(R.string.partial_quoted_string, listNames.get(listNames.size() - 2)),
					Application.getResourceString(R.string.partial_quoted_string, listNames.get(listNames.size() - 1)));

			StringBuilder listNameStringBuilder = new StringBuilder();
			for (int i = 0; i < listNames.size() - 2; i++) {
				listNameStringBuilder.append(Application.getResourceString(R.string.partial_quoted_string, listNames.get(i)));
				listNameStringBuilder.append(", ");
			}
			listNameStringBuilder.append(lastTwoNames);
			return listNameStringBuilder.toString();
		}
	}

	/**
	 * Display the first use message if not yet shown before.
	 *
	 * @param activity The calling activity.
	 */
	public static void displayFirstUseMessageIfRequired(final Activity activity) {
		boolean firstUseInfoWasDisplayed = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_hint_first_use);

		if (!firstUseInfoWasDisplayed) {
			// Check if another version of the app is installed (pro vs. standard)
			String packageName = Application.getAppContext().getPackageName();
			String altPackageName;
			if ("de.jeisfeld.randomimagepro".equals(packageName)) {
				altPackageName = "de.jeisfeld.randomimage";
			}
			else {
				altPackageName = "de.jeisfeld.randomimagepro";
			}
			boolean isAltPackageInstalled = SystemUtil.isAppInstalled(altPackageName);
			String appName = activity.getString(R.string.app_name);

			if (isAltPackageInstalled) {
				DialogUtil.displayInfo(activity, null, R.string.key_hint_first_use, R.string.dialog_hint_first_use_upgrade, appName);
			}
			else {
				Bundle bundle = new Bundle();

				boolean skipDialog = PreferenceUtil.getSharedPreferenceBoolean(R.string.key_hint_first_use);
				if (skipDialog) {
					return;
				}
				bundle.putInt(PARAM_SKIPPREFERENCE, R.string.key_hint_first_use);

				bundle.putInt(PARAM_VIEW, R.layout.dialog_first_use);
				bundle.putString(PARAM_TITLE, activity.getString(R.string.title_dialog_first_use));
				bundle.putInt(PARAM_ICON, R.drawable.ic_launcher);
				DisplayMessageDialogFragment fragment = new DisplayMessageDialogFragment();
				fragment.setArguments(bundle);
				fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
			}
		}
	}

	/**
	 * Search for image folders if this was not done before.
	 *
	 * @param activity The calling activity.
	 * @param reparse  flag indicating if the folders should be reparsed if this was already done before.
	 * @return true if the dialog was started.
	 */
	public static boolean displaySearchForImageFoldersIfRequired(final Activity activity, final boolean reparse) {
		if (!reparse && PreferenceUtil.getSharedPreferenceString(R.string.key_all_image_folders) != null) {
			return false;
		}
		SystemUtil.lockOrientation(activity, true);
		SearchImageFoldersDialogFragment fragment = new SearchImageFoldersDialogFragment();
		fragment.setCancelable(false);
		fragment.show(activity.getFragmentManager(), fragment.getClass().toString());
		return true;
	}

	/**
	 * Get a formatted String out of a message resource and parameter strings.
	 *
	 * @param resourceId The message resource.
	 * @param args       The parameter strings.
	 * @return The formatted string.
	 */
	private static String getFormattedString(final int resourceId, final Object... args) {
		return Application.getResourceString(resourceId, args);
	}

	/**
	 * Capitalize the first letter of a String.
	 *
	 * @param input The input String
	 * @return The same string with the first letter capitalized.
	 */
	public static String capitalizeFirst(final String input) {
		if (input == null || input.length() == 0) {
			return input;
		}
		else {
			return input.substring(0, 1).toUpperCase(Locale.getDefault()) + input.substring(1);
		}
	}

	/**
	 * Convert a html String into a text.
	 *
	 * @param html The html
	 * @return the text
	 */
	public static Spanned fromHtml(final String html) {
		if (VERSION.SDK_INT >= VERSION_CODES.N) {
			return fromHtml24(html);
		}
		else {
			return fromHtml23(html);
		}
	}

	/**
	 * Convert a html String into a text (Android version below N).
	 *
	 * @param html The html
	 * @return the text
	 */
	private static Spanned fromHtml23(final String html) {
		return Html.fromHtml(html);
	}

	/**
	 * Convert a html String into a text (Android version N or higher).
	 *
	 * @param html The html
	 * @return the text
	 */
	@RequiresApi(api = VERSION_CODES.N)
	private static Spanned fromHtml24(final String html) {
		return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
	}

	/**
	 * Fragment to display an error and go back to the current activity.
	 */
	public static class DisplayMessageDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private MessageDialogListener mListener = null;

		public final void setListener(final MessageDialogListener listener) {
			mListener = listener;
		}

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			final CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			final String title = getArguments().getString(PARAM_TITLE);
			final int iconResource = getArguments().getInt(PARAM_ICON);
			final int viewResource = getArguments().getInt(PARAM_VIEW);
			final int skipPreference = getArguments().getInt(PARAM_SKIPPREFERENCE); // STORE_PROPERTY

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				mListener = null;
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(title) //
					.setIcon(iconResource);

			if (viewResource == 0) {
				builder.setMessage(message);
			}
			else {
				View view = getActivity().getLayoutInflater().inflate(viewResource, null);
				builder.setView(view);
			}

			if (skipPreference == 0) {
				builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int id) {
						if (mListener != null) {
							mListener.onDialogFinished();
						}
						dialog.dismiss();
					}
				});
			}
			else {
				builder
						.setPositiveButton(R.string.button_show_later, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int id) {
								if (mListener != null) {
									mListener.onDialogFinished();
								}
								dialog.dismiss();
							}
						})
						.setNegativeButton(R.string.button_dont_show, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int id) {
								PreferenceUtil.setSharedPreferenceBoolean(skipPreference, true);
								if (mListener != null) {
									mListener.onDialogFinished();
								}
								dialog.dismiss();
							}
						});
			}

			return builder.create();
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (mListener != null) {
				mListener.onDialogFinished();
			}
			super.onCancel(dialogInterface);
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (mListener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				mListener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * A callback handler for the dialog.
		 */
		public interface MessageDialogListener {
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
		private ConfirmDialogListener mListener = null;

		public final void setListener(final ConfirmDialogListener listener) {
			mListener = listener;
		}

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int confirmButtonResource = getArguments().getInt(PARAM_BUTTON_RESOURCE);
			int titleResource = getArguments().getInt(PARAM_TITLE_RESOURCE);

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				mListener = null;
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(titleResource) //
					.setIcon(R.drawable.ic_title_warning) //
					.setMessage(message) //
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the positive button event back to the host activity
							if (mListener != null) {
								mListener.onDialogNegativeClick(ConfirmDialogFragment.this);
							}
						}
					}) //
					.setPositiveButton(confirmButtonResource, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the negative button event back to the host activity
							if (mListener != null) {
								mListener.onDialogPositiveClick(ConfirmDialogFragment.this);
							}
						}
					});
			return builder.create();
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (mListener != null) {
				mListener.onDialogNegativeClick(ConfirmDialogFragment.this);
			}
			super.onCancel(dialogInterface);
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (mListener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				mListener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * A callback handler for the dialog.
		 */
		public interface ConfirmDialogListener {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog the confirmation dialog fragment.
			 */
			void onDialogPositiveClick(DialogFragment dialog);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(DialogFragment dialog);
		}
	}

	/**
	 * Fragment to request an input.
	 */
	public static class RequestInputDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private RequestInputDialogListener mListener = null;

		public final void setListener(final RequestInputDialogListener listener) {
			mListener = listener;
		}

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			// VARIABLE_DISTANCE:OFF
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int confirmButtonResource = getArguments().getInt(PARAM_BUTTON_RESOURCE);
			int titleResource = getArguments().getInt(PARAM_TITLE_RESOURCE);
			// VARIABLE_DISTANCE:ON

			final EditText input = new EditText(getActivity());
			input.setText(getArguments().getString(PARAM_TEXT_VALUE));

			// Listeners cannot retain functionality when automatically recreated.
			// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				mListener = null;
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
							if (mListener != null) {
								mListener.onDialogNegativeClick(RequestInputDialogFragment.this);
							}
						}
					}) //
					.setPositiveButton(confirmButtonResource, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the negative button event back to the host activity
							if (mListener != null) {
								mListener.onDialogPositiveClick(RequestInputDialogFragment.this, input.getText().toString());
							}
						}
					});
			return builder.create();
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (mListener != null) {
				mListener.onDialogNegativeClick(RequestInputDialogFragment.this);
			}
			super.onCancel(dialogInterface);
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (mListener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				mListener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * A callback handler for the dialog.
		 */
		public interface RequestInputDialogListener {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog the confirmation dialog fragment.
			 * @param text   the text returned from the input.
			 */
			void onDialogPositiveClick(DialogFragment dialog, String text);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(DialogFragment dialog);
		}
	}

	/**
	 * Fragment to request an selection from a set of choices.
	 */
	public static class SelectFromListDialogFragment extends DialogFragment {
		/**
		 * The listener called when the dialog is ended.
		 */
		private SelectFromListDialogListener mListener = null;

		public final void setListener(final SelectFromListDialogListener listener) {
			mListener = listener;
		}

		@SuppressLint("InflateParams")
		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			// VARIABLE_DISTANCE:OFF
			CharSequence message = getArguments().getCharSequence(PARAM_MESSAGE);
			int titleResource = getArguments().getInt(PARAM_TITLE_RESOURCE);
			// VARIABLE_DISTANCE:ON

			ArrayList<String> itemList = getArguments().getStringArrayList(PARAM_LIST_ITEMS);
			final String[] items = itemList == null ? new String[0] : itemList.toArray(new String[0]);
			final int iconId = getArguments().getInt(PARAM_ICON);

			// setMessage is not combinable with setItems, therefore using setView for the list of names.
			ListView listView = new ListView(getActivity());

			TextView messageView = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.text_view_list_header, null);
			messageView.setText(message);
			listView.addHeaderView(messageView, null, false);

			ArrayAdapter<String> listViewAdapter = new ArrayAdapter<>(getActivity(), R.layout.adapter_list_names, items);
			listView.setAdapter(listViewAdapter);
			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
					int listPosition = (int) id;
					mListener.onDialogPositiveClick(SelectFromListDialogFragment.this, listPosition, items[listPosition]);
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
				mListener = null;
				dismiss();
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(titleResource)
					.setView(listView)
					.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int id) {
							// Send the positive button event back to the host activity
							if (mListener != null) {
								mListener.onDialogNegativeClick(SelectFromListDialogFragment.this);
							}
						}
					});

			if (iconId != 0) {
				builder.setIcon(iconId);
			}
			return builder.create();
		}

		@Override
		public final void onCancel(final DialogInterface dialogInterface) {
			if (mListener != null) {
				mListener.onDialogNegativeClick(SelectFromListDialogFragment.this);
			}
			super.onCancel(dialogInterface);
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			if (mListener != null) {
				// Typically cannot serialize the listener due to its reference to the activity.
				mListener = null;
				outState.putBoolean(PREVENT_RECREATION, true);
			}
			super.onSaveInstanceState(outState);
		}

		/**
		 * A callback handler for the dialog.
		 */
		public interface SelectFromListDialogListener {
			/**
			 * Callback method for positive click from the confirmation dialog.
			 *
			 * @param dialog   the confirmation dialog fragment.
			 * @param position the position of the array that was selected.
			 * @param text     the text returned from the input.
			 */
			void onDialogPositiveClick(DialogFragment dialog, int position, String text);

			/**
			 * Callback method for negative click from the confirmation dialog.
			 *
			 * @param dialog the confirmation dialog fragment.
			 */
			void onDialogNegativeClick(DialogFragment dialog);
		}
	}

	/**
	 * Fragment to display a dialog while searching for image folders.
	 */
	public static class SearchImageFoldersDialogFragment extends DialogFragment {
		/**
		 * The view displaying folders while searching.
		 */
		private TextView mMessageView;
		/**
		 * The number of found folders.
		 */
		private int mFolderCount = 0;

		@Override
		public final Dialog onCreateDialog(final Bundle savedInstanceState) {
			// Do not restart on orientation change.
			boolean preventRecreation = false;
			if (savedInstanceState != null) {
				preventRecreation = savedInstanceState.getBoolean(PREVENT_RECREATION);
			}
			if (preventRecreation) {
				dismiss();
			}

			LayoutInflater inflater = getActivity().getLayoutInflater();
			@SuppressLint("InflateParams")
			View view = inflater.inflate(R.layout.dialog_searching_images, null);

			mMessageView = view.findViewById(R.id.textViewImageFolder);

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.title_dialog_searching_image_folders) //
					.setIcon(R.drawable.ic_title_info) //
					.setView(view);

			ImageUtil.getAllImageFolders(new ImageUtil.OnImageFoldersFoundListener() {
				@Override
				public void handleImageFolders(final ArrayList<String> imageFolders) {
					ImageList imageList = ImageRegistry.getCurrentImageListRefreshed(false);
					if (imageList.isEmpty() && imageFolders.size() > 0) {
						imageList.addAllSdRoots();
						if (getActivity() instanceof StartActivity) {
							((StartActivity) getActivity()).updateAfterFirstImageListCreated();
						}
					}

					try {
						SystemUtil.lockOrientation(getActivity(), false);
						dismiss();
					}
					catch (Exception e) {
						// ignore
					}
				}

				@Override
				public void handleImageFolder(final String imageFolder) {
					mFolderCount++;
					if (mMessageView != null) {
						mMessageView.setText(mFolderCount + "\n\n" + imageFolder);
					}
				}
			});

			return builder.create();
		}

		@Override
		public final void onSaveInstanceState(final Bundle outState) {
			outState.putBoolean(PREVENT_RECREATION, true);
			super.onSaveInstanceState(outState);
		}
	}
}
