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
package org.mapsforge.server.routing.highwayHierarchies;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.preprocessing.util.DBConnection;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.IVertex;
import org.mapsforge.server.routing.RouterFactory;
import org.mapsforge.server.routing.highwayHierarchies.EdgeMapper.EdgeMapping;
import org.mapsforge.server.routing.highwayHierarchies.HHStaticGraph.HHStaticEdge;
import org.mapsforge.server.routing.highwayHierarchies.HHStaticGraph.HHStaticVertex;

public class RouterImpl implements IRouter {

	private static final String ALGORITHM_NAME = "Highway Hierarchies";

	// core
	private final HHAlgorithm algorithm;
	public final HHStaticGraph routingGraph;

	// index structures
	private final HHEdgeExpanderRecursive edgeExpander;
	private final DistanceTable distanceTable;
	private final HHEdgeReverser edgeReverser;
	public final GeoCoordinateKDTree vertexIndex;

	// mapping between rgEdgeIds to hhEdgeIds and vice versa.
	private final EdgeMapper mapper;

	// storage components indexed by routing graph edgeIds
	private final RgEdgeNames edgeNames;
	private final EdgeIndex edgeIndex;

	private RouterImpl(HHAlgorithm algorithm, HHStaticGraph routingGraph,
			HHEdgeExpanderRecursive edgeExpander, DistanceTable distanceTable,
			HHEdgeReverser edgeReverser, GeoCoordinateKDTree vertexIndex, EdgeMapper mapper,
			RgEdgeNames edgeNames, EdgeIndex edgeIndex) {
		this.algorithm = algorithm;
		this.routingGraph = routingGraph;
		this.edgeExpander = edgeExpander;
		this.distanceTable = distanceTable;
		this.edgeReverser = edgeReverser;
		this.vertexIndex = vertexIndex;
		this.mapper = mapper;
		this.edgeNames = edgeNames;
		this.edgeIndex = edgeIndex;
	}

	public void serialize(OutputStream oStream) throws IOException {
		routingGraph.serialize(oStream);
		edgeExpander.serialize(oStream);
		distanceTable.serialize(oStream);
		edgeReverser.serialize(oStream);
		vertexIndex.serialize(oStream);
		mapper.serialize(oStream);
		edgeNames.serialize(oStream);
		edgeIndex.serialize(oStream);
	}

	public static RouterImpl deserialize(InputStream iStream) throws IOException,
			ClassNotFoundException {
		HHAlgorithm algorithm = new HHAlgorithm();
		HHStaticGraph routingGraph = HHStaticGraph.deserialize(iStream);

		// index structures
		HHEdgeExpanderRecursive edgeExpander = HHEdgeExpanderRecursive.deserialize(iStream);
		DistanceTable distanceTable = DistanceTable.deserialize(iStream);
		HHEdgeReverser edgeReverser = HHEdgeReverser.deserialize(iStream);
		GeoCoordinateKDTree vertexIndex = GeoCoordinateKDTree.deserialize(iStream);

		// mapping between rgEdgeIds to hhEdgeIds and vice versa.
		EdgeMapper mapper = EdgeMapper.deserialize(iStream);

		// storage components indexed by routing graph edgeIds
		RgEdgeNames edgeNames = RgEdgeNames.deserialize(iStream);
		EdgeIndex edgeIndex = EdgeIndex.deserialize(iStream);

		return new RouterImpl(algorithm, routingGraph, edgeExpander, distanceTable,
				edgeReverser, vertexIndex, mapper, edgeNames, edgeIndex);
	}

	public static RouterImpl getFromDb(Connection conn) throws SQLException {
		HHAlgorithm algorithm = new HHAlgorithm();
		HHStaticGraph routingGraph = HHStaticGraph.getFromHHDb(conn);

		// index structures
		HHEdgeExpanderRecursive edgeExpander = HHEdgeExpanderRecursive.createIndex(
				routingGraph, HHEdgeExpanderRecursive.getEMinLvl(conn));
		DistanceTable distanceTable = DistanceTable.getFromHHDb(conn);
		HHEdgeReverser edgeReverser = new HHEdgeReverser(routingGraph);
		GeoCoordinateKDTree vertexIndex = GeoCoordinateKDTree.buildHHVertexIndex(conn);

		// mapping between rgEdgeIds to hhEdgeIds and vice versa.
		EdgeMapper mapper = EdgeMapper.importFromDb(conn);

		// storage components indexed by routing graph edgeIds
		RgEdgeNames edgeNames = RgEdgeNames.importFromDb(conn);
		EdgeIndex edgeIndex = EdgeIndex.importFromDb(conn);

		return new RouterImpl(algorithm, routingGraph, edgeExpander, distanceTable,
				edgeReverser, vertexIndex, mapper, edgeNames, edgeIndex);
	}

	@Override
	public String getAlgorithmName() {
		return ALGORITHM_NAME;
	}

	@Override
	public HHEdge[] getNearestEdges(GeoCoordinate coord) {
		int rgEdgeId = edgeIndex
				.getNearestEdge(coord.getLongitudeInt(), coord.getLatitudeInt());
		EdgeMapping[] mapping = mapper.mapFromRgEdgeId(rgEdgeId);
		return getEdgesFromMapping(mapping);
	}

	@Override
	public IVertex getNearestVertex(GeoCoordinate coord) {
		int id = vertexIndex.getNearestNeighborIdx(coord.getLongitudeInt(), coord
				.getLatitudeInt());
		return new HHVertex(routingGraph.getVertex(id));
	}

	@Override
	public HHEdge[] getShortestPath(int sourceId, int targetId) {
		LinkedList<HHStaticEdge> searchSpace = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> fwd = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> bwd = new LinkedList<HHStaticEdge>();
		LinkedList<HHStaticEdge> expandedBwd = new LinkedList<HHStaticEdge>();
		int distance = algorithm.shortestPath(routingGraph, sourceId, targetId, distanceTable,
				fwd, bwd, searchSpace);
		if (distance == Integer.MAX_VALUE) {
			return null;
		}
		LinkedList<HHStaticEdge> sp = new LinkedList<HHStaticEdge>();
		edgeExpander.expandShortestPath(fwd, sp);
		edgeExpander.expandShortestPath(bwd, expandedBwd);
		edgeReverser.reverseEdges(expandedBwd, sp);

		HHEdge[] e = new HHEdge[sp.size()];
		int i = 0;
		for (Iterator<HHStaticEdge> iter = sp.iterator(); iter.hasNext();) {
			e[i++] = new HHEdge(iter.next());
		}
		return e;
	}

	private HHEdge[] getEdgesFromMapping(EdgeMapping[] mapping) {
		LinkedList<HHEdge> edges = new LinkedList<HHEdge>();
		for (EdgeMapping m : mapping) {
			HHStaticEdge e = routingGraph.getEdge(m.hhEdgeId);
			if (e.getDirection(HHStaticGraph.FWD)) {
				HHEdge e_ = new HHEdge(e);
				edges.add(e_);
			}
		}
		HHEdge[] arr = new HHEdge[edges.size()];
		edges.toArray(arr);
		return arr;
	}

	@Override
	public Iterator<HHVertex> getVerticesWithinBox(int minLon, int minLat, int maxLon,
			int maxLat) {
		final TIntArrayList ids = vertexIndex.getIndicesByBoundingBox(minLon, minLat, maxLon,
				maxLat);
		return new Iterator<HHVertex>() {

			private TIntIterator iter = ids.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public HHVertex next() {
				if (iter.hasNext()) {
					return new HHVertex(routingGraph.getVertex(iter.next()));
				}
				return null;

			}

			@Override
			public void remove() {

			}
		};

	}

	@Override
	public int getMaxLatitude() {
		return Math.max(vertexIndex.getMaxLatitude(), edgeIndex.getMaxLatitude());
	}

	@Override
	public int getMaxLongitude() {
		return Math.max(vertexIndex.getMaxLongitude(), edgeIndex.getMaxLongitude());
	}

	@Override
	public int getMinLatitude() {
		return Math.min(vertexIndex.getMinLatitude(), edgeIndex.getMinLatitude());
	}

	@Override
	public int getMinLongitude() {
		return Math.min(vertexIndex.getMinLongitude(), edgeIndex.getMinLongitude());
	}

	private class HHEdge implements IEdge {

		private HHStaticEdge e;

		public HHEdge(HHStaticEdge e) {
			this.e = e;
		}

		@Override
		public int getId() {
			return e.getId();
		}

		@Override
		public String getName() {
			EdgeMapping mapping = mapper.mapFromHHEdgeId(e.getId());
			return edgeNames.getName(mapping.rgEdgeId);
		}

		@Override
		public IVertex getSource() {
			return new HHVertex(e.getSource());
		}

		@Override
		public IVertex getTarget() {
			return new HHVertex(e.getTarget());
		}

		@Override
		public GeoCoordinate[] getWaypoints() {
			EdgeMapping mapping = mapper.mapFromHHEdgeId(e.getId());
			if (mapping == null) {
				System.out.println("mapping error : shortcut = " + e.isShortcut() + " id ="
						+ e.getId() + " : " + e.getSource().getId() + " -> "
						+ e.getTarget().getId() + " weight = " + e.getWeight());
				return new GeoCoordinate[0];
			}

			GeoCoordinate[] waypoints = edgeIndex.getWaypoints(mapping.rgEdgeId);
			if (waypoints != null && mapping.isReversed) {
				// reverse array
				int i = 0;
				int j = waypoints.length - 1;
				while (i < j) {
					GeoCoordinate tmp = waypoints[i];
					waypoints[i] = waypoints[j];
					waypoints[j] = tmp;
					i++;
					j--;
				}
			}
			return waypoints;
		}

		@Override
		public int getWeight() {
			return e.getWeight();
		}
	}

	private class HHVertex implements IVertex {

		private HHStaticVertex v;

		public HHVertex(HHStaticVertex v) {
			this.v = v;
		}

		@Override
		public GeoCoordinate getCoordinate() {
			return vertexIndex.getCoordinate(v.getId());
		}

		@Override
		public int getId() {
			return v.getId();
		}

		@Override
		public IEdge[] getOutboundEdges() {
			HHStaticEdge[] e = v.getAdjacentLevel0Edges();
			HHEdge[] e_ = new HHEdge[e.length];
			for (int i = 0; i < e.length; i++) {
				e_[i] = new HHEdge(e[i]);
			}
			return e_;
		}
	}

	public static void main(String[] args) throws SQLException, FileNotFoundException,
			IOException, ClassNotFoundException {
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "osm_base",
				"osm", "osm");

		// RouterImpl router = RouterImpl.getFromDb(conn);
		// router.serialize(new FileOutputStream("router"));
		IRouter router = RouterFactory.getRouter();
		IEdge[] sp = router.getShortestPath(12, 12312);

		for (IEdge e : sp) {
			System.out.print(e.getSource().getId() + " -> " + e.getTarget().getId() + " : "
					+ e.getName() + " : ");
			System.out.print(e.getSource().getCoordinate() + ", ");
			for (GeoCoordinate c : e.getWaypoints()) {
				System.out.print(c + ", ");
			}
			System.out.println(e.getTarget().getCoordinate());
		}

	}

}