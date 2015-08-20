package de.jeisfeld.randomimage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * Class to present a dialog for selection of a directory.
 *
 * <p>
 * Inspired by http://www.codeproject.com/Articles/547636/Android-Ready-to-use-simple-directory-chooser-dial
 */
public class DirectoryChooserDialogFragment extends DialogFragment {
	/**
	 * The resource key for the initial folder name.
	 */
	private static final String STRING_EXTRA_FOLDER = "de.jeisfeld.randomimage.FOLDER";
	/**
	 * The resource key for the listener notified when finishing the dialog.
	 */
	private static final String STRING_EXTRA_LISTENER = "de.jeisfeld.randomimage.LISTENER";
	/**
	 * The resource key for the optional listener reacting on long clicks.
	 */
	private static final String STRING_EXTRA_LONG_CLICK_LISTENER = "de.jeisfeld.randomimage.LONG_CLICK_LISTENER";

	/**
	 * The text view showing the current folder.
	 */
	private TextView currentFolderView;

	/**
	 * The list view showing the sub-elements.
	 */
	private ListView listView;

	/**
	 * The list view showing the sub-elements.
	 */
	private GridView gridView;

	/**
	 * The current folder.
	 */
	private String currentFolder = "";

	/**
	 * The sub-elements of the current folder.
	 */
	private List<String> subdirs = null;

	/**
	 * The list adapter handling the sub-elements of the current folder.
	 */
	private ArrayAdapter<String> listAdapter = null;

	/**
	 * The backstack of last browsed folders.
	 */
	private Stack<String> backStack = new Stack<String>();

	/**
	 * A reference to the shown dialog.
	 */
	private AlertDialog dirsDialog;

	/**
	 * The listener notified when finishing the dialog.
	 */
	private ChosenDirectoryListener listener = null;

	/**
	 * An optional listener reacting on item long clicks.
	 */
	private OnFolderLongClickListener longClickListener = null;

	/**
	 * Create a DirectoryChooserDialogFragment.
	 *
	 * @param activity
	 *            The activity calling the dialog.
	 * @param listener
	 *            The callback listener reacting on the dialog response.
	 * @param longClickListener
	 *            An optional listener waiting for long clicks.
	 * @param dir
	 *            The start folder.
	 */
	public static void displayDirectoryChooserDialog(final Activity activity, final ChosenDirectoryListener listener,
			final OnFolderLongClickListener longClickListener, final String dir) {
		Bundle bundle = new Bundle();
		bundle.putString(STRING_EXTRA_FOLDER, dir);
		bundle.putSerializable(STRING_EXTRA_LISTENER, listener);
		if (longClickListener != null) {
			bundle.putSerializable(STRING_EXTRA_LONG_CLICK_LISTENER, longClickListener);
		}

		DirectoryChooserDialogFragment fragment = new DirectoryChooserDialogFragment();
		fragment.setArguments(bundle);
		fragment.show(activity.getFragmentManager(), DirectoryChooserDialogFragment.class.toString());
	}

	/*
	 * Instantiate the view of the dialog.
	 */
	@Override
	public final Dialog onCreateDialog(final Bundle savedInstanceState) {
		// Listeners cannot retain functionality when automatically recreated.
		// Therefore, dialogs with listeners must be re-created by the activity on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean("preventRecreation");
		}
		if (preventRecreation) {
			dismiss();
		}

		// retrieve arguments
		String dir = getArguments().getString(STRING_EXTRA_FOLDER);
		listener = (ChosenDirectoryListener) getArguments().getSerializable(STRING_EXTRA_LISTENER);
		longClickListener =
				(OnFolderLongClickListener) getArguments().getSerializable(STRING_EXTRA_LONG_CLICK_LISTENER);

		if (dir == null) {
			dir = PreferenceUtil.getSharedPreferenceString(R.string.key_directory_chooser_last_folder);
		}

		File dirFile = dir == null ? null : new File(dir);
		if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
			dir = getSdCardDirectory();
		}

		try {
			dir = new File(dir).getCanonicalPath();
		}
		catch (IOException ioe) {
			return null;
		}

		currentFolder = dir;
		subdirs = getDirectories(dir);

		Context context = getActivity();

		// Create dialog

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

		dialogBuilder.setTitle(R.string.title_dialog_select_folder);

		View layout =
				LayoutInflater.from(context).inflate(R.layout.dialog_directory_chooser, new LinearLayout(context));
		dialogBuilder.setView(layout);

		((TextView) layout.findViewById(R.id.textViewMessage)).setText(R.string.dialog_select_image_folder_for_add);
		currentFolderView = (TextView) layout.findViewById(R.id.textCurrentFolder);
		currentFolderView.setText(dir);

		listView = (ListView) layout.findViewById(R.id.listViewSubfolders);
		listAdapter = createListAdapter(subdirs);
		listView.setAdapter(listAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				backStack.push(currentFolder);
				currentFolder += File.separator + listAdapter.getItem(position);
				updateDirectory();
			}
		});

		if (longClickListener != null) {
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position,
						final long id) {
					final String selectedFolder = currentFolder + File.separator + listAdapter.getItem(position);
					return longClickListener.onFolderLongClick(selectedFolder);
				}
			});
		}

		gridView = (GridView) layout.findViewById(R.id.gridViewImages);

		dialogBuilder.setCancelable(false);

		dialogBuilder.setPositiveButton(R.string.button_select_folder, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// Current directory chosen
				if (listener != null) {
					// Call registered listener supplied with the chosen directory
					PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, currentFolder);
					listener.onChosenDir(currentFolder);
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

		dirsDialog = dialogBuilder.create();

		dirsDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					// Back button pressed - go to the last directory if existing - otherwise cancel the dialog.
					if (backStack.size() == 0) {
						dirsDialog.dismiss();
						if (listener != null) {
							listener.onCancelled();
						}
					}
					else {
						currentFolder = backStack.pop();
						updateDirectory();
					}

					return true;
				}
				else {
					return false;
				}
			}
		});

		return dirsDialog;
	}

	@Override
	public final void onStart() {
		super.onStart();
		fillGridView();
	}

	/**
	 * Update the enablement status of the positive button.
	 *
	 * @param enabled
	 *            The new enablement status.
	 */
	private void enablePositiveButton(final boolean enabled) {
		dirsDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
	}

	/**
	 * Fill the GridView to display the images.
	 */
	private void fillGridView() {
		ArrayList<String> imageFiles = ImageUtil.getImagesInFolder(currentFolder);

		enablePositiveButton(imageFiles.size() > 0);

		gridView.setAdapter(new DisplayImagesAdapter(imageFiles));
		gridView.setVisibility(imageFiles.size() > 0 ? View.VISIBLE : View.GONE);
	}

	/**
	 * Get the list of subdirectories of the current directory. Returns ".." as first value if appropriate.
	 *
	 * @param dir
	 *            The current directory.
	 * @return The list of subdirectories.
	 */
	private List<String> getDirectories(final String dir) {
		List<String> dirs = new ArrayList<String>();

		if (dir != null && dir.startsWith(File.separator) && !dir.equals(File.separator)) {
			dirs.add("..");
		}

		try {
			File dirFile = new File(dir);
			if (!dirFile.exists() || !dirFile.isDirectory()) {
				return dirs;
			}
			File[] files = dirFile.listFiles();
			if (files == null) {
				return dirs;
			}

			for (File file : files) {
				if (file.isDirectory()) {
					dirs.add(file.getName());
				}
			}
		}
		catch (Exception e) {
			Log.e(Application.TAG, "Could not get directories", e);
		}

		Collections.sort(dirs, new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				return o1.compareTo(o2);
			}
		});

		return dirs;
	}

	/**
	 * Update the current directory.
	 */
	private void updateDirectory() {
		try {
			currentFolder = new File(currentFolder).getCanonicalPath();
		}
		catch (IOException e) {
			// i
		}
		if (currentFolder == null || "".equals(currentFolder)) {
			currentFolder = File.separator;
		}

		subdirs.clear();
		subdirs.addAll(getDirectories(currentFolder));
		currentFolderView.setText(currentFolder);

		listAdapter.notifyDataSetChanged();
		listView.smoothScrollToPositionFromTop(0, 0, 100); // MAGIC_NUMBER

		fillGridView();
	}

	/**
	 * Create the list adapter for the list of folders.
	 *
	 * @param items
	 *            The list of folders.
	 * @return The list adapter.
	 */
	private ArrayAdapter<String> createListAdapter(final List<String> items) {
		return new ArrayAdapter<String>(getActivity(), R.layout.adapter_list_names, android.R.id.text1, items) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				View v = super.getView(position, convertView, parent);

				if (v instanceof TextView) {
					// Enable list item (directory) text wrapping
					TextView tv = (TextView) v;
					tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
					tv.setEllipsize(null);
				}
				return v;
			}
		};
	}

	/**
	 * Get the SD card directory.
	 *
	 * @return The SD card directory.
	 */
	private String getSdCardDirectory() {
		String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

		try {
			sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
		}
		catch (IOException ioe) {
			Log.e(Application.TAG, "Could not get SD directory", ioe);
		}
		return sdCardDirectory;
	}

	@Override
	public final void onSaveInstanceState(final Bundle outState) {
		if (listener != null || longClickListener != null) {
			// Typically cannot serialize the listener due to its reference to the activity.
			listener = null;
			longClickListener = null;
			outState.putBoolean("preventRecreation", true);
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * Callback interface for selected directory.
	 */
	public interface ChosenDirectoryListener extends Serializable {
		/**
		 * Called when a folder is selected.
		 *
		 * @param chosenDir
		 *            The selected folder.
		 */
		void onChosenDir(final String chosenDir);

		/**
		 * Called when the dialog is cancelled.
		 */
		void onCancelled();
	}

	/**
	 * Callback interface for long click on folders.
	 */
	public interface OnFolderLongClickListener extends Serializable {
		/**
		 * Method called after long click on a folder.
		 *
		 * @param folderName
		 *            The folder name.
		 * @return true if the long click was consumed.
		 */
		boolean onFolderLongClick(final String folderName);
	}

	/**
	 * Adapter for the GridView displaying the image files.
	 */
	private class DisplayImagesAdapter extends ArrayAdapter<String> {
		/**
		 * The names of the image files displayed.
		 */
		private ArrayList<String> fileNames;

		/**
		 * Constructor for the adapter.
		 *
		 * @param fileNames
		 *            The names of the image files to be displayed.
		 */
		public DisplayImagesAdapter(final ArrayList<String> fileNames) {
			super(getActivity(), R.layout.text_view_initializing, fileNames);
			this.fileNames = fileNames;
		}

		/**
		 * Default adapter to be used by the framework.
		 *
		 * @param context
		 *            The Context the view is running in.
		 */
		public DisplayImagesAdapter(final Context context) {
			super(context, R.layout.text_view_initializing);
		}

		@Override
		public final View getView(final int position, final View convertView, final ViewGroup parent) {
			final ImageView imageView;
			if (convertView != null && convertView instanceof ImageView) {
				imageView = (ImageView) convertView;
			}
			else {
				imageView = new ImageView(getActivity());
			}

			imageView.setAdjustViewBounds(true);

			new Thread() {
				@Override
				public void run() {
					final Bitmap bitmap =
							ImageUtil.getImageBitmap(fileNames.get(position), MediaStoreUtil.MINI_THUMB_SIZE);

					try {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								imageView.setImageBitmap(bitmap);
							}
						});
					}
					catch (Exception e) {
						// prevent NullPointerException
					}
				}
			}.start();

			return imageView;
		}
	}

}
