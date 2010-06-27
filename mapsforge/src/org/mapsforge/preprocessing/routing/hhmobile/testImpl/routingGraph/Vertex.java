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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph;


public class Vertex {

	private final int neighborhood;
	private final int id, idSubj, idOverly, idLvlZero;
	private final byte lvl;
	private final int lon, lat;
	private final Edge[] outboundEdges;

	public Vertex(int neighborhood, int id, int idSubj, int idOverly, int idLvlZero, byte lvl,
			int lon, int lat, Edge[] outboundEdges) {
		this.neighborhood = neighborhood;
		this.id = id;
		this.idSubj = idSubj;
		this.idOverly = idOverly;
		this.idLvlZero = idLvlZero;
		this.lvl = lvl;
		this.lon = lon;
		this.lat = lat;
		this.outboundEdges = outboundEdges;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Vertex.class.getName() + " (\n");
		sb.append("  neighborhood = " + neighborhood + "\n");
		sb.append("  id = " + id + "\n");
		sb.append("  idSubj = " + idSubj + "\n");
		sb.append("  idOverly = " + idOverly + "\n");
		sb.append("  idLvlZero = " + idLvlZero + "\n");
		sb.append("  lvl = " + lvl + "\n");
		sb.append("  lon = " + lon + "\n");
		sb.append("  lat = " + lat + "\n");
		sb.append("  outboundEdges = " + outboundEdges.length + "\n");
		sb.append("(");
		return sb.toString();
	}

	public int getNeighborhood() {
		return neighborhood;
	}

	public int getId() {
		return id;
	}

	public int getIdSubj() {
		return idSubj;
	}

	public int getIdOverly() {
		return idOverly;
	}

	public int getIdLvlZero() {
		return idLvlZero;
	}

	public byte getLvl() {
		return lvl;
	}

	public int getLon() {
		return lon;
	}

	public int getLat() {
		return lat;
	}

	public Edge[] getOutboundEdges() {
		return outboundEdges;
	}
}