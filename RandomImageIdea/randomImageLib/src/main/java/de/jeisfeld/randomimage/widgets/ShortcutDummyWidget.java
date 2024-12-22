package de.jeisfeld.randomimage.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import de.jeisfeld.randomimage.util.PreferenceUtil;
import de.jeisfeld.randomimagelib.R;

/**
 * The base widget, allowing to open the app for a specific list.
 */
public class ShortcutDummyWidget extends GenericWidget {
	@Override
	public final void onUpdateWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId,
									 final UpdateType updateType) {
		// do nothing
	}

	@Override
	public final void onDeleted(final Context context, final int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		for (int appWidgetId : appWidgetIds) {
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_timeout, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_allowed_call_frequency, appWidgetId);
			PreferenceUtil.removeIndexedSharedPreference(R.string.key_widget_icon_image, appWidgetId);
		}
	}

	/**
	 * Check if there is a ShortcutDummyWidget of this id.
	 *
	 * @param appWidgetId The widget id.
	 * @return true if there is a ShortcutDummyWidget of this id.
	 */
	public static boolean hasWidgetOfId(final int appWidgetId) {
		return hasWidgetOfId(ShortcutDummyWidget.class, appWidgetId);
	}

	@Override
	protected final Class<? extends GenericWidgetConfigurationActivity> getConfigurationActivityClass() {
		return null;
	}
}
