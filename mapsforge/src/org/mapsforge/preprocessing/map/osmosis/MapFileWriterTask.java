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
package org.mapsforge.preprocessing.map.osmosis;

import gnu.trove.list.array.TLongArrayList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.Rect;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * An Osmosis plugin that reads OpenStreetMap data and converts it to a mapsforge binary file.
 * 
 * @author bross
 * 
 */
public class MapFileWriterTask implements Sink {
	private static final Logger logger = Logger.getLogger(MapFileWriterTask.class
			.getName());

	private TileBasedDataStore tileBasedGeoObjectStore;

	// temporary node data
	// IndexStore<Long, MapNode> indexStore;

	// Accounting
	private int amountOfNodesProcessed = 0;
	private int amountOfWaysProcessed = 0;
	private int amountOfRelationsProcessed = 0;
	private int amountOfMultipolygons = 0;

	// configuration parameters
	private File outFile;
	private GeoCoordinate mapStartPosition;
	private boolean debugInfo;
	private boolean waynodeCompression;
	private boolean pixelFilter;
	private boolean polygonClipping;
	private String comment;
	private ZoomIntervalConfiguration zoomIntervalConfiguration;
	private int threadpoolSize;
	private String type;
	private int bboxEnlargement;

	MapFileWriterTask(String outFile, String bboxString, String mapStartPosition,
			String comment,
			String zoomIntervalConfigurationString, boolean debugInfo,
			boolean waynodeCompression, boolean pixelFilter, boolean polygonClipping,
			int threadpoolSize, String type, int bboxEnlargement) {
		this.outFile = new File(outFile);
		if (this.outFile.isDirectory()) {
			throw new IllegalArgumentException(
					"file parameter points to a directory, must be a file");
		}
		this.mapStartPosition = mapStartPosition == null ? null : GeoCoordinate
				.fromString(mapStartPosition);
		this.debugInfo = debugInfo;
		this.waynodeCompression = waynodeCompression;
		this.pixelFilter = pixelFilter;
		this.polygonClipping = polygonClipping;
		this.comment = comment;

		if (threadpoolSize < 1 || threadpoolSize > 128)
			throw new IllegalArgumentException("make sure that 1 <= threadpool size <= 128");
		this.threadpoolSize = threadpoolSize;

		Rect bbox = bboxString == null ? null : Rect.fromString(bboxString);
		this.zoomIntervalConfiguration = zoomIntervalConfigurationString == null ? ZoomIntervalConfiguration
				.getStandardConfiguration()
				: ZoomIntervalConfiguration.fromString(zoomIntervalConfigurationString);

		this.type = type;
		if (!type.equalsIgnoreCase("ram") && !type.equalsIgnoreCase("hd"))
			throw new IllegalArgumentException("type argument must equal ram or hd, found: "
					+ type);

		if (bbox != null) {
			if (type.equalsIgnoreCase("ram"))
				this.tileBasedGeoObjectStore = RAMTileBasedDataStore.newInstance(bbox,
						zoomIntervalConfiguration, bboxEnlargement);
			else
				this.tileBasedGeoObjectStore = HDTileBasedDataStore.newInstance(bbox,
						zoomIntervalConfiguration, bboxEnlargement);
		}
		this.bboxEnlargement = bboxEnlargement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openstreetmap.osmosis.core.lifecycle.Completable#complete()
	 */
	@Override
	public void complete() {
		NumberFormat nfMegabyte = NumberFormat.getInstance();
		NumberFormat nfCounts = NumberFormat.getInstance();
		nfCounts.setGroupingUsed(true);
		nfMegabyte.setMaximumFractionDigits(2);

		logger.info("completing read...");
		tileBasedGeoObjectStore.complete();

		logger.info("start writing file...");

		try {
			if (outFile.exists() && !outFile.isDirectory()) {
				logger.info("overwriting file " + outFile.getAbsolutePath());
				outFile.delete();
			}
			RandomAccessFile file = new RandomAccessFile(outFile, "rw");
			MapFileWriter mfw = new MapFileWriter(tileBasedGeoObjectStore, file,
					threadpoolSize, bboxEnlargement);
			// mfw.writeFileWithDebugInfos(System.currentTimeMillis(), 1, (short) 256);
			mfw.writeFile(System.currentTimeMillis(), 1, (short) 256, comment, debugInfo,
					waynodeCompression, polygonClipping, pixelFilter, mapStartPosition);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error while writing file", e);
		}

		logger.info("finished...");
		logger.info("total processed nodes: " + nfCounts.format(amountOfNodesProcessed));
		logger.info("total processed ways: " + nfCounts.format(amountOfWaysProcessed));
		logger.info("total processed relations: " + nfCounts.format(amountOfRelationsProcessed));
		logger.info("total processed multipolygons: " + amountOfMultipolygons);
		logger.info("estimated memory consumption: " + nfMegabyte.format(
						+((Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
								.freeMemory()) / Math.pow(1024, 2))) + "MB");
	}

	@Override
	public void release() {
		// nothing to do here
	}

	@Override
	public void process(EntityContainer entityContainer) {

		Entity entity = entityContainer.getEntity();

		switch (entity.getType()) {

			case Bound:
				Bound bound = (Bound) entity;
				if (tileBasedGeoObjectStore == null) {
					if (type.equalsIgnoreCase("ram"))
						tileBasedGeoObjectStore =
								RAMTileBasedDataStore.newInstance(
										bound.getBottom(), bound.getTop(),
										bound.getLeft(), bound.getRight(),
										zoomIntervalConfiguration, bboxEnlargement);
					else
						tileBasedGeoObjectStore =
								HDTileBasedDataStore.newInstance(
										bound.getBottom(), bound.getTop(),
										bound.getLeft(), bound.getRight(),
										zoomIntervalConfiguration, bboxEnlargement);
				}
				logger.info("start reading data...");
				break;

			// *******************************************************
			// ****************** NODE PROCESSING*********************
			// *******************************************************
			case Node: {

				if (tileBasedGeoObjectStore == null) {
					logger.severe("No valid bounding box found in input data.\n" +
							"Please provide valid bounding box via command " +
							"line parameter 'bbox=minLat,minLon,maxLat,maxLon'.\n" +
							"Tile based data store not initialized. Aborting...");
					throw new IllegalStateException(
							"tile based data store not initialized, missing bounding " +
									"box information in input data");
				}
				tileBasedGeoObjectStore.addNode((Node) entity);
				// hint to GC
				entity = null;
				amountOfNodesProcessed++;
				break;
			}

				// *******************************************************
				// ******************* WAY PROCESSING*********************
				// *******************************************************
			case Way: {
				tileBasedGeoObjectStore.addWay((Way) entity);
				entity = null;
				amountOfWaysProcessed++;
				break;
			}

				// *******************************************************
				// ****************** RELATION PROCESSING*********************
				// *******************************************************
			case Relation:
				Relation currentRelation = (Relation) entity;

				if (isWayMultiPolygon(currentRelation)) {
					EnumSet<WayEnum> relationTags = EnumSet.noneOf(WayEnum.class);
					for (Tag tag : currentRelation.getTags()) {
						WayEnum wayTag = WayEnum
								.fromString(tag.getKey() + "=" + tag.getValue());
						if (wayTag != null)
							relationTags.add(wayTag);
					}

					List<Long> outerMemberIDs = new ArrayList<Long>();
					TLongArrayList innerMemberIDs = new TLongArrayList();
					// currentRelation.get
					for (RelationMember member : currentRelation.getMembers()) {
						if ("outer".equals(member.getMemberRole()))
							outerMemberIDs.add(member.getMemberId());
						else if ("inner".equals(member.getMemberRole()))
							innerMemberIDs.add(member.getMemberId());
					}

					if (innerMemberIDs.size() > 0) {
						long[] innerMemberIDsArray = innerMemberIDs.toArray();
						for (Long outerID : outerMemberIDs) {
							if (tileBasedGeoObjectStore.addWayMultipolygon(outerID,
									innerMemberIDsArray, relationTags))
								amountOfMultipolygons++;
						}
					}

				}

				amountOfRelationsProcessed++;
				entity = null;
				break;
			default:
				System.out.println(entity.getTags());
		}

	}

	private boolean isWayMultiPolygon(Relation candidate) {
		assert candidate != null;
		if (candidate.getTags() == null)
			return false;

		for (RelationMember member : candidate.getMembers()) {
			if (member.getMemberType() != EntityType.Way)
				return false;
		}
		for (Tag tag : candidate.getTags()) {
			if (tag.getKey().equalsIgnoreCase("type")
					&& tag.getValue().equalsIgnoreCase("multipolygon"))
				return true;
		}
		return false;

	}
	// private class MapNode extends org.mapsforge.core.Node implements IndexElement<Long> {
	//
	// public MapNode(long id, double latitude, double longitude) {
	// super(id, latitude, longitude);
	// }
	//
	// @Override
	// public void store(StoreWriter sw, StoreClassRegister scr) {
	// scr.storeIdentifierForClass(sw, MapNode.class);
	// sw.writeDouble(getLatitude());
	// sw.writeDouble(getLongitude());
	//
	// }
	//
	// @Override
	// public Long getKey() {
	// return getId();
	// }
	//
	// }

}
