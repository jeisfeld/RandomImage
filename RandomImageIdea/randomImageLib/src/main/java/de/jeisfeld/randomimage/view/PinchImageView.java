package de.jeisfeld.randomimage.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import de.jeisfeld.randomimage.util.ImageUtil;
import de.jeisfeld.randomimage.util.SystemUtil;

/**
 * A view for displaying an image, allowing moving and resizing with pinching.
 */
public class PinchImageView extends ImageView {
	/**
	 * One half - the relative middle position.
	 */
	protected static final float ONE_HALF = 0.5f;

	/**
	 * The minimum scale factor allowed.
	 */
	private static final float MIN_SCALE_FACTOR = 0.1f;

	/**
	 * The maximum scale factor allowed.
	 */
	private static final float MAX_SCALE_FACTOR = 25f;

	/**
	 * The duration of the animation used in animateOut.
	 */
	private static final int ANIMATION_DURATION = SystemUtil.isTablet() ? 150 : 100; // MAGIC_NUMBER

	/**
	 * Pointer id used in case of invalid pointer.
	 */
	private static final int INVALID_POINTER_ID = -1;

	/**
	 * Indicator if the view is initialized with the image bitmap, i.e. the initial scaling has been done.
	 */
	private boolean mInitialized = false;

	/**
	 * Indicator if the view has been populated with the bitmap.
	 */
	private boolean mIsBitmapSet = false;

	/**
	 * Field used to check if a gesture was moving the image (then no context menu will appear).
	 */
	private boolean mHasMoved = false;

	/**
	 * Field used to check if the view was touched - only if touched, the size will be maintained later.
	 */
	private boolean mWasTouched = false;

	/**
	 * These are the relative positions of the Bitmap which are displayed in center of the screen. Range: [0,1]
	 */
	private float mPosX, mPosY;

	/**
	 * This is the scale factor of the image.
	 */
	private float mScaleFactor = 1.f;

	/**
	 * The last touch position.
	 */
	private float mLastTouchX, mLastTouchY;

	/**
	 * The last average touch position (used when pinching and moving at the same time).
	 */
	private float mLastTouchX0, mLastTouchY0;

	/**
	 * The primary pointer id.
	 */
	private int mActivePointerId = INVALID_POINTER_ID;

	/**
	 * The secondary pointer id.
	 */
	private int mActivePointerId2 = INVALID_POINTER_ID;

	/**
	 * A ScaleGestureDetector detecting the scale change.
	 */
	private ScaleGestureDetector mScaleDetector;

	/**
	 * An additional GestureDetector which may be applied.
	 */
	private GestureDetector mGestureDetector = null;

	/**
	 * An additional gesture detector for up/down movements which may be applied.
	 */
	private UpDownListener mUpDownListener = null;

	/**
	 * The path name of the displayed image.
	 */
	private String mPathName = null;

	/**
	 * The resource id of the displayed image.
	 */
	private int mImageResource = -1;

	/**
	 * The displayed bitmap.
	 */
	private Bitmap mBitmap = null;

	/**
	 * The maximum allowed resolution of the bitmap. The image is scaled to this size.
	 */
	private static int mMaxBitmapSize = ImageUtil.MAX_BITMAP_SIZE;

	/**
	 * The last scale factor.
	 */
	private float mLastScaleFactor = 1.f;

	/**
	 * The initial scale type to be used.
	 */
	private ScaleType mScaleType = ScaleType.FIT;

	/**
	 * The background color.
	 */
	private int mBackgroundColor;

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context)
	 */
	public PinchImageView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 * @see android.view.View#View(Context, AttributeSet)
	 */
	public PinchImageView(final Context context, final AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context  The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs    The attributes of the XML tag that is inflating the view.
	 * @param defStyle An attribute in the current theme that contains a reference to a style resource that supplies default
	 *                 values for the view. Can be 0 to not look for defaults.
	 * @see android.view.View#View(Context, AttributeSet, int)
	 */
	public PinchImageView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setScaleType(ImageView.ScaleType.MATRIX);
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}

	/**
	 * Reset the view - may be used in order to reuse the view with a new image.
	 */
	public void reset() {
		mInitialized = false;
		mIsBitmapSet = false;
		mHasMoved = false;
		mWasTouched = false;
		mScaleFactor = 1.f;
		mActivePointerId = INVALID_POINTER_ID;
		mActivePointerId2 = INVALID_POINTER_ID;
		mPathName = null;
		mImageResource = -1;
		mBitmap = null;
		mLastScaleFactor = 1.f;
	}

	/**
	 * Fill with an image, making the image fit into the view. If the pathName is unchanged (restored), then it is not
	 * refilled. The sizing (for fit) happens only once at first initialization of the view.
	 *
	 * @param pathName   The pathname of the image
	 * @param activity   The triggering activity (required for bitmap caching)
	 * @param cacheIndex A unique index of the view in the activity
	 */
	public final void setImage(final String pathName, final Activity activity, final int cacheIndex) {
		// retrieve bitmap from cache if possible
		final RetainFragment retainFragment = RetainFragment.findOrCreateRetainFragment(activity.getFragmentManager(), cacheIndex);
		if (!pathName.equals(retainFragment.getPathName())) {
			retainFragment.setBitmap(null);
			retainFragment.setPathName(pathName);
		}

		mBitmap = retainFragment.getBitmap();

		if (mBitmap == null) {
			final Handler handler = new Handler();
			// populate bitmaps in separate thread, so that screen keeps fluid.
			// This also ensures that this happens only after view is visible and sized.
			new Thread() {
				@Override
				public void run() {
					mBitmap = ImageUtil.getImageBitmap(pathName, mMaxBitmapSize);

					retainFragment.setBitmap(mBitmap);
					mPathName = pathName;
					handler.post(new Runnable() {
						@Override
						public void run() {
							PinchImageView.super.setImageBitmap(mBitmap);
							mIsBitmapSet = true;
							doInitialScaling();
						}
					});
				}
			}.start();
		}
		else {
			super.setImageBitmap(mBitmap);
			mPathName = pathName;
			mIsBitmapSet = true;
			doInitialScaling();
		}
	}

	/**
	 * Return the natural scale factor that fits the image into the view.
	 *
	 * @return The natural scale factor fitting the image into the view.
	 */
	private float getNaturalScaleFactor() {
		float heightFactor = 1f * getHeight() / mBitmap.getHeight();
		float widthFactor = 1f * getWidth() / mBitmap.getWidth();

		switch (mScaleType) {
		case STRETCH:
		case TURN_STRETCH:
			return Math.max(widthFactor, heightFactor);
		case HALF_SIZE:
			return Math.min(
					Math.min(widthFactor, heightFactor) * 0.9f, // MAGIC_NUMBER - ensure 5% border even on the bigger side.
					Math.max(widthFactor, heightFactor) * 0.6f // MAGIC_NUMBER - ensure that one dimension is only 60% of the page.
			);
		case FIT:
		case TURN_FIT:
		default:
			return Math.min(widthFactor, heightFactor);
		}
	}

	/**
	 * Return an orientation independent scale factor that fits the smaller image dimension into the smaller view
	 * dimension.
	 *
	 * @return A scale factor fitting the image independent of the orientation.
	 */
	protected final float getOrientationIndependentScaleFactor() {
		float viewSize = Math.min(getWidth(), getHeight());
		float imageSize = Math.min(mBitmap.getWidth(), mBitmap.getHeight());
		return 1f * viewSize / imageSize;
	}

	/**
	 * Scale the image to fit into the view, if not yet scaled before.
	 */
	public final void doInitialScaling() {
		if (mIsBitmapSet && !mInitialized) {
			doScalingToFit();
		}
	}

	/**
	 * Scale the image to fit into the view.
	 */
	public final void doScalingToFit() {
		if (mBitmap == null) {
			return;
		}
		mPosX = ONE_HALF;
		mPosY = ONE_HALF;
		rotateIfRequired();
		mScaleFactor = getNaturalScaleFactor();
		if (mScaleFactor > 0) {
			mInitialized = true;
			mLastScaleFactor = mScaleFactor;
			requestLayout();
			invalidate();
		}
	}

	/**
	 * Rotate the bitmap if requested and if it fits better into the view.
	 */
	private void rotateIfRequired() {
		if (mScaleType == ScaleType.TURN_FIT
				|| mScaleType == ScaleType.TURN_STRETCH) {
			int rotationAngle = getRotationAngle();
			if (rotationAngle != 0) {
				Matrix matrix = new Matrix();
				matrix.setRotate(rotationAngle);
				mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
				super.setImageBitmap(mBitmap);
			}
		}
	}

	/**
	 * Check if the image has the wrong aspect ratio for the view.
	 *
	 * @return The rotation angle to optimize the image view.
	 */
	private int getRotationAngle() {
		if (mBitmap.getWidth() > mBitmap.getHeight() && getWidth() < getHeight()) {
			return 90; // MAGIC_NUMBER
		}
		else if (mBitmap.getWidth() < mBitmap.getHeight() && getWidth() > getHeight()) {
			return -90; // MAGIC_NUMBER
		}
		else {
			return 0;
		}

	}

	/**
	 * Set the scale type.
	 *
	 * @param scaleType The scaletype, as defined in the preference resource array.
	 */
	public final void setScaleType(final ScaleType scaleType) {
		mScaleType = scaleType;
	}


	/**
	 * Animate the image out of the view.
	 *
	 * @param velocityX      The x velocity specifying the direction of animation.
	 * @param velocityY      The y velocity specifying the direction of animation.
	 * @param postActivities Activities to be done after the animation is finished.
	 */
	public final void animateOut(final float velocityX, final float velocityY, final Runnable postActivities) {
		float velocity = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
		if (velocity == 0) {
			postActivities.run();
			return;
		}

		float relativeX = velocityX / velocity;
		float relativeY = velocityY / velocity;

		final PropertyValuesHolder animPosX = PropertyValuesHolder.ofFloat("mPosX", mPosX, mPosX - 2 * relativeX);
		final PropertyValuesHolder animPosY = PropertyValuesHolder.ofFloat("mPosY", mPosY, mPosY - 2 * relativeY);

		final ObjectAnimator objectAnim = ObjectAnimator.ofPropertyValuesHolder(this, animPosX, animPosY);
		objectAnim.setDuration(ANIMATION_DURATION);

		objectAnim.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(final Animator animation) {
				// do nothing
			}

			@Override
			public void onAnimationRepeat(final Animator animation) {
				// do nothing
			}

			@Override
			public void onAnimationEnd(final Animator animation) {
				postActivities.run();
			}

			@Override
			public void onAnimationCancel(final Animator animation) {
				// do nothing
			}
		});

		final AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(objectAnim);

		animatorSet.start();
	}

	/**
	 * Set the X position. Required for animation. Should not be used otherwise.
	 *
	 * @param posX The new x position.
	 */
	@SuppressWarnings("unused")
	private void setMPosX(final float posX) {
		mPosX = posX;
		// Matrix is not set, as mPosY is set directly afterwards
	}

	/**
	 * Set the X position. Required for animation. Should not be used otherwise.
	 *
	 * @param posY The new y position.
	 */
	@SuppressWarnings("unused")
	private void setMPosY(final float posY) {
		mPosY = posY;
		setMatrix();
	}

	/**
	 * Set the maximum size in which a bitmap is held in memory.
	 *
	 * @param size the maximum size (pixels)
	 */
	public static void setMaxBitmapSize(final int size) {
		mMaxBitmapSize = size;
	}

	/**
	 * Redo the scaling.
	 */
	protected final void setMatrix() {
		if (mBitmap != null) {
			Matrix matrix = new Matrix();
			matrix.setTranslate(-mPosX * mBitmap.getWidth(), -mPosY * mBitmap.getHeight());
			matrix.postScale(mScaleFactor, mScaleFactor);
			matrix.postTranslate(getWidth() / 2.0f, getHeight() / 2.0f);
			setImageMatrix(matrix);
		}
	}

	/**
	 * Override requestLayout to reposition the image.
	 */
	@Override
	public final void requestLayout() {
		super.requestLayout();
		setMatrix();
	}

	@Override
	protected final void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mIsBitmapSet) {
			if (mInitialized) {
				requestLayout();
				invalidate();
			}
			else {
				doInitialScaling();
			}
		}
	}

	/*
	 * Method to do the scaling based on pinching.
	 */
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public final boolean onTouchEvent(final MotionEvent ev) {
		boolean isProcessed = false;
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);

		// If available, do the same for the Gesture Detector.
		if (mGestureDetector != null) {
			isProcessed = mGestureDetector.onTouchEvent(ev);
		}

		final int action = ev.getActionMasked();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (mUpDownListener != null) {
				mUpDownListener.onDown();
			}
			mHasMoved = false;
			mLastTouchX = ev.getX();
			mLastTouchY = ev.getY();
			mActivePointerId = ev.getPointerId(0);
			mWasTouched = true;
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			mHasMoved = true;
			if (ev.getPointerCount() == 2) {
				final int pointerIndex = ev.getActionIndex();
				mActivePointerId2 = ev.getPointerId(pointerIndex);
				mLastTouchX0 = (ev.getX(pointerIndex) + mLastTouchX) / 2;
				mLastTouchY0 = (ev.getY(pointerIndex) + mLastTouchY) / 2;
			}
			break;

		case MotionEvent.ACTION_MOVE:
			// Prevent NullPointerException if bitmap is not yet loaded
			try {
				boolean moved = handlePointerMove(ev);
				mHasMoved = mHasMoved || moved;
			}
			catch (RuntimeException e) {
				// ignore
			}
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mUpDownListener != null) {
				mUpDownListener.onUp();
			}
			if (!mHasMoved && !isProcessed) {
				isProcessed = super.performClick();
			}
			mHasMoved = false;
			mActivePointerId = INVALID_POINTER_ID;
			mActivePointerId2 = INVALID_POINTER_ID;
			break;

		case MotionEvent.ACTION_POINTER_UP:
			final int pointerIndex = ev.getActionIndex();
			final int pointerId = ev.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastTouchX = ev.getX(newPointerIndex);
				mLastTouchY = ev.getY(newPointerIndex);
				mActivePointerId = ev.getPointerId(newPointerIndex);
				if (mActivePointerId == mActivePointerId2) {
					mActivePointerId2 = INVALID_POINTER_ID;
				}
			}
			else if (pointerId == mActivePointerId2) {
				mActivePointerId2 = INVALID_POINTER_ID;
			}
			break;

		default:
			break;

		}

		return isProcessed || !isLongClickable() || super.onTouchEvent(ev);
	}

	/*
	 * Perform long click only if no move has happened.
	 */
	@Override
	public final boolean performLongClick() {
		return mHasMoved || super.performLongClick();
	}

	/**
	 * Utility method to make the calculations in case of a pointer move.
	 *
	 * @param ev The motion event.
	 * @return true if a move has been made (i.e. the position of the image changed).
	 */
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "FE_FLOATING_POINT_EQUALITY",
			justification = "Using floating point equality to see if value has changed")
	protected final boolean handlePointerMove(final MotionEvent ev) {
		if (!mInitialized) {
			return false;
		}

		boolean moved = false;
		final int pointerIndex = ev.findPointerIndex(mActivePointerId);
		final float x = ev.getX(pointerIndex);
		final float y = ev.getY(pointerIndex);

		if (mActivePointerId2 == INVALID_POINTER_ID) {
			// Only move if the ScaleGestureDetector isn't processing a gesture.
			final float dx = x - mLastTouchX;
			final float dy = y - mLastTouchY;
			mPosX -= dx / mScaleFactor / mBitmap.getWidth();
			mPosY -= dy / mScaleFactor / mBitmap.getHeight();
		}
		else {
			// When resizing, move according to the center of the two pinch points
			final int pointerIndex2 = ev.findPointerIndex(mActivePointerId2);
			final float x0 = (ev.getX(pointerIndex2) + x) / 2;
			final float y0 = (ev.getY(pointerIndex2) + y) / 2;
			final float dx = x0 - mLastTouchX0;
			final float dy = y0 - mLastTouchY0;
			mPosX -= dx / mScaleFactor / mBitmap.getWidth();
			mPosY -= dy / mScaleFactor / mBitmap.getHeight();
			if (mScaleFactor != mLastScaleFactor) {
				// When resizing, then position also changes
				final float changeFactor = mScaleFactor / mLastScaleFactor;
				mPosX = mPosX + (x0 - getWidth() / 2.0f) * (changeFactor - 1) / mScaleFactor / mBitmap.getWidth();
				mPosY = mPosY + (y0 - getHeight() / 2.0f) * (changeFactor - 1) / mScaleFactor / mBitmap.getHeight();
				mLastScaleFactor = mScaleFactor;
				moved = true;
			}
			mLastTouchX0 = x0;
			mLastTouchY0 = y0;
		}

		if (x != mLastTouchX || y != mLastTouchY) {
			mLastTouchX = x;
			mLastTouchY = y;
			moved = true;
		}

		// setMatrix invalidates if matrix is changed.
		setMatrix();

		return moved;
	}

	public final void setGestureDetector(final GestureDetector gestureDetector) {
		this.mGestureDetector = gestureDetector;
	}

	public final void setUpDownListener(final UpDownListener upDownListener) {
		this.mUpDownListener = upDownListener;
	}

	/*
	 * Save scale factor, center position, path name and bitmap. (Bitmap to be retained if the view is recreated with
	 * same pathname.)
	 */
	@Override
	protected final Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putFloat("mScaleFactor", mScaleFactor);
		bundle.putFloat("mPosX", mPosX);
		bundle.putFloat("mPosY", mPosY);
		bundle.putString("mPathName", mPathName);
		bundle.putInt("mImageResource", mImageResource);
		bundle.putBoolean("mInitialized", mInitialized);
		bundle.putBoolean("mWasTouched", mWasTouched);
		return bundle;
	}

	@Override
	protected final void onRestoreInstanceState(final Parcelable state) {
		Parcelable enhancedState = state;
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			mPathName = bundle.getString("mPathName");
			mImageResource = bundle.getInt("mImageResource");
			mWasTouched = bundle.getBoolean("mWasTouched");
			if (mWasTouched) {
				mInitialized = bundle.getBoolean("mInitialized");
				mScaleFactor = bundle.getFloat("mScaleFactor");
				mLastScaleFactor = bundle.getFloat("mScaleFactor");
				mPosX = bundle.getFloat("mPosX");
				mPosY = bundle.getFloat("mPosY");
			}
			enhancedState = bundle.getParcelable("instanceState");
		}
		super.onRestoreInstanceState(enhancedState);
	}

	public final int getBackgroundColor() {
		return mBackgroundColor;
	}

	@Override
	public final void setBackgroundColor(final int backgroundColor) {
		this.mBackgroundColor = backgroundColor;
		super.setBackgroundColor(backgroundColor);
	}

	/**
	 * A listener determining the scale factor.
	 */
	protected class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public final boolean onScale(final ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();
			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(mScaleFactor, MAX_SCALE_FACTOR));
			invalidate();
			return true;
		}
	}

	/**
	 * Helper listFoldersFragment to retain the bitmap on configuration change.
	 */
	public static class RetainFragment extends Fragment {
		/**
		 * Tag to be used as identifier of the fragment.
		 */
		private static final String TAG = "RetainFragment";
		/**
		 * The bitmap to be stored.
		 */
		private Bitmap mRetainBitmap;

		public final Bitmap getBitmap() {
			return mRetainBitmap;
		}

		public final void setBitmap(final Bitmap bitmap) {
			this.mRetainBitmap = bitmap;
		}

		/**
		 * The pathName of this bitmap.
		 */
		private String mRetainPathName;

		public final String getPathName() {
			return mRetainPathName;
		}

		public final void setPathName(final String pathName) {
			this.mRetainPathName = pathName;
		}

		/**
		 * Get the retainFragment - search it by the index. If not found, create a new one.
		 *
		 * @param fm    The fragment manager handling this fragment.
		 * @param index The index of the view (required in case of multiple PinchImageViews to be retained).
		 * @return the retainFragment.
		 */
		public static RetainFragment findOrCreateRetainFragment(final FragmentManager fm, final int index) {
			RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG + index);
			if (fragment == null) {
				fragment = new RetainFragment();
				fm.beginTransaction().add(fragment, TAG + index).commitAllowingStateLoss();
			}
			return fragment;
		}

		@Override
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
	}

	/**
	 * Callback for up/down gestures.
	 */
	public interface UpDownListener {
		/**
		 * Callback for tapping down.
		 */
		void onDown();

		/**
		 * Callback for going up again.
		 */
		void onUp();
	}


	/**
	 * The way in which the image is initially scaled.
	 */
	public enum ScaleType {
		/**
		 * Fit into window, keeping orientation.
		 */
		FIT,
		/**
		 * Stretch to fill window, keeping orientation.
		 */
		STRETCH,
		/**
		 * Fit into window, optimizing orientation.
		 */
		TURN_FIT,
		/**
		 * Stretch to fill window, optimizing orientation.
		 */
		TURN_STRETCH,
		/**
		 * Put it at random position within the view at half the possible size.
		 */
		HALF_SIZE;

		/**
		 * Get the scale type from the scaleType value as defined in the preference resource array.
		 *
		 * @param resourceScaleType The scale type, as defined in the preference resource array.
		 * @return The corresponding ScaleType.
		 */
		public static ScaleType fromResourceScaleType(final int resourceScaleType) {
			switch (resourceScaleType) {
			case 0:
				return FIT;
			case 1:
				return STRETCH;
			case 2:
				return TURN_FIT;
			case 3: // MAGIC_NUMBER
				return TURN_STRETCH;
			default:
				return FIT;
			}
		}
	}
}
