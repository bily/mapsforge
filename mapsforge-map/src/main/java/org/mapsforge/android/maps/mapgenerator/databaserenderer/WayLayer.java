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

public class WayLayer {
	public final boolean[] levelActive;
	public final WayLevel[] wayLevels;
	public final int levels;

	WayLayer(int levels) {
		this.levels = levels;
		this.wayLevels = new WayLevel[this.levels];
		this.levelActive = new boolean[this.levels];
	}

	/**
	 * @param level
	 * @return
	 */
	public WayLevel get(int level) {
		WayLevel wayLevel = this.wayLevels[level];

		if (wayLevel == null)
			this.wayLevels[level] = wayLevel = new WayLevel();

		this.levelActive[level] = true;
		return wayLevel;
	}

	/**
	 * 
	 */
	public void clear() {
		for (int i = this.levels - 1; i >= 0; i--)
			if (this.levelActive[i]) {
				this.levelActive[i] = false;
				this.wayLevels[i].clear();
			}
	}

}
