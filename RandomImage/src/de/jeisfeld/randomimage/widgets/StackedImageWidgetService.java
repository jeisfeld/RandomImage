package de.jeisfeld.randomimage.widgets;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageList;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.MediaStoreUtil;

/**
 * Service handling the image stack for the stacked image widget.
 */
public class StackedImageWidgetService extends RemoteViewsService {

	@Override
	public final RemoteViewsFactory onGetViewFactory(final Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	/**
	 * Factory class handling the image stack for the stacked image widget.
	 */
	private class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
		/**
		 * The number of stacked images.
		 */
		private int stackSize = 1;

		/**
		 * The file names of the stacked images.
		 */
		private String[] fileNames;

		/**
		 * The application context.
		 */
		private Context context;

		/**
		 * The width of the view of the widget.
		 */
		private int viewWidth;

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

			viewWidth = intent.getIntExtra(StackedImageWidget.STRING_EXTRA_WIDTH, MediaStoreUtil.MINI_THUMB_SIZE);
		}

		@Override
		public final void onCreate() {
			ImageList imageList = ImageRegistry.getCurrentImageList();
			stackSize = imageList.size();
			fileNames = imageList.getShuffledFileNames();
		}

		@Override
		public final void onDestroy() {
		}

		@Override
		public final int getCount() {
			return stackSize;
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
				remoteViews.setImageViewBitmap(
						R.id.imageViewWidget,
						ImageUtil.getImageBitmap(currentFileName,
								Math.min(ImageUtil.MAX_BITMAP_SIZE, viewWidth)));
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
		}
	}
}
