package de.jeisfeld.randomimage;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * A ListView that is used like a Menu.
 */
public class ListMenu extends ListView {
	/**
	 * The ids of resources defining the list entries.
	 */
	private final ArrayList<Integer> resourceIds = new ArrayList<Integer>();

	/**
	 * The listeners for the list entries.
	 */
	private final ArrayList<OnClickListener> listeners = new ArrayList<OnClickListener>();

	/**
	 * A map from resourceId to its position in the list.
	 */
	private final SparseIntArray resourcePositions = new SparseIntArray();

	/**
	 * The entries in the list. Key is the list position!
	 */
	private final ArrayList<String> listValues = new ArrayList<String>();

	/**
	 * The listener reacting on clicks on the list items.
	 */
	private final OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
			OnClickListener listener = listeners.get((int) id);
			if (listener != null) {
				listener.onClick(view);
			}
		}
	};

	/**
	 * Standard constructor to be implemented for all views.
	 *
	 * @param context
	 *            The Context the view is running in, through which it can access the current theme, resources, etc.
	 * @see android.view.View#View(Context)
	 */
	public ListMenu(final Context context) {
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
	public ListMenu(final Context context, final AttributeSet attrs) {
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
	public ListMenu(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		setOnItemClickListener(onItemClickListener);
		setAdapter();
	}

	/**
	 * Add an item to the list.
	 *
	 * @param resourceId
	 *            The resourceId of the added item.
	 * @param listener
	 *            A listener to be called when the item is clicked.
	 */
	public final void addItem(final int resourceId, final OnClickListener listener) {
		if (!resourceIds.contains(resourceId)) {
			resourceIds.add(resourceId);
			resourcePositions.put(resourceId, resourceIds.size() - 1);
			listValues.add(getContext().getString(resourceId));
		}
		// If resourceId is already there, just replace the listener.
		listeners.add(listener);
	}

	/**
	 * Remove the item of a given id.
	 *
	 * @param resourceId
	 *            The resourceId of the item to be removed.
	 */
	public final void removeItem(final int resourceId) {
		if (resourceIds.contains(resourceId)) {
			int position = resourcePositions.get(resourceId);
			listeners.remove(position);
			resourceIds.remove(position);
			listValues.remove(position);
			resourcePositions.delete(resourceId);
		}
	}

	/**
	 * Set the adapter of the list (either first time or update).
	 */
	private void setAdapter() {
		ArrayAdapter<String> directoryListAdapter =
				new ArrayAdapter<String>(getContext(), R.layout.adapter_list_names, listValues);
		setAdapter(directoryListAdapter);
	}

}
