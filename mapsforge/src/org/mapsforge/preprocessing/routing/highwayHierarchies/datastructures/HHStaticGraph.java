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
package org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.preprocessing.routing.highwayHierarchies.HHGraphProperties;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHGraphProperties.HHLevelStats;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader.HHEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.sql.HHDbReader.HHVertexLvl;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.arrays.BitArray;

/**
 * Array based implementation of a graph. Level are collapsed (no extra adjacency list per
 * level). By specifying the min level when getting adjacent edges, lower levels can be skipped
 * with regard to increasing performance.
 * 
 * Provide an object oriented access layer, a primitive type access layer and a low level access
 * layer. Data must not be written only read!!! Immutable and Thread safe.
 * 
 * @author Frank Viernau
 */
public class HHStaticGraph implements Serializable {

	public static final int FWD = 0;
	public static final int BWD = 1;

	private static final long serialVersionUID = 5052572584967363425L;

	private final int[] vFirstLvlVertex, vLvlVNh, vLvlFirstEdge, eTarget, eWeight;
	private final BitArray[] eDirection;
	private final BitArray eShortcut;
	private final int numVertices, numLvlVertices, numEdges;
	private final HHGraphProperties graphProperties;

	public final PrimitivAccessLayer primitiveAccessLayer;
	public final LowLevelAccessLayer lowLevelAccessLayer;

	private HHStaticGraph(int numVertices, int numLvlVertices, int numEdges,
			HHGraphProperties metaData) {
		this.numVertices = numVertices;
		this.numLvlVertices = numLvlVertices;
		this.numEdges = numEdges;
		this.graphProperties = metaData;

		vFirstLvlVertex = new int[numVertices + 1];
		vFirstLvlVertex[numVertices] = numLvlVertices;
		vLvlVNh = new int[numLvlVertices];
		vLvlFirstEdge = new int[numLvlVertices + 1];
		vLvlFirstEdge[numLvlVertices] = numEdges;
		eTarget = new int[numEdges];
		eWeight = new int[numEdges];
		eDirection = new BitArray[] { new BitArray(numEdges), new BitArray(numEdges) };
		eShortcut = new BitArray(numEdges);
		this.primitiveAccessLayer = new PrimitivAccessLayer();
		this.lowLevelAccessLayer = new LowLevelAccessLayer();
	}

	public static HHStaticGraph getFromHHDb(Connection conn) throws SQLException {
		HHDbReader reader = new HHDbReader(conn);

		HHStaticGraph g = new HHStaticGraph(reader.numVertices(), reader.numLevelVertices(),
				reader.numEdges(), reader.getGraphProperties());
		int offset = 0;
		for (Iterator<HHVertexLvl> iter = reader.getVertexLvls(); iter.hasNext();) {
			HHVertexLvl v = iter.next();
			g.vLvlVNh[offset] = v.neighborhood;
			if (v.lvl == 0) {
				g.vFirstLvlVertex[v.id] = offset;
			}
			offset++;
		}
		for (int i = 0; i < g.numLvlVertices; i++) {
			g.vLvlFirstEdge[i] = -1;
		}
		offset = 0;
		for (Iterator<HHEdge> iter = reader.getEdges(); iter.hasNext();) {
			HHEdge e = iter.next();
			g.eTarget[offset] = e.targetId;
			g.eWeight[offset] = e.weight;
			g.eDirection[FWD].set(offset, e.fwd);
			g.eDirection[BWD].set(offset, e.bwd);
			g.eShortcut.set(offset, e.minLvl > 0);
			for (int i = 0; i <= e.maxLvl; i++) {
				if (g.vLvlFirstEdge[g.vFirstLvlVertex[e.sourceId] + i] == -1) {
					g.vLvlFirstEdge[g.vFirstLvlVertex[e.sourceId] + i] = offset;
				}
			}
			offset++;
		}
		return g;
	}

	public class LowLevelAccessLayer implements Serializable {

		private static final long serialVersionUID = 1872462475856880498L;

		public final int[] vFirstLvlVertex = HHStaticGraph.this.vFirstLvlVertex;
		public final int[] vLvlVNh = HHStaticGraph.this.vLvlVNh;
		public final int[] vLvlFirstEdge = HHStaticGraph.this.vLvlFirstEdge;
		public final int[] eTarget = HHStaticGraph.this.eTarget;
		public final int[] eWeight = HHStaticGraph.this.eWeight;
		public final BitArray[] eDirection = HHStaticGraph.this.eDirection;
		public final BitArray eShortcut = HHStaticGraph.this.eShortcut;
		public final int numVertices = HHStaticGraph.this.numVertices;
		public final int numLvlVertices = HHStaticGraph.this.numLvlVertices;
		public final int numEdges = HHStaticGraph.this.numEdges;
	}

	public class PrimitivAccessLayer implements Serializable {

		private static final long serialVersionUID = 1105544622411938920L;

		public int vGetLevel(int vertexId) {
			return (vFirstLvlVertex[vertexId + 1] - vFirstLvlVertex[vertexId]) - 1;
		}

		public int vGetNeighborhood(int vertexId, int lvl) {
			return vLvlVNh[vFirstLvlVertex[vertexId] + lvl];
		}

		public int vGetHopIdxFirstAdjacentEdges(int vertexId, int minLvl) {
			return vLvlFirstEdge[vFirstLvlVertex[vertexId] + minLvl]
					- vLvlFirstEdge[vFirstLvlVertex[vertexId]];
		}

		public int vGetHopIdxLastAdjacentEdge(int vertexId) {
			return vLvlFirstEdge[vFirstLvlVertex[vertexId + 1]] - 1
					- vLvlFirstEdge[vFirstLvlVertex[vertexId]];
		}

		public int vGetAdjacentEdge(int vertexId, int hopIdx) {
			return vLvlFirstEdge[vFirstLvlVertex[vertexId]] + hopIdx;
		}

		public int eGetTarget(int edgeId) {
			return eTarget[edgeId];
		}

		public int eGetWeight(int edgeId) {
			return eWeight[edgeId];
		}

		public boolean eHasDirection(int edgeId, int direction) {
			return eDirection[direction].get(edgeId);
		}

		public boolean eIsShortcut(int edgeId) {
			return eShortcut.get(edgeId);
		}

		public boolean eIsLvlGEQ(int sourceId, int edgeId, int lvl) {
			return vLvlFirstEdge[vFirstLvlVertex[sourceId] + lvl] <= edgeId;
		}

		public int eGetLvl(int sourceId, int edgeId) {
			int lvl = 0;
			while (vLvlFirstEdge[vFirstLvlVertex[sourceId] + lvl] <= edgeId) {
				lvl++;
			}
			return lvl - 1;
		}
	}

	public void serialize(File f) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(this);
		oos.close();
		fos.close();
	}

	public static HHStaticGraph getFromSerialization(File f) throws IOException,
			ClassNotFoundException {
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		HHStaticGraph graph = (HHStaticGraph) ois.readObject();
		ois.close();
		fis.close();
		return graph;
	}

	public HHStaticVertex getVertex(int id) {
		return new HHStaticVertex(id);
	}

	public int numVertices() {
		return numVertices;
	}

	public int numEdges() {
		return numEdges;
	}

	public int numLevels() {
		return graphProperties.levelStats.length;
	}

	public HHGraphProperties getGraphPropterties() {
		return graphProperties;
	}

	@Override
	public String toString() {
		String str = "";
		for (HHLevelStats ls : graphProperties.levelStats) {
			str += ls + "\n";
		}
		return str;
	}

	public class HHStaticVertex {

		private final int id;

		public HHStaticVertex(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public HHStaticEdge[] getAdjacentEdges(int minLvl) {
			int startIdx = vLvlFirstEdge[vFirstLvlVertex[id] + minLvl];
			int endIdx = vLvlFirstEdge[vFirstLvlVertex[id + 1]];
			HHStaticEdge[] e = new HHStaticEdge[Math.max(endIdx - startIdx, 0)];
			for (int i = startIdx; i < endIdx; i++) {
				e[i - startIdx] = new HHStaticEdge(i, id);
			}
			return e;
		}

		public HHStaticEdge getAdjacentEdge(int hopIdx) {
			return new HHStaticEdge(vLvlFirstEdge[vFirstLvlVertex[id]] + hopIdx, id);
		}

		public int numAdjacentEdges() {
			return vLvlFirstEdge[vFirstLvlVertex[id + 1]] - vLvlFirstEdge[vFirstLvlVertex[id]];
		}

		public int getNeighborhood(int lvl) {
			return vLvlVNh[vFirstLvlVertex[id] + lvl];
		}

		public int getLevel() {
			return (vFirstLvlVertex[id + 1] - vFirstLvlVertex[id]) - 1;
		}
	}

	public class HHStaticEdge {

		private final int id, sourceId;

		public HHStaticEdge(int id, int sourceId) {
			this.id = id;
			this.sourceId = sourceId;
		}

		public int getId() {
			return id;
		}

		public HHStaticVertex getSource() {
			return new HHStaticVertex(sourceId);
		}

		public HHStaticVertex getTarget() {
			return new HHStaticVertex(eTarget[id]);
		}

		public int getWeight() {
			return eWeight[id];
		}

		public boolean getDirection(int direction) {
			return eDirection[direction].get(id);
		}

		public boolean isLvlGEQ(int lvl) {
			return vLvlFirstEdge[vFirstLvlVertex[sourceId] + lvl] <= id;
		}

		public boolean isShortcut() {
			return eShortcut.get(id);
		}

		public int getLvl() {
			// no use in query algorithm, slow
			int lvl = 0;
			while (vLvlFirstEdge[vFirstLvlVertex[sourceId] + lvl] <= id) {
				lvl++;
			}
			return lvl - 1;
		}

		@Override
		public String toString() {
			return sourceId + " -> " + getTarget().getId();
		}

	}

	// public static void main(String[] args) throws SQLException, IOException,
	// ClassNotFoundException {
	// // StaticLevelGraph g = StaticLevelGraph.getFromHHDb(new
	// HHDbReader(DbConnection.getBerlinDbConn()));
	// // g.serialize(new File("x"));
	// HHStaticGraph g = HHStaticGraph.getFromSerialization(new File("x"));
	//		
	// HHAlgorithm a = new HHAlgorithm();
	// Random rnd = new Random(20111981);
	//		
	// int num = 10;
	// int[] sources = new int[num];
	// int[] targets = new int[num];
	// for(int i=0;i<num;i++) {
	// sources[i] = rnd.nextInt(g.numVertices());
	// targets[i] = rnd.nextInt(g.numVertices());
	// }
	//		
	// long startTime = System.currentTimeMillis();
	// for(int i=0;i<num;i++) {
	// HHStaticVertex s = g.getVertex(sources[i]);
	// HHStaticVertex t = g.getVertex(targets[i]);
	// int d = a.shortestDistance(s, t);
	// }
	// long time = System.currentTimeMillis() - startTime;
	// System.out.println("hh object layer " + time + "ms");
	//		
	// startTime = System.currentTimeMillis();
	// for(int i=0;i<num;i++) {
	// int d = a.shortestDistanceFast(g, sources[i], targets[i]);
	// }
	// time = System.currentTimeMillis() - startTime;
	// System.out.println("hh primitiv layer " + time + "ms");
	//		
	// startTime = System.currentTimeMillis();
	// for(int i=0;i<num;i++) {
	// int d = a.shortestDistanceLowLevel(g, sources[i], targets[i]);
	// }
	// time = System.currentTimeMillis() - startTime;
	// System.out.println("hh low  level layer " + time + "ms");
	// }
}
