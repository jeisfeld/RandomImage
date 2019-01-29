package de.jeisfeld.randomimage;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.DialogUtil.ConfirmDialogFragment.ConfirmDialogListener;
import de.jeisfeld.randomimage.util.FileUtil;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.TrackingUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * Add images to the list via browsing folders. Alternative use: select one image via browsing folders.
 */
public class SelectDirectoryActivity extends BaseActivity {
	/**
	 * The request code used to finish the triggering activity.
	 */
	public static final int REQUEST_CODE = 6;
	/**
	 * The resource key for the name of the image list to be displayed.
	 */
	private static final String STRING_EXTRA_LISTNAME = "de.jeisfeld.randomimage.LISTNAME";
	/**
	 * The resource key for the flag if just an image should be selected.
	 */
	private static final String STRING_EXTRA_SELECT_IMAGE = "de.jeisfeld.randomimage.SELECT_IMAGE";
	/**
	 * The resource key for the flag indicating that the list was updated.
	 */
	private static final String STRING_RESULT_UPDATED = "de.jeisfeld.randomimage.UPDATED";
	/**
	 * The resource key for the selected image.
	 */
	private static final String STRING_RESULT_SELECTED_IMAGE = "de.jeisfeld.randomimage.SELECTED_IMAGE";
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
	 * The backstack of last browsed folders.
	 */
	private Stack<String> mBackStack = new Stack<>();

	/**
	 * Flag storing information if the image list was updated.
	 */
	private boolean mUpdatedList = false;

	/**
	 * The name of the image list to which files should be added.
	 */
	private String mListName;

	/**
	 * The flag indicating if an image should be selected instead of updating an image list.
	 */
	private boolean mSelectImage = false;

	/**
	 * Flag indicating if the current folder is an image folder.
	 */
	private boolean mIsImageFolder;

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity The activity starting this activity.
	 * @param listName the triggering image list to which files should be added.
	 */
	public static void startActivity(final Activity activity, final String listName) {
		Intent intent = new Intent(activity, SelectDirectoryActivity.class);
		if (listName != null) {
			intent.putExtra(STRING_EXTRA_LISTNAME, listName);
		}
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	/**
	 * Static helper method to start the activity.
	 *
	 * @param activity The activity starting this activity.
	 */
	public static void startActivity(final Activity activity) {
		Intent intent = new Intent(activity, SelectDirectoryActivity.class);
		intent.putExtra(STRING_EXTRA_SELECT_IMAGE, true);
		activity.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mListName = getIntent().getStringExtra(STRING_EXTRA_LISTNAME);
		mSelectImage = getIntent().getBooleanExtra(STRING_EXTRA_SELECT_IMAGE, false);
		setContentView(R.layout.activity_select_directory);

		if (savedInstanceState != null) {
			mUpdatedList = savedInstanceState.getBoolean("updatedList", false);
			mCurrentFolder = savedInstanceState.getString("currentFolder");
		}
		else {
			mCurrentFolder = PreferenceUtil.getSharedPreferenceString(R.string.key_directory_chooser_last_folder);
		}

		if (mSelectImage) {
			setTitle(R.string.title_activity_select_widget_icon);
			((TextView) findViewById(R.id.textViewMessage)).setText(R.string.text_info_select_widget_icon);
		}

		File dirFile = mCurrentFolder == null ? null : new File(mCurrentFolder);
		if (dirFile == null || !dirFile.exists() || !dirFile.isDirectory()) {
			mCurrentFolder = FileUtil.getDefaultCameraFolder();
			dirFile = new File(mCurrentFolder);

			if (!dirFile.exists() || !dirFile.isDirectory()) {
				mCurrentFolder = FileUtil.SD_CARD_PATH;
			}
		}

		try {
			mCurrentFolder = new File(mCurrentFolder).getCanonicalPath();
		}
		catch (IOException ioe) {
			// ignore
		}

		mSubdirs = getDirectories(mCurrentFolder);

		mCurrentFolderView = findViewById(R.id.textCurrentFolder);
		mCurrentFolderView.setText(mCurrentFolder);
		mListView = findViewById(R.id.listViewSubfolders);
		mListAdapter = createListAdapter(mSubdirs);
		mListView.setAdapter(mListAdapter);
		mGridView = findViewById(R.id.gridViewImages);

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

				final ImageList imageList = ImageRegistry.getImageListByName(mListName, true);

				String folderShortName = ImageUtil.getImageFolderShortName(selectedFolder);

				if (ImageUtil.isImageFolder(selectedFolder) && !imageList.contains(selectedFolder)) {
					DialogUtil.displayConfirmationMessage(SelectDirectoryActivity.this,
							new ConfirmDialogListener() {
								@Override
								public void onDialogPositiveClick(final DialogFragment dialog) {
									boolean success = imageList.addFolder(selectedFolder);

									if (success) {
										String addedFoldersString =
												DialogUtil.createFileFolderMessageString(null, Collections.singletonList(selectedFolder), null);
										DialogUtil.displayToast(SelectDirectoryActivity.this, R.string.toast_added_single, addedFoldersString);
										NotificationUtil.notifyUpdatedList(SelectDirectoryActivity.this, mListName, false, null,
												Collections.singletonList(selectedFolder), null);
										imageList.update(true);
										mUpdatedList = true;
									}
								}

								@Override
								public void onDialogNegativeClick(final DialogFragment dialog) {
									// do nothing
								}
							}, null, R.string.button_add_folder, R.string.dialog_confirmation_add_folder,
							folderShortName);

				}
				return true;
			}
		});

		updateListLayout();
		fillGridView();
	}

	@Override
	protected final void onResume() {
		super.onResume();
		TrackingUtil.sendScreen(this);
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

		Collections.sort(dirs, Collator.getInstance());

		return dirs;
	}

	/**
	 * Create the list adapter for the list of folders.
	 *
	 * @param items The list of folders.
	 * @return The list adapter.
	 */
	private ArrayAdapter<String> createListAdapter(final List<String> items) {
		return new ArrayAdapter<String>(this, R.layout.adapter_directory_names, android.R.id.text1, items) {
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
		updateListLayout();

		mCurrentFolderView.setText(mCurrentFolder);

		mListAdapter.notifyDataSetChanged();
		mListView.smoothScrollToPositionFromTop(0, 0, 100); // MAGIC_NUMBER

		fillGridView();
	}

	/**
	 * Update the layout of the list, either to have its own natural size or to fill same space as gridview, based on the number of entries.
	 */
	private void updateListLayout() {
		android.widget.LinearLayout.LayoutParams layoutParams = (android.widget.LinearLayout.LayoutParams) mListView.getLayoutParams();
		if (mSubdirs.size() > 3) { // MAGIC_NUMBER
			layoutParams.height = 0;
			layoutParams.weight = 1;
		}
		else {
			layoutParams.height = LayoutParams.WRAP_CONTENT;
			layoutParams.weight = 0;
		}
		mListView.requestLayout();
	}


	/**
	 * Fill the GridView to display the images.
	 */
	private void fillGridView() {
		ArrayList<String> imageFiles = ImageUtil.getImagesInFolder(mCurrentFolder);

		mIsImageFolder = imageFiles.size() > 0;
		invalidateOptionsMenu();

		mGridView.setAdapter(new DisplayImagesAdapter(imageFiles));
		mGridView.setVisibility(imageFiles.size() > 0 ? View.VISIBLE : View.GONE);
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
	public final boolean onCreateOptionsMenu(final Menu menu) {
		if (mSelectImage) {
			getMenuInflater().inflate(R.menu.select_icon, menu);
		}
		else {
			getMenuInflater().inflate(R.menu.select_directory, menu);

			if (!mIsImageFolder) {
				MenuItem menuItemOkay = menu.findItem(R.id.action_select_folder);
				MenuItem menuItemAddThisFolder = menu.findItem(R.id.action_add_only_this_folder);

				menuItemOkay.setEnabled(false);
				menuItemAddThisFolder.setEnabled(false);
				menuItemOkay.setIcon(ImageUtil.getTransparentIcon(R.drawable.ic_action_okay));
			}
			if (!FileUtil.hasSubfolders(mCurrentFolder)) {
				MenuItem menuItemAddThis = menu.findItem(R.id.action_add_only_this_folder);
				menuItemAddThis.setEnabled(false);
				if (!mIsImageFolder) {
					MenuItem menuItemAddFolder = menu.findItem(R.id.action_add_image_folder);
					menuItemAddFolder.setIcon(ImageUtil.getTransparentIcon(R.drawable.ic_action_add_folder));
					menuItemAddFolder.setEnabled(false);
				}
			}
		}

		return true;
	}

	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		int menuId = item.getItemId();

		if (menuId == R.id.action_cancel) {
			if (mSelectImage) {
				returnResult(null);
			}
			else {
				returnResult(false);
			}
			return true;
		}
		else if (menuId == R.id.action_add_only_this_folder || menuId == R.id.action_add_all_subfolders || menuId == R.id.action_add_image_folder) {
			if (menuId == R.id.action_add_image_folder && FileUtil.hasSubfolders(mCurrentFolder)) {
				return false;
			}

			PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
			final ImageList imageList = ImageRegistry.getImageListByName(mListName, true);

			String folderToBeAdded = mCurrentFolder;
			if (menuId == R.id.action_add_all_subfolders) {
				folderToBeAdded = mCurrentFolder + ImageUtil.RECURSIVE_SUFFIX;
			}
			boolean success = imageList.addFolder(folderToBeAdded);
			if (success) {
				String addedFoldersString =
						DialogUtil.createFileFolderMessageString(null, Collections.singletonList(mCurrentFolder), null);
				DialogUtil.displayToast(SelectDirectoryActivity.this, R.string.toast_added_single, addedFoldersString);
				NotificationUtil.notifyUpdatedList(SelectDirectoryActivity.this, mListName, false, null,
						Collections.singletonList(mCurrentFolder), null);
				imageList.update(true);
				mUpdatedList = true;
			}
			returnResult(true);
			return true;
		}
		else if (menuId == R.id.action_select_folder) {
			PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
			DisplayImagesFromFolderActivity.startActivity(SelectDirectoryActivity.this, mCurrentFolder, mListName, true);
			return true;
		}
		else {
			return false;
		}
	}


	@Override
	public final void onSaveInstanceState(final Bundle outState) {
		outState.putBoolean("updatedList", mUpdatedList);
		outState.putString("currentFolder", mCurrentFolder);
		super.onSaveInstanceState(outState);
	}

	/**
	 * Static helper method to get information if the list was updated.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the flag indicating that the list was updated
	 */
	public static boolean getUpdatedFlag(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res != null && res.getBoolean(STRING_RESULT_UPDATED, false);
		}
		else {
			return false;
		}
	}

	/**
	 * Static helper method to retrieve the selected image.
	 *
	 * @param resultCode The result code indicating if the response was successful.
	 * @param data       The activity response data.
	 * @return the selected image.
	 */
	public static String getSelectedImage(final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			Bundle res = data.getExtras();
			return res == null ? null : res.getString(STRING_RESULT_SELECTED_IMAGE);
		}
		else {
			return null;
		}
	}

	/**
	 * Helper method: Return the flag indicating that the list was updated.
	 *
	 * @param isUpdated The flag indicating that the list was updated.
	 */
	private void returnResult(final boolean isUpdated) {
		Bundle resultData = new Bundle();
		resultData.putBoolean(STRING_RESULT_UPDATED, mUpdatedList || isUpdated);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Helper method: Return the selected image.
	 *
	 * @param selectedImage The selected image.
	 */
	private void returnResult(final String selectedImage) {
		Bundle resultData = new Bundle();
		resultData.putString(STRING_RESULT_SELECTED_IMAGE, selectedImage);
		Intent intent = new Intent();
		intent.putExtras(resultData);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected final void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case DisplayImagesFromFolderActivity.REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				boolean isUpdated = DisplayImagesFromFolderActivity.getResultFilesAdded(resultCode, data);
				returnResult(isUpdated);
			}
			break;
		default:
			break;
		}
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

			if (mSelectImage) {
				imageView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						PreferenceUtil.setSharedPreferenceString(R.string.key_directory_chooser_last_folder, mCurrentFolder);
						returnResult(mFileNames.get(position));
						finish();
					}
				});
			}

			new Thread() {
				@Override
				public void run() {
					final Bitmap bitmap = ImageUtil.getImageBitmap(mFileNames.get(position), MediaStoreUtil.MINI_THUMB_SIZE);

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
