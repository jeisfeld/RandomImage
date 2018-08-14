package de.jeisfeld.randomimage.util;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A RandomFileProvider where previous results are cached, so that it appears as a list.
 */
public class CachedRandomFileProvider implements RandomFileListProvider {
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
	 * The cached file names with positive index.
	 */
	private final List<String> mCachedFileNames = new ArrayList<>();

	/**
	 * Create a Cached RandomFileProvider based on a RandomFileProvider.
	 *
	 * @param provider The base RandomFileProvider.
	 */
	public CachedRandomFileProvider(final RandomFileProvider provider) {
		mProvider = provider;
	}

	/**
	 * Create a Cached RandomFileProvider based on a RandomFileProvider, giving the first file name.
	 *
	 * @param provider      The base RandomFileProvider.
	 * @param firstFileName the first file name.
	 * @param initData      a CachedRandomFileProvider that may be used to pre-define values
	 */
	public CachedRandomFileProvider(final RandomFileProvider provider, final String firstFileName, final RandomFileListProvider initData) {
		mProvider = provider;
		if (initData != null && initData instanceof CachedRandomFileProvider) {
			CachedRandomFileProvider initData2 = (CachedRandomFileProvider) initData;
			mHasFile = initData2.mHasFile;
			mCurrentPosition = initData2.mCurrentPosition;
			mCachedFileNames.addAll(initData2.mCachedFileNames);
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
			mCachedFileNames.add(getRandomFileName());
		}

		synchronized (mCachedFileNames) {
			mCurrentPosition++;

			if (mCurrentPosition == mCachedFileNames.size()) {
				mCachedFileNames.add(getRandomFileName());
			}
		}
	}

	@Override
	public final void goBackward() {
		if (!mHasFile) {
			mCachedFileNames.add(getRandomFileName());
		}

		synchronized (mCachedFileNames) {

			if (mCurrentPosition == 0) {
				mCachedFileNames.add(0, getRandomFileName());
			}
			else {
				mCurrentPosition--;
			}
		}
	}

	@Override
	public final String updateCurrentFileName() {
		if (!mHasFile) {
			return getCurrentFileName();
		}

		synchronized (mCachedFileNames) {
			mCachedFileNames.set(mCurrentPosition, getRandomFileName());
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
	}
}
