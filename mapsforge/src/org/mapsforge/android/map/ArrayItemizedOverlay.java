package org.mapsforge.android.map;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * Implementation of the Overlay class using an arrayList as data-structure.
 * 
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public class ArrayItemizedOverlay extends ItemizedOverlay {

	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context context;

	/**
	 * Constructs an overlay.
	 * 
	 * @param defaultMarker
	 *            the default marker.
	 * @param context
	 *            the context for the alert-dialog.
	 */
	public ArrayItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.context = context;
	}

	@Override
	public void addOverLay(OverlayItem overlayItem) {
		this.mOverlays.add(overlayItem);
	}

	@Override
	protected OverlayItem createItem(int i) {
		return this.mOverlays.get(i);
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = this.mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(this.context);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return false;
	}

	@Override
	public int size() {
		return this.mOverlays.size();
	}
}
