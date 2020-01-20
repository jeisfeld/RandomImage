package de.jeisfeld.randomimage.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.jeisfeld.randomimage.view.PinchImageView;

/**
 * Handler for animated GIF.
 */
public final class GifAnimationDrawable extends AnimationDrawable {
	/**
	 * The decoder.
	 */
	private GifDecoder mGifDecoder;

	/**
	 * Temporary bitmap.
	 */
	private Bitmap mTmpBitmap;

	/**
	 * The height.
	 */
	private int mHeight;

	/**
	 * The width.
	 */
	private int mWidth;

	/**
	 * Constructor from file.
	 *
	 * @param context       the context.
	 * @param file          The file.
	 * @param rotationAngle a rotation angle of 0, 90 or -90 degrees.
	 * @throws IOException Exception while reading.
	 */
	@SuppressWarnings("resource")
	public GifAnimationDrawable(final Context context, final File file, final int rotationAngle) throws IOException {
		super();
		InputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(file), 32768); // MAGIC_NUMBER
			mGifDecoder = new GifDecoder();
			mGifDecoder.read(bis);
			mTmpBitmap = mGifDecoder.getFrame(0);

			if (rotationAngle == 0) {
				mHeight = mTmpBitmap.getHeight();
				mWidth = mTmpBitmap.getWidth();
			}
			else {
				mHeight = mTmpBitmap.getWidth();
				mWidth = mTmpBitmap.getHeight();
			}
			addFrame(new BitmapDrawable(context.getResources(), PinchImageView.rotateIfRequired(mTmpBitmap, rotationAngle)), mGifDecoder.getDelay(0));
			setOneShot(mGifDecoder.getLoopCount() != 0);
			setVisible(true, true);

			mGifDecoder.readContents();
			int n = mGifDecoder.getFrameCount();

			if (n < 2) {
				throw new IOException("Less than two frames - no animated GIF");
			}

			for (int i = 1; i < n; i++) {
				addFrame(new BitmapDrawable(context.getResources(), PinchImageView.rotateIfRequired(mGifDecoder.getFrame(i), rotationAngle)),
						mGifDecoder.getDelay(i));
			}
		}
		finally {
			if (bis != null) {
				try {
					bis.close();
				}
				catch (IOException e) {
					// do nothing.
				}
			}
		}

		mGifDecoder = null;
	}

	@Override
	public int getMinimumHeight() {
		return mHeight;
	}

	@Override
	public int getMinimumWidth() {
		return mWidth;
	}

	@Override
	public int getIntrinsicHeight() {
		return mHeight;
	}

	@Override
	public int getIntrinsicWidth() {
		return mWidth;
	}
}
