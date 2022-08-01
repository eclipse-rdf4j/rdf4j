/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.config;

import static org.eclipse.rdf4j.sail.lucene.config.LuceneSailConfigSchema.INDEX_DIR;

import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public abstract class AbstractLuceneSailConfig extends AbstractDelegatingSailImplConfig {
	/*-----------*
	 * Variables *
	 *-----------*/

	private String indexDir;

	private final Properties parameters = new Properties();

	/*--------------*
	 * Constructors *
	 *--------------*/

	protected AbstractLuceneSailConfig(String type) {
		super(type);
	}

	protected AbstractLuceneSailConfig(String type, SailImplConfig delegate) {
		super(type, delegate);
	}

	protected AbstractLuceneSailConfig(String type, String luceneDir) {
		super(type);
		setIndexDir(luceneDir);
	}

	protected AbstractLuceneSailConfig(String type, String luceneDir, SailImplConfig delegate) {
		super(type, delegate);
		setIndexDir(luceneDir);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getIndexDir() {
		return indexDir;
	}

	public void setIndexDir(String luceneDir) {
		this.indexDir = luceneDir;
	}

	public String getParameter(String key) {
		return parameters.getProperty(key);
	}

	public void setParameter(String key, String value) {
		parameters.setProperty(key, value);
	}

	public Set<String> getParameterNames() {
		return parameters.stringPropertyNames();
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		ValueFactory vf = SimpleValueFactory.getInstance();
		m.setNamespace("sl", LuceneSailConfigSchema.NAMESPACE);
		if (indexDir != null) {
			m.add(implNode, INDEX_DIR, SimpleValueFactory.getInstance().createLiteral(indexDir));
		}

		for (String key : getParameterNames()) {
			m.add(implNode, vf.createIRI(LuceneSailConfigSchema.NAMESPACE, key), vf.createLiteral(getParameter(key)));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		Literal indexDirLit = Models.objectLiteral(graph.getStatements(implNode, INDEX_DIR, null))
				.orElseThrow(() -> new SailConfigException("no value found for " + INDEX_DIR));

		setIndexDir(indexDirLit.getLabel());
		for (Statement stmt : graph.getStatements(implNode, null, null)) {
			if (stmt.getPredicate().getNamespace().equals(LuceneSailConfigSchema.NAMESPACE)) {
				if (stmt.getObject() instanceof Literal) {
					String key = stmt.getPredicate().getLocalName();
					setParameter(key, stmt.getObject().stringValue());
				}
			}
		}
	}
}
