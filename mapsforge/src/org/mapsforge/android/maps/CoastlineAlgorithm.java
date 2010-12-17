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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Class to generate closed polygons from disjoint coastline segments. The algorithm is based on
 * the close-areas.pl script, written by Frederik Ramm for the Osmarender program. This
 * implementation is optimized for high performance and memory reusing.
 */
class CoastlineAlgorithm {
	/**
	 * Interface which must be implemented to handle all events during polygon creation.
	 */
	interface ClosedPolygonHandler {
		/**
		 * Called when an invalid coastline segment has been detected.
		 * 
		 * @param coastline
		 *            the coordinates of the invalid coastline segment.
		 */
		void onInvalidCoastlineSegment(float[] coastline);

		/**
		 * Called when a closed island polygon has been generated.
		 * 
		 * @param coastline
		 *            the coordinates of the closed island polygon.
		 */
		void onIslandPolygon(float[] coastline);

		/**
		 * Called when a closed water polygon has been generated.
		 * 
		 * @param coastline
		 *            the coordinates of the closed water polygon.
		 */
		void onWaterPolygon(float[] coastline);

		/**
		 * Called when a water tile has been detected.
		 */
		void onWaterTile();
	}

	private final ArrayList<ImmutablePoint> additionalCoastlinePoints;
	private CoastlineWay coastlineEnd;
	private int coastlineEndLength;
	private ImmutablePoint coastlineEndPoint;
	private final TreeMap<ImmutablePoint, float[]> coastlineEnds;
	private CoastlineWay coastlineStart;
	private int coastlineStartLength;
	private ImmutablePoint coastlineStartPoint;
	private final TreeMap<ImmutablePoint, float[]> coastlineStarts;
	private final Comparator<CoastlineWay> coastlineWayComparator;
	private final ArrayList<CoastlineWay> coastlineWays;
	private float[] coordinates;
	private int currentSide;
	private EndPoints endPoints;
	private final HashSet<EndPoints> handledCoastlineSegments;
	private final ImmutablePoint[] helperPoints;
	private boolean islandSituation;
	private float[] matchPath;
	private boolean needHelperPoint;
	private float[] newPath;
	private float[] nodesSequence;
	private boolean noWaterBackground;

	/**
	 * Constructs a new CoastlineAlgorithm instance to generate closed polygons.
	 */
	CoastlineAlgorithm() {
		// set up the comparator for coastline segments entering the tile
		this.coastlineWayComparator = new Comparator<CoastlineWay>() {
			@Override
			public int compare(CoastlineWay o1, CoastlineWay o2) {
				if (o1.entryAngle > o2.entryAngle) {
					return 1;
				}
				return -1;
			}
		};

		// create the four helper points at the tile corners
		this.helperPoints = new ImmutablePoint[4];
		this.helperPoints[0] = new ImmutablePoint(Tile.TILE_SIZE, Tile.TILE_SIZE);
		this.helperPoints[1] = new ImmutablePoint(0, Tile.TILE_SIZE);
		this.helperPoints[2] = new ImmutablePoint(0, 0);
		this.helperPoints[3] = new ImmutablePoint(Tile.TILE_SIZE, 0);

		this.additionalCoastlinePoints = new ArrayList<ImmutablePoint>(4);
		this.coastlineWays = new ArrayList<CoastlineWay>(4);

		// create the data structures for the coastline segments
		this.coastlineEnds = new TreeMap<ImmutablePoint, float[]>();
		this.coastlineStarts = new TreeMap<ImmutablePoint, float[]>();
		this.handledCoastlineSegments = new HashSet<EndPoints>(64);
	}

	/**
	 * Adds a coastline segment to the internal data structures. Coastline segments are
	 * automatically merged into longer parts when they share the same start or end point.
	 * Adding the same coastline segment more than once has no effect.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 */
	void addCoastlineSegment(float[] coastline) {
		// all coastline segments are accumulated and merged together if possible
		this.nodesSequence = coastline;
		this.coastlineStartPoint = new ImmutablePoint(this.nodesSequence[0],
				this.nodesSequence[1]);
		this.coastlineEndPoint = new ImmutablePoint(
				this.nodesSequence[this.nodesSequence.length - 2],
				this.nodesSequence[this.nodesSequence.length - 1]);
		this.endPoints = new EndPoints(this.coastlineStartPoint, this.coastlineEndPoint);

		// check to avoid duplicate coastline segments
		if (!this.handledCoastlineSegments.contains(this.endPoints)) {
			// update the set of handled coastline segments
			this.handledCoastlineSegments.add(new EndPoints(this.coastlineStartPoint,
					this.coastlineEndPoint));

			// check if a data way starts with the last point of the current way
			if (this.coastlineStarts.containsKey(this.coastlineEndPoint)) {
				// merge both way segments
				this.matchPath = this.coastlineStarts.remove(this.coastlineEndPoint);
				this.newPath = new float[this.nodesSequence.length + this.matchPath.length - 2];
				System.arraycopy(this.nodesSequence, 0, this.newPath, 0,
						this.nodesSequence.length - 2);
				System.arraycopy(this.matchPath, 0, this.newPath,
						this.nodesSequence.length - 2, this.matchPath.length);
				this.nodesSequence = this.newPath;
				this.coastlineEndPoint = new ImmutablePoint(
						this.nodesSequence[this.nodesSequence.length - 2],
						this.nodesSequence[this.nodesSequence.length - 1]);
			}

			// check if a data way ends with the first point of the current way
			if (this.coastlineEnds.containsKey(this.coastlineStartPoint)) {
				this.matchPath = this.coastlineEnds.remove(this.coastlineStartPoint);
				// check if the merged way is already a circle
				if (!this.coastlineStartPoint.equals(this.coastlineEndPoint)) {
					// merge both way segments
					this.newPath = new float[this.nodesSequence.length + this.matchPath.length
							- 2];
					System.arraycopy(this.matchPath, 0, this.newPath, 0,
							this.matchPath.length - 2);
					System.arraycopy(this.nodesSequence, 0, this.newPath,
							this.matchPath.length - 2, this.nodesSequence.length);
					this.nodesSequence = this.newPath;
					this.coastlineStartPoint = new ImmutablePoint(this.nodesSequence[0],
							this.nodesSequence[1]);
				}
			}

			this.coastlineStarts.put(this.coastlineStartPoint, this.nodesSequence);
			this.coastlineEnds.put(this.coastlineEndPoint, this.nodesSequence);
		}
	}

	/**
	 * Clears the internal data structures. Must be called between tiles.
	 */
	void clearCoastlineSegments() {
		this.coastlineStarts.clear();
		this.coastlineEnds.clear();
		this.handledCoastlineSegments.clear();
	}

	/**
	 * Generates closed water and land polygons from unconnected coastline segments. Closed
	 * segments are handled either as water or islands, depending on their orientation.
	 * 
	 * @param closedPolygonHandler
	 *            the implementation which will be called to handle the generated polygons.
	 */
	void generateClosedPolygons(ClosedPolygonHandler closedPolygonHandler) {
		// check if there are any coastline segments
		if (this.coastlineStarts.isEmpty()) {
			return;
		}

		this.islandSituation = false;
		this.noWaterBackground = false;
		for (float[] coastline : this.coastlineStarts.values()) {
			// is the current segment already closed?
			if (CoastlineWay.isClosed(coastline)) {
				// depending on the orientation we have either water or an island
				if (CoastlineWay.isClockWise(coastline)) {
					// water
					this.noWaterBackground = true;
					closedPolygonHandler.onWaterPolygon(coastline);
				} else {
					// island
					this.islandSituation = true;
					closedPolygonHandler.onIslandPolygon(coastline);
				}
			} else if (CoastlineWay.isValid(coastline)) {
				coastline = CoastlineWay.shortenCoastlineSegment(coastline);
				if (coastline != null) {
					this.coastlineWays.add(new CoastlineWay(coastline));
				}
			} else {
				this.noWaterBackground = true;
				closedPolygonHandler.onInvalidCoastlineSegment(coastline);
			}
		}

		// check if there are no errors and the tile needs a water background
		if (this.islandSituation && !this.noWaterBackground && this.coastlineWays.isEmpty()) {
			// add a water polygon for the whole tile
			closedPolygonHandler.onWaterTile();
			return;
		}

		// order all coastline segments ascending by their entering angle
		Collections.sort(this.coastlineWays, this.coastlineWayComparator);

		// join coastline segments to create closed water segments
		while (!this.coastlineWays.isEmpty()) {
			this.coastlineStart = this.coastlineWays.get(0);
			this.coastlineEnd = null;
			// try to find a matching coastline segment
			for (CoastlineWay coastline : this.coastlineWays) {
				if (coastline.entryAngle > this.coastlineStart.exitAngle) {
					this.coastlineEnd = coastline;
					break;
				}
			}
			if (this.coastlineEnd == null) {
				// no coastline segment was found, take the first one
				this.coastlineEnd = this.coastlineWays.get(0);
			}
			this.coastlineWays.remove(0);

			// if the segment orientation is clockwise, we need at least one helper point
			if (this.coastlineEnd.entrySide == 0 && this.coastlineStart.exitSide == 0) {
				this.needHelperPoint = (this.coastlineStart.exitAngle > this.coastlineEnd.entryAngle && (this.coastlineStart.exitAngle - this.coastlineEnd.entryAngle) < Math.PI)
						|| (this.coastlineStart.exitAngle < Math.PI && this.coastlineEnd.entryAngle > Math.PI);
			} else {
				this.needHelperPoint = this.coastlineStart.exitAngle > this.coastlineEnd.entryAngle;
			}

			this.additionalCoastlinePoints.clear();
			this.currentSide = this.coastlineStart.exitSide;

			// walk around the tile and add additional points to the list
			while (this.currentSide != this.coastlineEnd.entrySide || this.needHelperPoint) {
				this.needHelperPoint = false;
				this.additionalCoastlinePoints.add(this.helperPoints[this.currentSide]);
				this.currentSide = (this.currentSide + 1) % 4;
			}

			// check if the start segment is also the end segment
			if (this.coastlineStart == this.coastlineEnd) {
				// calculate the length of the new way
				this.coastlineStartLength = this.coastlineStart.data.length;
				this.coordinates = new float[this.coastlineStartLength
						+ this.additionalCoastlinePoints.size() * 2 + 2];

				// copy the start segment
				System.arraycopy(this.coastlineStart.data, 0, this.coordinates, 0,
						this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					this.coordinates[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints
							.get(i).x;
					this.coordinates[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints
							.get(i).y;
				}

				// close the way
				this.coordinates[this.coordinates.length - 2] = this.coordinates[0];
				this.coordinates[this.coordinates.length - 1] = this.coordinates[1];

				// add the now closed way as a water polygon to the way list
				closedPolygonHandler.onWaterPolygon(this.coordinates);

			} else {
				// calculate the length of the new coastline segment
				this.coastlineStartLength = this.coastlineStart.data.length;
				this.coastlineEndLength = this.coastlineEnd.data.length;
				float[] newSegment = new float[this.coastlineStartLength
						+ this.additionalCoastlinePoints.size() * 2 + this.coastlineEndLength];

				// copy the start segment
				System.arraycopy(this.coastlineStart.data, 0, newSegment, 0,
						this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					newSegment[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints
							.get(i).x;
					newSegment[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints
							.get(i).y;
				}

				// copy the end segment
				System.arraycopy(this.coastlineEnd.data, 0, newSegment,
						this.coastlineStartLength + this.additionalCoastlinePoints.size() * 2,
						this.coastlineEndLength);

				// replace the end segment in the list with the new segment
				this.coastlineWays.remove(this.coastlineEnd);
				newSegment = CoastlineWay.shortenCoastlineSegment(newSegment);
				if (newSegment != null) {
					this.coastlineWays.add(new CoastlineWay(newSegment));
					Collections.sort(this.coastlineWays, this.coastlineWayComparator);
				}
			}
		}
	}
}