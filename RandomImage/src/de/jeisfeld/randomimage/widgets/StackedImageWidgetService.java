package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.DialogUtil;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimage.util.StandardImageList;

/**
 * Service handling the image stack for the stacked image widget.
 */
public class StackedImageWidgetService extends RemoteViewsService {
	/**
	 * Factor by which the image sizes are reduced compared to allowed size.
	 */
	private static final float IMAGE_SCALE_FACTOR = 0.6f;

	/**
	 * A map storing instances of the factory.
	 */
	private static SparseArray<StackRemoteViewsFactory> factoryMap =
			new SparseArray<StackedImageWidgetService.StackRemoteViewsFactory>();

	@Override
	public final RemoteViewsFactory onGetViewFactory(final Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	/**
	 * Calculate the planned image size out of the view width.
	 *
	 * @param viewWidth
	 *            The view width.
	 * @return The planned image size.
	 */
	private static int calculateImageSize(final int viewWidth) {
		return Math.max((int) Math.ceil(IMAGE_SCALE_FACTOR * viewWidth), MediaStoreUtil.MINI_THUMB_SIZE);
	}

	/**
	 * Factory class handling the image stack for the stacked image widget.
	 */
	private class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
		/**
		 * The file names of the stacked images.
		 */
		private String[] fileNames = new String[0];

		/**
		 * The application context.
		 */
		private Context context;

		/**
		 * The app widget id.
		 */
		private int appWidgetId;

		/**
		 * The size to which the images are scaled.
		 */
		private int imageSize;

		/**
		 * The name of the imageList to be displayed.
		 */
		private String listName;

		/**
		 * Constructor.
		 *
		 * @param context
		 *            The context in which the factory is called.
		 * @param intent
		 *            The intent for the views
		 */
		public StackRemoteViewsFactory(final Context context, final Intent intent) {
			this.context = context;

			appWidgetId =
					intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

			int viewWidth = intent.getIntExtra(StackedImageWidget.STRING_EXTRA_WIDTH, MediaStoreUtil.MINI_THUMB_SIZE);
			imageSize = calculateImageSize(viewWidth);
			listName = intent.getStringExtra(StackedImageWidget.STRING_EXTRA_LISTNAME);

			factoryMap.put(appWidgetId, this);
		}

		@Override
		public final void onCreate() {
			StandardImageList imageList = ImageRegistry.getStandardImageListByName(listName, false);

			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + listName + " for StackedImageWidget creation");
				DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
				fileNames = new String[0];
			}
			else {
				fileNames = imageList.getShuffledFileNames();
			}
		}

		@Override
		public final void onDestroy() {
			factoryMap.remove(appWidgetId);
		}

		@Override
		public final int getCount() {
			return fileNames.length;
		}

		@Override
		public final RemoteViews getViewAt(final int position) {

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_stacked_image_item);

			String currentFileName = fileNames[position];

			if (currentFileName == null) {
				remoteViews.setImageViewResource(
						R.id.imageViewWidget,
						R.drawable.ic_launcher);
			}
			else {
				// scale only by half the view width
				remoteViews.setImageViewBitmap(
						R.id.imageViewWidget,
						ImageUtil.getImageBitmap(currentFileName,
								Math.min(ImageUtil.MAX_BITMAP_SIZE, imageSize)));
			}

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
			// update image size
			int viewWidth =
					PreferenceUtil.getIndexedSharedPreferenceInt(R.string.key_widget_view_width, appWidgetId, 0);
			imageSize = calculateImageSize(viewWidth);

			// create new image list
			StandardImageList imageList = ImageRegistry.getStandardImageListByName(listName, false);
			if (imageList == null) {
				Log.e(Application.TAG, "Could not load image list " + listName + " for StackedImageWidget data change");
				DialogUtil.displayToast(context, R.string.toast_error_while_loading, listName);
				fileNames = new String[0];
			}
			else {
				fileNames = imageList.getShuffledFileNames();
			}
		}
	}
}
