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
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.SystemUtil;
import de.jeisfeld.randomimagelib.R;

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
	 * The background image for a folder.
	 */
	private static final Bitmap BITMAP_FOLDER_BACKGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_background, R.color.folder_background, R.color.folder_foreground);

	/**
	 * The foreground image for a folder.
	 */
	private static final Bitmap BITMAP_FOLDER_FOREGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_foreground, R.color.folder_background, R.color.folder_foreground);

	/**
	 * The background image for a folder.
	 */
	private static final Bitmap BITMAP_FOLDER_RECURSIVE_BACKGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_background, R.color.folder_recursive_background, R.color.folder_recursive_foreground);

	/**
	 * The foreground image for a folder.
	 */
	private static final Bitmap BITMAP_FOLDER_RECURSIVE_FOREGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_foreground, R.color.folder_recursive_background, R.color.folder_recursive_foreground);

	/**
	 * The background image for a list.
	 */
	private static final Bitmap BITMAP_LIST_BACKGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_background, R.color.list_background, R.color.list_foreground);

	/**
	 * The foreground image for a list.
	 */
	private static final Bitmap BITMAP_LIST_FOREGROUND =
			ImageUtil.getColorizedBitmap(R.drawable.folder_foreground, R.color.list_background, R.color.list_foreground);

	/**
	 * The context in which this is used.
	 */
	private Context mContext;

	/**
	 * Flag indicating if the view is marked. (Similar to selection, but more stable)
	 */
	private boolean mIsMarked = false;

	/**
	 * The style in which the thumb is displayed.
	 */
	private ThumbStyle mThumbStyle = ThumbStyle.IMAGE;

	/**
	 * The imageView displaying the thumb.
	 */
	private ImageView mImageView;

	/**
	 * The checkbox for marking.
	 */
	private CheckBox mCheckBoxMarked;

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context)
	 */
	public ThumbImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs The attributes of the XML tag that is inflating the view.
	 * @see android.view.View#View(Context, AttributeSet)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs The attributes of the XML tag that is inflating the view.
	 * @param defStyle An attribute in the current theme that contains a reference to a style resource that supplies default
	 *            values for the view. Can be 0 to not look for defaults.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public ThumbImageView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		this.mContext = context;
	}

	/**
	 * Initialize the View, depending on the style.
	 *
	 * @param activity The activity displaying the view.
	 * @param style The style of the view.
	 */
	public final void initWithStyle(final Activity activity, final ThumbStyle style) {
		this.mThumbStyle = style;

		int layoutId = 0;
		Bitmap bitmapBackground = null;
		Bitmap bitmapForeground = null;

		switch (mThumbStyle) {
		case IMAGE:
			layoutId = R.layout.view_thumb_image;
			break;
		case FOLDER:
			layoutId = R.layout.view_thumb_folder;
			bitmapBackground = BITMAP_FOLDER_BACKGROUND;
			bitmapForeground = BITMAP_FOLDER_FOREGROUND;
			break;
		case FOLDER_RECURSIVE:
			layoutId = R.layout.view_thumb_folder;
			bitmapBackground = BITMAP_FOLDER_RECURSIVE_BACKGROUND;
			bitmapForeground = BITMAP_FOLDER_RECURSIVE_FOREGROUND;
			break;
		case LIST:
			layoutId = R.layout.view_thumb_folder;
			bitmapBackground = BITMAP_LIST_BACKGROUND;
			bitmapForeground = BITMAP_LIST_FOREGROUND;
			break;
		default:
			break;
		}

		LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(layoutId, this, true);

		if (bitmapBackground != null) {
			((ImageView) findViewById(R.id.imageViewFolderBackground)).setImageBitmap(bitmapBackground);
		}
		if (bitmapForeground != null) {
			((ImageView) findViewById(R.id.imageViewFolderForeground)).setImageBitmap(bitmapForeground);
		}

		mImageView = findViewById(R.id.imageViewThumb);
		mCheckBoxMarked = findViewById(R.id.checkBoxMark);
	}

	/**
	 * Set the image and create the bitmap.
	 *
	 * @param activity The activity holding the view.
	 * @param loadableFileName A provider for the name of the image to be displayed.
	 * @param sameThread if true, then image load will be done on the same thread. Otherwise a separate thread will be spawned.
	 */
	public final void setImage(final Activity activity, final LoadableFileName loadableFileName,
			final boolean sameThread) {
		if (sameThread) {
			loadImage(activity, loadableFileName);
		}
		else {
			// Fill pictures in separate thread, for performance reasons
			Thread thread = new Thread() {
				@Override
				public void run() {
					loadImage(activity, loadableFileName);
				}
			};
			thread.start();
		}
	}

	/**
	 * Inner helper method of set Image, handling the loading of the image.
	 *
	 * @param activity The activity triggering the load.
	 * @param loadableFileName a provider of the name of the file to be loaded.
	 */
	private void loadImage(final Activity activity, final LoadableFileName loadableFileName) {
		if (loadableFileName == null) {
			return;
		}
		final Bitmap imageBitmap;
		if (loadableFileName.getFileName() == null) {
			imageBitmap = ImageUtil.getDummyBitmap();
		}
		else {
			imageBitmap = ImageUtil.getImageBitmap(loadableFileName.getFileName(), THUMB_SIZE);
		}
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mImageView.setImageBitmap(imageBitmap);
				mImageView.invalidate();
			}
		});
	}

	/**
	 * Set the marking type of the view.
	 *
	 * @param markingType The type in which the view should be marked.
	 */
	public final void setMarkable(final MarkingType markingType) {
		if (markingType == MarkingType.NONE && mIsMarked) {
			setMarked(false);
		}
		mCheckBoxMarked.setVisibility(markingType == MarkingType.NONE ? View.INVISIBLE : View.VISIBLE);
	}

	/**
	 * Set the marking status of the view.
	 *
	 * @param marked if true, it is marked, otherwise unmarked.
	 */
	public final void setMarked(final boolean marked) {
		mIsMarked = marked;
		mCheckBoxMarked.setChecked(marked);
	}

	public final boolean isMarked() {
		return mIsMarked;
	}

	/**
	 * Set the folderName for display.
	 *
	 * @param folderName the folderName to be displayed.
	 */
	public final void setFolderName(final String folderName) {
		if (mThumbStyle != ThumbStyle.IMAGE) {
			TextView textViewName = findViewById(R.id.textViewFolderName);
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
	 * Helper class that allows to provide the fileName asynchronously.
	 */
	public static class LoadableFileName {
		/**
		 * The fileName.
		 */
		private String mFileName = null;
		/**
		 * An asynchronous provider of the file name.
		 */
		private FileNameProvider mFileNameProvider = null;

		/**
		 * Constructor providing the file name.
		 *
		 * @param fileName The file name.
		 */
		public LoadableFileName(final String fileName) {
			this.mFileName = fileName;
		}

		/**
		 * Constructor providing a method to retrieve the file name.
		 *
		 * @param fileNameProvider The provider for the file name.
		 */
		public LoadableFileName(final FileNameProvider fileNameProvider) {
			this.mFileNameProvider = fileNameProvider;
		}

		/**
		 * Get the file name.
		 *
		 * @return The file name.
		 */
		public final String getFileName() {
			if (mFileName == null && mFileNameProvider != null) {
				mFileName = mFileNameProvider.getFileName();
			}
			return mFileName;
		}

		/**
		 * Interface for asynchronous delivery of the file name.
		 */
		public interface FileNameProvider {
			/**
			 * Method to get the file name.
			 *
			 * @return The file name.
			 */
			String getFileName();
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
		 * Marking with a green hook.
		 */
		HOOK
	}

	/**
	 * The styles in which the thumb may be displayed.
	 */
	public enum ThumbStyle {
		/**
		 * A thumb representing an image.
		 */
		IMAGE,
		/**
		 * A thumb representing a folder.
		 */
		FOLDER,
		/**
		 * A thumb representing a recursive folder.
		 */
		FOLDER_RECURSIVE,
		/**
		 * A thumb representing an image list.
		 */
		LIST
	}

}
