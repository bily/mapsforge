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
package org.mapsforge.server.routing;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.HHRenderer;
import org.mapsforge.preprocessing.util.DBConnection;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.highwayHierarchies.RouterImpl;

public class RouterFactory {

	private final static String PROPERTIES_FILE = "res/conf/routerFactory.properties";
	private final static Logger logger = Logger.getLogger(RouterFactory.class.getName());

	public static IRouter getRouter() {
		Properties props = loadProperties();
		if (props != null) {
			String algorithm = props.getProperty("algorithm");
			if (algorithm == null) {
				logger.info("No algorithm specified in properties file.");
			} else if (algorithm.equals("hh")) {
				return getHHRouter(props);
			} else {
				logger.info("Algorithm not found : '" + algorithm + "'");
			}
		} else {
			logger.info("Could not Load properties file");
		}
		return null;
	}

	private static IRouter getHHRouter(Properties props) {
		String filename = props.getProperty("hh.file");
		if (filename == null) {
			logger.info("No file name specified for HHRouter.");
			return null;
		}

		RouterImpl hhRouter = null;
		// try read from file :
		try {
			FileInputStream iStream = new FileInputStream(filename);
			hhRouter = RouterImpl.deserialize(iStream);

		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		}

		// try get from db :

		if (hhRouter == null) {
			logger.info("Could not load HHRouter from file.");
			Connection conn;
			try {
				String hostName = props.getProperty("hh.input.db.host");
				String dbName = props.getProperty("hh.input.db.name");
				String username = props.getProperty("hh.input.db.user");
				String password = props.getProperty("hh.input.db.pass");
				int port = Integer.parseInt(props.getProperty("hh.input.db.port"));

				conn = DBConnection.getJdbcConnectionPg(hostName, port, dbName, username,
						password);
				hhRouter = RouterImpl.getFromDb(conn);
			} catch (SQLException e) {
				logger.info("Could not load HHRouter from db.");
			} catch (Exception e) {
				logger.info("Invalid properties for HHRouter.");
			}

			// try write to file :

			if (hhRouter != null) {
				try {
					File f = new File(filename);
					File dir = new File(f.getAbsolutePath().substring(0,
							f.getAbsolutePath().lastIndexOf(File.separatorChar))
							+ File.separatorChar);
					dir.mkdirs();
					hhRouter.serialize(new FileOutputStream(f));
					logger.info("Written HHRouter to '" + filename + "'.");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					logger.info("Cannot write HHRouter to '" + filename + "'.");
				} catch (IOException e) {
					e.printStackTrace();
					logger.info("Cannot write HHRouter to '" + filename + "'.");
				}
			}
		}
		return hhRouter;
	}

	private static Properties loadProperties() {
		Properties props = null;
		try {
			props = new Properties();
			props.load(new FileInputStream(PROPERTIES_FILE));
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		return props;
	}

	public static void main(String[] args) throws SQLException, FileNotFoundException,
			IOException {
		RouterImpl router = (RouterImpl) RouterFactory.getRouter();

		Random rnd = new Random(1122);
		HHRenderer renderer = new HHRenderer(1920, 1200, router.routingGraph,
				router.vertexIndex, 26);

		for (int i = 0; i < 100; i++) {

			int s = rnd.nextInt(router.routingGraph.numVertices());
			int t = rnd.nextInt(router.routingGraph.numVertices());
			System.out.println("s = " + s + " t = " + t);
			IEdge[] sp = router.getShortestPath(s, t);
			System.out.println("s = " + s + " t = " + t);

			if (sp != null) {
				LinkedList<GeoCoordinate> coords = new LinkedList<GeoCoordinate>();
				for (IEdge e : sp) {
					for (GeoCoordinate c : e.getWaypoints()) {
						coords.add(c);
					}
					coords.add(e.getTarget().getCoordinate());
				}
				for (int j = 1; j < coords.size(); j++) {
					renderer.drawLine(coords.get(j - 1), coords.get(j), Color.RED, 1);
				}
			}
		}
		IEdge[] es = router.getNearestEdges(new GeoCoordinate(52.508058, 13.462372));
		IEdge[] et = router.getNearestEdges(new GeoCoordinate(52.533388, 13.348131));

		System.out.println((es[0] == null) + " " + (et[0] == null));

		IEdge[] sp = router.getShortestPath(es[0].getSource().getId(), et[0].getTarget()
				.getId());

		if (sp != null) {
			LinkedList<GeoCoordinate> coords = new LinkedList<GeoCoordinate>();
			for (IEdge e : sp) {
				for (GeoCoordinate c : e.getWaypoints()) {
					coords.add(c);
				}
				coords.add(e.getTarget().getCoordinate());
				System.out.println(e.getName());
			}
			for (int j = 1; j < coords.size(); j++) {
				renderer.drawLine(coords.get(j - 1), coords.get(j), Color.RED, 1);
			}
		}

		renderer.update();
		System.out.println("ready");

	}
}