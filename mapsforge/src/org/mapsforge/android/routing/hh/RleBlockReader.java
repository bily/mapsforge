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
package org.mapsforge.android.routing.hh;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.mapsforge.preprocessing.routing.hhmobile.util.HHGlobals;

final class RleBlockReader {

	private static final byte[] HEADER_MAGIC = HHGlobals.RLE_CLUSTER_BLOCKS_HEADER_MAGIC;
	private static final int HEADER_LENGTH = HHGlobals.RLE_CLUSTER_BLOCKS_HEADER_LENGTH;

	private final RandomAccessFile raf;
	private final AddressLookupTable blockIndex;
	private final long startAddrFirstClusterBlock;
	private final int bitMask;

	// header data
	final byte bpClusterId, bpVertexCount, bpEdgeCount, bpNeighborhood, numLevels;
	final boolean includeHopIndices;

	// used for address lookup, avoids object creation
	private Pointer tmpPointer = new Pointer();

	private int bytesRead;

	public RleBlockReader(File f, long startAddrClusterBlocks, AddressLookupTable blockIndex)
			throws IOException {
		this.raf = new RandomAccessFile(f, "r");
		this.startAddrFirstClusterBlock = startAddrClusterBlocks + HEADER_LENGTH;
		this.blockIndex = blockIndex;
		this.bytesRead = 0;

		// ----------- READ CLUSTER BLOCKS HEADER ------------------

		// read header from secondary storage and verify header magic
		byte[] header = new byte[HEADER_LENGTH];
		raf.seek(startAddrClusterBlocks);
		raf.readFully(header);
		for (int i = 0; i < HEADER_MAGIC.length; i++) {
			if (header[i] != HEADER_MAGIC[i]) {
				throw new IOException("invalid header.");
			}
		}

		// extract data from header
		DataInputStream iStream = new DataInputStream(new ByteArrayInputStream(header));
		iStream.skip(HEADER_MAGIC.length);
		this.bpClusterId = iStream.readByte();
		this.bpVertexCount = iStream.readByte();
		this.bpEdgeCount = iStream.readByte();
		this.bpNeighborhood = iStream.readByte();
		this.numLevels = iStream.readByte();
		this.includeHopIndices = iStream.readBoolean();

		this.bitMask = getBitmask(bpVertexCount);
	}

	public synchronized RleBlock readBlock(int blockId) throws IOException {
		if (blockIndex.getPointer(blockId, tmpPointer)) {
			raf.seek(startAddrFirstClusterBlock + tmpPointer.startAddr);
			byte[] buff = new byte[tmpPointer.lengthBytes];
			bytesRead += buff.length;
			raf.readFully(buff);
			return new RleBlock(buff, this, blockId);
		}
		return null;
	}

	private static int getBitmask(int shiftClusterId) {
		int bMask = 0;
		for (int i = 0; i < shiftClusterId; i++) {
			bMask = (bMask << 1) | 1;
		}
		return bMask;
	}

	public int getBlockId(int vertexId) {
		return vertexId >>> bpVertexCount;
	}

	public int getVertexOffset(int vertexId) {
		return vertexId & bitMask;
	}

	public int getVertexId(int blockId, int vertexOffset) {
		return (blockId << bpVertexCount) | vertexOffset;
	}

	public int getNumBlocks() {
		return blockIndex.size();
	}

	public int getBytesRead() {
		return bytesRead;
	}

	public void resetBytesRead() {
		bytesRead = 0;
	}
}
