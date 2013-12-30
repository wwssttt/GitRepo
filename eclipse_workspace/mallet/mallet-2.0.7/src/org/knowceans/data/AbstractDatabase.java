/*
 * Created on 24.08.2006
 */
package org.knowceans.data;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/**
 * AbstractDatabase provides basis functionality to database classes.
 * 
 * @author gregor
 */
public abstract class AbstractDatabase {

	protected ConnectionPool cons;
	protected String defaultcon = "default transactional connection";

	/**
	 * Initialise the database, with NO database connection, which must be
	 * initialised by the subclass.
	 * 
	 * @throws SQLException
	 */
	protected AbstractDatabase() throws SQLException {
		cons = ConnectionPool.getInstance();
	}

	/**
	 * Initialise the database connection with expected connection name (for
	 * transactions).
	 * 
	 * @throws SQLException
	 */
	protected AbstractDatabase(String connectionName) throws SQLException {
		cons = ConnectionPool.getInstance();
		defaultcon = connectionName;
	}

	/**
	 * Initialise the database connection, using the singleton connection pool
	 * 
	 * @throws SQLException
	 */
	public AbstractDatabase(ConnectionPool cons) throws SQLException {
		this.cons = cons;
	}

	/**
	 * Initialise the database connection with a separate instance of a
	 * connection pool and the connection parameters given.
	 * 
	 * @param url
	 * @param user
	 * @param pass
	 * @throws SQLException
	 */
	public AbstractDatabase(String url, String user, String pass)
			throws SQLException {
		cons = new ConnectionPool(url, user, pass);
	}

	/**
	 * Set autocommit status of this database connection.
	 * 
	 * @param autoCommit
	 * @throws SQLException
	 */
	public void setAutocommit(boolean autoCommit) throws SQLException {
		cons.useCon(defaultcon).setAutoCommit(autoCommit);
	}

	public void startTransaction() throws SQLException {
		setAutocommit(false);
		updateQuery("START TRANSACTION");
	}

	public void commit() throws SQLException {
		cons.useCon(defaultcon).commit();
	}

	public void rollback() throws SQLException {
		cons.useCon(defaultcon).rollback();
	}

	/**
	 * lock tables for faster inserts (non-transactional): no indexes are
	 * flushed
	 * 
	 * @param tablist
	 *            comma-separated table names
	 * @throws SQLException
	 */
	public void lockTables(String tablist) throws SQLException {
		String[] tt = tablist.split(",");
		String q = String.format("LOCK TABLES %s WRITE", tt[0]);
		for (int i = 1; i < tt.length; i++) {
			q += ", " + tt[i] + " WRITE";
		}
	}

	/**
	 * unlock tables
	 * 
	 * @throws SQLException
	 */
	public void unlockTables() throws SQLException {
		updateQuery("UNLOCK TABLES");
	}

	/**
	 * @throws SQLException
	 */
	public boolean getAutocommit() throws SQLException {
		return cons.useCon(defaultcon).getAutoCommit();
	}

	/**
	 * Executes an update query.
	 * 
	 * @param query
	 * @return number of affected rows.
	 * @throws SQLException
	 */
	// protected
	public int updateQuery(String query) throws SQLException {
		int a = 0;
		Connection con = cons.useCon(defaultcon);
		Statement stmt = con.createStatement();
		a = stmt.executeUpdate(query);
		stmt.close();
		cons.freeCon(con);

		// Log.getLogger().info(e.getMessage());
		return a;
	}

	/**
	 * Executes the query and returns the result set.
	 * 
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	// protected
	public ResultSet selectQuery(String query) throws SQLException {

		ResultSet rs = null;
		try {
			Connection con = cons.useCon(defaultcon);
			Statement stmt = con.createStatement();
			rs = stmt.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// cons.freeCon(con);
		return rs;
	}

	/**
	 * Finish all database connections by closing the connection pool.
	 * 
	 * @throws SQLException
	 */
	public void finish() throws SQLException {
		cons.closeAll();
	}

	/**
	 * Re-initialises the database connections by closing and reopening them.
	 * 
	 * @throws SQLException
	 */
	public void reinit() throws SQLException {
		finish();
		cons = ConnectionPool.getInstance();
	}

	/**
	 * Check whether the table exists in the schema
	 * 
	 * @param table
	 * @return
	 */
	public boolean exists(String table) {
		boolean exists = false;

		try {
			Connection con = cons.useCon(defaultcon);
			DatabaseMetaData dm = con.getMetaData();
			ResultSet rs = dm.getTables(null, null, table, null);
			if (rs.next()) {
				exists = true;
			}
		} catch (SQLException e) {
		}
		return exists;
	}

	protected long now() {
		long now = new Date().getTime();
		return now;
	}

}
