/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.config;

import static org.eclipse.rdf4j.sail.lucene.config.LuceneSailConfigSchema.INDEX_DIR;

import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

public abstract class AbstractLuceneSailConfig extends AbstractDelegatingSailImplConfig {
	/*-----------*
	 * Variables *
	 *-----------*/

	private String indexDir;

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

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		if (indexDir != null) {
			m.add(implNode, INDEX_DIR, SimpleValueFactory.getInstance().createLiteral(indexDir));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode)
		throws SailConfigException
	{
		super.parse(graph, implNode);

		Literal indexDirLit = Models.objectLiteral(graph.filter(implNode, INDEX_DIR, null)).orElseThrow(
				() -> new SailConfigException("no value found for " + INDEX_DIR));

		setIndexDir(indexDirLit.getLabel());
	}
}
