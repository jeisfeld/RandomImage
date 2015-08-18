package de.jeisfeld.randomimage.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.SystemUtil;

/**
 * A view for displaying a thumbnail.
 */
public class ThumbImageView extends FrameLayout {
	/**
	 * The thumb size in pixels.
	 */
	private static final int THUMB_SIZE = Math.min(Application.getAppContext().getResources()
			.getDimensionPixelSize(R.dimen.grid_pictures_size), MediaStoreUtil.MINI_THUMB_SIZE);

	/**
	 * The context in which this is used.
	 */
	private Context context;

	/**
	 * Flag indicating if the view is marked. (Similar to selection, but more stable)
	 */
	private boolean isMarked = false;

	/**
	 * Flag indicating if it is possible to mark the view.
	 */
	private MarkingType markingType = MarkingType.NONE;

	/**
	 * Flag indicating if this view represents a folder instead of a file.
	 */
	private boolean isFolder = false;

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

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context
	 *            The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context)
	 */
	public ThumbImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context
	 *            The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the view.
	 * @see android.view.View#View(Context, AttributeSet)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context
	 *            The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the view.
	 * @param defStyle
	 *            An attribute in the current theme that contains a reference to a style resource that supplies default
	 *            values for the view. Can be 0 to not look for defaults.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	/**
	 * Set the image and create the bitmap.
	 *
	 * @param activity
	 *            The activity holding the view.
	 * @param fileName
	 *            The image to be displayed.
	 * @param sameThread
	 *            if true, then image load will be done on the same thread. Otherwise a separate thread will be spawned.
	 * @param isImageFolder
	 *            flag indicating if this view is the thumb of a folder.
	 * @param postActivities
	 *            Activities that may be run on the UI thread after loading the image.
	 */
	public final void setImage(final Activity activity, final String fileName, final boolean sameThread,
			final boolean isImageFolder, final Runnable postActivities) {
		this.isFolder = isImageFolder;

		LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(isFolder ? R.layout.view_thumb_image_folder : R.layout.view_thumb_image, this, true);

		imageView = (ImageView) findViewById(R.id.imageViewThumb);
		checkBoxMarked = (CheckBox) findViewById(R.id.checkBoxMark);

		if (sameThread) {
			loadImage(activity, fileName, postActivities);
		}
		else {
			// Fill pictures in separate thread, for performance reasons
			Thread thread = new Thread() {
				@Override
				public void run() {
					loadImage(activity, fileName, postActivities);
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
	 * @param fileName
	 *            the name of the file to be loaded.
	 * @param postActivities
	 *            Actions to be done after loading the image.
	 */
	private void loadImage(final Activity activity, final String fileName, final Runnable postActivities) {
		final Bitmap imageBitmap;
		if (fileName == null) {
			imageBitmap = ImageUtil.getDummyBitmap();
		}
		else {
			imageBitmap = ImageUtil.getImageBitmap(fileName, THUMB_SIZE);
		}
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
		imageView.setImageBitmap(null);
		isMarked = false;
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
	 * Set the marking type of the view.
	 *
	 * @param newMarkingType
	 *            The type in which the view should be marked.
	 */
	public final void setMarkable(final MarkingType newMarkingType) {
		markingType = newMarkingType;
		if (markingType == MarkingType.NONE && isMarked) {
			setMarked(false);
		}
		checkBoxMarked.setVisibility(markingType == MarkingType.NONE ? View.INVISIBLE : View.VISIBLE);
		checkBoxMarked.setButtonDrawable(markingType == MarkingType.CROSS ? R.drawable.checkbox_negative
				: R.drawable.checkbox_positive);
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

	/**
	 * Set the folderName for display.
	 *
	 * @param folderName
	 *            the folderName to be displayed.
	 */
	public final void setFolderName(final String folderName) {
		if (isFolder) {
			TextView textViewName = (TextView) findViewById(R.id.textViewFolderName);
			if (folderName.length() > 50) { // MAGIC_NUMBER
				textViewName.setText(folderName.substring(0, 50)); // MAGIC_NUMBER
			}
			else {
				textViewName.setText(folderName);
			}
			if (folderName.length() > 25) { // MAGIC_NUMBER
				textViewName.setTextSize(TypedValue.COMPLEX_UNIT_SP, SystemUtil.isTablet() ? 15 : 10); // MAGIC_NUMBER
			}
		}
	}

	/**
	 * The types in which the view can be marked.
	 */
	public enum MarkingType {
		/**
		 * No marking.
		 */
		NONE,
		/**
		 * Marking with a red cross.
		 */
		CROSS,
		/**
		 * Marking with a green hook.
		 */
		HOOK
	}
}
