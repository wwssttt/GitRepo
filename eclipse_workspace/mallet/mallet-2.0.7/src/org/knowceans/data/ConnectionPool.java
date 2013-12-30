/*
 * Created on 27.03.2006
 */
package org.knowceans.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import org.knowceans.util.Conf;

/**
 * ConnectionPool handles a pool of connections in order to reuse them. There
 * are two possiblities to reuse connections: Use anonymous connections without
 * deterministic association between database manupulation tasks and
 * connections, and use named connections that can be reused according to a
 * string identity. Transactions that are forced to one connection can be
 * implemented by the second method.
 * <p>
 * TODO: use J2EE naming-based connection pooling.
 * 
 * @author gregor with some snippets from
 *         http://www.javareference.com/jrexamples/viewexample.jsp?id=41
 */
public class ConnectionPool implements Runnable {

	private static ConnectionPool instance = null;

	public static ConnectionPool getInstance() throws SQLException {
		if (instance == null) {
			instance = new ConnectionPool();
		}
		return instance;
	}

	/**
	 * initial connections to make TODO: from Conf
	 */
	private int nInitialCons = 5;

	/**
	 * interval for cleanup thread in seconds TODO: from Conf
	 */
	private double cleanupInterval = 10;

	/**
	 * thread to cleanup finally
	 */
	private Thread cleanupThread = null;

	/**
	 * available connections
	 */
	private Vector<Connection> availCons;

	/**
	 * connections currently used
	 */
	private Vector<Connection> usedCons;

	/**
	 * a hashtable for named connections that can be reused and are not closed
	 */
	private Hashtable<String, Connection> namedCons;

	private String jdbcDriver = null;
	private String jdbcUrl = null;
	private String jdbcUser = null;
	private String jdbcPassword = null;

	/**
	 * Whether the connection pool is about to be closed.
	 */
	private boolean finishing;

	/**
	 * Constructor for an instantiated connection pool. This is the alternative
	 * to using the singleton access (i.e. to close session connections).
	 */
	public ConnectionPool() throws SQLException {

		// initialise database connection.
		jdbcDriver = Conf.get("database.driver");
		jdbcUrl = Conf.get("database.url");
		jdbcUser = Conf.get("database.user");
		jdbcPassword = Conf.get("database.pass");
		init();
	}

	public ConnectionPool(String url, String user, String pass)
			throws SQLException {
		// initialise database connection.
		jdbcDriver = Conf.get("database.driver");
		jdbcUrl = url;
		jdbcUser = user;
		jdbcPassword = pass;
		init();
	}

	private void init() throws SQLException {
		System.out.println("database: " + jdbcUrl);

		availCons = new Vector<Connection>();
		usedCons = new Vector<Connection>();
		namedCons = new Hashtable<String, Connection>();

		initDriver();

		for (int i = 0; i < nInitialCons; i++) {
			availCons.add(getConnection());
		}

		// Create the cleanup thread
		cleanupThread = new Thread(this, "connection pool cleanup");
		cleanupThread.start();
	}

	/**
	 * Loads the database driver
	 */
	private void initDriver() {
		try {
			Class.forName(jdbcDriver).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a new connection to the database
	 * 
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
	}

	/**
	 * Use an existing anonymous connection, which marks it as used until it is
	 * freeCon()'ed. Synchronized for multi-threads
	 * 
	 * @return
	 * @throws SQLException
	 */
	public synchronized Connection useCon() throws SQLException {
		Connection con = null;

		if (availCons.size() == 0) {
			// Create one more connections.
			con = getConnection();
			con.setAutoCommit(true);
			usedCons.add(con);
		} else {
			con = availCons.lastElement();
			availCons.removeElement(con);
			usedCons.add(con);
		}
		return con;
	}

	/**
	 * Use a named connection. If the name does not exist, the connection is
	 * created and can be reused any later call to this method with the same
	 * name.
	 * 
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	public synchronized Connection useCon(String name) throws SQLException {
		Connection con = namedCons.get(name);
		if (con == null) {
			// TODO: should named connections come from the pool of unnamed
			// ones?
			// now we choose not to but this is not a final decision.
			// con = (Connection) availCons.lastElement();
			// availCons.removeElement(con);
			// usedCons.add(con);
			con = getConnection();
			namedCons.put(name, con);
		}
		con.setAutoCommit(false);
		return con;
	}

	/**
	 * Make connection available for reuse. Synchronized for multi-threads.
	 * 
	 * @param c
	 */
	public synchronized void freeCon(Connection c) {
		if (c != null) {
			boolean wasUsed = usedCons.removeElement(c);
			if (wasUsed)
				availCons.addElement(c);
		}
	}

	/**
	 * Close the named connection and remove it from the table of named
	 * connections.
	 * 
	 * @param name
	 * @throws SQLException
	 */
	public synchronized void removeCon(String name) throws SQLException {
		Connection con = namedCons.get(name);
		if (con != null) {
			con.close();
			namedCons.remove(name);
		}
	}

	/**
	 * Get number of available anonymous connections
	 * 
	 * @return
	 */
	public int availableCons() {
		return availCons.size();
	}

	/**
	 * Get the name of available named connections
	 * 
	 * @return
	 */
	public Set<String> availableNamedCons() {
		return namedCons.keySet();
	}

	/**
	 * Close all connections handled by this manager and remove them from the
	 * respective lists.
	 * 
	 * @throws SQLException
	 */
	public void closeAll() throws SQLException {
		finishing = true;
		HashSet<Connection> allCons = new HashSet<Connection>();
		allCons.addAll(availCons);
		allCons.addAll(usedCons);
		allCons.addAll(namedCons.values());
		for (Connection con : allCons) {
			if (con != null) {
				con.close();
			}
		}
		availCons.clear();
		usedCons.clear();
		namedCons.clear();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeAll();
	}

	/**
	 * Handle cleanup of connections in certain intervals.
	 */
	public void run() {
		try {
			while (!finishing) {
				synchronized (this) {
					while (availCons.size() > nInitialCons) {
						Connection c = availCons.lastElement();
						availCons.removeElement(c);
						c.close();
					}
				}
				// TODO: anything for named connections?
				// wait until next cleanup
				Thread.sleep((long) (cleanupInterval * 1000.));
			}
			closeAll();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
