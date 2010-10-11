/*
 * Copyright 2010 mapsforge.org
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * Custom implementation of the ItemizedOverlay class from the google maps library.
 * 
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public abstract class ItemizedOverlay extends Overlay {
	private Bitmap bitmap;
	private Canvas bitmapWrapper;
	private Drawable defaultMarker;
	private Point displayPositonAfterDrawing;
	private Point displayPositonBeforeDrawing;
	private OverlayItem item;
	private Drawable itemMarker;
	private Point itemPixelPositon;
	private Point itemPosOnDisplay;
	private Matrix matrix;
	private Bitmap shaddowBitmap;
	private Bitmap tempBitmapForSwap;

	/**
	 * Construct an Overlay
	 * 
	 * @param defaultMarker
	 *            the default drawable for each item in the overlay.
	 */
	public ItemizedOverlay(Drawable defaultMarker) {
		this.defaultMarker = defaultMarker;
		setup();
	}

	/**
	 * Add an overlayItem to this overlay.
	 * 
	 * @param overlayItem
	 *            the new overlay item.
	 */
	public abstract void addOverLay(OverlayItem overlayItem);

	@Override
	public void draw(Canvas canvas, MapView mapview, boolean shadow) {
		canvas.drawBitmap(this.bitmap, this.matrix, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapview) {
		// iterate over all overlay items
		for (int i = 0; i < size(); i++) {
			this.item = createItem(i);
			if (hitTest(this.item, this.item.getMarker(), (int) event.getX(), (int) event
					.getY())) {
				onTap(i);
				return true;
			}
		}
		return true;
	}

	/**
	 * Pause the Thread.
	 * 
	 * @param pauseInSeconds
	 *            time in seconds to sleep.
	 */
	public void pause(int pauseInSeconds) {
		try {
			Thread.sleep(pauseInSeconds * 1000);
		} catch (InterruptedException e) {
			// restore the interrupted status
			interrupt();
			Logger.e(new Exception("Not Implemented"));
		}
	}

	/**
	 * Return the numbers of items.
	 * 
	 * @return numbers of items in this overlay.
	 */
	abstract public int size();

	private Point calculateDisplayPoint(GeoPoint geoPoint) {
		return new Point((float) MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(),
				this.internalMapView.zoomLevel) - this.internalMapView.getWidth() / 2,
				(float) MercatorProjection.latitudeToPixelY(geoPoint.getLatitude(),
						this.internalMapView.zoomLevel) - this.internalMapView.getHeight() / 2);
	}

	private Point calculateItemPoint(GeoPoint geoPoint) {
		return new Point((float) MercatorProjection.longitudeToPixelX(geoPoint.getLongitude(),
				this.internalMapView.zoomLevel), (float) MercatorProjection.latitudeToPixelY(
				geoPoint.getLatitude(), this.internalMapView.zoomLevel));
	}

	private Point calculateItemPostionRelativeToDisplay(GeoPoint itemPostion) {
		Point itemPixelPosition = calculateItemPoint(itemPostion);
		Point displayPixelPosition = calculateDisplayPoint(new GeoPoint(
				this.internalMapView.latitude,
				this.internalMapView.longitude));
		Point distance = Point.substract(itemPixelPosition, displayPixelPosition);
		return distance;
	}

	private void drawItem(OverlayItem currentItem) {
		if (hasValidDisplayPosition(currentItem)) {
			this.itemPixelPositon = currentItem.posOnDisplay;
		} else {
			currentItem.posOnDisplay = calculateItemPoint(currentItem.getPoint());
			this.itemPixelPositon = currentItem.posOnDisplay;
			currentItem.zoomLevel = this.internalMapView.zoomLevel;
		}
		this.itemPosOnDisplay = Point.substract(this.itemPixelPositon,
				this.displayPositonBeforeDrawing);
		setCostumOrDeaultItemMarker(currentItem);
		if (isItemOnDisplay(this.itemPosOnDisplay)) {
			boundCenter(this.itemMarker, this.itemPosOnDisplay).draw(this.bitmapWrapper);
		}
	}

	private void drawItemsOnShaddowBitmap() {
		this.shaddowBitmap.eraseColor(Color.TRANSPARENT);
		this.bitmapWrapper.setBitmap(this.shaddowBitmap);
		for (int i = 0; i < size(); i++) {
			drawItem(createItem(i));
		}
	}

	private boolean hasValidDisplayPosition(OverlayItem currentItem) {
		boolean displayPositionValid = true;
		displayPositionValid &= (this.internalMapView.zoomLevel == currentItem.zoomLevel);
		return displayPositionValid;
	}

	private boolean isItemOnDisplay(Point itemPos) {
		boolean isOnDisplay = true;
		isOnDisplay &= itemPos.x > 0;
		isOnDisplay &= itemPos.x < this.bitmap.getWidth();
		isOnDisplay &= itemPos.y > 0;
		isOnDisplay &= itemPos.y < this.bitmap.getHeight();
		return isOnDisplay;
	}

	private void notifyMapViewToRedraw() {
		this.internalMapView.postInvalidate();
	}

	private void saveDisplayPositionAfterDrawing() {
		this.displayPositonAfterDrawing = calculateDisplayPoint(new GeoPoint(
				this.internalMapView.latitude, this.internalMapView.longitude));
	}

	private void saveDisplayPositionBeforeDrawing() {
		this.displayPositonBeforeDrawing = calculateDisplayPoint(new GeoPoint(
				this.internalMapView.latitude, this.internalMapView.longitude));
	}

	private void setCostumOrDeaultItemMarker(OverlayItem item) {
		if (item.getMarker() == null) {
			this.itemMarker = this.defaultMarker;
			item.setMarker(this.defaultMarker, 0);
		} else {
			this.itemMarker = item.getMarker();
		}
	}

	private void setup() {
		this.matrix = new Matrix();
		this.start();
	}

	private void swapBitmapAndCorrectMatrix(Point displayPosBefore, Point displayPosAfter) {
		synchronized (this.matrix) {
			this.matrix.reset();
			Point diff = Point.substract(displayPosBefore, displayPosAfter);
			this.matrix.postTranslate(diff.x, diff.y);
			// swap the two bitmaps
			this.tempBitmapForSwap = this.bitmap;
			this.bitmap = this.shaddowBitmap;
			this.shaddowBitmap = this.tempBitmapForSwap;
		}
	}

	/**
	 * Adjusts a drawable of an item so that (0,0) is the center.
	 * 
	 * @param balloon
	 *            the drawable to center.
	 * @param itemPosRelative
	 *            the position of the item.
	 * @return the adjusted drawable.
	 */
	protected Drawable boundCenter(Drawable balloon, Point itemPosRelative) {
		balloon.setBounds((int) itemPosRelative.x - balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y - balloon.getIntrinsicHeight() / 2,
				(int) itemPosRelative.x + balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y + balloon.getIntrinsicHeight() / 2);
		return balloon;
	}

	/**
	 * Adjusts the drawable of an item so that (0,0) is the center of the bottom row.
	 * 
	 * @param balloon
	 *            the drawable to center.
	 * @param itemPosRelative
	 *            the position of the item.
	 * @return the adjusted drawable.
	 */
	protected Drawable boundCenterBottom(Drawable balloon, Point itemPosRelative) {
		balloon.setBounds((int) itemPosRelative.x - balloon.getIntrinsicWidth() / 2,
				(int) itemPosRelative.y - balloon.getIntrinsicHeight(), (int) itemPosRelative.x
						+ balloon.getIntrinsicWidth() / 2, (int) itemPosRelative.y);
		return balloon;
	}

	/**
	 * Access and create the actual Items.
	 * 
	 * @param i
	 *            the index of the item.
	 * @return the overlay item.
	 */
	abstract protected OverlayItem createItem(int i);

	@Override
	final protected void createOverlayBitmapsAndCanvas(int width, int height) {
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.shaddowBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.bitmapWrapper = new Canvas();
	}

	@Override
	protected Matrix getMatrix() {
		return this.matrix;
	}

	/**
	 * Calculate if a given point is within the bounds of an item.
	 * 
	 * @param currentItem
	 *            the item to test.
	 * @param marker
	 *            the marker of the item.
	 * @param hitX
	 *            the x-coordinate of the point.
	 * @param hitY
	 *            the y-coordinate of the point.
	 * @return true if the point is within the bounds of the item.
	 */
	protected boolean hitTest(OverlayItem currentItem, Drawable marker, int hitX, int hitY) {
		Point eventPos = new Point(hitX, hitY);
		Point itemHitPosOnDisplay = calculateItemPostionRelativeToDisplay(this.item.getPoint());
		Point distance = Point.substract(eventPos, itemHitPosOnDisplay);
		if (marker == null) {
			marker = this.defaultMarker;
		}
		if (Math.abs(distance.x) < marker.getIntrinsicWidth() / 2
				&& Math.abs(distance.y) < marker.getIntrinsicHeight() / 2) {
			return true;
		}
		return false;
	}

	/**
	 * Handle a tap event.
	 * 
	 * @param index
	 *            the position of the item.
	 * 
	 * @return true
	 */
	abstract protected boolean onTap(int index);

	@Override
	final protected void prepareOverlayBitmap(MapView mapview) {
		saveDisplayPositionBeforeDrawing();
		drawItemsOnShaddowBitmap();
		saveDisplayPositionAfterDrawing();
		swapBitmapAndCorrectMatrix(this.displayPositonBeforeDrawing,
				this.displayPositonAfterDrawing);
		notifyMapViewToRedraw();
	}
}