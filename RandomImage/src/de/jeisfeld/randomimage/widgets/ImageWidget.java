package de.jeisfeld.randomimage.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.RemoteViews;
import de.jeisfeld.randomimage.Application;
import de.jeisfeld.randomimage.DisplayRandomImageActivity;
import de.jeisfeld.randomimage.R;
import de.jeisfeld.randomimage.util.ImageRegistry;
import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.PreferenceUtil;

/**
 * The extended widget, also displaying a changing image.
 */
public class ImageWidget extends AppWidgetProvider {
	/**
	 * The file names of the currently displayed images - mapped from appWidgetId.
	 */
	private static SparseArray<String> currentFileNames = new SparseArray<String>();

	/**
	 * Number of pixels per dip.
	 */
	private static final float DENSITY = Application.getAppContext().getResources().getDisplayMetrics().density;

	/**
	 * The names of the image lists associated to the widget.
	 */
	private static SparseArray<String> listNames = new SparseArray<String>();

	@Override
	public final void
			onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; i++) {
			int appWidgetId = appWidgetIds[i];

			String listName = getListName(appWidgetId);

			String fileName = ImageRegistry.getImageListByName(listName).getRandomFileName();

			setImage(context, appWidgetManager, appWidgetId, listName, fileName);
		}
	}

	@Override
	public final void onAppWidgetOptionsChanged(final Context context, final AppWidgetManager appWidgetManager,
			final int appWidgetId,
			final Bundle newOptions) {
		super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

		String listName = getListName(appWidgetId);

		String fileName = currentFileNames.get(appWidgetId);
		if (fileName == null) {
			fileName = ImageRegistry.getImageListByName(listName).getRandomFileName();
		}

		setImage(context, appWidgetManager, appWidgetId, listName, fileName);
	}

	/**
	 * Get the list name associated to an instance of the widget.
	 *
	 * @param appWidgetId
	 *            The app widget id.
	 * @return The list name.
	 */
	private String getListName(final int appWidgetId) {
		String listName = listNames.get(appWidgetId);
		if (listName == null) {
			listName = PreferenceUtil.getIndexedSharedPreferenceString(R.string.key_widget_list_name, appWidgetId);
			if (listName == null || listName.length() == 0) {
				listName = ImageRegistry.getCurrentListName();
			}
			listNames.put(appWidgetId, listName);
		}
		return listName;
	}

	/**
	 * Set the image in an instance of the widget.
	 *
	 * @param context
	 *            The {@link android.content.Context Context} in which this receiver is running.
	 * @param appWidgetManager
	 *            A {@link AppWidgetManager} object you can call {@link AppWidgetManager#updateAppWidget} on.
	 * @param appWidgetId
	 *            The appWidgetId of the widget whose size changed.
	 * @param listName
	 *            The name of the image list from which the file is taken.
	 * @param fileName
	 *            The filename of the image to be displayed.
	 */
	private void setImage(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
			final String listName, final String fileName) {
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_image);

		Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
		int width = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH));
		int height = (int) Math.ceil(DENSITY * options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));

		if (fileName == null) {
			remoteViews.setImageViewResource(
					R.id.imageViewWidget,
					R.drawable.ic_launcher);
		}
		else {
			currentFileNames.put(appWidgetId, fileName);
			remoteViews.setImageViewBitmap(
					R.id.imageViewWidget,
					ImageUtil.getImageBitmap(fileName,
							Math.min(ImageUtil.MAX_BITMAP_SIZE, Math.max(width, height))));
		}

		Intent intent = DisplayRandomImageActivity.createIntent(context, listName, fileName);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		remoteViews.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntent);
		appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
	}

	/**
	 * Update instances of the widget.
	 *
	 * @param appWidgetId
	 *            the list of instances to be updated. If empty, then all instances will be updated.
	 */
	public static final void updateInstances(final int... appWidgetId) {
		Context context = Application.getAppContext();
		Intent intent = new Intent(context, ImageWidget.class);
		intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

		int[] ids;
		if (appWidgetId.length == 0) {
			ids =
					AppWidgetManager.getInstance(context).getAppWidgetIds(
							new ComponentName(Application.getAppContext(), ImageWidget.class));
		}
		else {
			ids = appWidgetId;
		}
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

}
