package de.jeisfeld.randomimage;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.view.ThumbImageView.LoadableFileName;
import de.jeisfeld.randomimage.view.ThumbImageView.MarkingType;
import de.jeisfeld.randomimage.view.ThumbImageView.ThumbStyle;
import de.jeisfeld.randomimagelib.R;

/**
 * Array adapter class to display an eye photo pair in a list.
 */
public class DisplayImageListAdapter extends BaseAdapter {
	/**
	 * Number of views to be preloaded.
	 */
	private static final int CACHE_SIZE;

	/**
	 * The cache where views of the displays are stored for smoother scrolling.
	 */
	private ViewCache mViewCache;

	/**
	 * The activity holding this adapter.
	 */
	private final DisplayImageListActivity mActivity;

	/**
	 * The names of the lists.
	 */
	private List<String> mListNames;

	/**
	 * The names of the folders.
	 */
	private List<String> mFolderNames;

	/**
	 * The names of the files.
	 */
	private List<String> mFileNames;

	/**
	 * Flag indicating if fixed thumbnail images should be used (for performance reasons).
	 */
	private boolean mFixedThumbs;

	/**
	 * The set of list names selected for deletion.
	 */
	private final Set<String> mSelectedListNames = new HashSet<>();

	/**
	 * The set of folder names selected for deletion.
	 */
	private final Set<String> mSelectedFolderNames = new HashSet<>();

	/**
	 * The set of filenames selected for deletion.
	 */
	private final Set<String> mSelectedFileNames = new HashSet<>();

	/**
	 * Flag indicating how selection is handled.
	 */
	private volatile SelectionMode mSelectionMode = SelectionMode.ONE;

	/**
	 * The way in which the thumbs are marked.
	 */
	private MarkingType mMarkingType = MarkingType.NONE;

	/**
	 * The highest position displayed by now.
	 */
	private int mMaxReachedPosition = 0;
	/**
	 * The folders which wait to be added.
	 */
	private final List<String> mFoldersNotYetAdded = new ArrayList<>();

	static {
		// Set cache size in dependence of device memory.
		int memoryClass = SystemUtil.getLargeMemoryClass();

		if (memoryClass >= 2048) { // MAGIC_NUMBER
			CACHE_SIZE = 400; // MAGIC_NUMBER
		}
		else if (memoryClass >= 1024) { // MAGIC_NUMBER
			CACHE_SIZE = 200; // MAGIC_NUMBER
		}
		else if (memoryClass >= 512) { // MAGIC_NUMBER
			CACHE_SIZE = 100; // MAGIC_NUMBER
		}
		else if (memoryClass >= 256) { // MAGIC_NUMBER
			CACHE_SIZE = 40; // MAGIC_NUMBER
		}
		else {
			CACHE_SIZE = 10; // MAGIC_NUMBER
		}
	}

	/**
	 * Set the selection mode.
	 *
	 * @param selectionMode The selection mode.
	 */
	public final void setSelectionMode(final SelectionMode selectionMode) {
		this.mSelectionMode = selectionMode;

		if (selectionMode == SelectionMode.ONE) {
			mSelectedFolderNames.clear();
			mSelectedFileNames.clear();
			mSelectedListNames.clear();
		}

		mMarkingType = getMarkingTypeFromSelectionMode(selectionMode);
		setMarkabilityStatus(mMarkingType);
	}

	/**
	 * Map the selectionMode of the adapter to the markingType of the thumb.
	 *
	 * @param selectionMode The selectionMode
	 * @return the markingType
	 */
	public static MarkingType getMarkingTypeFromSelectionMode(final SelectionMode selectionMode) {
		switch (selectionMode) {
		case ONE:
			return MarkingType.NONE;
		case MULTIPLE:
			return MarkingType.HOOK;
		default:
			return null;
		}
	}

	/**
	 * Constructor for the adapter.
	 *
	 * @param activity    The activity using the adapter.
	 * @param listNames   The names of lists to be displayed.
	 * @param folderNames The names of folders to be displayed.
	 * @param fileNames   The names of files to be displayed.
	 * @param fixedThumbs Flag indicating if fixed thumbnail images should be used (for performance reasons)
	 */
	public DisplayImageListAdapter(final DisplayImageListActivity activity,
								   final List<String> listNames, final List<String> folderNames,
								   final List<String> fileNames, final boolean fixedThumbs) {
		this.mActivity = activity;
		this.mFixedThumbs = fixedThumbs;

		if (listNames == null) {
			this.mListNames = new ArrayList<>();
		}
		else {
			this.mListNames = new ArrayList<>(listNames);
		}
		if (folderNames == null) {
			this.mFolderNames = new ArrayList<>();
		}
		else {
			this.mFolderNames = new ArrayList<>(folderNames);
		}
		if (fileNames == null) {
			this.mFileNames = new ArrayList<>();
		}
		else {
			this.mFileNames = new ArrayList<>(fileNames);
		}
		notifyDataSetChanged();

		this.mViewCache = new ViewCache(CACHE_SIZE);
	}

	/**
	 * Default adapter to be used by the framework.
	 *
	 * @param context The Context the view is running in.
	 */
	public DisplayImageListAdapter(final Context context) {
		this.mActivity = (DisplayImageListActivity) context;
	}

	/**
	 * Add a folder to the list. Only allowed for lists containing no files.
	 *
	 * @param folderName The folder added.
	 */
	public final void addFolder(final String folderName) {
		if (!mFileNames.isEmpty()) {
			// only allowed if there are no files in the list
			throw new UnsupportedOperationException("DisplayImageListAdapter: added folderName after having fileNames");
		}
		if (mFolderNames.contains(folderName)) {
			return;
		}

		if (mFolderNames.size() < mMaxReachedPosition + CACHE_SIZE) {
			mFolderNames.add(folderName);
			notifyDataSetChanged();
		}
		else {
			// Do not add folders if not yet required, as adding brings trouble to onClick behaviour.
			synchronized (mFoldersNotYetAdded) {
				mFoldersNotYetAdded.add(folderName);
			}
		}
	}

	/**
	 * Add the folders waiting to be added.
	 */
	private void addFoldersNotYetAdded() {
		synchronized (mFoldersNotYetAdded) {
			if (!mFoldersNotYetAdded.isEmpty()) {
				mFolderNames.addAll(mFoldersNotYetAdded);
				mFoldersNotYetAdded.clear();
				notifyDataSetChanged();
			}
		}
	}

	@Override
	public final int getCount() {
		return mListNames.size() + mFolderNames.size() + mFileNames.size();
	}

	@Override
	public final String getItem(final int position) {
		if (position < mListNames.size()) {
			return mListNames.get(position);
		}
		else if (position < mListNames.size() + mFolderNames.size()) {
			return mFolderNames.get(position - mListNames.size());
		}
		else {
			return mFileNames.get(position - mListNames.size() - mFolderNames.size());
		}
	}

	@Override
	public final long getItemId(final int position) {
		return position;
	}

	@Override
	public final int getItemViewType(final int position) {
		// Ensure that views are not reused by GridView framework
		return position == 0 ? 0 : 1;
	}

	@Override
	public final int getViewTypeCount() {
		return 2;
	}

	@Override
	public final View getView(final int position, final View convertView, final ViewGroup parent) {
		if (position > mMaxReachedPosition) {
			mMaxReachedPosition = position;
			addFoldersNotYetAdded();
		}
		return mViewCache.get(position, parent);
	}

	/**
	 * Create a new ThumbImageView for the file on a certain position.
	 *
	 * @param position   The position.
	 * @param parent     The parent view.
	 * @param sameThread if true, then image load will be done on the same thread. Otherwise a separate thread will be spawned.
	 * @return The ThumbImageView.
	 */
	private ThumbImageView createThumbImageView(final int position, final ViewGroup parent, final boolean sameThread) {
		final ItemType itemType;
		if (position < mListNames.size()) {
			itemType = ItemType.LIST;
		}
		else if (position < mListNames.size() + mFolderNames.size()) {
			itemType = ItemType.FOLDER;
		}
		else {
			itemType = ItemType.FILE;
		}

		final ThumbImageView thumbImageView =
				(ThumbImageView) LayoutInflater.from(mActivity).inflate(R.layout.adapter_display_images,
						parent, false);

		final String entryName;
		final LoadableFileName displayFileName;

		switch (itemType) {
		case LIST:
			entryName = mListNames.get(position);

			if (mFixedThumbs) {
				displayFileName = new LoadableFileName(PreferenceUtil.getIndexedSharedPreferenceString(
						R.string.key_indexed_current_list_thumb, entryName));
			}
			else {
				displayFileName = new LoadableFileName(() -> {
					ImageList imageList = ImageRegistry.getImageListByName(entryName, true);
					if (imageList == null) {
						return null;
					}
					imageList.waitUntilReady();
					String fileName = imageList.getRandomFileName();
					PreferenceUtil.setIndexedSharedPreferenceString(R.string.key_indexed_current_list_thumb, entryName, fileName);
					return fileName;
				});
			}

			thumbImageView.initWithStyle(mActivity, ThumbStyle.LIST);
			thumbImageView.setMarked(mSelectedListNames.contains(entryName));
			thumbImageView.setFolderName(entryName);
			break;
		case FOLDER:
			entryName = mFolderNames.get(position - mListNames.size());

			displayFileName = new LoadableFileName(() -> {
				ArrayList<String> imageFiles = new ArrayList<>(ImageUtil.getImagesInFolder(entryName));
				if (!imageFiles.isEmpty()) {
					if (mFixedThumbs) {
						return imageFiles.get(0);
					}
					else {
						return imageFiles.get(new Random().nextInt(imageFiles.size()));
					}
				}
				else {
					return null;
				}
			});

			thumbImageView.initWithStyle(mActivity, entryName.endsWith(ImageUtil.RECURSIVE_SUFFIX) ? ThumbStyle.FOLDER_RECURSIVE : ThumbStyle.FOLDER);
			thumbImageView.setMarked(mSelectedFolderNames.contains(entryName));
			thumbImageView.setFolderName(ImageUtil.getImageFolderShortName(entryName));
			break;
		case FILE:
		default:
			entryName = mFileNames.get(position - mListNames.size() - mFolderNames.size());
			displayFileName = new LoadableFileName(entryName);

			thumbImageView.initWithStyle(mActivity, ThumbStyle.IMAGE);
			thumbImageView.setMarked(mSelectedFileNames.contains(entryName));
			break;
		}

		thumbImageView.setMarkable(mMarkingType);

		final Set<String> selectionList;
		switch (itemType) {
		case LIST:
			selectionList = mSelectedListNames;
			break;
		case FOLDER:
			selectionList = mSelectedFolderNames;
			break;
		case FILE:
		default:
			selectionList = mSelectedFileNames;
			break;
		}

		thumbImageView.setOnClickListener(v -> {
			ThumbImageView view = (ThumbImageView) v;
			switch (mSelectionMode) {
			case ONE:
				mActivity.onItemClick(itemType, entryName);
				break;
			case MULTIPLE:
				if (view.isMarked()) {
					view.setMarked(false);
					selectionList.remove(entryName);
				}
				else {
					view.setMarked(true);
					selectionList.add(entryName);
				}
				break;
			default:
				break;
			}
		});

		thumbImageView.setOnLongClickListener(v -> {
			mActivity.onItemLongClick(itemType, entryName);
			return true;
		});

		// Setting of thumb may be in separate thread. Therefore, do this last.
		thumbImageView.setImage(mActivity, displayFileName, sameThread);

		return thumbImageView;
	}

	/**
	 * Get the list of selected files.
	 *
	 * @return The selected files.
	 */
	public final ArrayList<String> getSelectedFiles() {
		return new ArrayList<>(mSelectedFileNames);
	}

	/**
	 * Set the list of selected files.
	 *
	 * @param selectedFiles The names of the files.
	 */
	public final void setSelectedFiles(final List<String> selectedFiles) {
		mSelectedFileNames.clear();
		if (selectedFiles != null) {
			mSelectedFileNames.addAll(selectedFiles);
		}
	}

	/**
	 * Get the list of selected folders.
	 *
	 * @return The selected folders.
	 */
	public final ArrayList<String> getSelectedFolders() {
		return new ArrayList<>(mSelectedFolderNames);
	}

	/**
	 * Set the list of selected folders.
	 *
	 * @param selectedFolders The names of the folders.
	 */
	public final void setSelectedFolders(final List<String> selectedFolders) {
		mSelectedFolderNames.clear();
		if (selectedFolders != null) {
			mSelectedFolderNames.addAll(selectedFolders);
		}
	}

	/**
	 * Get the list of selected lists.
	 *
	 * @return The selected lists.
	 */
	public final ArrayList<String> getSelectedLists() {
		return new ArrayList<>(mSelectedListNames);
	}

	/**
	 * Set the list of selected lists.
	 *
	 * @param selectedLists The names of the lists.
	 */
	public final void setSelectedLists(final List<String> selectedLists) {
		mSelectedListNames.clear();
		if (selectedLists != null) {
			mSelectedListNames.addAll(selectedLists);
		}
	}

	/**
	 * Set the markability status for all images in the cache.
	 *
	 * @param newMarkingType the new marking type.
	 */
	private void setMarkabilityStatus(final MarkingType newMarkingType) {
		for (ThumbImageView view : mViewCache.getCachedImages()) {
			view.setMarkable(newMarkingType);
		}
	}

	/**
	 * If in selection mode, select all images. If all images are selected, then deselect all images.
	 *
	 * @return true if all have been selected, false if all have been deselected or if not in selection mode.
	 */
	public final boolean toggleSelectAll() {
		if (mSelectionMode != SelectionMode.ONE) {
			if (mSelectedFileNames.size() < mFileNames.size() || mSelectedFolderNames.size() < mFolderNames.size()
					|| mSelectedListNames.size() < mListNames.size()) {
				setSelectedFiles(mFileNames);
				setSelectedFolders(mFolderNames);
				setSelectedLists(mListNames);
				for (ThumbImageView view : mViewCache.getCachedImages()) {
					view.setMarked(true);
				}
				return true;
			}
			else {
				setSelectedFiles(null);
				setSelectedFolders(null);
				setSelectedLists(null);
				for (ThumbImageView view : mViewCache.getCachedImages()) {
					view.setMarked(false);
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * Cleanup the adapter cache and stop further preloading.
	 */
	public final void cleanupCache() {
		mViewCache.clear();
	}

	/**
	 * Mode defining how selection works.
	 */
	public enum SelectionMode {
		/**
		 * One file can be selected for display.
		 */
		ONE,
		/**
		 * Multiple files can be selected for adding.
		 */
		MULTIPLE,
	}

	/**
	 * Cache to store views from the GridView.
	 */
	private final class ViewCache {
		/**
		 * The map from positions to views.
		 */
		private final SparseArray<ThumbImageView> mCache;

		/**
		 * The parentView view holding the cached views.
		 */
		private ViewGroup mParentView = null;

		/**
		 * Constructor of the cache.
		 *
		 * @param cacheSize The number of views in the cache.
		 */
		private ViewCache(final int cacheSize) {
			mCache = new SparseArray<>(cacheSize);
		}

		/**
		 * Clean the cache.
		 */
		private void clear() {
			mCache.clear();
		}

		/**
		 * Get a view from the cache.
		 *
		 * @param position The position of the view.
		 * @param parent   the parentView view.
		 * @return The view from cache, if existing. Otherwise a new view.
		 */
		private ThumbImageView get(final int position, final ViewGroup parent) {
			if (mParentView == null) {
				mParentView = parent;
			}
			else if (mParentView != parent) {
				mParentView = parent;
				synchronized (mCache) {
					mCache.clear();
				}
			}

			ThumbImageView thumbImageView;
			if (mCache.indexOfKey(position) >= 0) {
				thumbImageView = mCache.get(position);
			}
			else {
				thumbImageView = createThumbImageView(position, mParentView, false);
				synchronized (mCache) {
					mCache.put(position, thumbImageView);
				}
			}

			return thumbImageView;
		}

		/**
		 * Get all cached images.
		 *
		 * @return the array of cached images.
		 */
		private ThumbImageView[] getCachedImages() {
			ThumbImageView[] result = new ThumbImageView[mCache.size()];
			for (int i = 0; i < mCache.size(); i++) {
				result[i] = mCache.valueAt(i);
			}
			return result;
		}

	}

	/**
	 * The type of a listed item.
	 */
	public enum ItemType {
		/**
		 * A list.
		 */
		LIST,
		/**
		 * A folder.
		 */
		FOLDER,
		/**
		 * A file.
		 */
		FILE
	}

}
