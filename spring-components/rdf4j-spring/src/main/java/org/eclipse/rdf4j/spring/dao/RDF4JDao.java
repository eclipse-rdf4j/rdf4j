/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.spring.dao.exception.RDF4JDaoException;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.GraphQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.UpdateExecutionBuilder;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Experimental
public abstract class RDF4JDao {
	private final RDF4JTemplate rdf4JTemplate;

	private final Map<String, NamedSparqlSupplier> namedSparqlSuppliers = new ConcurrentHashMap<>();

	public RDF4JDao(RDF4JTemplate rdf4JTemplate) {
		this.rdf4JTemplate = rdf4JTemplate;
		prepareNamedSparqlSuppliers(new NamedSparqlSupplierPreparer());
	}

	protected RDF4JTemplate getRdf4JTemplate() {
		return rdf4JTemplate;
	}

	protected abstract NamedSparqlSupplierPreparer prepareNamedSparqlSuppliers(
			NamedSparqlSupplierPreparer preparer);

	/**
	 * Prepares the specified SPARQL string for later use, e.g. in
	 * {@link RDF4JTemplate#tupleQuery(Class, NamedSparqlSupplier)}.
	 */
	private void prepareNamedSparqlSupplier(String key, String sparql) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(sparql);
		namedSparqlSuppliers.put(key, new NamedSparqlSupplier(key, () -> sparql));
	}

	/**
	 * Reads the SPARQL string from the specified resource using a {@link org.springframework.core.io.ResourceLoader}
	 * and prepares it for later use, e.g. in {@link RDF4JTemplate#tupleQuery(Class, NamedSparqlSupplier)}.
	 */
	private void prepareNamedSparqlSupplierFromResource(String key, String resourceName) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(resourceName);
		String sparqlString = getRdf4JTemplate().getStringSupplierFromResourceContent(resourceName).get();
		namedSparqlSuppliers.put(key, new NamedSparqlSupplier(key, () -> sparqlString));
	}

	/**
	 * Obtains the {@link NamedSparqlSupplier} with the specified key for use in, e.g.,
	 * {@link RDF4JTemplate#tupleQuery(Class, NamedSparqlSupplier)}.
	 */
	protected NamedSparqlSupplier getNamedSparqlSupplier(String key) {
		Objects.requireNonNull(key);
		NamedSparqlSupplier supplier = namedSparqlSuppliers.get(key);
		if (supplier == null) {
			throw new RDF4JDaoException(
					String.format(
							"No NamedSparqlOperation found for key %s. Prepare it using Rdf4JDao.prepareNamedSparqlSuppliers() before calling this method!",
							key));
		}
		return supplier;
	}

	protected String getNamedSparqlString(String key) {
		return getNamedSparqlSupplier(key).getSparqlSupplier().get();
	}

	protected TupleQueryEvaluationBuilder getNamedTupleQuery(String key) {
		return getRdf4JTemplate().tupleQuery(getClass(), getNamedSparqlSupplier(key));
	}

	protected GraphQueryEvaluationBuilder getNamedGraphQuery(String key) {
		return getRdf4JTemplate().graphQuery(getClass(), getNamedSparqlSupplier(key));
	}

	protected UpdateExecutionBuilder getNamedUpdate(String key) {
		return getRdf4JTemplate().update(getClass(), getNamedSparqlSupplier(key));
	}

	public class NamedSparqlSupplierPreparer {

		private NamedSparqlSupplierPreparer() {
		}

		/**
		 * For the specified <code>key</code>, {@link java.util.function.Supplier<String>} is registered with the
		 * subsequent <code>supplySparql*</code> method.
		 */
		public NamedSparqlSupplierFinishBuilder forKey(String key) {
			return new NamedSparqlSupplierFinishBuilder(key);
		}
	}

	public class NamedSparqlSupplierFinishBuilder {
		private final String key;

		public NamedSparqlSupplierFinishBuilder(String key) {
			this.key = key;
		}

		/** Supplies the specified SPARQL String. */
		public NamedSparqlSupplierPreparer supplySparql(String sparql) {
			prepareNamedSparqlSupplier(key, sparql);
			return new NamedSparqlSupplierPreparer();
		}

		/**
		 * Loads the specified <code>resource</code> using a {@link org.springframework.core.io.ResourceLoader} and
		 * supplies its content as String, the assumption is that it contains a SPARQL operation.
		 */
		public NamedSparqlSupplierPreparer supplySparqlFromResource(String resource) {
			prepareNamedSparqlSupplierFromResource(key, resource);
			return new NamedSparqlSupplierPreparer();
		}
	}
}
