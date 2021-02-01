package de.jeisfeld.randomimage.view;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.view.DirectoryChooserDialogFragment.ChosenDirectoryListener;
import de.jeisfeld.randomimagelib.R;

/**
 * Class to present a dialog for selection of multiple directories.
 */
public class MultiDirectoryChooserDialogFragment extends DialogFragment {
	/**
	 * Resource String for the title resource.
	 */
	private static final String STRING_TITLE_RESOURCE = "de.jeisfeld.randomimage.TITLE_RESOURCE";
	/**
	 * Resource String for the start folders.
	 */
	private static final String STRING_START_FOLDERS = "de.jeisfeld.randomimage.START_FOLDERS";
	/**
	 * Resource String for the listener.
	 */
	private static final String STRING_LISTENER = "de.jeisfeld.randomimage.LISTENER";
	/**
	 * The margin used for dialog title.
	 */
	private static final int TITLE_MARGIN = 40;
	/**
	 * The list of directories.
	 */
	private List<String> mDirs = null;
	/**
	 * The list adapter handling the sub-elements of the current folder.
	 */
	private ArrayAdapter<String> mListAdapter = null;

	/**
	 * Create a MultiDirectoryChooserDialogFragment.
	 *
	 * @param activity      The activity calling the dialog.
	 * @param titleResource The title resource.
	 * @param listener      The callback listener reacting on the dialog response.
	 * @param dirs          The start folders.
	 */
	public static void displayMultiDirectoryChooserDialog(final Activity activity, final int titleResource,
														  final ChosenDirectoriesListener listener, final List<String> dirs) {
		Bundle bundle = new Bundle();
		bundle.putStringArrayList(STRING_START_FOLDERS, new ArrayList<>(dirs));
		bundle.putInt(STRING_TITLE_RESOURCE, titleResource);
		bundle.putSerializable(STRING_LISTENER, listener);
		MultiDirectoryChooserDialogFragment fragment = new MultiDirectoryChooserDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), MultiDirectoryChooserDialogFragment.class.toString());
	}

	/*
	 * Instantiate the view of the dialog.
	 */
	@Override
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		// retrieve arguments

		mDirs = getArguments().getStringArrayList(STRING_START_FOLDERS);
		final ChosenDirectoriesListener listener = (ChosenDirectoriesListener) getArguments().getSerializable(STRING_LISTENER);
		final int titleResource = getArguments().getInt(STRING_TITLE_RESOURCE);

		Context context = getActivity();

		// Create dialog
		Builder dialogBuilder = new Builder(context);
		TextView titleView = new TextView(getActivity());
		if (VERSION.SDK_INT >= VERSION_CODES.M) {
			titleView.setTextAppearance(android.R.style.TextAppearance_DialogWindowTitle);
			titleView.setPadding(TITLE_MARGIN, TITLE_MARGIN, TITLE_MARGIN, TITLE_MARGIN);
		}
		titleView.setText(titleResource);
		dialogBuilder.setCustomTitle(titleView);

		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_multi_directory_chooser, new LinearLayout(context));
		dialogBuilder.setView(layout);

		ListView listView = layout.findViewById(R.id.listViewSelectedFolders);
		mListAdapter = createListAdapter(mDirs);
		listView.setAdapter(mListAdapter);

		Button addFolderButton = layout.findViewById(R.id.buttonAddFolder);
		addFolderButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				DirectoryChooserDialogFragment.displayDirectoryChooserDialog(getActivity(), new ChosenDirectoryListener() {
					/**
					 * The default serial version UID.
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void onChosenDir(final String chosenDir) {
						if (chosenDir != null) {
							mDirs.add(chosenDir);
						}
						mListAdapter.notifyDataSetChanged();
					}

					@Override
					public void onCancelled() {

					}
				}, null);
			}
		});

		dialogBuilder.setCancelable(false);

		dialogBuilder.setPositiveButton(R.string.button_ok, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (listener != null) {
					listener.onChosenDirs(mDirs);
				}
			}
		}).setNegativeButton(R.string.button_cancel, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (listener != null) {
					listener.onCancelled();
				}
			}
		});

		return dialogBuilder.create();
	}

	/**
	 * Create the list adapter for the list of folders.
	 *
	 * @param items The list of folders.
	 * @return The list adapter.
	 */
	private ArrayAdapter<String> createListAdapter(final List<String> items) {
		return new ArrayAdapter<String>(getActivity(), R.layout.adapter_list_names_delete_button, R.id.text, items) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				final String value = mListAdapter.getItem(position);

				TextView textView = view.findViewById(R.id.text);
				textView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
				textView.setEllipsize(null);

				textView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						DirectoryChooserDialogFragment.displayDirectoryChooserDialog(getActivity(), new ChosenDirectoryListener() {
							/**
							 * The default serial version UID.
							 */
							private static final long serialVersionUID = 1L;

							@Override
							public void onChosenDir(final String chosenDir) {
								mDirs.remove(position);
								if (chosenDir != null) {
									mDirs.add(position, chosenDir);
								}
								mListAdapter.notifyDataSetChanged();
							}

							@Override
							public void onCancelled() {

							}
						}, value);
					}
				});

				ImageButton deleteButton = view.findViewById(R.id.buttonDelete);
				deleteButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						DialogUtil.displayConfirmationMessage(getActivity(), new ConfirmDialogListener() {
							@Override
							public void onDialogPositiveClick(final DialogFragment dialog) {
								mDirs.remove(position);
								mListAdapter.notifyDataSetChanged();
							}

							@Override
							public void onDialogNegativeClick(final DialogFragment dialog) {

							}
						}, null, R.string.button_delete, R.string.dialog_confirmation_remove_preferred_folder, value);
					}
				});

				return view;
			}
		};
	}

	/**
	 * Callback interface for selected directories.
	 */
	public interface ChosenDirectoriesListener extends Serializable {
		/**
		 * Called when afolders are selected.
		 *
		 * @param chosenDirs The selected folders.
		 */
		void onChosenDirs(List<String> chosenDirs);

		/**
		 * Called when the dialog is cancelled.
		 */
		void onCancelled();
	}
}
