/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps.mapgenerator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.android.maps.Tile;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * A thread-safe cache for tile images with a fixed size and LRU policy.
 */
public class InMemoryTileCache extends TileCache {

	private static List<Bitmap> createBitmapPool(int poolSize) {
		List<Bitmap> bitmaps = new ArrayList<Bitmap>();

		for (int i = 0; i < poolSize; ++i) {
			Bitmap bitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Config.RGB_565);
			bitmaps.add(bitmap);
		}

		return bitmaps;
	}

	private static Map<MapGeneratorJob, Bitmap> createMap(final int mapCapacity, final List<Bitmap> bitmapPool) {
		int initialCapacity = (int) (mapCapacity / LOAD_FACTOR) + 2;

		return new LinkedHashMap<MapGeneratorJob, Bitmap>(initialCapacity, LOAD_FACTOR, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<MapGeneratorJob, Bitmap> eldestEntry) {
				if (size() > mapCapacity) {
					remove(eldestEntry.getKey());
					bitmapPool.add(eldestEntry.getValue());
				}
				return false;
			}
		};
	}

	private final List<Bitmap> bitmapPool;
	private final Map<MapGeneratorJob, Bitmap> map;
	private final ByteBuffer byteBuffer;

	/**
	 * @param cacheCapacity
	 *            the maximum number of entries in the cache.
	 */
	public InMemoryTileCache(int cacheCapacity) {
		super(cacheCapacity);
		this.bitmapPool = createBitmapPool(this.cacheCapacity + 1);
		this.map = createMap(this.cacheCapacity, this.bitmapPool);
		this.byteBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);
	}

	@Override
	public boolean containsKey(MapGeneratorJob mapGeneratorJob) {
		synchronized (this.map) {
			return this.map.containsKey(mapGeneratorJob);
		}
	}

	@Override
	public void destroy() {
		synchronized (this.map) {
			for (Bitmap bitmap : this.map.values()) {
				bitmap.recycle();
			}
			this.map.clear();
		}

		synchronized (this.bitmapPool) {
			for (Bitmap bitmap : this.bitmapPool) {
				bitmap.recycle();
			}
			this.bitmapPool.clear();
		}
	}

	@Override
	public Bitmap get(MapGeneratorJob mapGeneratorJob) {
		synchronized (this.map) {
			return this.map.get(mapGeneratorJob);
		}
	}

	@Override
	public boolean isPersistent() {
		return false;
	}

	@Override
	public void put(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		if (this.cacheCapacity == 0) {
			return;
		}

		Bitmap pooledBitmap;
		synchronized (this.bitmapPool) {
			pooledBitmap = this.bitmapPool.remove(this.bitmapPool.size() - 1);
		}

		synchronized (this.byteBuffer) {
			bitmap.copyPixelsToBuffer(this.byteBuffer);
			this.byteBuffer.rewind();
			pooledBitmap.copyPixelsFromBuffer(this.byteBuffer);
		}

		synchronized (this.map) {
			this.map.put(mapGeneratorJob, pooledBitmap);
		}
	}

	@Override
	public void setPersistent(boolean persistent) {
		throw new UnsupportedOperationException();
	}
}
