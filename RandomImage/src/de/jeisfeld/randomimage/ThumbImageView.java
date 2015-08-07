package de.jeisfeld.randomimage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;

/**
 * A view for displaying a thumbnail.
 */
public class ThumbImageView extends FrameLayout {
	/**
	 * Flag indicating if the view is marked. (Similar to selection, but more stable)
	 */
	private boolean isMarked = false;

	/**
	 * Flag indicating if it is possible to mark the view.
	 */
	private boolean isMarkable = false;

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

	/**
	 * The checkbox for marking.
	 */
	private CheckBox checkBoxMarked;

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
		checkBoxMarked = (CheckBox) findViewById(R.id.checkBoxMark);
	}

	// JAVADOC:ON

	/**
	 * Set the image and create the bitmap.
	 *
	 * @param activity
	 *            The activity holding the view.
	 * @param newFileName
	 *            The image to be displayed.
	 * @param sameThread
	 *            if true, then image load will be done on the same thread. Otherwise a separate thread will be spawned.
	 * @param postActivities
	 *            Activities that may be run on the UI thread after loading the image.
	 */
	public final void setImage(final Activity activity, final String newFileName, final boolean sameThread,
			final Runnable postActivities) {
		if (newFileName == null || newFileName.equals(this.fileName)) {
			return;
		}
		this.fileName = newFileName;

		if (sameThread) {
			loadImage(activity, postActivities);
		}
		else {
			// Fill pictures in separate thread, for performance reasons
			Thread thread = new Thread() {
				@Override
				public void run() {
					loadImage(activity, postActivities);
				}
			};
			thread.start();
		}

	}

	/**
	 * Inner helper method of set Image, handling the loading of the image.
	 *
	 * @param activity
	 *            The activity triggering the load.
	 * @param postActivities
	 *            Actions to be done after loading the image.
	 */
	private void loadImage(final Activity activity, final Runnable postActivities) {
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
	 * Set the markability status of the view.
	 *
	 * @param markable
	 *            if true, the view is set markable, otherwise not markable.
	 */
	public final void setMarkable(final boolean markable) {
		isMarkable = markable;
		if (!isMarkable && isMarked) {
			setMarked(false);
		}
		checkBoxMarked.setVisibility(isMarkable ? View.VISIBLE : View.INVISIBLE);
	}

	/**
	 * Set the marking status of the view.
	 *
	 * @param marked
	 *            if true, it is marked, otherwise unmarked.
	 */
	public final void setMarked(final boolean marked) {
		isMarked = marked;
		checkBoxMarked.setChecked(marked);
	}

	public final boolean isMarked() {
		return isMarked;
	}

}
