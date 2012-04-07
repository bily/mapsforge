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
package org.mapsforge.android.maps.rendertheme;

import java.util.List;

import org.mapsforge.core.model.Tag;

class MatchingCacheKey {
	private final Closed closed;
	private final int hashCodeValue;
	private final List<Tag> tags;
	private final byte zoomLevel;

	MatchingCacheKey(List<Tag> tags, byte zoomLevel, Closed closed) {
		this.tags = tags;
		this.zoomLevel = zoomLevel;
		this.closed = closed;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MatchingCacheKey)) {
			return false;
		}
		MatchingCacheKey other = (MatchingCacheKey) obj;
		if (this.closed == null && other.closed != null) {
			return false;
		} else if (this.closed != null && !this.closed.equals(other.closed)) {
			return false;
		} else if (this.tags == null && other.tags != null) {
			return false;
		} else if (this.tags != null && !this.tags.equals(other.tags)) {
			return false;
		} else if (this.zoomLevel != other.zoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + ((this.closed == null) ? 0 : this.closed.hashCode());
		result = 31 * result + ((this.tags == null) ? 0 : this.tags.hashCode());
		result = 31 * result + this.zoomLevel;
		return result;
	}

	public boolean matches(List<Tag> tags2, byte zoomLevel2, Closed closed2) {
		if (this.zoomLevel != zoomLevel2) {
			return false;
		} else if (this.closed != closed2) {
			return false;
		} else if (this.tags == null) {
			return (tags2 == null);
		}

		int size;
		if ((size = this.tags.size()) != tags2.size())
			return false;

		for (int i = 0; i < size; i++)
			if (this.tags.get(i) != tags2.get(i))
				return false;

		return true;
	}
}
