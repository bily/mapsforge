/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.mapgenerator.databaserenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.rendertheme.RenderCallback;
import org.mapsforge.android.maps.rendertheme.RenderTheme;
import org.mapsforge.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.PointOfInterest;
import org.mapsforge.map.reader.Way;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.xml.sax.SAXException;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A DatabaseRenderer renders map tiles by reading from a {@link MapDatabase}.
 */
public class DatabaseRenderer implements MapGenerator, RenderCallback {
	private static final Byte DEFAULT_START_ZOOM_LEVEL = Byte.valueOf((byte) 12);
	private static final byte LAYERS = 11;
	private static final Logger LOGGER = Logger.getLogger(DatabaseRenderer.class.getName());
	private static final Paint PAINT_WATER_TILE_HIGHTLIGHT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final double STROKE_INCREASE = 1.5;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
	private static final float[][] WATER_TILE_COORDINATES = new float[][] { { 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE,
			Tile.TILE_SIZE, 0, Tile.TILE_SIZE, 0, 0 } };
	private static final byte ZOOM_MAX = 22;

	private static RenderTheme getRenderTheme(JobTheme jobTheme) {
		try {
			return RenderThemeHandler.getRenderTheme(jobTheme);
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (SAXException e) {
			LOGGER.log(Level.SEVERE, null, e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}
		return null;
	}

	private static byte getValidLayer(byte layer) {
		if (layer < 0) {
			return 0;
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	private final List<PointTextContainer> areaLabels;
	private final CanvasRasterer canvasRasterer;
	private float[][] coordinates;
	private Tile currentTile;
	private WayLayer drawingLayer;
	private final LabelPlacement labelPlacement;
	private MapDatabase mapDatabase;
	private List<PointTextContainer> nodes;
	private final List<SymbolContainer> pointSymbols;
	private float poiX;
	private float poiY;
	private JobTheme previousJobTheme;
	private float previousTextScale;
	private byte previousZoomLevel;
	private RenderTheme renderTheme;
	private ShapeContainer shapeContainer;
	private final List<Tag> tagList;
	private final List<WayTextContainer> wayNames;
	private final List<WayLayer> ways;
	private final List<SymbolContainer> waySymbols;

	private Tag nameTag, refTag, houseNrTag, elevationTag;

	/**
	 * Constructs a new DatabaseRenderer.
	 */
	public DatabaseRenderer() {
		this.canvasRasterer = new CanvasRasterer();
		this.labelPlacement = new LabelPlacement();

		this.ways = new ArrayList<WayLayer>(LAYERS);
		this.wayNames = new ArrayList<WayTextContainer>(64);
		this.nodes = new ArrayList<PointTextContainer>(64);
		this.areaLabels = new ArrayList<PointTextContainer>(64);
		this.waySymbols = new ArrayList<SymbolContainer>(64);
		this.pointSymbols = new ArrayList<SymbolContainer>(64);
		this.tagList = new ArrayList<Tag>(2);

		PAINT_WATER_TILE_HIGHTLIGHT.setStyle(Paint.Style.FILL);
		PAINT_WATER_TILE_HIGHTLIGHT.setColor(Color.CYAN);
	}

	@Override
	public void cleanup() {
		if (this.renderTheme != null) {
			this.renderTheme.destroy();
		}
	}

	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		this.currentTile = mapGeneratorJob.tile;

		JobTheme jobTheme = mapGeneratorJob.jobParameters.jobTheme;
		if (!jobTheme.equals(this.previousJobTheme)) {
			this.renderTheme = getRenderTheme(jobTheme);
			if (this.renderTheme == null) {
				this.previousJobTheme = null;
				return false;
			}
			createWayLists();
			this.previousJobTheme = jobTheme;
			this.previousZoomLevel = Byte.MIN_VALUE;
		}

		byte zoomLevel = this.currentTile.zoomLevel;
		if (zoomLevel != this.previousZoomLevel) {
			setScaleStrokeWidth(zoomLevel);
			this.previousZoomLevel = zoomLevel;
		}

		float textScale = mapGeneratorJob.jobParameters.textScale;
		if (Float.compare(textScale, this.previousTextScale) != 0) {
			this.renderTheme.scaleTextSize(textScale);
			this.previousTextScale = textScale;
		}

		if (this.mapDatabase != null) {
			MapReadResult mapReadResult = this.mapDatabase.readMapData(this.currentTile);
			processReadMapData(mapReadResult);
		}

		this.nodes = this.labelPlacement.placeLabels(this.nodes, this.pointSymbols, this.areaLabels, this.currentTile);

		this.canvasRasterer.setCanvasBitmap(bitmap);
		this.canvasRasterer.fill(this.renderTheme.getMapBackground());
		this.canvasRasterer.drawWays(this.ways);
		this.canvasRasterer.drawSymbols(this.waySymbols);
		this.canvasRasterer.drawSymbols(this.pointSymbols);
		this.canvasRasterer.drawWayNames(this.wayNames);
		this.canvasRasterer.drawNodes(this.nodes);
		this.canvasRasterer.drawNodes(this.areaLabels);

		if (mapGeneratorJob.debugSettings.drawTileFrames) {
			this.canvasRasterer.drawTileFrame();
		}

		if (mapGeneratorJob.debugSettings.drawTileCoordinates) {
			this.canvasRasterer.drawTileCoordinates(this.currentTile);
		}

		clearLists();

		return true;
	}

	@Override
	public GeoPoint getStartPoint() {
		if (this.mapDatabase != null && this.mapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = this.mapDatabase.getMapFileInfo();
			if (mapFileInfo.startPosition != null) {
				return mapFileInfo.startPosition;
			} else if (mapFileInfo.mapCenter != null) {
				return mapFileInfo.mapCenter;
			}
		}

		return null;
	}

	@Override
	public Byte getStartZoomLevel() {
		if (this.mapDatabase != null && this.mapDatabase.hasOpenFile()) {
			MapFileInfo mapFileInfo = this.mapDatabase.getMapFileInfo();
			if (mapFileInfo.startZoomLevel != null) {
				return mapFileInfo.startZoomLevel;
			}
		}

		return DEFAULT_START_ZOOM_LEVEL;
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}

	@Override
	public void renderArea(Paint paint, int level) {
		this.drawingLayer.get(level).add(this.shapeContainer, paint);
	}

	@Override
	public void renderAreaCaption(String textKey, float verticalOffset, Paint paint, Paint stroke) {
		String caption = getTextTagValue(textKey);
		if (caption == null)
			return;

		float[] centerPosition = GeometryUtils.calculateCenterOfBoundingBox(this.coordinates[0]);
		this.areaLabels.add(new PointTextContainer(caption, centerPosition[0], centerPosition[1], paint, stroke));
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol, int width, int height) {
		float[] centerPosition = GeometryUtils.calculateCenterOfBoundingBox(this.coordinates[0]);
		this.pointSymbols.add(new SymbolContainer(symbol, centerPosition[0] - (width >> 1), centerPosition[1]
				- (height >> 1)));
	}

	@Override
	public void renderPointOfInterestCaption(String textKey, float verticalOffset, Paint paint, Paint stroke) {
		String caption = getTextTagValue(textKey);
		if (caption == null)
			return;
		this.nodes.add(new PointTextContainer(caption, this.poiX, this.poiY + verticalOffset, paint, stroke));
	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint outline, int level) {
		this.drawingLayer.get(level).add(new CircleContainer(this.poiX, this.poiY, radius), outline);
	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap symbol, int width, int height) {
		this.pointSymbols.add(new SymbolContainer(symbol, this.poiX - (width >> 1), this.poiY - (height >> 1)));
	}

	@Override
	public void renderWay(Paint paint, int level) {
		this.drawingLayer.get(level).add(this.shapeContainer, paint);
	}

	@Override
	public void renderWaySymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol) {
		WayDecorator.renderSymbol(symbolBitmap, alignCenter, repeatSymbol, this.coordinates, this.waySymbols);
	}

	@Override
	public void renderWayText(String textKey, Paint paint, Paint outline) {
		String text = getTextTagValue(textKey);
		if (text != null)
			WayDecorator.renderText(text, paint, outline, this.coordinates, this.wayNames);
	}

	@Override
	public boolean requiresInternetConnection() {
		return false;
	}

	/**
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	public void setMapDatabase(MapDatabase mapDatabase) {
		this.mapDatabase = mapDatabase;
	}

	private void clearLists() {
		for (int i = this.ways.size() - 1; i >= 0; --i) {
			this.ways.get(i).clear();
		}

		this.areaLabels.clear();
		this.nodes.clear();
		this.pointSymbols.clear();
		this.wayNames.clear();
		this.waySymbols.clear();
	}

	private void createWayLists() {
		int levels = this.renderTheme.getLevels();
		this.ways.clear();

		for (byte i = LAYERS - 1; i >= 0; --i) {
			this.ways.add(new WayLayer(levels));
		}
	}

	private void processReadMapData(MapReadResult mapReadResult) {
		if (mapReadResult == null) {
			return;
		}

		for (PointOfInterest pointOfInterest : mapReadResult.pointOfInterests) {
			renderPointOfInterest(pointOfInterest);
		}

		for (Way way : mapReadResult.ways) {
			renderWay(way);
		}

		if (mapReadResult.isWater) {
			renderWaterBackground();
		}
	}

	private void renderPointOfInterest(PointOfInterest pointOfInterest) {
		this.drawingLayer = this.ways.get(getValidLayer(pointOfInterest.layer));
		this.poiX = scaleLongitude(pointOfInterest.position.longitudeE6);
		this.poiY = scaleLatitude(pointOfInterest.position.latitudeE6);
		filterTags(pointOfInterest.tags);
		this.renderTheme.matchNode(this, pointOfInterest.tags, this.currentTile.zoomLevel);
	}

	private void renderWaterBackground() {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_WATER);
		this.coordinates = WATER_TILE_COORDINATES;
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	private static float PI180 = (float) (Math.PI / 180) / 1000000.0f;
	private static float PIx4 = (float) Math.PI * 4;

	private void renderWay(Way way) {
		this.drawingLayer = this.ways.get(getValidLayer(way.layer));
		// TODO what about the label position?

		filterTags(way.tags);

		this.coordinates = way.wayNodes;

		long x = this.currentTile.getPixelX();
		long y = this.currentTile.getPixelY();
		long z = Tile.TILE_SIZE << this.currentTile.zoomLevel;

		for (int i = 0, n = this.coordinates.length; i < n; ++i) {
			float[] coords = this.coordinates[i];
			for (int j = 0, m = coords.length; j < m; j += 2) {
				coords[j] = (float) ((coords[j] / 1000000.0 + 180) / 360 * z) - x;

				double sinLatitude = Math.sin(coords[j + 1] * PI180);
				coords[j + 1] = (float) (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / PIx4) * z - y;
			}
		}
		this.shapeContainer = new WayContainer(this.coordinates);

		if (GeometryUtils.isClosedWay(this.coordinates[0])) {
			this.renderTheme.matchClosedWay(this, way.tags, this.currentTile.zoomLevel);
		} else {
			this.renderTheme.matchLinearWay(this, way.tags, this.currentTile.zoomLevel);
		}
	}

	private void filterTags(List<Tag> tags) {
		this.nameTag = null;
		this.refTag = null;
		this.houseNrTag = null;
		this.elevationTag = null;

		// filter out 'individual' tags before passing tag list
		// to RenderTheme matcher
		for (int i = tags.size() - 1; i >= 0; i--) {
			Tag tag = tags.get(i);
			if (Tag.TAG_KEY_NAME == tag.key)
				this.nameTag = tags.remove(i);
			else if (Tag.TAG_KEY_HOUSE_NUMBER == tag.key)
				this.houseNrTag = tags.remove(i);
			else if (Tag.TAG_KEY_ELE == tag.key)
				this.elevationTag = tags.remove(i);
			else if (Tag.TAG_KEY_REF == tag.key)
				this.refTag = tags.remove(i);
			else
				// all individual tags are at the end of tag list
				break;
		}
	}

	/**
	 * Converts a latitude value into an Y coordinate on the current tile.
	 * 
	 * @param latitude
	 *            the latitude value.
	 * @return the Y coordinate on the current tile.
	 */
	private float scaleLatitude(float latitude) {
		return (float) (MercatorProjection.latitudeToPixelY(latitude / (double) 1000000, this.currentTile.zoomLevel) - this.currentTile
				.getPixelY());
	}

	/**
	 * Converts a longitude value into an X coordinate on the current tile.
	 * 
	 * @param longitude
	 *            the longitude value.
	 * @return the X coordinate on the current tile.
	 */
	private float scaleLongitude(float longitude) {
		return (float) (MercatorProjection.longitudeToPixelX(longitude / (double) 1000000, this.currentTile.zoomLevel) - this.currentTile
				.getPixelX());
	}

	/**
	 * Sets the scale stroke factor for the given zoom level.
	 * 
	 * @param zoomLevel
	 *            the zoom level for which the scale stroke factor should be set.
	 */
	private void setScaleStrokeWidth(byte zoomLevel) {
		int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		this.renderTheme.scaleStrokeWidth((float) Math.pow(STROKE_INCREASE, zoomLevelDiff));
	}

	private String getTextTagValue(String textKey) {
		Tag tag = null;
		if (textKey == Tag.TAG_KEY_NAME)
			tag = this.nameTag;
		else if (textKey == Tag.TAG_KEY_HOUSE_NUMBER)
			tag = this.houseNrTag;
		else if (textKey == Tag.TAG_KEY_REF)
			tag = this.refTag;
		else if (textKey == Tag.TAG_KEY_ELE)
			tag = this.elevationTag;

		if (tag != null)
			return tag.value;

		return null;
	}
}
