package de.jeisfeld.randomimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

/**
 * Array adapter class to display an eye photo pair in a list.
 */
public class DeleteImagesArrayAdapter extends ArrayAdapter<String> {
	/**
	 * The activity holding this adapter.
	 */
	private final DeleteImagesActivity activity;

	/**
	 * The names of the files.
	 */
	private String[] fileNames;

	/**
	 * The set of filenames selected for deletion.
	 */
	private Set<String> selectedFileNames = new HashSet<String>();

	/**
	 * Constructor for the adapter.
	 *
	 * @param activity
	 *            The activity using the adapter.
	 * @param fileNames
	 *            The names of files to be displayed.
	 */
	public DeleteImagesArrayAdapter(final DeleteImagesActivity activity, final String[] fileNames) {
		super(activity, R.layout.text_view_initializing, fileNames);
		this.activity = activity;
		this.fileNames = fileNames;
	}

	/**
	 * Default adapter to be used by the framework.
	 *
	 * @param context
	 *            The Context the view is running in.
	 */
	public DeleteImagesArrayAdapter(final Context context) {
		super(context, R.layout.text_view_initializing);
		this.activity = (DeleteImagesActivity) context;
	}

	/*
	 * Fill the display of the view (date and pictures) Details on selection are handled within the
	 * TwoImageSelectionHandler class.
	 */
	@Override
	public final View getView(final int position, final View convertView, final ViewGroup parent) {
		final String fileName = fileNames[position];

		final ThumbImageView thumbImageView;
		if (convertView != null && convertView instanceof ImageView) {
			thumbImageView = (ThumbImageView) convertView;
			thumbImageView.cleanImage();
		}
		else {
			thumbImageView = (ThumbImageView) LayoutInflater.from(activity).inflate(R.layout.adapter_delete_images,
					parent, false);
		}

		thumbImageView.setImage(activity, fileName, new Runnable() {
			@Override
			public void run() {
				if (selectedFileNames.contains(fileName)) {
					thumbImageView.setMarked(true);
				}
			}
		});

		thumbImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				ThumbImageView view = (ThumbImageView) v;

				if (view.isMarked()) {
					view.setMarked(false);
					selectedFileNames.remove(view.getFileName());

				}
				else {
					view.setMarked(true);
					selectedFileNames.add(view.getFileName());
				}
			}
		});

		return thumbImageView;
	}

	/**
	 * Get the list of selected files.
	 *
	 * @return The selected files.
	 */
	public final String[] getSelectedFiles() {
		return new ArrayList<String>(selectedFileNames).toArray(new String[0]);
	}

	/**
	 * Set the list of selected files.
	 *
	 * @param selectedFiles
	 *            The names of the files.
	 */
	public final void setSelectedFiles(final String[] selectedFiles) {
		if (selectedFiles == null) {
			selectedFileNames.clear();
		}
		else {
			selectedFileNames = new HashSet<String>(Arrays.asList(selectedFiles));
		}
	}

}
