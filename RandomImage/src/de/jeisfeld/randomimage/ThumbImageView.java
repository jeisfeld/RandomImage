package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;

/**
 * A view for displaying a thumbnail.
 */
public class ThumbImageView extends FrameLayout {
	/**
	 * The color used as background for highlighted images.
	 */
	private static final int COLOR_HIGHLIGHT = Color.CYAN;

	/**
	 * Flag indicating if the view is marked. (Similar to selection, but more stable)
	 */
	private boolean isMarked = false;

	/**
	 * The EyePhoto shown in the view.
	 */
	private String fileName;
	/**
	 * Indicates if the view is initialized.
	 */
	private boolean initialized = false;

	/**
	 * The imageView displaying the thumb.
	 */
	private ImageView imageView;

	// JAVADOC:OFF
	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @see #View(Context)
	 */
	public ThumbImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @see #View(Context, AttributeSet)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @see #View(Context, AttributeSet, int)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);

		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.view_thumb_image, this, true);

		imageView = (ImageView) findViewById(R.id.imageViewThumb);
	}

	// JAVADOC:ON

	/**
	 * Set the image and create the bitmap.
	 *
	 * @param activity
	 *            The activity holding the view.
	 * @param newFileName
	 *            The image to be displayed.
	 * @param postActivities
	 *            Activities that may be run on the UI thread after loading the image.
	 */
	public final void setImage(final Activity activity, final String newFileName, final Runnable postActivities) {
		if (newFileName == null || newFileName.equals(this.fileName)) {
			return;
		}
		this.fileName = newFileName;

		// Fill pictures in separate thread, for performance reasons
		Thread thread = new Thread() {
			@Override
			public void run() {
				final Bitmap imageBitmap = ImageUtil.getImageBitmap(fileName, MediaStoreUtil.MINI_THUMB_SIZE);
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						imageView.setImageBitmap(imageBitmap);
						imageView.invalidate();
						setMarked(false);
						initialized = true;
						if (postActivities != null) {
							postActivities.run();
						}
					}
				});
			}
		};
		thread.start();
	}

	/**
	 * Clean the eye photo from the view.
	 */
	public final void cleanImage() {
		this.fileName = null;
		imageView.setImageBitmap(null);
		isMarked = false;
	}

	/**
	 * Retrieve the eyePhoto object.
	 *
	 * @return the eye photo.
	 */
	public final String getFileName() {
		return fileName;
	}

	/**
	 * Mark as mInitialized to prevent double initialization.
	 */
	public final void setInitialized() {
		initialized = true;
	}

	/**
	 * Check if it is mInitialized.
	 *
	 * @return true if it is initialized.
	 */
	public final boolean isInitialized() {
		return initialized;
	}

	/**
	 * Set the marking status of the view.
	 *
	 * @param marked
	 *            if true, it is marked, otherwise unmarked.
	 */
	public final void setMarked(final boolean marked) {
		isMarked = marked;
		if (marked) {
			imageView.setBackgroundColor(COLOR_HIGHLIGHT);
		}
		else {
			imageView.setBackgroundColor(Color.TRANSPARENT);
		}
	}

	public final boolean isMarked() {
		return isMarked;
	}

}
