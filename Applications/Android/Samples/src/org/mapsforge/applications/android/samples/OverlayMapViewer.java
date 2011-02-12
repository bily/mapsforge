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
package org.mapsforge.applications.android.samples;

import org.mapsforge.android.maps.ArrayItemizedOverlay;
import org.mapsforge.android.maps.CircleOverlay;
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.OverlayItem;
import org.mapsforge.android.maps.RouteOverlay;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * An application which demonstrates how to use different types of Overlays.
 */
public class OverlayMapViewer extends MapActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setMapFile("/sdcard/berlin.map");
		setContentView(mapView);

		// create some points to be shown on top of the map
		GeoPoint geoPoint1 = new GeoPoint(52.514446, 13.350150); // Berlin Victory Column
		GeoPoint geoPoint2 = new GeoPoint(52.516272, 13.377722); // Brandenburg Gate
		GeoPoint geoPoint3 = new GeoPoint(52.525, 13.369444); // Berlin Central Station

		// create the paint objects for the CircleOverlay and set all parameters
		Paint circleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleFillPaint.setStyle(Paint.Style.FILL);
		circleFillPaint.setColor(Color.BLUE);
		circleFillPaint.setAlpha(64);

		Paint circleOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleOutlinePaint.setStyle(Paint.Style.STROKE);
		circleOutlinePaint.setColor(Color.BLUE);
		circleOutlinePaint.setAlpha(128);
		circleOutlinePaint.setStrokeWidth(3);

		// create the CircleOverlay and set the parameters
		CircleOverlay circleOverlay = new CircleOverlay(circleFillPaint, circleOutlinePaint);
		circleOverlay.setCircleData(geoPoint3, 250);

		// create the paint objects for the RouteOverlay and set all parameters
		Paint routePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
		routePaint1.setStyle(Paint.Style.STROKE);
		routePaint1.setColor(Color.BLUE);
		routePaint1.setAlpha(160);
		routePaint1.setStrokeWidth(7);
		routePaint1.setStrokeCap(Paint.Cap.BUTT);
		routePaint1.setStrokeJoin(Paint.Join.ROUND);
		routePaint1.setPathEffect(new DashPathEffect(new float[] { 20, 20 }, 0));

		Paint routePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		routePaint2.setStyle(Paint.Style.STROKE);
		routePaint2.setColor(Color.BLUE);
		routePaint2.setAlpha(96);
		routePaint2.setStrokeWidth(7);
		routePaint2.setStrokeCap(Paint.Cap.BUTT);
		routePaint2.setStrokeJoin(Paint.Join.ROUND);

		// create the RouteOverlay and set the way nodes
		RouteOverlay routeOverlay = new RouteOverlay(routePaint1, routePaint2);
		routeOverlay.setRouteData(new GeoPoint[] { geoPoint1, geoPoint2 });

		// create the ItemizedOverlay with a default marker and set the items
		Drawable defaultMarker = getResources().getDrawable(R.drawable.marker);
		ArrayItemizedOverlay itemizedOverlay = new ArrayItemizedOverlay(defaultMarker, this);

		OverlayItem item1 = new OverlayItem(geoPoint1, "Berlin Victory Column",
				"The Victory Column is a monument in Berlin, Germany.");
		OverlayItem item2 = new OverlayItem(geoPoint2, "Brandenburg Gate",
				"The Brandenburg Gate is one of the main symbols of Berlin and Germany.");

		itemizedOverlay.addOverlay(item1);
		itemizedOverlay.addOverlay(item2);

		// add all Overlays to the MapView
		mapView.getOverlays().add(circleOverlay);
		mapView.getOverlays().add(routeOverlay);
		mapView.getOverlays().add(itemizedOverlay);
	}
}