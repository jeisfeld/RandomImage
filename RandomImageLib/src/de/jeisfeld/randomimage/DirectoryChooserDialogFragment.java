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
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class to present a dialog for selection of a directory.
 *
 * <p>Inspired by
 * http://www.codeproject.com/Articles/547636/Android-Ready-to-use-simple-
 * directory-chooser-dial
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
	private TextView mCurrentFolderView;

	/**
	 * The list view showing the sub-elements.
	 */
	private ListView mListView;

	/**
	 * The list view showing the sub-elements.
	 */
	private GridView mGridView;

	/**
	 * The current folder.
	 */
	private String mCurrentFolder = "";

	/**
	 * The sub-elements of the current folder.
	 */
	private List<String> mSubdirs = null;

	/**
	 * The list adapter handling the sub-elements of the current folder.
	 */
	private ArrayAdapter<String> mListAdapter = null;

	/**
	 * The backstack of last browsed folders.
	 */
	private Stack<String> mBackStack = new Stack<String>();

	/**
	 * A reference to the shown dialog.
	 */
	private AlertDialog mDirsDialog;

	/**
	 * The listener notified when finishing the dialog.
	 */
	private ChosenDirectoryListener mListener = null;

	/**
	 * An optional listener reacting on item long clicks.
	 */
	private OnFolderLongClickListener mLongClickListener = null;

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
		// Therefore, dialogs with listeners must be re-created by the activity
		// on orientation change.
		boolean preventRecreation = false;
		if (savedInstanceState != null) {
			preventRecreation = savedInstanceState.getBoolean("preventRecreation");
		}
		if (preventRecreation) {
			dismiss();
		}

		// retrieve arguments
		String dir = getArguments().getString(STRING_EXTRA_FOLDER);
		mListener = (ChosenDirectoryListener) getArguments().getSerializable(STRING_EXTRA_LISTENER);
		mLongClickListener = (OnFolderLongClickListener) getArguments()
				.getSerializable(STRING_EXTRA_LONG_CLICK_LISTENER);

		if (dir == null) {
			dir = PreferenceUtil.getSharedPreferenceString(R.string.key_directory_chooser_last_folder);
		}

		File dirFile = dir == null ? null : new File(dir);
		if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
			dir = FileUtil.getDefaultCameraFolder();
			dirFile = dir == null ? null : new File(dir);

			if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
				dir = FileUtil.getSdCardPath();
			}
		}

		try {
			dir = new File(dir).getCanonicalPath();
		}
		catch (IOException ioe) {
			return null;
		}

		mCurrentFolder = dir;
		mSubdirs = getDirectories(dir);

		Context context = getActivity();

		// Create dialog

		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

		dialogBuilder.setTitle(R.string.title_dialog_select_folder);

		View layout = LayoutInflater.from(context).inflate(R.layout.dialog_directory_chooser,
				new LinearLayout(context));
		dialogBuilder.setView(layout);

		((TextView) layout.findViewById(R.id.textViewMessage)).setText(R.string.dialog_select_image_folder_for_add);
		mCurrentFolderView = (TextView) layout.findViewById(R.id.textCurrentFolder);
		mCurrentFolderView.setText(dir);

		mListView = (ListView) layout.findViewById(R.id.listViewSubfolders);
		mListAdapter = createListAdapter(mSubdirs);
		mListView.setAdapter(mListAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				mBackStack.push(mCurrentFolder);
				mCurrentFolder += File.separator + mListAdapter.getItem(position);
				updateDirectory();
			}
		});

		if (mLongClickListener != null) {
			mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position,
						final long id) {
					final String selectedFolder = mCurrentFolder + File.separator + mListAdapter.getItem(position);
					return mLongClickListener.onFolderLongClick(selectedFolder);
				}
			});
		}

		mGridView = (GridView) layout.findViewById(R.id.gridViewImages);

		dialogBuilder.setCancelable(false);

		dialogBuilder.setNeutralButton(R.string.button_add_images, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (mListener != null) {
					// Call registered listener supplied with the chosen directory
					PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
					mListener.onAddImages(mCurrentFolder);
				}
			}
		}).setPositiveButton(R.string.button_add_folder, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (mListener != null) {
					// Call registered listener supplied with the chosen directory
					PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
					mListener.onAddFolder(mCurrentFolder);
				}
			}
		}).setNegativeButton(R.string.button_cancel, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				if (mListener != null) {
					mListener.onCancelled();
				}
			}
		});

		mDirsDialog = dialogBuilder.create();

		mDirsDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
					// Back button pressed - go to the last directory if
					// existing - otherwise cancel the dialog.
					if (mBackStack.size() == 0) {
						mDirsDialog.dismiss();
						if (mListener != null) {
							mListener.onCancelled();
						}
					}
					else {
						mCurrentFolder = mBackStack.pop();
						updateDirectory();
					}

					return true;
				}
				else {
					return false;
				}
			}
		});

		return mDirsDialog;
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
		mDirsDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
		mDirsDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(enabled);
	}

	/**
	 * Fill the GridView to display the images.
	 */
	private void fillGridView() {
		ArrayList<String> imageFiles = ImageUtil.getImagesInFolder(mCurrentFolder);

		enablePositiveButton(imageFiles.size() > 0);

		mGridView.setAdapter(new DisplayImagesAdapter(imageFiles));
		mGridView.setVisibility(imageFiles.size() > 0 ? View.VISIBLE : View.GONE);
	}

	/**
	 * Get the list of subdirectories of the current directory. Returns ".." as
	 * first value if appropriate.
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
			mCurrentFolder = new File(mCurrentFolder).getCanonicalPath();
		}
		catch (IOException e) {
			// i
		}
		if (mCurrentFolder == null || "".equals(mCurrentFolder)) {
			mCurrentFolder = File.separator;
		}

		mSubdirs.clear();
		mSubdirs.addAll(getDirectories(mCurrentFolder));
		mCurrentFolderView.setText(mCurrentFolder);

		mListAdapter.notifyDataSetChanged();
		mListView.smoothScrollToPositionFromTop(0, 0, 100); // MAGIC_NUMBER

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

	@Override
	public final void onSaveInstanceState(final Bundle outState) {
		if (mListener != null || mLongClickListener != null) {
			// Typically cannot serialize the listener due to its reference to
			// the activity.
			mListener = null;
			mLongClickListener = null;
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
		void onAddImages(final String chosenDir);

		/**
		 * Called when a folder is selected.
		 *
		 * @param chosenDir
		 *            The selected folder.
		 */
		void onAddFolder(final String chosenDir);

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
		private ArrayList<String> mFileNames;

		/**
		 * Constructor for the adapter.
		 *
		 * @param fileNames
		 *            The names of the image files to be displayed.
		 */
		public DisplayImagesAdapter(final ArrayList<String> fileNames) {
			super(getActivity(), R.layout.text_view_initializing, fileNames);
			this.mFileNames = fileNames;
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
					final Bitmap bitmap = ImageUtil.getImageBitmap(mFileNames.get(position),
							MediaStoreUtil.MINI_THUMB_SIZE);

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
