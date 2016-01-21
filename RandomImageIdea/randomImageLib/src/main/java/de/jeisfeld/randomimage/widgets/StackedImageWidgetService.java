package de.jeisfeld.randomimage.widgets;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.notifications.NotificationUtil;
import de.jeisfeld.randomimage.notifications.NotificationUtil.NotificationType;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.StandardImageList;
import de.jeisfeld.randomimagelib.R;

/**
 * Service handling the image stack for the stacked image widget.
 */
public class StackedImageWidgetService extends RemoteViewsService {
	/**
	 * Factor by which the image sizes are reduced compared to allowed size in square view.
	 */
	private static final float SQUARE_IMAGE_SCALE_FACTOR = 0.6f;
	/**
	 * Factor by which the image sizes are reduced compared to allowed size in stretched view.
	 */
	private static final float STRETCHED_IMAGE_SCALE_FACTOR = 0.95f;
	/**
	 * The maximum aspect ratio of the image view.
	 */
	private static final float MAX_ASPECT_RATIO = 16.0f / 9;

	/**
	 * The size of the border around the image.
	 */
	private static final int IMAGE_BORDER_SIZE = Application.getAppContext().getResources()
			.getDimensionPixelSize(R.dimen.stack_image_widget_border_size);

	@Override
	public final RemoteViewsFactory onGetViewFactory(final Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	/**
	 * Factory class handling the image stack for the stacked image widget.
	 */
	private final class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
		/**
		 * The file names of the stacked images.
		 */
		private ArrayList<String> mFileNames = new ArrayList<>();

		/**
		 * The application context.
		 */
		private Context mContext;

		/**
		 * The app widget id.
		 */
		private int mAppWidgetId;

		/**
		 * The width to which the images are scaled.
		 */
		private int mImageWidth;
		/**
		 * The height to which the images are scaled.
		 */
		private int mImageHeight;

		/**
		 * The name of the imageList to be displayed.
		 */
		private String mListName;

		/**
		 * Constructor.
		 *
		 * @param context The context in which the factory is called.
		 * @param intent  The intent for the views
		 */
		private StackRemoteViewsFactory(final Context context, final Intent intent) {
			this.mContext = context;
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

			determineImageDimensions();

			mListName = intent.getStringExtra(StackedImageWidget.STRING_EXTRA_LISTNAME);
		}

		/**
		 * Determine the dimensions of the images.
		 */
		private void determineImageDimensions() {
			int viewWidth = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_view_width, mAppWidgetId, 0);
			int viewHeight = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_view_height, mAppWidgetId, 0);

			double targetWidth = SQUARE_IMAGE_SCALE_FACTOR * viewWidth;
			double targetHeight = SQUARE_IMAGE_SCALE_FACTOR * viewHeight;

			if (targetWidth > MAX_ASPECT_RATIO * targetHeight) {
				double correctionFactor = Math.sqrt(targetWidth / targetHeight / MAX_ASPECT_RATIO);
				targetWidth /= correctionFactor;
				targetHeight *= correctionFactor;
				if (targetHeight > viewHeight * STRETCHED_IMAGE_SCALE_FACTOR) {
					targetHeight = viewHeight * STRETCHED_IMAGE_SCALE_FACTOR;
				}
			}
			else if (targetHeight > MAX_ASPECT_RATIO * targetWidth) {
				double correctionFactor = Math.sqrt(targetHeight / targetWidth / MAX_ASPECT_RATIO);
				targetHeight /= correctionFactor;
				targetWidth *= correctionFactor;
				if (targetWidth > viewWidth * STRETCHED_IMAGE_SCALE_FACTOR) {
					targetWidth = viewWidth * STRETCHED_IMAGE_SCALE_FACTOR;
				}
			}

			mImageWidth = Math.min(ImageUtil.MAX_BITMAP_SIZE, (int) Math.ceil(targetWidth));
			mImageHeight = Math.min(ImageUtil.MAX_BITMAP_SIZE, (int) Math.ceil(targetHeight));
		}

		@Override
		public void onCreate() {
			StandardImageList imageList = ImageRegistry.getStandardImageListByName(mListName, false);

			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + mListName + " for StackedImageWidget creation");
				DialogUtil.displayToast(mContext, R.string.toast_error_while_loading, mListName);
				NotificationUtil.displayNotification(mContext, mListName, NotificationType.ERROR_LOADING_LIST,
						R.string.title_notification_failed_loading, R.string.toast_error_while_loading, mListName);
				mFileNames = new ArrayList<>();
			}
			else {
				NotificationUtil.cancelNotification(mContext, mListName, NotificationType.ERROR_LOADING_LIST);
				mFileNames = imageList.getShuffledFileNames();
			}
		}

		@Override
		public void onDestroy() {
		}

		@Override
		public int getCount() {
			return mFileNames.size();
		}

		@Override
		public RemoteViews getViewAt(final int position) {
			RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_stacked_image_item);

			String currentFileName = mFileNames.get(position);

			if (currentFileName == null) {
				remoteViews.setImageViewResource(R.id.imageViewWidget, R.drawable.ic_launcher);
			}
			else {
				boolean stretchToFit = PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_background_style, mAppWidgetId, -1) == 0;
				Bitmap bitmap;
				if (mImageWidth > 0 && mImageHeight > 0) {
					bitmap = ImageUtil.getBitmapOfExactSize(currentFileName, mImageWidth, mImageHeight, stretchToFit ? -1 : IMAGE_BORDER_SIZE);
				}
				else {
					bitmap = ImageUtil.getImageBitmap(currentFileName, MediaStoreUtil.MINI_THUMB_SIZE);
				}
				remoteViews.setImageViewBitmap(R.id.imageViewWidget, bitmap);
			}

			GenericImageWidget.configureBackground(mContext, remoteViews, AppWidgetManager.getInstance(mContext), mAppWidgetId);

			// Next, we set a fill-intent which will be used to fill-in the pending intent template
			// which is set on the collection view in StackedImageWidget.
			Bundle extras = new Bundle();
			extras.putInt(StackedImageWidget.STRING_EXTRA_ITEM, position);
			extras.putString(DisplayRandomImageActivity.STRING_EXTRA_FILENAME, currentFileName);
			Intent fillInIntent = new Intent();
			fillInIntent.putExtras(extras);
			remoteViews.setOnClickFillInIntent(R.id.imageViewWidget, fillInIntent);

			return remoteViews;
		}

		@Override
		public RemoteViews getLoadingView() {
			return null;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public long getItemId(final int position) {
			return position;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public void onDataSetChanged() {
			// update image dimensions
			determineImageDimensions();
			// Update the list name
			mListName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, mAppWidgetId);

			// create new image list
			StandardImageList imageList = ImageRegistry.getStandardImageListByName(mListName, false);
			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + mListName + " for StackedImageWidget data change");
				DialogUtil.displayToast(mContext, R.string.toast_error_while_loading, mListName);
				NotificationUtil.displayNotification(mContext, mListName, NotificationType.ERROR_LOADING_LIST,
						R.string.title_notification_failed_loading, R.string.toast_error_while_loading, mListName);
				mFileNames = new ArrayList<>();
			}
			else {
				NotificationUtil.cancelNotification(mContext, mListName, NotificationType.ERROR_LOADING_LIST);
				mFileNames = imageList.getShuffledFileNames();
			}
		}
	}
}
