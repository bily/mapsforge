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
package org.mapsforge.preprocessing.routing.hhmobile;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelEdge;
import org.mapsforge.preprocessing.routing.hhmobile.LevelGraph.Level.LevelVertex;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayOutputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;

class RleBlockWriter2 {

	private final static int BUFFER_SIZE = 25000000;
	private final static byte[] BUFFER = new byte[BUFFER_SIZE];
	private static final int OUTPUT_BUFFER_SIZE = 16 * 1000 * 1024;
	private static final int HEADER_LENGTH = 4096;
	private static final int MESSAGE_INTEVAL = 100;

	private static LevelGraph _levelGraph;
	private static Clustering[] _clustering;

	private static TIntIntHashMap[] mapVertexIdToVertexOffset;
	private static TObjectByteHashMap<Cluster> mapClusterToLevel;
	private static ClusterBlockMapping mapClusterToBlockId;
	private static byte bitsPerBlockId;
	private static byte bitsPerVertexOffset;
	private static byte bitsPerEdgeWeight;
	private static DijkstraAlgorithm dijkstraAlgorithm;

	public static int[] writeClusterBlocks(File targetFile, LevelGraph levelGraph,
			Clustering[] clustering, ClusterBlockMapping mapping, boolean includeHopIndices)
			throws IOException {

		_levelGraph = levelGraph;
		_clustering = clustering;
		dijkstraAlgorithm = new DijkstraAlgorithm(_levelGraph);

		sortClusterVerticesByLevelAndNeighborhood(_levelGraph, clustering);

		mapVertexIdToVertexOffset = mapVertexToVertexOffset(clustering);
		mapClusterToLevel = mapClusterToLevel(clustering);
		mapClusterToBlockId = mapping;
		bitsPerBlockId = Utils.numBitsToEncode(0, mapClusterToBlockId.size());
		bitsPerVertexOffset = Utils.numBitsToEncode(0,
				getMaxNumVerticesPerCluster(clustering) - 1);
		bitsPerEdgeWeight = Utils.numBitsToEncode(0,
				getMaxEdgeWeight());

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
				new FileOutputStream(
				targetFile), OUTPUT_BUFFER_SIZE));

		// write header
		out.writeByte(_levelGraph.numLevels());
		out.writeByte(bitsPerBlockId);
		out.writeByte(bitsPerVertexOffset);
		out.writeByte(bitsPerEdgeWeight);
		out.writeBoolean(includeHopIndices);
		out.write(new byte[HEADER_LENGTH - out.size()]);

		// serialize blocks only to get byte sizes
		int[] blockSize = new int[mapClusterToBlockId.size()];
		for (int blockId = 0; blockId < mapping.size(); blockId++) {
			Cluster cluster = mapping.getCluster(blockId);
			byte[] block = serializeBlock(cluster, includeHopIndices);
			blockSize[blockId] = block.length;
			if ((blockId + 1) % MESSAGE_INTEVAL == 0) {
				System.out.println("dummy write blocks " + (blockId + 1 - MESSAGE_INTEVAL)
						+ " - " + (blockId + 1));
			}
		}

		// reassign block id's by ascending byte size
		sortClustersByByteSize(blockSize, mapClusterToBlockId);

		// write blocks
		for (int blockId = 0; blockId < mapping.size(); blockId++) {
			Cluster cluster = mapping.getCluster(blockId);
			byte[] block = serializeBlock(cluster, includeHopIndices);
			blockSize[blockId] = block.length;
			out.write(block);
			if ((blockId + 1) % MESSAGE_INTEVAL == 0) {
				System.out.println("write blocks " + (blockId + 1 - MESSAGE_INTEVAL) + " - "
						+ (blockId + 1));
			}
		}
		out.flush();
		out.close();

		return blockSize;
	}

	private static void sortClustersByByteSize(final int[] byteSize,
			final ClusterBlockMapping mapping) {
		QuickSort quicksort = new QuickSort();
		quicksort.sort(new IndexedSortable() {

			@Override
			public void swap(int i, int j) {
				mapping.swapBlockIds(i, j);
				Utils.swap(byteSize, i, j);
			}

			@Override
			public int compare(int i, int j) {
				return byteSize[i] - byteSize[j];
			}
		}, 0, mapping.size());
	}

	private static int getMaxNumVerticesPerCluster(Clustering[] clustering) {
		int max = 0;
		for (int level = 0; level < clustering.length; level++) {
			for (Cluster c : clustering[level].getClusters()) {
				max = Math.max(max, c.size());
			}
		}
		return max;
	}

	private static void sortClusterVerticesByLevelAndNeighborhood(LevelGraph levelGraph,
			Clustering[] clustering) {
		QuickSort quicksort = new QuickSort();
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			Level graph = levelGraph.getLevel(lvl);
			for (final Cluster c : clustering[lvl].getClusters()) {
				final int[] vertexIds = c.getVertices();
				final int[] nh = new int[vertexIds.length];
				final int[] level = new int[vertexIds.length];
				for (int i = 0; i < vertexIds.length; i++) {
					nh[i] = graph.getVertex(vertexIds[i]).getNeighborhood();
					level[i] = graph.getVertex(vertexIds[i]).getMaxLevel();
				}
				quicksort.sort(new IndexedSortable() {

					@Override
					public void swap(int i, int j) {
						Utils.swap(level, i, j);
						Utils.swap(nh, i, j);
						Utils.swap(vertexIds, i, j);
						c.swapVertices(i, j);
					}

					@Override
					public int compare(int i, int j) {
						if (level[i] != level[j]) {
							return level[j] - level[i];
						}
						return nh[i] - nh[j];
					}
				}, 0, c.size());
			}
		}
	}

	private static TIntIntHashMap[] mapVertexToVertexOffset(Clustering[] clustering) {
		TIntIntHashMap vertex2Offset[] = new TIntIntHashMap[clustering.length];
		for (int lvl = 0; lvl < clustering.length; lvl++) {
			vertex2Offset[lvl] = new TIntIntHashMap();
			for (Cluster c : clustering[lvl].getClusters()) {
				int offs = 0;
				for (int vId : c.getVertices()) {
					vertex2Offset[lvl].put(vId, offs++);
				}
			}
		}
		return vertex2Offset;
	}

	private static TObjectByteHashMap<Cluster> mapClusterToLevel(Clustering[] clustering) {
		TObjectByteHashMap<Cluster> cluster2Level = new TObjectByteHashMap<Cluster>();
		for (byte lvl = 0; lvl < clustering.length; lvl++) {
			for (Cluster c : clustering[lvl].getClusters()) {
				cluster2Level.put(c, lvl);
			}
		}
		return cluster2Level;
	}

	private static int getMaxNeighborHood(Cluster cluster) {
		int maxNeighborhood = Integer.MIN_VALUE;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			LevelVertex v = graph.getVertex(vertexId);
			if (v.getNeighborhood() != HHComputation.INFINITY_1
					&& v.getNeighborhood() != HHComputation.INFINITY_2) {
				maxNeighborhood = Math.max(maxNeighborhood, v.getNeighborhood());
			}
		}
		return maxNeighborhood;
	}

	private static int getMaxEdgeWeight() {
		int maxEdgeWeight = Integer.MIN_VALUE;
		for (int level = 0; level < _clustering.length; level++) {
			Level graph = _levelGraph.getLevel(level);
			for (Iterator<LevelVertex> iter = graph.getVertices(); iter.hasNext();) {
				LevelVertex v = iter.next();
				for (LevelEdge e : v.getOutboundEdges()) {
					maxEdgeWeight = Math.max(maxEdgeWeight, e.getWeight());
				}
			}
		}
		return maxEdgeWeight;
	}

	private static short countVerticesTypeA(Cluster cluster) {
		short count = 0;
		int level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getMaxLevel() > level) {
				count++;
			}
		}
		return count;
	}

	private static short countVerticesTypeB(Cluster cluster) {
		short count = 0;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getMaxLevel() == level
					&& graph.getVertex(vertexId).getNeighborhood() != HHComputation.INFINITY_1
					&& graph.getVertex(vertexId).getNeighborhood() != HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	private static short countVerticesTypeC(Cluster cluster) {
		short count = 0;
		byte level = mapClusterToLevel.get(cluster);
		Level graph = _levelGraph.getLevel(level);
		for (int vertexId : cluster.getVertices()) {
			if (graph.getVertex(vertexId).getNeighborhood() == HHComputation.INFINITY_1
					|| graph.getVertex(vertexId).getNeighborhood() == HHComputation.INFINITY_2) {
				count++;
			}
		}
		return count;
	}

	private static Rect getBoundingBox(Cluster cluster) {
		Level graph = _levelGraph.getLevel(0);
		int minLat = Integer.MAX_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLon = Integer.MIN_VALUE;
		for (int vertexId : cluster.getVertices()) {
			LevelVertex v = graph.getVertex(vertexId);
			minLat = Math.min(minLat, v.getCoordinate().getLatitudeE6());
			minLon = Math.min(minLon, v.getCoordinate().getLongitudeE6());
			maxLat = Math.max(maxLat, v.getCoordinate().getLatitudeE6());
			maxLon = Math.max(maxLon, v.getCoordinate().getLongitudeE6());
			for (LevelEdge e : v.getOutboundEdges()) {
				for (GeoCoordinate c : e.getWaypoints()) {
					minLat = Math.min(minLat, c.getLatitudeE6());
					minLon = Math.min(minLon, c.getLongitudeE6());
					maxLat = Math.max(maxLat, c.getLatitudeE6());
					maxLon = Math.max(maxLon, c.getLongitudeE6());
				}
			}
		}
		return new Rect(minLon, maxLon, minLat, maxLat);
	}

	private static void clearBuffer() {
		for (int i = 0; i < BUFFER.length; i++) {
			BUFFER[i] = 0x00;
		}
	}

	private static byte[] serializeBlock(Cluster cluster, boolean includeHopIndices)
			throws IOException {

		// write everything two times,
		// during the first run, the edge offsets stored
		// at each vertex are unknown
		// in the second run these values are known from the first run

		int[] vertexEdgeOffset = new int[cluster.size()];
		BitArrayOutputStream out = new BitArrayOutputStream(BUFFER);
		clearBuffer();

		byte level = mapClusterToLevel.get(cluster);
		Rect bbox = getBoundingBox(cluster);
		int maxNeighborhood = getMaxNeighborHood(cluster);

		for (int run = 0; run < 2; run++) {
			if (run == 1) {
				clearBuffer();
				out = new BitArrayOutputStream(BUFFER);
			}

			/* BLOCK HEADER */

			// level
			out.writeByte(level);

			// number of vertices which are in next level
			out.writeShort(countVerticesTypeA(cluster));

			// number of core vertices not in next level
			out.writeShort(countVerticesTypeB(cluster));

			// number of non core vertices
			out.writeShort(countVerticesTypeC(cluster));

			// minimal latitude
			out.writeInt(bbox.minLatitudeE6);

			// minimal longitude
			out.writeInt(bbox.minLongitudeE6);

			// bits per coordinate
			byte bitsPerCoordinate = Utils.numBitsToEncode(bbox.minLatitudeE6,
					bbox.maxLatitudeE6);
			bitsPerCoordinate = (byte) Math.max(bitsPerCoordinate, Utils.numBitsToEncode(
					bbox.minLongitudeE6, bbox.maxLongitudeE6));
			out.writeByte(bitsPerCoordinate);

			// bit per neighborhood
			byte bitsPerNeighborhood = Utils.numBitsToEncode(0, maxNeighborhood);
			out.writeByte(bitsPerNeighborhood);

			/* VERTICES */
			{
				int[] vertexIds = cluster.getVertices();
				for (int i = 0; i < vertexIds.length; i++) {
					LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexIds[i]);
					// vertex id's of lower levels
					for (int j = 0; j < level; j++) {
						int blockId = mapClusterToBlockId
								.getBlockId(_clustering[j].getCluster(v.getId()));
						int vertexOffset = mapVertexIdToVertexOffset[j].get(v.getId());
						out.writeUInt(blockId, bitsPerBlockId);
						out.writeUInt(vertexOffset, bitsPerVertexOffset);
					}

					// vertex id of next level
					if (v.getMaxLevel() > level) {
						int blockId = mapClusterToBlockId.getBlockId(_clustering[level + 1]
								.getCluster(v.getId()));
						int vertexOffset = mapVertexIdToVertexOffset[level + 1].get(v.getId());
						out.writeUInt(blockId, bitsPerBlockId);
						out.writeUInt(vertexOffset, bitsPerVertexOffset);
					}

					// neighborhood
					if (v.getNeighborhood() != HHComputation.INFINITY_1
							&& v.getNeighborhood() != HHComputation.INFINITY_2) {
						out.writeUInt(v.getNeighborhood(), bitsPerNeighborhood);
					}

					// first outbound edge Offset
					out.writeInt(vertexEdgeOffset[i]);

					// coordinate
					if (v.getLevel() == 0) {
						out.writeUInt(v.getCoordinate().getLatitudeE6() - bbox.minLatitudeE6,
								bitsPerCoordinate);
						out.writeUInt(v.getCoordinate().getLongitudeE6() - bbox.minLongitudeE6,
								bitsPerCoordinate);
					}
				}
			}

			/* EDGES */
			{
				int[] vertexIds = cluster.getVertices();
				for (int i = 0; i < vertexIds.length; i++) {
					LevelVertex v = _levelGraph.getLevel(level).getVertex(vertexIds[i]);
					vertexEdgeOffset[i] = (out.getByteOffset() * 8) + out.getBitOffset();
					// number of edges (4 bits if less than 15, else escape and append a 24 bit
					// value)
					if (v.getOutboundEdges().length < 15) {
						out.writeUInt(v.getOutboundEdges().length, 4);
					} else {
						// escape by 0xff
						out.writeUInt(15, 4);
						// write number of edges as 24bit unsigned integer
						out.writeUInt(v.getOutboundEdges().length, 24);
					}

					for (LevelEdge e : v.getOutboundEdges()) {
						// weight
						out.writeUInt(e.getWeight(), bitsPerEdgeWeight);
						// target id
						int blockId = mapClusterToBlockId.getBlockId(_clustering[level]
								.getCluster(e.getTarget().getId()));
						int vertexOffset = mapVertexIdToVertexOffset[level].get(e.getTarget()
								.getId());

						out.writeUInt(blockId, bitsPerBlockId);
						out.writeUInt(vertexOffset, bitsPerVertexOffset);

						// is forward
						out.writeBit(e.isForward());

						// is backward
						out.writeBit(e.isBackward());

						// is core
						out
								.writeBit(e.getSource().getNeighborhood() != HHComputation.INFINITY_1
										&& e.getTarget().getNeighborhood() != HHComputation.INFINITY_1);

						if (level == 0 && e.isForward()) {
							// isMotorwayLink
							out
									.writeBit(e.isMotorwayLink());

							// isRoundabout
							out.writeBit(e.isRoundabout());

							// name
							String name = e.getName();
							if (name == null) {
								name = "";
							}
							if (name.length() > 30) {
								name = name.substring(0, 30);
							}
							out.writeByte((byte) name.getBytes().length);
							out.write(name.getBytes());

							// ref
							String ref = e.getRef();
							if (ref == null) {
								ref = "";
							}
							if (ref.length() > 30) {
								ref = ref.substring(0, 30);
							}
							out.writeByte((byte) ref.getBytes().length);
							out.write(ref.getBytes());

							// way points
							GeoCoordinate[] waypoints = e.getWaypoints();
							if (waypoints.length < 15) {
								out.writeUInt(waypoints.length, 4);
							} else {
								// escape length field by 0xf and write 16 bit length field
								out.writeUInt(15, 4);
								out.writeUInt(waypoints.length, 16);
							}
							for (GeoCoordinate c : waypoints) {
								out.writeUInt(c.getLatitudeE6() - bbox.minLatitudeE6,
										bitsPerCoordinate);
								out.writeUInt(c.getLongitudeE6() - bbox.minLongitudeE6,
										bitsPerCoordinate);
							}
						}

						// lowest level this edge belongs to level
						if (level > 0) {
							out.writeByte((byte) e.getMinLevel());
						}

						// write hop indices for expanding shortcuts
						if (e.getMinLevel() > 0 && includeHopIndices) {
							LinkedList<Integer> hopIndices = new LinkedList<Integer>();
							dijkstraAlgorithm.getShortestPath(e.getSource().getId(), e
									.getTarget().getId(), e.getMinLevel() - 1,
									new LinkedList<LevelVertex>(), hopIndices, e.isForward(), e
									.isBackward());
							out.writeUInt(hopIndices.size(), 5);
							for (int hopIdx : hopIndices) {
								if (hopIdx < 15) {
									out.writeUInt(hopIdx, 4);
								} else {
									out.writeUInt(15, 4);
									out.writeUInt(hopIdx, 24);
								}
							}
						}
					}
				}
			}
		}

		// copy written data from buffer and return it
		out.alignPointer(1);
		byte[] result = new byte[out.getByteOffset()];
		for (int i = 0; i < result.length; i++) {
			result[i] = BUFFER[i];
		}
		return result;
	}
}
