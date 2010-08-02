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
package org.mapsforge.preprocessing.graph.osm2rg;

import gnu.trove.function.TIntFunction;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;

import org.mapsforge.core.DBConnection;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.graph.model.osmxml.OsmNode;
import org.mapsforge.preprocessing.graph.model.osmxml.OsmWay_withNodeRefs;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.IOsmNodeListener;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.IOsmWayListener;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.OsmXmlParser;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgVertex;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.xml.sax.SAXException;

/**
 * @author Frank Viernau
 * 
 *         Extracts the routing graph directly from xml. requires about 42 byte of memory per
 *         vertex / waypoint used in the resulting routing graph. config can be done via
 *         res/conf/osm2rg.properties.
 * 
 *         Prior to running create tables file has to be executed by hand.
 *         /res/sql/osm2rgCreateTables.sql
 */
public class RgExtractor {

	private static final int MSG_INT = 100000;
	private static final double COORDINATE_FAC = 1E7d;
	private static final DecimalFormat df = new DecimalFormat("##.#");

	public static void extractGraph(File osmFile, HashSet<EHighwayLevel> hwyLvlss,
			Connection outputDb) throws IOException, SAXException, SQLException {
		long startTime = System.currentTimeMillis();
		TLongIntHashMap idMapping = new TLongIntHashMap();
		int numVertices = IdAssigner.assignNodeIds(osmFile, hwyLvlss, idMapping);
		RoutingGraphWriter.writeGraph(osmFile, hwyLvlss, idMapping, numVertices, outputDb);
		double time = System.currentTimeMillis() - startTime;

		System.out
				.println("\nextraction finished in " + df.format(time / 60000d) + " minutes.");

		System.out.println("\nextracted highway levels :");
		for (EHighwayLevel hwyLvl : hwyLvlss) {
			System.out.println(hwyLvl.toString() + " [" + hwyLvl.ordinal() + "]");
		}

		System.out.println("\n|nodes| = " + IdAssigner.numOverallNodes);
		System.out.println("|nodes used| = " + idMapping.size());
		System.out.println("|waypoints| = " + (idMapping.size() - numVertices));
		System.out.println("|ways| = " + IdAssigner.numOverallWays);
		System.out.println("|ways used| = " + IdAssigner.numUsedWays);
		System.out.println("|V| = " + numVertices);
		System.out.println("|E| = " + RoutingGraphWriter.nextEdgeId);
	}

	private static class RoutingGraphWriter {
		private static int[] latitudes, longitudes;
		public static int countOverallNodes, countOverallWays, nextEdgeId;

		public static void writeGraph(File osmFile, final HashSet<EHighwayLevel> hwyLevels,
				final TLongIntHashMap idMapping, final int numVertices, Connection outputDb)
				throws IOException, SAXException, SQLException {
			final RgDbWriter writer = new RgDbWriter(outputDb);
			countOverallNodes = countOverallWays = nextEdgeId = 0;
			latitudes = new int[idMapping.size()];
			longitudes = new int[idMapping.size()];

			writer.clearTables();
			writer.flush();
			writer.insertHighwayLevels(hwyLevels);
			OsmXmlParser parser = new OsmXmlParser();
			parser.addNodeListener(new IOsmNodeListener() {
				@Override
				public void handleNode(OsmNode node) {
					if ((++countOverallNodes) % MSG_INT == 0) {
						System.out.println("[write routingGraph] - processed nodes "
								+ (countOverallNodes - MSG_INT) + " - " + countOverallNodes);
					}
					if (idMapping.containsKey(node.getId())) {
						int id = idMapping.get(node.getId());
						longitudes[id] = doubleCoordinateDegreeToInt(node.getLongitude());
						latitudes[id] = doubleCoordinateDegreeToInt(node.getLatitude());
						if (id < numVertices) {
							try {
								writer.insertVertex(new RgVertex(id,
										intCoordinateDegreeToDouble(longitudes[id]),
										intCoordinateDegreeToDouble(latitudes[id]), node
												.getId()));
							} catch (SQLException e) {
								e.printStackTrace();
								while (e.getNextException() != null) {
									e = e.getNextException();
									e.printStackTrace();
								}
							}
						}
					}
				}
			});
			parser.addWayListener(new IOsmWayListener() {
				@Override
				public void handleWay(OsmWay_withNodeRefs way) {
					if ((++countOverallWays) % MSG_INT == 0) {
						System.out.println("[write routingGraph] - processed ways "
								+ (countOverallWays - MSG_INT) + " - " + countOverallWays);
					}
					if (writer.numInsertedEdges() == 0) {
						try {
							writer.flush();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}

					EHighwayLevel hwyLvl = way.getHighwayLevel();
					if (way.isVisible() && way.getNodeRefs().size() > 1 && hwyLvl != null
							&& hwyLevels.contains(hwyLvl)) {
						LinkedList<Integer> indices = new LinkedList<Integer>();
						for (int i = 0; i < way.getNodeRefs().size(); i++) {
							int idx = idMapping.get(way.getNodeRefs().get(i));

							if (idx < numVertices) {
								indices.addLast(i);
							}
						}
						for (int i = 1; i < indices.size(); i++) {
							int start = indices.get(i - 1);
							int end = indices.get(i);
							double[] lon = new double[end - start + 1];
							double[] lat = new double[end - start + 1];

							for (int j = start; j <= end; j++) {
								int idx = idMapping.get(way.getNodeRefs().get(j));
								lon[j - start] = intCoordinateDegreeToDouble(longitudes[idx]);
								lat[j - start] = intCoordinateDegreeToDouble(latitudes[idx]);
							}

							try {
								int sourceId;
								int targetId;
								boolean oneway;
								double distanceMeters = 0d;
								for (int k = 1; k < lon.length; k++) {
									distanceMeters += GeoCoordinate.sphericalDistance(
											lon[k - 1], lat[k - 1], lon[k], lat[k]);
								}
								if (way.isOneway() == -1) {
									sourceId = idMapping.get(way.getNodeRefs().get(end));
									targetId = idMapping.get(way.getNodeRefs().get(start));
									oneway = true;
									lon = reverseDoubleArray(lon);
									lat = reverseDoubleArray(lat);
								} else {
									sourceId = idMapping.get(way.getNodeRefs().get(start));
									targetId = idMapping.get(way.getNodeRefs().get(end));
									oneway = way.isOneway() == 1;
								}
								writer.insertEdge(new RgEdge(nextEdgeId++, sourceId, targetId,
										lon, lat, !oneway, way.isUrban(), way.getId(), way
												.getName(), distanceMeters, hwyLvl));
							} catch (SQLException e) {
								e.printStackTrace();
								while (e.getNextException() != null) {
									e = e.getNextException();
									e.printStackTrace();
								}
							}
						}
					}
				}
			});
			parser.parse(osmFile);
			writer.flush();
		}
	}

	private static class IdAssigner {

		public static long numOverallNodes, numOverallWays, numUsedWays;
		private static int nextVertexId, nextWaypointId, numVertices;

		public static int assignNodeIds(File osmFile, final HashSet<EHighwayLevel> hwyLevels,
				final TLongIntHashMap buff) throws IOException, SAXException {
			numOverallNodes = numOverallWays = numUsedWays = nextVertexId = nextWaypointId = numVertices = 0;
			buff.clear();

			OsmXmlParser parser = new OsmXmlParser();
			parser.addNodeListener(new IOsmNodeListener() {

				@Override
				public void handleNode(OsmNode node) {
					if ((++numOverallNodes) % MSG_INT == 0) {
						System.out.println("[assign node id] - processed nodes "
								+ (numOverallNodes - MSG_INT) + " - " + numOverallNodes);
					}
				}
			});
			parser.addWayListener(new IOsmWayListener() {
				@Override
				public void handleWay(OsmWay_withNodeRefs way) {
					EHighwayLevel hwyLvl = way.getHighwayLevel();
					if (way.isVisible() && way.getNodeRefs().size() > 1 && hwyLvl != null
							&& hwyLevels.contains(hwyLvl)) {
						if (buff.containsKey(way.getNodeRefs().getFirst())) {
							buff.adjustValue(way.getNodeRefs().getFirst(), 1);
						} else {
							buff.put(way.getNodeRefs().getFirst(), 1);
						}
						for (int i = 1; i < way.getNodeRefs().size() - 1; i++) {
							long id = way.getNodeRefs().get(i);
							if (buff.containsKey(id)) {
								buff.adjustValue(id, buff.get(id) + 1);
							} else {
								buff.put(id, 0);
							}
						}
						if (buff.containsKey(way.getNodeRefs().getLast())) {
							buff.adjustValue(way.getNodeRefs().getLast(), 1);
						} else {
							buff.put(way.getNodeRefs().getLast(), 1);
						}
						numUsedWays++;
					}
					if ((++numOverallWays) % MSG_INT == 0) {
						System.out.println("[assign node id] - processed ways "
								+ (numOverallWays - MSG_INT) + " - " + numOverallWays);
					}
				}
			});
			parser.parse(osmFile);
			buff.forEachValue(new TIntProcedure() {
				@Override
				public boolean execute(int v) {
					if (v > 0) {
						numVertices++;
					}
					return true;
				}
			});
			assignVertexIds(numVertices, buff);
			return numVertices;
		}

		private static void assignVertexIds(int firstWaypointId, TLongIntHashMap map) {
			nextVertexId = 0;
			nextWaypointId = firstWaypointId;
			map.transformValues(new TIntFunction() {
				@Override
				public int execute(int v) {
					if (v == 0) {
						return nextWaypointId++;
					}
					return nextVertexId++;
				}
			});
		}
	}

	private static double[] reverseDoubleArray(double[] array) {
		double[] tmp = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			tmp[array.length - 1 - i] = array[i];
		}
		return tmp;
	}

	private static int doubleCoordinateDegreeToInt(double c) {
		return (int) Math.rint(c * COORDINATE_FAC);
	}

	private static double intCoordinateDegreeToDouble(int c) {
		return c / COORDINATE_FAC;
	}

	private static String usage() {
		return "osm2rg <properties file> \n"
				+ "osm2rg <db.host> <db.port> <db.name> <db.user> <db.pass> <comma separated ways whitelist> <input.file>";
	}

	public static void main(String[] args) throws IOException, SAXException, SQLException {

		// the parameters
		String dbHost, dbName, dbUser, dbPass;
		String[] waysWhiteList;
		File inputFile;
		int dbPort;

		// initialize parameters
		if (args.length == 1) {
			// get parameters form properties file
			Properties props = new Properties();
			props.load(new FileInputStream(args[0]));
			dbHost = props.getProperty("osm2rg.output.db.host");
			dbPort = Integer.parseInt(props.getProperty("osm2rg.output.db.port"));
			dbName = props.getProperty("osm2rg.output.db.name");
			dbUser = props.getProperty("osm2rg.output.db.username");
			dbPass = props.getProperty("osm2rg.output.db.password");
			waysWhiteList = props.getProperty("osm2rg.whitelist.ways.highwaylvl").split(",");
			inputFile = new File(props.getProperty("osm2rg.input.file"));
		} else if (args.length == 7) {
			// get parameter from command line
			dbHost = args[0];
			dbPort = Integer.parseInt(args[1]);
			dbName = args[2];
			dbUser = args[3];
			dbPass = args[4];
			waysWhiteList = args[5].split(",");
			inputFile = new File(args[6]);
		} else {
			System.out.println(usage());
			return;
		}

		// extract the routing graph

		HashSet<EHighwayLevel> hwyLevels = new HashSet<EHighwayLevel>();
		for (String hwyLvl : waysWhiteList) {
			hwyLevels.add(EHighwayLevel.valueOf(hwyLvl));
		}
		Connection outputDb = new DBConnection(dbHost, dbName, dbUser, dbPass, dbPort)
				.getConnection();
		RgExtractor.extractGraph(inputFile, hwyLevels, outputDb);

		outputDb.close();
	}
}
