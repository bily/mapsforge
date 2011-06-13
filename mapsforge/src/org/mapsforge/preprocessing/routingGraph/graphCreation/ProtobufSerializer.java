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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.AllGraphDataPBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.CompleteEdgePBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.CompleteNodePBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.CompleteRelationPBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.CompleteVertexPBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.GeoCoordinatePBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.KeyValuePairPBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.RelationMemberPBF;
import org.mapsforge.preprocessing.routingGraph.graphCreation.GraphCreatorProtos.RelationMemberPBF.MemberType;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;

/**
 * This class can be used to save or load the data, necessary for a routing graph.
 * 
 * @author Michael Bartel
 * 
 */
public class ProtobufSerializer {

	/**
	 * This methods loads data from a pbf-file.
	 * 
	 * @param path
	 *            , where the file is located
	 * @param vertices
	 *            , all nodes will be written into that map
	 * @param edges
	 *            , all nodes will be written into that map
	 * @param relations
	 *            , all nodes will be written into that map
	 */
	public static void loadFromFile(String path, HashMap<Integer, CompleteVertex> vertices,
			HashMap<Integer, CompleteEdge> edges,
			HashMap<Integer, CompleteRelation> relations) {
		AllGraphDataPBF allGraphData = null;
		try {
			allGraphData = AllGraphDataPBF.parseFrom(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH.mm.ss");
		String dateTime = sdf.format(new Date());

		System.out.println("[" + dateTime + "] Start of object(" + path + ") read!!");
		readVertices(allGraphData, vertices);
		readEdges(allGraphData, edges, vertices);
		readRelations(allGraphData, relations);
		dateTime = sdf.format(new Date());
		System.out.println("[" + dateTime + "] End of object read!!");

	}

	/**
	 * This method saves the lists to a protobuf file, using GraphCreatorProtos.java
	 * 
	 * @param path
	 *            , where the file is located
	 * @param vertices
	 *            the vertices to be saved
	 * @param edges
	 *            the edges to be saved
	 * @param relations
	 *            the relations to be saved
	 */
	public static void saveToFile(String path,
			HashMap<Integer, CompleteVertex> vertices,
			HashMap<Integer, CompleteEdge> edges,
			HashMap<Integer, CompleteRelation> relations) {

		AllGraphDataPBF.Builder allGraphData = AllGraphDataPBF.newBuilder();

		writeEdges(allGraphData, edges);

		writeVertices(allGraphData, vertices);

		writeRelations(allGraphData, relations);

		FileOutputStream output;
		try {
			output = new FileOutputStream(path);
			allGraphData.build().writeTo(output);
			output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void readEdges(AllGraphDataPBF allGraphData,
			HashMap<Integer, CompleteEdge> edges, HashMap<Integer, CompleteVertex> vertices) {

		edges.clear();
		int i = 1;
		for (CompleteEdgePBF ce_pbf : allGraphData.getAllEdgesList()) {

			HashSet<KeyValuePair> additionalTags = new HashSet<KeyValuePair>();

			for (KeyValuePairPBF kv_pbf : ce_pbf.getAdditionalTagsList()) {
				additionalTags.add(new KeyValuePair(kv_pbf.getValue(), kv_pbf.getKey()));
			}

			GeoCoordinate[] allWP = new GeoCoordinate[ce_pbf.getAllWaypointsCount()];

			for (int j = 0; j < allWP.length; j++) {
				GeoCoordinatePBF geo_pbf = ce_pbf.getAllWaypointsList().get(j);
				allWP[j] = new GeoCoordinate(geo_pbf.getLatitude(), geo_pbf.getLongitude());
			}
			// read nodes
			HashSet<CompleteNode> allUsedNodes = new HashSet<CompleteNode>();

			for (CompleteNodePBF node_pbf : ce_pbf.getAllUsedNodesList()) {
				GeoCoordinate coordinate = new GeoCoordinate(node_pbf.getCoordinate().getLatitude(),
						node_pbf.getCoordinate().getLongitude());
				HashSet<KeyValuePair> hs = new HashSet<KeyValuePair>();

				for (KeyValuePairPBF kv_pbf : node_pbf.getAdditionalTagsList()) {
					hs.add(new KeyValuePair(kv_pbf.getValue(), kv_pbf.getKey()));
				}

				allUsedNodes.add(new CompleteNode(node_pbf.getId(), coordinate, hs));
			}

			CompleteEdge ce = new CompleteEdge(
					ce_pbf.getId(),
					vertices.get(ce_pbf.getSourceID()),
					vertices.get(ce_pbf.getTargetID()),
					null,
					allWP,
					ce_pbf.getName(),
					ce_pbf.getType(),
					ce_pbf.getRoundabout(),
					ce_pbf.getIsOneWay(),
					ce_pbf.getRef(),
					ce_pbf.getDestination(),
					ce_pbf.getWeight(),
					additionalTags,
					allUsedNodes);

			edges.put(i, ce);
			i++;

		}

	}

	private static void readVertices(AllGraphDataPBF allGraphData,
			HashMap<Integer, CompleteVertex> vertices) {

		vertices.clear();

		for (CompleteVertexPBF cv_pbf : allGraphData.getAllVerticesList()) {

			HashSet<KeyValuePair> additionalTags = new HashSet<KeyValuePair>();

			for (KeyValuePairPBF kv_PBF : cv_pbf.getAdditionalTagsList()) {
				additionalTags.add(new KeyValuePair(kv_PBF.getValue(), kv_PBF.getKey()));
			}

			CompleteVertex cv = new CompleteVertex(cv_pbf.getId(),
					null,
					new GeoCoordinate(cv_pbf.getCoordinate().getLatitude(), cv_pbf
							.getCoordinate().getLongitude()),
					additionalTags);
			vertices.put(cv.id, cv);

		}
	}

	private static void readRelations(AllGraphDataPBF allGraphData,
			HashMap<Integer, CompleteRelation> relations) {

		relations.clear();

		int j = 0;
		for (CompleteRelationPBF cr_pbf : allGraphData.getAllRelationsList()) {

			RelationMember[] member = new RelationMember[cr_pbf.getMemberCount()];

			int i = 0;
			for (RelationMemberPBF rm_pbf : cr_pbf.getMemberList()) {
				EntityType memberType = null;

				if (rm_pbf.getMemberType() == MemberType.NODE)
					memberType = EntityType.Node;
				if (rm_pbf.getMemberType() == MemberType.WAY)
					memberType = EntityType.Way;
				if (rm_pbf.getMemberType() == MemberType.RELATION)
					memberType = EntityType.Relation;

				member[i] = new RelationMember(rm_pbf.getMemberId(), memberType,
						rm_pbf.getMemberRole());
				i++;
			}

			HashSet<KeyValuePair> additionalTags = new HashSet<KeyValuePair>();

			for (KeyValuePairPBF kv_PBF : cr_pbf.getTagsList()) {
				additionalTags.add(new KeyValuePair(kv_PBF.getValue(), kv_PBF.getKey()));
			}
			CompleteRelation cr = new CompleteRelation(member, additionalTags);
			relations.put(j, cr);
			j++;
		}

	}

	private static void writeEdges(AllGraphDataPBF.Builder allGraphData,
			HashMap<Integer, CompleteEdge> edges) {
		for (CompleteEdge ce : edges.values()) {
			CompleteEdgePBF.Builder ce_PBF = CompleteEdgePBF.newBuilder();

			ce_PBF.setId(ce.id);
			ce_PBF.setSourceID(ce.source.getId());
			ce_PBF.setTargetID(ce.target.getId());
			if (ce.name != null)
				ce_PBF.setName(ce.name);
			if (ce.type != null)
				ce_PBF.setType(ce.type);
			ce_PBF.setRoundabout(ce.roundabout);
			ce_PBF.setIsOneWay(ce.isOneWay);
			if (ce.ref != null)
				ce_PBF.setRef(ce.ref);
			if (ce.destination != null)
				ce_PBF.setDestination(ce.destination);
			ce_PBF.setWeight(ce.weight);

			for (KeyValuePair kv : ce.additionalTags) {
				KeyValuePairPBF.Builder kv_PBF = KeyValuePairPBF.newBuilder().setKey(kv.key);
				kv_PBF.setValue(kv.value);
				ce_PBF.addAdditionalTags(kv_PBF);
			}

			for (GeoCoordinate geo : ce.allWaypoints) {
				GeoCoordinatePBF.Builder geo_PBF = GeoCoordinatePBF.newBuilder();
				geo_PBF.setLatitude(geo.getLatitude());
				geo_PBF.setLongitude(geo.getLongitude());
				ce_PBF.addAllWaypoints(geo_PBF);
			}
			// write nodes
			for (CompleteNode node : ce.allUsedNodes) {
				CompleteNodePBF.Builder node_PBF = CompleteNodePBF.newBuilder();
				node_PBF.setId(node.id);

				GeoCoordinatePBF.Builder geo_PBF = GeoCoordinatePBF.newBuilder();
				geo_PBF.setLatitude(node.coordinate.getLatitude());
				geo_PBF.setLongitude(node.coordinate.getLongitude());
				node_PBF.setCoordinate(geo_PBF);

				for (KeyValuePair kv : node.additionalTags) {
					KeyValuePairPBF.Builder kv_PBF = KeyValuePairPBF.newBuilder();
					kv_PBF.setKey(kv.key);
					kv_PBF.setValue(kv.value);
					node_PBF.addAdditionalTags(kv_PBF);
				}

				ce_PBF.addAllUsedNodes(node_PBF);

			}
			allGraphData.addAllEdges(ce_PBF);
		}

	}

	private static void writeVertices(AllGraphDataPBF.Builder allGraphData,
			HashMap<Integer, CompleteVertex> vertices) {
		for (CompleteVertex cv : vertices.values()) {
			CompleteVertexPBF.Builder cv_PBF = CompleteVertexPBF.newBuilder();
			cv_PBF.setId(cv.id);

			GeoCoordinatePBF.Builder geo_PBF = GeoCoordinatePBF.newBuilder();
			geo_PBF.setLatitude(cv.coordinate.getLatitude());
			geo_PBF.setLongitude(cv.coordinate.getLongitude());
			cv_PBF.setCoordinate(geo_PBF);

			for (KeyValuePair kv : cv.additionalTags) {
				KeyValuePairPBF.Builder kv_PBF = KeyValuePairPBF.newBuilder();
				kv_PBF.setKey(kv.key);
				kv_PBF.setValue(kv.value);
				cv_PBF.addAdditionalTags(kv_PBF);
			}

			allGraphData.addAllVertices(cv_PBF);

		}
	}

	private static void writeRelations(AllGraphDataPBF.Builder allGraphData,
			HashMap<Integer, CompleteRelation> relations) {
		for (CompleteRelation cr : relations.values()) {
			CompleteRelationPBF.Builder cr_PBF = CompleteRelationPBF.newBuilder();

			for (RelationMember rm : cr.member) {
				RelationMemberPBF.Builder rm_PBF =
						RelationMemberPBF.newBuilder();

				rm_PBF.setMemberId(rm.getMemberId());
				rm_PBF.setMemberRole(rm.getMemberRole());

				switch (rm.getMemberType()) {
					case Node:
						rm_PBF.setMemberType(MemberType.NODE);
						break;
					case Way:
						rm_PBF.setMemberType(MemberType.WAY);
						break;
					case Relation:
						rm_PBF.setMemberType(MemberType.RELATION);
						break;
					case Bound:
						break;
				}

				cr_PBF.addMember(rm_PBF);
			}

			for (KeyValuePair kv : cr.tags) {
				KeyValuePairPBF.Builder kv_PBF =
						KeyValuePairPBF.newBuilder();
				kv_PBF.setKey(kv.key);
				kv_PBF.setValue(kv.value);
				cr_PBF.addTags(kv_PBF);
			}

			allGraphData.addAllRelations(cr_PBF);

		}
	}

}