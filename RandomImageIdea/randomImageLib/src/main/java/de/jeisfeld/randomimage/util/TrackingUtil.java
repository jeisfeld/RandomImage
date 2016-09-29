package de.jeisfeld.randomimage.util;

import com.google.android.gms.analytics.HitBuilders.EventBuilder;
import com.google.android.gms.analytics.HitBuilders.ScreenViewBuilder;
import com.google.android.gms.analytics.HitBuilders.TimingBuilder;

import de.jeisfeld.randomimage.Application;

/**
 * Utility class for sending Google Analytics Events.
 */
public final class TrackingUtil {

	/**
	 * Hide default constructor.
	 */
	private TrackingUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Send a screen opening event.
	 *
	 * @param object The activity or fragment showing the screen.
	 */
	public static void sendScreen(final Object object) {
		Application.getAppTracker().setScreenName(object.getClass().getSimpleName());
		Application.getAppTracker().send(new ScreenViewBuilder().build());
	}

	/**
	 * Start a new session.
	 */
	public static void startSession() {
		Application.getAppTracker().send(new ScreenViewBuilder().setNewSession().build());
	}

	/**
	 * Send a specific event.
	 *
	 * @param category The event category.
	 * @param action   The action.
	 * @param label    The label.
	 */
	public static void sendEvent(final Category category, final String action, final String label) {
		EventBuilder eventBuilder = new EventBuilder();
		eventBuilder.setCategory(category.toString());
		if (action != null) {
			eventBuilder.setAction(action);
		}
		if (label != null) {
			eventBuilder.setLabel(label);
		}
		Application.getAppTracker().send(eventBuilder.build());
	}

	/**
	 * Send timing information.
	 *
	 * @param category The event category.
	 * @param label    The label.
	 * @param duration The duration.
	 */
	public static void sendTiming(final Category category, final String label, final long duration) {
		TimingBuilder timingBuilder = new TimingBuilder();
		timingBuilder.setCategory(category.toString());
		timingBuilder.setLabel(label);
		timingBuilder.setValue(duration);
		Application.getAppTracker().send(timingBuilder.build());
	}


	/**
	 * Categories of app events.
	 */
	public enum Category {
		/**
		 * Setup activities of the user.
		 */
		SETUP,
		/**
		 * Image viewing activities of the user.
		 */
		VIEW,
		/**
		 * Background jobs of the app.
		 */
		BACKGROUND
	}
}
