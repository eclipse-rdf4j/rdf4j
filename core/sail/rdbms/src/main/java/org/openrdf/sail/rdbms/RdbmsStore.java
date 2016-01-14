/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Iterator;

import javax.imageio.spi.ServiceRegistry;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.AbstractSail;
import org.openrdf.sail.rdbms.exceptions.RdbmsException;

/**
 * The RDBMS SAIL for relational database storage in Sesame. This class acts
 * both as a base class for database specific stores as well as a generic store
 * that can infer the type of database through the JDBC connection.
 * 
 * @author James Leigh
 * @deprecated the RDBMS SAIL is no longer actively supported and will be
 *             removed in a future Sesame release. Consider switching to a
 *             different SAIL backend.
 */
@Deprecated
public class RdbmsStore extends AbstractSail implements FederatedServiceResolverClient {

	private RdbmsConnectionFactory factory;

	private String jdbcDriver;

	private String url;

	private String user;

	private String password;

	private int maxTripleTables;

	private boolean triplesIndexed = true;

	private boolean sequenced = true;

	private BasicDataSource ds;

	/** independent life cycle */
	private FederatedServiceResolver serviceResolver;

	/** dependent life cycle */
	private FederatedServiceResolverImpl dependentServiceResolver;

	public RdbmsStore() {
		super();
	}

	/**
	 * Creates a new RDBMS RDF Store using the provided database connection.
	 * 
	 * @param url
	 *        JDNI url of a DataSource
	 */
	public RdbmsStore(String url) {
		this.url = url;
	}

	/**
	 * Creates a new RDBMS RDF Store using the provided database connection.
	 * 
	 * @param url
	 *        JDNI url of a DataSource
	 * @param user
	 * @param password
	 */
	public RdbmsStore(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}

	/**
	 * Creates a new RDBMS RDF Store using the provided database connection.
	 * 
	 * @param jdbcDriver
	 * @param jdbcUrl
	 */
	public RdbmsStore(String jdbcDriver, String jdbcUrl) {
		this.jdbcDriver = jdbcDriver;
		this.url = jdbcUrl;
	}

	/**
	 * Creates a new RDBMS RDF Store using the provided database connection.
	 * 
	 * @param jdbcDriver
	 * @param jdbcUrl
	 * @param user
	 * @param password
	 */
	public RdbmsStore(String jdbcDriver, String jdbcUrl, String user, String password) {
		this.jdbcDriver = jdbcDriver;
		this.url = jdbcUrl;
		this.user = user;
		this.password = password;
	}

	public int getMaxNumberOfTripleTables() {
		return maxTripleTables;
	}

	public void setMaxNumberOfTripleTables(int max) {
		maxTripleTables = max;
	}

	public boolean isIndexed() {
		return triplesIndexed;
	}

	public void setIndexed(boolean indexed)
		throws SailException
	{
		triplesIndexed = indexed;
		if (factory != null) {
			factory.setTriplesIndexed(triplesIndexed);
		}
	}

	public boolean isSequenced() {
		return sequenced;
	}

	public void setSequenced(boolean useSequence) {
		this.sequenced = useSequence;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new FederatedServiceResolverImpl();
			}
			return serviceResolver = dependentServiceResolver;
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but
	 * the given resolver is not shutDown when this instance is.
	 * 
	 * @param reslover The SERVICE resolver to set.
	 */
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver reslover) {
		this.serviceResolver = reslover;
	}

	@Override
	protected void initializeInternal()
		throws SailException
	{
		if (factory == null) {
			try {
				factory = createFactory(jdbcDriver, url, user, password);
			}
			catch (SailException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RdbmsException(e);
			}
		}
		factory.setMaxNumberOfTripleTables(maxTripleTables);
		factory.setTriplesIndexed(triplesIndexed);
		factory.setSequenced(sequenced);
		factory.init();
	}

	public boolean isWritable()
		throws SailException
	{
		return factory.isWritable();
	}

	public RdbmsValueFactory getValueFactory() {
		return factory.getValueFactory();
	}

	@Override
	protected SailConnection getConnectionInternal()
		throws SailException
	{
		return factory.createConnection();
	}

	@Override
	protected void shutDownInternal()
		throws SailException
	{
		factory.shutDown();
		try {
			if (ds != null) {
				ds.close();
			}
		}
		catch (SQLException e) {
			throw new RdbmsException(e);
		} finally {
			if (dependentServiceResolver != null) {
				dependentServiceResolver.shutDown();
			}
		}
	}

	protected void setConnectionFactory(RdbmsConnectionFactory factory) {
		this.factory = factory;
	}

	protected void setBasicDataSource(BasicDataSource ds) {
		this.ds = ds;
	}

	private RdbmsConnectionFactory createFactory(String jdbcDriver, String url, String user, String password)
		throws Exception
	{
		if (jdbcDriver != null) {
			Class.forName(jdbcDriver);
		}
		DataSource ds = lookupDataSource(url, user, password);
		Connection con;
		if (user == null || url.startsWith("jdbc:")) {
			con = ds.getConnection();
		}
		else {
			con = ds.getConnection(user, password);
		}
		try {
			DatabaseMetaData metaData = con.getMetaData();
			RdbmsConnectionFactory factory = newFactory(metaData);
			factory.setSail(this);
			if (user == null || url.startsWith("jdbc:")) {
				factory.setDataSource(ds);
			}
			else {
				factory.setDataSource(ds, user, password);
			}
			return factory;
		}
		finally {
			con.close();
		}
	}

	private DataSource lookupDataSource(String url, String user, String password)
		throws NamingException
	{
		if (url.startsWith("jdbc:")) {
			BasicDataSource ds = new BasicDataSource();
			ds.setUrl(url);
			ds.setUsername(user);
			ds.setPassword(password);
			setBasicDataSource(ds);
			return ds;
		}
		return (DataSource)new InitialContext().lookup(url);
	}

	private RdbmsConnectionFactory newFactory(DatabaseMetaData metaData)
		throws SQLException
	{
		String dbn = metaData.getDatabaseProductName();
		String dbv = metaData.getDatabaseProductVersion();
		RdbmsConnectionFactory factory;
		Iterator<RdbmsProvider> providers;
		providers = ServiceRegistry.lookupProviders(RdbmsProvider.class);
		while (providers.hasNext()) {
			RdbmsProvider provider = providers.next();
			factory = provider.createRdbmsConnectionFactory(dbn, dbv);
			if (factory != null)
				return factory;
		}
		return new RdbmsConnectionFactory();
	}

}
