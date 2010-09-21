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

package org.mapsforge.android.map;

class Interval<Type> {
	public Type value;
	public final int low;
	public final int high;

	// precondition: left <= right
	public Interval(int low, int high) {
		if (low <= high) {
			this.low = low;
			this.high = high;
			this.value = null;
		} else
			throw new RuntimeException("Illegal interval");
	}

	public Interval(int left, int right, Type value) {
		if (left <= right) {
			this.low = left;
			this.high = right;
			this.value = value;
		} else
			throw new RuntimeException("Illegal interval");
	}

	// does this interval a intersect b?
	public boolean intersects(Interval<?> b) {
		Interval<Type> a = this;
		if (((a.low < b.low) || (a.low <
				b.high))
				&& ((a.high > b.low) || (a.high >
				b.high)))
			return true;
		return false;
	}

	// does this interval a intersect b?
	public boolean contains(int x) {
		return (low <= x) && (x <= high);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Interval<?>))
			return false;
		Interval<?> b = (Interval<?>) obj;
		if (this.low != b.low)
			return false;
		if (this.high != b.high)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}