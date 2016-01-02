package de.jeisfeld.randomimage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.NotificationUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Add images to the list via browsing folders.
 */
public class SelectDirectoryActivity extends Activity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 6;
	/**
	 * The resource key for the returned folder.
	 */
	private static final String STRING_RESULT_FOLDER = "de.jeisfeld.randomimage.FOLDER";
	/**
	 * The resource key for the flag indicating if the image list was updated.
	 */
	private static final String STRING_RESULT_UPDATED_LIST = "de.jeisfeld.randomimage.UPDATED_LIST";
	/**
	 * The size of the displayed thumbnails.
	 */
	private static final int GRIDVIEW_THUMB_SIZE = Application.getAppContext().getResources().getDimensionPixelSize(R.dimen.mini_grid_pictures_size);

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
	 * Button to cancel the activity.
	 */
	private Button mBtnCancel;

	/**
	 * Button to go to the selection of individual images.
	 */
	private Button mBtnSelectImages;

	/**
	 * Button to select the whole folder.
	 */
	private Button mBtnSelectFolder;

	/**
	 * The backstack of last browsed folders.
	 */
	private Stack<String> mBackStack = new Stack<>();

	/**
	 * Flag storing information if the image list was updated.
	 */
	private boolean mUpdatedList = false;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity The activity starting this activity.
	 */
	public static void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SelectDirectoryActivity.class);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_directory);

		if (savedInstanceState != null) {
			mUpdatedList = savedInstanceState.getBoolean("updatedList", false);
			mCurrentFolder = savedInstanceState.getString("currentFolder");
		}
		else {
			mCurrentFolder = PreferenceUtil.getSharedPreferenceString(R.string.key_directory_chooser_last_folder);
		}

		File dirFile = mCurrentFolder == null ? null : new File(mCurrentFolder);
		if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
			mCurrentFolder = FileUtil.getDefaultCameraFolder();
			dirFile = new File(mCurrentFolder);

			if (!dirFile.exists() || !dirFile.isDirectory()) {
				mCurrentFolder = FileUtil.getSdCardPath();
			}
		}

		try {
			mCurrentFolder = new File(mCurrentFolder).getCanonicalPath();
		}
		catch (IOException ioe) {
			// ignore
		}

		mSubdirs = getDirectories(mCurrentFolder);

		((TextView) findViewById(R.id.textViewMessage)).setText(R.string.dialog_select_image_folder_for_add);
		mCurrentFolderView = (TextView) findViewById(R.id.textCurrentFolder);
		mCurrentFolderView.setText(mCurrentFolder);
		mListView = (ListView) findViewById(R.id.listViewSubfolders);
		mListAdapter = createListAdapter(mSubdirs);
		mListView.setAdapter(mListAdapter);
		mGridView = (GridView) findViewById(R.id.gridViewImages);

		mBtnCancel = (Button) findViewById(R.id.buttonCancel);
		mBtnSelectImages = (Button) findViewById(R.id.buttonSelectImages);
		mBtnSelectFolder = (Button) findViewById(R.id.buttonSelectFolder);

		mBtnCancel.setText(R.string.button_cancel);
		mBtnSelectImages.setText(R.string.button_add_images);
		mBtnSelectFolder.setText(R.string.button_add_folder);

		mBtnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				sendResult(null);
			}
		});
		mBtnSelectImages.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
				sendResult(mCurrentFolder);
			}
		});
		mBtnSelectFolder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
				final ImageList imageList = ImageRegistry.getCurrentImageList(true);
				boolean success = imageList.addFolder(mCurrentFolder);
				if (success) {
					imageList.update(true);
					mUpdatedList = true;
				}
				sendResult(null);
			}
		});

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				mBackStack.push(mCurrentFolder);
				mCurrentFolder += File.separator + mListAdapter.getItem(position);
				updateDirectory();
			}
		});

		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position,
										   final long id) {
				final String selectedFolder = mCurrentFolder + File.separator + mListAdapter.getItem(position);

				final ImageList imageList = ImageRegistry.getCurrentImageList(true);

				String folderShortName = new File(selectedFolder).getName();

				if (ImageUtil.isImageFolder(selectedFolder) && !imageList.contains(selectedFolder)) {
					DialogUtil.displayConfirmationMessage(SelectDirectoryActivity.this,
							new ConfirmDialogListener() {
								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									boolean success = imageList.addFolder(selectedFolder);

									if (success) {
										ArrayList<String> addedFolderList = new ArrayList<>();
										addedFolderList.add(selectedFolder);
										String addedFoldersString = DialogUtil.createFileFolderMessageString(null, addedFolderList, null);
										DialogUtil.displayToast(SelectDirectoryActivity.this, R.string.toast_added_single, addedFoldersString);
										NotificationUtil.displayNotification(SelectDirectoryActivity.this, imageList.getListName(),
												NotificationUtil.ID_UPDATED_LIST, R.string.title_notification_updated_list,
												R.string.toast_added_single, addedFoldersString);
										imageList.update(true);
										mUpdatedList = true;
									}
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									// do nothing
								}
							}, R.string.button_add_folder, R.string.dialog_confirmation_add_folder,
							folderShortName);

				}
				return true;
			}
		});

		fillGridView();
	}

	/**
	 * Get the list of subdirectories of the current directory. Returns ".." as
	 * first value if appropriate.
	 *
	 * @param dir The current directory.
	 * @return The list of subdirectories.
	 */
	private static List<String> getDirectories(final String dir) {
		List<String> dirs = new ArrayList<>();

		if (dir != null && dir.startsWith(File.separator) && !dir.equals(File.separator)) {
			dirs.add("..");
		}

		try {
			if (dir == null) {
				return dirs;
			}
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
	 * Create the list adapter for the list of folders.
	 *
	 * @param items The list of folders.
	 * @return The list adapter.
	 */
	private ArrayAdapter<String> createListAdapter(final List<String> items) {
		return new ArrayAdapter<String>(this, R.layout.adapter_list_names, android.R.id.text1, items) {
			@Override
			public View getView(final int position, final View convertView, final ViewGroup parent) {
				View v = super.getView(position, convertView, parent);

				if (v instanceof TextView) {
					// Enable list item (directory) text wrapping
					TextView tv = (TextView) v;
					tv.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
					tv.setEllipsize(null);
				}
				return v;
			}
		};
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
	 * Fill the GridView to display the images.
	 */
	private void fillGridView() {
		ArrayList<String> imageFiles = ImageUtil.getImagesInFolder(mCurrentFolder);

		enablePositiveButton(imageFiles.size() > 0);

		mGridView.setAdapter(new DisplayImagesAdapter(imageFiles));
		mGridView.setVisibility(imageFiles.size() > 0 ? View.VISIBLE : View.GONE);
	}

	/**
	 * Update the enablement status of the positive button.
	 *
	 * @param enabled The new enablement status.
	 */
	private void enablePositiveButton(final boolean enabled) {
		mBtnSelectFolder.setEnabled(enabled);
		mBtnSelectImages.setEnabled(enabled);
	}

	@Override
	public final boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			// Back button pressed - go to the last directory if
			// existing - otherwise cancel the dialog.
			if (mBackStack.size() == 0) {
				super.onKeyDown(keyCode, event);
			}
			else {
				mCurrentFolder = mBackStack.pop();
				updateDirectory();
			}
			return true;
		}
		else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public final void onSaveInstanceState(final Bundle outState) {
		outState.putBoolean("updatedList", mUpdatedList);
		outState.putString("currentFolder", mCurrentFolder);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Static helper method to extract the result folder.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the result folder.
	 */
	public static final String getResultFolder(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			return data.getStringExtra(STRING_RESULT_FOLDER);
		}
		else {
			return null;
		}
	}

	/**
	 * Static helper method to extract the flag indicating if the image list was updated.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag indicating if the image list was updated.
	 */
	public static final boolean getResultUpdatedList(final int resultCode, final Intent data) {
		return resultCode == RESULT_OK && data.getBooleanExtra(STRING_RESULT_UPDATED_LIST, false);
	}

	/**
	 * Send the result to the calling activity.
	 *
	 * @param chosenDir The selected directory.
	 */
	private void sendResult(final String chosenDir) {
		Bundle resultData = new Bundle();
		if (chosenDir != null) {
			resultData.putString(STRING_RESULT_FOLDER, chosenDir);
		}
		resultData.putBoolean(STRING_RESULT_UPDATED_LIST, mUpdatedList);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(chosenDir != null || mUpdatedList ? RESULT_OK : RESULT_CANCELED, intent);
		finish();
	}


	/**
	 * Adapter for the GridView displaying the image files.
	 */
	private final class DisplayImagesAdapter extends ArrayAdapter<String> {
		/**
		 * The names of the image files displayed.
		 */
		private ArrayList<String> mFileNames;

		/**
		 * Constructor for the adapter.
		 *
		 * @param fileNames The names of the image files to be displayed.
		 */
		private DisplayImagesAdapter(final ArrayList<String> fileNames) {
			super(SelectDirectoryActivity.this, R.layout.text_view_initializing, fileNames);
			this.mFileNames = fileNames;
		}

		/**
		 * Default constructor to be used by the framework.
		 *
		 * @param context The Context the view is running in.
		 */
		private DisplayImagesAdapter(final Context context) {
			super(context, R.layout.text_view_initializing);
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			// Due to background loading, ignore convertView
			final ImageView imageView = new ImageView(SelectDirectoryActivity.this);

			imageView.setAdjustViewBounds(true);
			imageView.setLayoutParams(new LayoutParams(GRIDVIEW_THUMB_SIZE, GRIDVIEW_THUMB_SIZE));

			new Thread() {
				@Override
				public void run() {
					final Bitmap bitmap = ImageUtil.getImageBitmap(mFileNames.get(position),
							MediaStoreUtil.MICRO_THUMB_SIZE);

					try {
						SelectDirectoryActivity.this.runOnUiThread(new Runnable() {
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
