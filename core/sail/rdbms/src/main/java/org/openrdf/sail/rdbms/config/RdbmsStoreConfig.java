/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.config;

import static org.openrdf.model.util.GraphUtil.getOptionalObjectLiteral;
import static org.openrdf.sail.rdbms.config.RdbmsStoreSchema.JDBC_DRIVER;
import static org.openrdf.sail.rdbms.config.RdbmsStoreSchema.MAX_TRIPLE_TABLES;
import static org.openrdf.sail.rdbms.config.RdbmsStoreSchema.PASSWORD;
import static org.openrdf.sail.rdbms.config.RdbmsStoreSchema.URL;
import static org.openrdf.sail.rdbms.config.RdbmsStoreSchema.USER;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.sail.config.SailConfigException;
import org.openrdf.sail.config.AbstractSailImplConfig;

/**
 * Holds the JDBC Driver, URL, user and password, as well as the database
 * layout.
 * 
 * @author James Leigh
 */
public class RdbmsStoreConfig extends AbstractSailImplConfig {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String jdbcDriver;

	private String url;

	private String user;

	private String password;

	private int maxTripleTables = 256;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public RdbmsStoreConfig() {
		super(RdbmsStoreFactory.SAIL_TYPE);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getMaxTripleTables() {
		return maxTripleTables;
	}

	public void setMaxTripleTables(int maxTripleTables) {
		this.maxTripleTables = maxTripleTables;
	}

	@Override
	public void validate()
		throws SailConfigException
	{
		super.validate();

		if (url == null) {
			throw new SailConfigException("No URL specified for RdbmsStore");
		}
	}

	@Override
	public Resource export(Graph graph) {
		Resource implNode = super.export(graph);

		ValueFactory vf = graph.getValueFactory();

		if (jdbcDriver != null) {
			graph.add(implNode, JDBC_DRIVER, vf.createLiteral(jdbcDriver));
		}
		if (url != null) {
			graph.add(implNode, URL, vf.createLiteral(url));
		}
		if (user != null) {
			graph.add(implNode, USER, vf.createLiteral(user));
		}
		if (password != null) {
			graph.add(implNode, PASSWORD, vf.createLiteral(password));
		}
		graph.add(implNode, MAX_TRIPLE_TABLES, vf.createLiteral(maxTripleTables));

		return implNode;
	}

	@Override
	public void parse(Graph graph, Resource implNode)
		throws SailConfigException
	{
		super.parse(graph, implNode);

		try {
			Literal jdbcDriverLit = getOptionalObjectLiteral(graph, implNode, JDBC_DRIVER);
			if (jdbcDriverLit != null) {
				setJdbcDriver(jdbcDriverLit.getLabel());
			}

			Literal urlLit = getOptionalObjectLiteral(graph, implNode, URL);
			if (urlLit != null) {
				setUrl(urlLit.getLabel());
			}

			Literal userLit = getOptionalObjectLiteral(graph, implNode, USER);
			if (userLit != null) {
				setUser(userLit.getLabel());
			}

			Literal passwordLit = getOptionalObjectLiteral(graph, implNode, PASSWORD);
			if (passwordLit != null) {
				setPassword(passwordLit.getLabel());
			}

			Literal maxTripleTablesLit = getOptionalObjectLiteral(graph, implNode, MAX_TRIPLE_TABLES);
			if (maxTripleTablesLit != null) {
				try {
					setMaxTripleTables(maxTripleTablesLit.intValue());
				}
				catch (NumberFormatException e) {
					throw new SailConfigException("Invalid value for maxTripleTables: " + maxTripleTablesLit);
				}
			}
		}
		catch (GraphUtilException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
