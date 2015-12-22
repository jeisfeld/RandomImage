package de.jeisfeld.randomimage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageRegistry.CreationStyle;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimage.view.ThumbImageView;
import de.jeisfeld.randomimage.view.ThumbImageView.LoadableFileName;
import de.jeisfeld.randomimage.view.ThumbImageView.LoadableFileName.FileNameProvider;
import de.jeisfeld.randomimage.view.ThumbImageView.MarkingType;
import de.jeisfeld.randomimage.view.ThumbImageView.ThumbStyle;
import de.jeisfeld.randomimagelib.R;

/**
 * Array adapter class to display an eye photo pair in a list.
 */
public class DisplayImageListArrayAdapter extends ArrayAdapter<String> {
	/**
	 * Number of views to be preloaded.
	 */
	private static final int PRELOAD_SIZE;

	/**
	 * The cache where views of the displays are stored for smoother scrolling.
	 */
	private ViewCache mViewCache;

	/**
	 * The activity holding this adapter.
	 */
	private final DisplayImageListActivity mActivity;

	/**
	 * The names of the nested lists.
	 */
	private ArrayList<String> mNestedListNames;

	/**
	 * The names of the folders.
	 */
	private ArrayList<String> mFolderNames;

	/**
	 * The names of the files.
	 */
	private ArrayList<String> mFileNames;

	/**
	 * The set of nested list names selected for deletion.
	 */
	private Set<String> mSelectedNestedListNames = new HashSet<>();

	/**
	 * The set of folder names selected for deletion.
	 */
	private Set<String> mSelectedFolderNames = new HashSet<>();

	/**
	 * The set of filenames selected for deletion.
	 */
	private Set<String> mSelectedFileNames = new HashSet<>();

	/**
	 * Flag indicating how selection is handled.
	 */
	private volatile SelectionMode mSelectionMode = SelectionMode.ONE;

	/**
	 * The way in which the thumbs are marked.
	 */
	private MarkingType mMarkingType = MarkingType.NONE;

	static {
		// Set cache size in dependence of device memory.
		int memoryClass = SystemUtil.getLargeMemoryClass();

		if (memoryClass >= 512) { // MAGIC_NUMBER
			PRELOAD_SIZE = 50; // MAGIC_NUMBER
		}
		else if (memoryClass >= 256) { // MAGIC_NUMBER
			PRELOAD_SIZE = 20; // MAGIC_NUMBER
		}
		else {
			PRELOAD_SIZE = 5; // MAGIC_NUMBER
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
		case MULTIPLE_ADD:
			return MarkingType.HOOK;
		case MULTIPLE_REMOVE:
			return MarkingType.CROSS;
		default:
			return null;
		}
	}

	/**
	 * Constructor for the adapter.
	 *
	 * @param activity        The activity using the adapter.
	 * @param nestedListNames The names of nested lists to be displayed.
	 * @param folderNames     The names of folders to be displayed.
	 * @param fileNames       The names of files to be displayed.
	 */
	public DisplayImageListArrayAdapter(final DisplayImageListActivity activity,
										final ArrayList<String> nestedListNames, final ArrayList<String> folderNames,
										final ArrayList<String> fileNames) {
		super(activity, R.layout.text_view_initializing);
		this.mActivity = activity;

		if (nestedListNames == null) {
			this.mNestedListNames = new ArrayList<>();
		}
		else {
			this.mNestedListNames = nestedListNames;
			addAll(nestedListNames);
		}
		if (folderNames == null) {
			this.mFolderNames = new ArrayList<>();
		}
		else {
			this.mFolderNames = folderNames;
			addAll(folderNames);
		}
		if (fileNames == null) {
			this.mFileNames = new ArrayList<>();
		}
		else {
			this.mFileNames = fileNames;
			addAll(fileNames);
		}

		int totalSize = this.mFileNames.size() + this.mFolderNames.size() + this.mNestedListNames.size();

		this.mViewCache = new ViewCache(totalSize - 1, PRELOAD_SIZE);
	}

	/**
	 * Default adapter to be used by the framework.
	 *
	 * @param context The Context the view is running in.
	 */
	public DisplayImageListArrayAdapter(final Context context) {
		super(context, R.layout.text_view_initializing);
		this.mActivity = (DisplayImageListActivity) context;
	}

	/**
	 * Add a folder to the list. Only allowed for lists containing no files.
	 *
	 * @param folderName The folder added.
	 */
	public final void addFolder(final String folderName) {
		if (mFileNames.size() > 0) {
			// only allowed if there are no files in the list
			throw new UnsupportedOperationException("DisplayImageListArrayAdapter: added folderName after having fileNames");
		}
		if (mFolderNames.contains(folderName)) {
			return;
		}

		mFolderNames.add(folderName);
		add(folderName);
		mViewCache.incrementMaxPosition();
		notifyDataSetChanged();
	}

	@Override
	public final View getView(final int position, final View convertView, final ViewGroup parent) {
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
		final boolean isNestedList = position < mNestedListNames.size();
		final boolean isFolder = !isNestedList && position < mNestedListNames.size() + mFolderNames.size();

		final ThumbImageView thumbImageView =
				(ThumbImageView) LayoutInflater.from(mActivity).inflate(R.layout.adapter_display_images,
						parent, false);

		final String entryName;
		final LoadableFileName displayFileName;

		if (isNestedList) {
			entryName = mNestedListNames.get(position);

			displayFileName = new LoadableFileName(new FileNameProvider() {
				@Override
				public String getFileName() {
					ImageList imageList = ImageRegistry.getImageListByName(entryName, true);
					if (imageList == null) {
						return null;
					}
					ArrayList<String> imageFiles = imageList.getAllImageFiles();
					if (imageFiles.size() > 0) {
						return imageFiles.get(new Random().nextInt(imageFiles.size()));
					}
					else {
						return null;
					}
				}
			});

			thumbImageView.initWithStyle(mActivity, ThumbStyle.IMAGE_LIST);
			thumbImageView.setMarked(mSelectedNestedListNames.contains(entryName));
			thumbImageView.setFolderName(entryName);
		}
		else if (isFolder) {
			entryName = mFolderNames.get(position - mNestedListNames.size());

			displayFileName = new LoadableFileName(new FileNameProvider() {
				@Override
				public String getFileName() {
					ArrayList<String> imageFiles = new ArrayList<>(ImageList.getImageFilesInFolder(entryName));
					if (imageFiles.size() > 0) {
						return imageFiles.get(new Random().nextInt(imageFiles.size()));
					}
					else {
						return null;
					}
				}
			});

			thumbImageView.initWithStyle(mActivity, ThumbStyle.FOLDER);
			thumbImageView.setMarked(mSelectedFolderNames.contains(entryName));
			thumbImageView.setFolderName(new File(entryName).getName());
		}
		else {
			entryName = mFileNames.get(position - mNestedListNames.size() - mFolderNames.size());
			displayFileName = new LoadableFileName(entryName);

			thumbImageView.initWithStyle(mActivity, ThumbStyle.IMAGE);
			thumbImageView.setMarked(mSelectedFileNames.contains(entryName));
		}

		thumbImageView.setMarkable(mMarkingType);
		thumbImageView.setImage(mActivity, displayFileName, sameThread);

		final String listName;
		if (mActivity instanceof DisplayAllImagesActivity) {
			listName = ((DisplayAllImagesActivity) mActivity).getListName();
		}
		else {
			listName = null;
		}

		final Set<String> selectionList;
		if (isNestedList) {
			selectionList = mSelectedNestedListNames;
		}
		else if (isFolder) {
			selectionList = mSelectedFolderNames;
		}
		else {
			selectionList = mSelectedFileNames;
		}

		thumbImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				ThumbImageView view = (ThumbImageView) v;
				switch (mSelectionMode) {
				case ONE:
					if (isNestedList) {
						if (mActivity instanceof DisplayAllImagesActivity) {
							((DisplayAllImagesActivity) mActivity).switchToImageList(entryName, CreationStyle.NONE);
						}
					}
					else if (isFolder) {
						if (mActivity instanceof SelectImageFolderActivity) {
							((SelectImageFolderActivity) mActivity).returnResult(entryName);
						}
						else {
							DisplayImagesFromFolderActivity.startActivity(mActivity, entryName, false);
						}
					}
					else {
						if (mActivity instanceof DisplayImagesFromFolderActivity) {
							DisplayRandomImageActivity.startActivityForFolder(mActivity,
									new File(entryName).getParent(),
									displayFileName.getFileName());
						}
						else {
							mActivity.startActivityForResult(DisplayRandomImageActivity
											.createIntent(mActivity, null, displayFileName.getFileName(), true),
									DisplayRandomImageActivity.REQUEST_CODE);
						}
					}
					break;
				case MULTIPLE_ADD:
				case MULTIPLE_REMOVE:
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
			}
		});

		thumbImageView.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(final View v) {
				if (isNestedList) {
					DisplayNestedListDetailsActivity.startActivity(mActivity, entryName, listName);
				}
				else {
					DisplayImageDetailsActivity.startActivity(mActivity, entryName, listName, true);
				}
				return true;
			}
		});

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
	public final void setSelectedFiles(final ArrayList<String> selectedFiles) {
		if (selectedFiles == null) {
			mSelectedFileNames.clear();
		}
		else {
			mSelectedFileNames = new HashSet<>(selectedFiles);
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
	public final void setSelectedFolders(final ArrayList<String> selectedFolders) {
		if (selectedFolders == null) {
			mSelectedFolderNames.clear();
		}
		else {
			mSelectedFolderNames = new HashSet<>(selectedFolders);
		}
	}

	/**
	 * Get the list of selected nested lists.
	 *
	 * @return The selected nested lists.
	 */
	public final ArrayList<String> getSelectedNestedLists() {
		return new ArrayList<>(mSelectedNestedListNames);
	}

	/**
	 * Set the list of selected nested lists.
	 *
	 * @param selectedNestedLists The names of the nested lists.
	 */
	public final void setSelectedNestedLists(final ArrayList<String> selectedNestedLists) {
		if (selectedNestedLists == null) {
			mSelectedNestedListNames.clear();
		}
		else {
			mSelectedNestedListNames = new HashSet<>(selectedNestedLists);
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
					|| mSelectedNestedListNames.size() < mNestedListNames.size()) {
				setSelectedFiles(mFileNames);
				setSelectedFolders(mFolderNames);
				setSelectedNestedLists(mNestedListNames);
				for (ThumbImageView view : mViewCache.getCachedImages()) {
					view.setMarked(true);
				}
				return true;
			}
			else {
				setSelectedFiles(null);
				setSelectedFolders(null);
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
		mViewCache.interrupt();
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
		MULTIPLE_ADD,
		/**
		 * Multiple files can be selected for removal.
		 */
		MULTIPLE_REMOVE,
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
		 * The preload size.
		 */
		private int mPreloadSize;

		/**
		 * The planned size of the cache.
		 */
		private int mPlannedSize;

		/**
		 * The current number of views on the grid.
		 */
		private int mCurrentViewSize = 1;

		/**
		 * The maximum position.
		 */
		private int mMaxPosition;

		/**
		 * Center position of the cache.
		 */
		private volatile int mCurrentCenter;

		/**
		 * Flag indicating if a preload thread is running.
		 */
		private boolean mIsPreloadRunning = false;

		/**
		 * The parentView view holding the cached views.
		 */
		private ViewGroup mParentView = null;

		/**
		 * Waiting preload thread. Always contains one element - this is done so that the array can be used as
		 * synchronization object.
		 */
		private final Thread[] mWaitingThreads = new Thread[1];

		/**
		 * Flag indicating if the preload thread is interrupted and the thread is in the process of deletion.
		 */
		private boolean mIsInterrupted = false;

		/**
		 * Constructor of the cache.
		 *
		 * @param maxPosition The maximum position that can be stored.
		 * @param preloadSize The number of views to be preloaded.
		 */
		private ViewCache(final int maxPosition, final int preloadSize) {
			this.mPreloadSize = preloadSize;
			this.mMaxPosition = maxPosition;
			mPlannedSize = 2 * preloadSize;
			mCache = new SparseArray<>(mPlannedSize);
		}

		/**
		 * Increment the max position by 1.
		 */
		private void incrementMaxPosition() {
			mMaxPosition++;
		}

		/**
		 * Preload a range of views and clean the cache.
		 *
		 * @param position The position from where to do the preload
		 * @param atEnd    Flag indicating if we are at the end of the view or of the start.
		 */
		private void doPreload(final int position, final boolean atEnd) {
			synchronized (mCache) {
				mIsPreloadRunning = true;
				int startPosition;
				int endPosition;

				adjustPlannedSize();

				// Skipping 0, because this is frequently loaded.
				if (atEnd) {
					startPosition = Math.max(1, Math.min(position + mPreloadSize, mMaxPosition) - mPlannedSize + 1);
				}
				else {
					startPosition = Math.max(1, position - mPreloadSize);
				}
				endPosition = Math.min(startPosition + mPlannedSize - 1, mMaxPosition);

				mCurrentCenter = atEnd ? position - mCurrentViewSize / 2 : position + mCurrentViewSize / 2;

				// clean up, ignoring the first index which carries position 0.
				int index = 1;
				while (index < mCache.size()) {
					if (mCache.keyAt(index) < startPosition || mCache.keyAt(index) > endPosition) {
						mCache.removeAt(index);
					}
					else {
						index++;
					}
				}

				// preload.
				for (int i = startPosition; i <= endPosition; i++) {
					if (mCache.indexOfKey(i) < 0) {
						ThumbImageView view = createThumbImageView(i, mParentView, true);
						if (mIsInterrupted) {
							mIsPreloadRunning = false;
							return;
						}
						mCache.put(i, view);

						// Prevent wrong marking status in race conditions.
						view.setMarkable(mMarkingType);
					}
				}

				synchronized (mWaitingThreads) {
					if (mWaitingThreads[0] != null) {
						mWaitingThreads[0].start();
						mWaitingThreads[0] = null;
					}
				}

				mIsPreloadRunning = false;
			}
		}

		/**
		 * Trigger a preload thread, ensuring that only one such thread is running at a time.
		 *
		 * @param position The position from where to do the preload
		 * @param atEnd    Flag indicating if we are at the end of the view or of the start.
		 */
		private void triggerPreload(final int position, final boolean atEnd) {
			if (position == 0 || mIsInterrupted) {
				return;
			}

			Thread preloadThread = new Thread() {
				@Override
				public void run() {
					try {
						doPreload(position, atEnd);
					}
					catch (Exception e) {
						Log.e(Application.TAG, "Exception while preloading.", e);
					}
				}
			};

			synchronized (mWaitingThreads) {
				if (mIsPreloadRunning) {
					mWaitingThreads[0] = preloadThread;
				}
				else {
					mWaitingThreads[0] = null;
					preloadThread.start();
				}

			}
		}

		/**
		 * Interrupt the preloading thread and clean the cache.
		 */
		private void interrupt() {
			mIsInterrupted = true;
			mCache.clear();
		}

		/**
		 * Adjust the planned size according to the size of the view.
		 */
		private void adjustPlannedSize() {
			mCurrentViewSize = mParentView.getChildCount();
			int wishSize = Math.max(mCurrentViewSize * 2, mCurrentViewSize + 2 * mPreloadSize);
			if (mPlannedSize < wishSize) {
				mPlannedSize = wishSize;
			}
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
				mCache.clear();
			}

			ThumbImageView thumbImageView;
			if (mCache.indexOfKey(position) >= 0) {
				thumbImageView = mCache.get(position);
			}
			else {
				thumbImageView = createThumbImageView(position, mParentView, false);
				mCache.put(position, thumbImageView);
			}
			triggerPreload(position, position > mCurrentCenter);

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

}