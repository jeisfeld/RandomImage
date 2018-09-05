package de.jeisfeld.randomimage.util;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.jeisfeld.randomimage.DisplayRandomImageActivity.FlipType;

/**
 * A RandomFileProvider where previous results are cached, so that it appears as a list.
 */
public class CachedRandomFileProvider implements RandomFileListProvider {
	/**
	 * Number of retries to find a new image avoiding repetitions.
	 */
	private static final int RETRIES_FOR_AVOIDING_REPETITIONS = 10;

	/**
	 * Creator used for creating the object from a Parcel.
	 */
	public static final Parcelable.Creator<CachedRandomFileProvider> CREATOR = new Parcelable.Creator<CachedRandomFileProvider>() {
		@Override
		public CachedRandomFileProvider createFromParcel(final Parcel in) {
			return new CachedRandomFileProvider(in);
		}

		@Override
		public CachedRandomFileProvider[] newArray(final int size) {
			return new CachedRandomFileProvider[size];
		}
	};

	/**
	 * The RandomFileProvider to be cached.
	 */
	private RandomFileProvider mProvider;

	/**
	 * The Current position.
	 */
	private int mCurrentPosition = 0;

	/**
	 * Flag indicating if the provider has already a file at current position.
	 */
	private boolean mHasFile = false;

	/**
	 * Flag indicating if the cache size has been determined already.
	 */
	private boolean mHasCachSizeDetermined = false;

	/**
	 * The flipType defining the rules how new file names are provided.
	 */
	private FlipType mFlipType = FlipType.AVOID_REPETITIONS;

	/**
	 * The max size of the image cache.
	 */
	private int mCacheSize = 0;

	/**
	 * The cached file names with positive index.
	 */
	private final List<String> mCachedFileNames = new ArrayList<>();

	/**
	 * Create a Cached RandomFileProvider based on a RandomFileProvider.
	 *
	 * @param provider The base RandomFileProvider.
	 */
	public CachedRandomFileProvider(final RandomFileProvider provider) {
		this(provider, null, FlipType.AVOID_REPETITIONS, null);
	}

	/**
	 * Create a Cached RandomFileProvider based on a RandomFileProvider, giving the first file name.
	 *
	 * @param provider      The base RandomFileProvider.
	 * @param firstFileName the first file name.
	 * @param flipType      the flip type defining how to get new files
	 * @param initData      a CachedRandomFileProvider that may be used to pre-define values
	 */
	public CachedRandomFileProvider(final RandomFileProvider provider, final String firstFileName,
									final FlipType flipType, final RandomFileListProvider initData) {
		mProvider = provider;
		mFlipType = flipType;
		determineCacheSize();

		if (initData != null && initData instanceof CachedRandomFileProvider) {
			CachedRandomFileProvider initData2 = (CachedRandomFileProvider) initData;
			mHasFile = initData2.mHasFile;
			mCurrentPosition = initData2.mCurrentPosition;
			mCachedFileNames.addAll(initData2.mCachedFileNames);
			mFlipType = initData2.mFlipType;
			mCacheSize = initData2.mCacheSize;
		}
		else if (firstFileName != null) {
			mCachedFileNames.add(firstFileName);
			mHasFile = true;
		}
	}

	/**
	 * Constructor out of parcel.
	 *
	 * @param in The parcel.
	 */
	public CachedRandomFileProvider(final Parcel in) {
		mHasFile = in.readByte() > 0;
		mCurrentPosition = in.readInt();
		in.readStringList(mCachedFileNames);
		mFlipType = FlipType.fromResourceValue(in.readInt());
		mCacheSize = in.readInt();
		mHasCachSizeDetermined = in.readByte() > 0;
	}

	@Override
	public final String getCurrentFileName() {
		if (!mHasFile) {
			synchronized (mCachedFileNames) {
				mCachedFileNames.add(getRandomFileName());
				mHasFile = true;
				return mCachedFileNames.get(0);
			}
		}
		return mCachedFileNames.get(mCurrentPosition);
	}

	@Override
	public final void goForward() {
		if (!mHasFile) {
			synchronized (mCachedFileNames) {
				mCachedFileNames.add(getRandomFileName());
				mHasFile = true;
			}
		}
		determineCacheSize();

		synchronized (mCachedFileNames) {
			switch (mFlipType) {
			case NEW_IMAGE:
			case ONE_BACK:
			case MULTIPLE_BACK:
				if (mCurrentPosition == mCacheSize - 1) {
					mCachedFileNames.remove(0);
					mCachedFileNames.add(getRandomFileName());
				}
				else {
					mCurrentPosition++;
					if (mCurrentPosition == mCachedFileNames.size()) {
						mCachedFileNames.add(getRandomFileName());
					}
				}
				break;
			case AVOID_REPETITIONS:
				if (mCurrentPosition == mCacheSize - 1) {
					mCachedFileNames.remove(0);
					mCachedFileNames.add(getRandomFileNameAvoidingRepetitions());
				}
				else {
					mCurrentPosition++;
					if (mCurrentPosition == mCachedFileNames.size()) {
						mCachedFileNames.add(getRandomFileNameAvoidingRepetitions());
					}
				}
				break;
			case CYCLICAL:
				mCurrentPosition++;

				if (mCurrentPosition == mCachedFileNames.size()) {
					mCurrentPosition = 0;
				}
				break;
			default:
				// do nothing
			}
		}
	}

	@Override
	public final void goBackward() {
		if (!mHasFile) {
			synchronized (mCachedFileNames) {
				mCachedFileNames.add(getRandomFileName());
				mHasFile = true;
			}
		}
		determineCacheSize();

		synchronized (mCachedFileNames) {
			switch (mFlipType) {
			case NEW_IMAGE:
			case ONE_BACK:
			case MULTIPLE_BACK:
				if (mCurrentPosition == 0) {
					mCachedFileNames.add(0, getRandomFileName());
					if (mCachedFileNames.size() > mCacheSize) {
						mCachedFileNames.remove(mCacheSize);
					}
				}
				else {
					mCurrentPosition--;
				}
				break;
			case AVOID_REPETITIONS:
				if (mCurrentPosition == 0) {
					mCachedFileNames.add(0, getRandomFileNameAvoidingRepetitions());
					if (mCachedFileNames.size() > mCacheSize) {
						mCachedFileNames.remove(mCacheSize);
					}
				}
				else {
					mCurrentPosition--;
				}
				break;
			case CYCLICAL:
				if (mCurrentPosition == 0) {
					mCurrentPosition = mCachedFileNames.size() - 1;
					if (mCurrentPosition < 0) {
						mCurrentPosition = 0;
					}
				}
				else {
					mCurrentPosition--;
				}
				break;
			default:
				// do nothing
			}

		}
	}

	/**
	 * Get a new random image from the list, avoiding repetitions.
	 *
	 * @return A new random image from the list.
	 */
	private String getRandomFileNameAvoidingRepetitions() {
		String result = getRandomFileName();
		int counter = 0;

		while (mCachedFileNames.contains(result) && counter++ < RETRIES_FOR_AVOIDING_REPETITIONS) {
			result = getRandomFileName();
		}
		return result;
	}

	/**
	 * Retrieve the cache size from the flip type.
	 */
	private void determineCacheSize() {
		if (mHasCachSizeDetermined) {
			return;
		}
		switch (mFlipType) {
		case NEW_IMAGE:
			mHasCachSizeDetermined = true;
			mCacheSize = 2;
			return;
		case ONE_BACK:
			mHasCachSizeDetermined = true;
			mCacheSize = 3; // MAGIC_NUMBER
			return;
		case MULTIPLE_BACK:
			if (mProvider.isReady()) {
				mHasCachSizeDetermined = true;
				mCacheSize = (getAllImageFiles().size() + 1) / 2;
			}
			else {
				mCacheSize = 10; // MAGIC_NUMBER
			}
			return;
		case AVOID_REPETITIONS:
			if (mProvider.isReady()) {
				mHasCachSizeDetermined = true;
				mCacheSize = getAllImageFiles().size() * 9 / 10; // MAGIC_NUMBER
			}
			else {
				mCacheSize = 10; // MAGIC_NUMBER
			}
			return;
		case CYCLICAL:
			if (mProvider.isReady()) {
				mCacheSize = getAllImageFiles().size();
				mCachedFileNames.addAll(getAllImageFiles());
				Collections.shuffle(mCachedFileNames);

				if (mCachedFileNames.size() > 0) {
					String firstFileName = mCachedFileNames.get(0);
					mCachedFileNames.remove(firstFileName);
					mCachedFileNames.add(0, firstFileName);
				}
				mHasFile = true;
				mHasCachSizeDetermined = true;
			}
			else {
				mCacheSize = 10; // MAGIC_NUMBER
			}
			return;
		default:
			mCacheSize = 0;
		}
	}

	@Override
	public final String removeCurrentFileName() {
		if (mProvider instanceof ImageList) {
			((ImageList) mProvider).load(false);
		}

		if (!mHasFile) {
			return getCurrentFileName();
		}

		synchronized (mCachedFileNames) {
			mCachedFileNames.removeAll(Collections.singletonList(mCachedFileNames.get(mCurrentPosition)));
			if (mCurrentPosition >= mCachedFileNames.size()) {
				mCurrentPosition = mCachedFileNames.size() - 1;
			}
			if (mCurrentPosition < 0) {
				mCurrentPosition = 0;
				mCachedFileNames.add(getRandomFileName());
			}
			return getCurrentFileName();
		}
	}

	@Override
	public final String getRandomFileName() {
		return mProvider.getRandomFileName();
	}

	@Override
	public final boolean isReady() {
		return mProvider.isReady();
	}

	@Override
	public final void waitUntilReady() {
		mProvider.waitUntilReady();
	}

	@Override
	public final void executeWhenReady(final Runnable whileLoading, final Runnable afterLoading, final Runnable ifError) {
		mProvider.executeWhenReady(whileLoading, afterLoading, ifError);
	}

	@Override
	public final List<String> getAllImageFiles() {
		return mProvider.getAllImageFiles();
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeByte((byte) (mHasFile ? 1 : 0));
		dest.writeInt(mCurrentPosition);
		dest.writeStringList(mCachedFileNames);
		dest.writeInt(mFlipType.getResourceValue());
		dest.writeInt(mCacheSize);
		dest.writeByte((byte) (mHasCachSizeDetermined ? 1 : 0));
	}
}
