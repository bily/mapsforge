/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps;

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * ArrayItemizedOverlay is a thread-safe implementation of the {@link ItemizedOverlay} class
 * using an {@link ArrayList} as internal data structure. A default marker for all
 * {@link OverlayItem OverlayItems} without an individual marker can be defined via the
 * constructor.
 * <p>
 * The ArrayItemizedOverlay handles tap events on OverlayItems by displaying their title and
 * description in an {@link AlertDialog}. This behavior can easily be changed by overriding the
 * {@link #onTap} method.
 */
public class ArrayItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private static final int ARRAY_LIST_INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ArrayItemizedOverlay";

	private final Context context;
	private AlertDialog.Builder dialog;
	private OverlayItem item;
	private final ArrayList<OverlayItem> overlayItems;

	/**
	 * Constructs a new ArrayItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null). This marker is aligned to the center of its
	 *            bottom line to allow for conical symbols such as a pin or a needle.
	 * @param context
	 *            the reference to the application context.
	 */
	public ArrayItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker == null ? null : boundCenterBottom(defaultMarker));
		this.context = context;
		this.overlayItems = new ArrayList<OverlayItem>(ARRAY_LIST_INITIAL_CAPACITY);
	}

	/**
	 * Adds the given item to the overlay.
	 * 
	 * @param overlayItem
	 *            the item that should be added to the overlay.
	 */
	public void addOverlay(OverlayItem overlayItem) {
		synchronized (this.overlayItems) {
			this.overlayItems.add(overlayItem);
		}
		populate();
	}

	/**
	 * Adds all items of the given collection to the overlay.
	 * 
	 * @param c
	 *            collection whose items should be added to the overlay.
	 */
	public void addOverlays(Collection<? extends OverlayItem> c) {
		synchronized (this.overlayItems) {
			this.overlayItems.addAll(c);
		}
		populate();
	}

	/**
	 * Removes all items from the overlay.
	 */
	public void clear() {
		synchronized (this.overlayItems) {
			this.overlayItems.clear();
		}
		populate();
	}

	@Override
	public String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Removes the given item from the overlay.
	 * 
	 * @param overlayItem
	 *            the item that should be removed from the overlay.
	 */
	public void removeOverlay(OverlayItem overlayItem) {
		synchronized (this.overlayItems) {
			this.overlayItems.remove(overlayItem);
		}
		populate();
	}

	@Override
	public int size() {
		synchronized (this.overlayItems) {
			return this.overlayItems.size();
		}
	}

	@Override
	protected OverlayItem createItem(int i) {
		synchronized (this.overlayItems) {
			return this.overlayItems.get(i);
		}
	}

	@Override
	protected boolean onTap(int index) {
		synchronized (this.overlayItems) {
			this.item = this.overlayItems.get(index);
			this.dialog = new AlertDialog.Builder(this.context);
			this.dialog.setTitle(this.item.getTitle());
			this.dialog.setMessage(this.item.getSnippet());
			this.dialog.show();
			return true;
		}
	}
}