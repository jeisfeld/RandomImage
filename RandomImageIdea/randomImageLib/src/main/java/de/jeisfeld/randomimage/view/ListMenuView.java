package de.jeisfeld.randomimage.view;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.jeisfeld.randomimagelib.R;

/**
 * A ListView that is used like a Menu.
 */
public class ListMenuView extends ListView {
	/**
	 * The ids of resources defining the list entries.
	 */
	private final ArrayList<Integer> mResourceIds = new ArrayList<>();

	/**
	 * The listeners for the list entries.
	 */
	private final ArrayList<OnClickListener> mListeners = new ArrayList<>();

	/**
	 * A map from resourceId to its position in the list.
	 */
	private final SparseIntArray mResourcePositions = new SparseIntArray();

	/**
	 * The entries in the list. Key is the list position!
	 */
	private final ArrayList<String> mListValues = new ArrayList<>();

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context)
	 */
	public ListMenuView(final Context context) {
		this(context, null, 0);
	}

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 * @see android.view.View#View(Context, AttributeSet)
	 */
	public ListMenuView(final Context context, final AttributeSet attrs) {
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
	public ListMenuView(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				OnClickListener listener = mListeners.get((int) id);
				if (listener != null) {
					listener.onClick(view);
				}
			}
		});
	}

	/**
	 * Add an item to the list.
	 *
	 * @param resourceId The resourceId of the added item.
	 * @param listener   A listener to be called when the item is clicked.
	 */
	public final void addItem(final int resourceId, final OnClickListener listener) {
		if (!mResourceIds.contains(resourceId)) {
			mResourceIds.add(resourceId);
			mResourcePositions.put(resourceId, mResourceIds.size() - 1);
			mListValues.add(getContext().getString(resourceId));
		}
		// If resourceId is already there, just replace the listener.
		mListeners.add(listener);
	}

	/**
	 * Remove the item of a given id.
	 *
	 * @param resourceId The resourceId of the item to be removed.
	 */
	public final void removeItem(final int resourceId) {
		if (mResourceIds.contains(resourceId)) {
			int position = mResourcePositions.get(resourceId);
			mListeners.remove(position);
			mResourceIds.remove(position);
			mListValues.remove(position);
			mResourcePositions.delete(resourceId);
		}
	}

	/**
	 * Set the adapter of the list (either first time or update).
	 */
	public final void setAdapter() {
		ArrayAdapter<String> directoryListAdapter =
				new ArrayAdapter<>(getContext(), R.layout.adapter_list_names, mListValues);
		setAdapter(directoryListAdapter);
	}

}
