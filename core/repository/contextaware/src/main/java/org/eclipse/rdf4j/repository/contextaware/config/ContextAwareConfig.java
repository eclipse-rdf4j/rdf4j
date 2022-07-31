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
package org.eclipse.rdf4j.repository.contextaware.config;

import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.ADD_CONTEXT;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.ARCHIVE_CONTEXT;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.BASE_URI;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.INCLUDE_INFERRED;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.INSERT_CONTEXT;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.MAX_QUERY_TIME;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.QUERY_LANGUAGE;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.READ_CONTEXT;
import static org.eclipse.rdf4j.repository.contextaware.config.ContextAwareSchema.REMOVE_CONTEXT;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.config.AbstractDelegatingRepositoryImplConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareConnection;

/**
 * @author James Leigh
 */
public class ContextAwareConfig extends AbstractDelegatingRepositoryImplConfig {

	private static final IRI[] ALL_CONTEXTS = new IRI[0];

	private Boolean includeInferred = true;

	private int maxQueryTime = 0;

	private QueryLanguage queryLanguage = QueryLanguage.SPARQL;

	private String baseURI;

	private IRI[] readContexts = ALL_CONTEXTS;

	private IRI[] addContexts = ALL_CONTEXTS;

	private IRI[] removeContexts = ALL_CONTEXTS;

	private IRI[] archiveContexts = ALL_CONTEXTS;

	private IRI insertContext = null;

	public ContextAwareConfig() {
		super(ContextAwareFactory.REPOSITORY_TYPE);
	}

	public int getMaxQueryTime() {
		return maxQueryTime;
	}

	public void setMaxQueryTime(int maxQueryTime) {
		this.maxQueryTime = maxQueryTime;
	}

	/**
	 * @see ContextAwareConnection#getAddContexts()
	 */
	@Deprecated
	public IRI[] getAddContexts() {
		return addContexts;
	}

	/**
	 * @see ContextAwareConnection#getArchiveContexts()
	 */
	@Deprecated
	public IRI[] getArchiveContexts() {
		return archiveContexts;
	}

	/**
	 * @see ContextAwareConnection#getInsertContext()
	 */
	public IRI getInsertContext() {
		return insertContext;
	}

	/**
	 * @see ContextAwareConnection#getQueryLanguage()
	 */
	public QueryLanguage getQueryLanguage() {
		return queryLanguage;
	}

	/**
	 * @return Returns the default baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 * @see ContextAwareConnection#getReadContexts()
	 */
	public IRI[] getReadContexts() {
		return readContexts;
	}

	/**
	 * @see ContextAwareConnection#getRemoveContexts()
	 */
	public IRI[] getRemoveContexts() {
		return removeContexts;
	}

	/**
	 * @see ContextAwareConnection#isIncludeInferred()
	 */
	public boolean isIncludeInferred() {
		return includeInferred == null || includeInferred;
	}

	/**
	 * @see ContextAwareConnection#setAddContexts(IRI[])
	 */
	@Deprecated
	public void setAddContexts(IRI... addContexts) {
		this.addContexts = addContexts;
	}

	/**
	 * @see ContextAwareConnection#setArchiveContexts(IRI[])
	 */
	@Deprecated
	public void setArchiveContexts(IRI... archiveContexts) {
		this.archiveContexts = archiveContexts;
	}

	/**
	 * @see ContextAwareConnection#setInsertContext(IRI)
	 */
	public void setInsertContext(IRI insertContext) {
		this.insertContext = insertContext;
	}

	/**
	 * @see ContextAwareConnection#setIncludeInferred(boolean)
	 */
	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	/**
	 * @see ContextAwareConnection#setQueryLanguage(QueryLanguage)
	 */
	public void setQueryLanguage(QueryLanguage ql) {
		this.queryLanguage = ql;
	}

	/**
	 * @param baseURI The default baseURI to set.
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * @see ContextAwareConnection#setReadContexts(IRI[])
	 */
	public void setReadContexts(IRI... readContexts) {
		this.readContexts = readContexts;
	}

	/**
	 * @see ContextAwareConnection#setRemoveContexts(IRI[])
	 */
	public void setRemoveContexts(IRI... removeContexts) {
		this.removeContexts = removeContexts;
	}

	@Override
	public Resource export(Model model) {
		Resource repImplNode = super.export(model);

		ValueFactory vf = SimpleValueFactory.getInstance();

		if (includeInferred != null) {
			Literal bool = vf.createLiteral(includeInferred);
			model.add(repImplNode, INCLUDE_INFERRED, bool);
		}
		if (maxQueryTime > 0) {
			model.add(repImplNode, MAX_QUERY_TIME, vf.createLiteral(maxQueryTime));
		}
		if (queryLanguage != null) {
			model.add(repImplNode, QUERY_LANGUAGE, vf.createLiteral(queryLanguage.getName()));
		}
		if (baseURI != null) {
			model.add(repImplNode, BASE_URI, vf.createIRI(baseURI));
		}
		for (IRI uri : readContexts) {
			model.add(repImplNode, READ_CONTEXT, uri);
		}
		for (IRI resource : addContexts) {
			model.add(repImplNode, ADD_CONTEXT, resource);
		}
		for (IRI resource : removeContexts) {
			model.add(repImplNode, REMOVE_CONTEXT, resource);
		}
		for (IRI resource : archiveContexts) {
			model.add(repImplNode, ARCHIVE_CONTEXT, resource);
		}
		if (insertContext != null) {
			model.add(repImplNode, INSERT_CONTEXT, insertContext);
		}

		return repImplNode;
	}

	@Override
	public void parse(Model model, Resource resource) throws RepositoryConfigException {
		super.parse(model, resource);

		try {
			Models.objectLiteral(model.getStatements(resource, INCLUDE_INFERRED, null))
					.ifPresent(lit -> setIncludeInferred(lit.booleanValue()));

			Models.objectLiteral(model.getStatements(resource, MAX_QUERY_TIME, null))
					.ifPresent(lit -> setMaxQueryTime(lit.intValue()));

			Models.objectLiteral(model.getStatements(resource, QUERY_LANGUAGE, null))
					.ifPresent(lit -> setQueryLanguage(QueryLanguage.valueOf(lit.getLabel())));

			Models.objectIRI(model.getStatements(resource, QUERY_LANGUAGE, null))
					.ifPresent(iri -> setBaseURI(iri.stringValue()));

			Set<Value> objects = model.filter(resource, READ_CONTEXT, null).objects();
			setReadContexts(objects.toArray(new IRI[objects.size()]));

			objects = model.filter(resource, ADD_CONTEXT, null).objects();
			setAddContexts(objects.toArray(new IRI[objects.size()]));

			objects = model.filter(resource, REMOVE_CONTEXT, null).objects();
			setRemoveContexts(objects.toArray(new IRI[objects.size()]));

			objects = model.filter(resource, ARCHIVE_CONTEXT, null).objects();
			setArchiveContexts(objects.toArray(new IRI[objects.size()]));

			Models.objectIRI(model.getStatements(resource, INSERT_CONTEXT, null))
					.ifPresent(iri -> setInsertContext(iri));
		} catch (ArrayStoreException e) {
			throw new RepositoryConfigException(e);
		}
	}
}
