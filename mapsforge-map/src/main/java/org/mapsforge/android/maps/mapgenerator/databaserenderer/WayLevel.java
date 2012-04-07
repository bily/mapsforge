/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.mapgenerator.databaserenderer;

import java.util.ArrayList;

import android.graphics.Paint;

/**
 * @author jeff
 */
public class WayLevel {
	/**
	 * 
	 */
	public final ArrayList<Paint> paints;
	/**
	 * 
	 */
	public final ArrayList<ShapeContainer> shapeContainers;

	WayLevel() {
		this.shapeContainers = new ArrayList<ShapeContainer>();
		this.paints = new ArrayList<Paint>();
	}

	/**
	 * @param shapeContainer
	 * @param paint
	 */
	public void add(ShapeContainer shapeContainer, Paint paint) {
		boolean found = false;
		for (int i = this.paints.size() - 1; i >= 0; i--) {
			if (this.paints.get(i) == paint) {
				found = true;
				break;
			}
		}
		if (!found)
			this.paints.add(paint);

		this.shapeContainers.add(shapeContainer);
	}

	/**
	 * 
	 */
	public void clear() {
		this.shapeContainers.clear();
		this.paints.clear();
	}
}
