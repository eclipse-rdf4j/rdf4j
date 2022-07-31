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

package org.eclipse.rdf4j.spring.support;

import static org.eclipse.rdf4j.spring.util.TypeMappingUtils.toIri;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationException;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ModifyQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;
import org.eclipse.rdf4j.spring.dao.exception.RDF4JDaoException;
import org.eclipse.rdf4j.spring.dao.support.UpdateWithModelBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.GraphQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.UpdateExecutionBuilder;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.support.connectionfactory.RepositoryConnectionFactory;
import org.eclipse.rdf4j.spring.util.TypeMappingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 * @author Gabriel Pickl
 */
@Experimental
public class RDF4JTemplate {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final RepositoryConnectionFactory repositoryConnectionFactory;
	private final OperationInstantiator operationInstantiator;
	private final ResourceLoader resourceLoader;
	private final UUIDSource uuidSource;

	public RDF4JTemplate(
			@Autowired RepositoryConnectionFactory repositoryConnectionFactory,
			@Autowired OperationInstantiator operationInstantiator,
			@Autowired ResourceLoader resourceLoader,
			@Autowired(required = false) UUIDSource uuidSource) {
		this.repositoryConnectionFactory = repositoryConnectionFactory;
		this.operationInstantiator = operationInstantiator;
		this.resourceLoader = resourceLoader;
		if (uuidSource == null) {
			this.uuidSource = new DefaultUUIDSource();
		} else {
			this.uuidSource = uuidSource;
		}
	}

	public void consumeConnection(final Consumer<RepositoryConnection> fun) {
		RepositoryConnection con = getRepositoryConnection();
		if (logger.isDebugEnabled()) {
			logger.debug(
					"using connection {} of type {}",
					con.hashCode(),
					con.getClass().getSimpleName());
		}
		try {
			fun.accept(con);
		} catch (Exception e) {
			logIfShaclValidationFailure(e);
			throw e;
		}
	}

	public <T> T applyToConnection(final Function<RepositoryConnection, T> fun) {
		RepositoryConnection con = getRepositoryConnection();
		if (logger.isDebugEnabled()) {
			logger.debug(
					"using connection {} of type {}",
					con.hashCode(),
					con.getClass().getSimpleName());
		}
		try {
			return fun.apply(con);
		} catch (Exception e) {
			logIfShaclValidationFailure(e);
			throw e;
		}
	}

	/**
	 * Bypassing any caches, generates a new Update from the specified SPARQL string and returns a Builder for its
	 * execution. Should be avoided in favor of one of the methods that apply caching unless the update is not reusable.
	 */
	public UpdateExecutionBuilder update(String updateString) {
		return applyToConnection(
				con -> new UpdateExecutionBuilder(
						operationInstantiator.getUpdate(con, updateString), this));
	}

	/**
	 * Uses a cached {@link Update} if one is available under the specified <code>operationName
	 * </code> for the {@link RepositoryConnection} that is used, otherwise the query string is obtained from the
	 * specified supplier, a new Update is instantiated and cached for future calls to this method.
	 *
	 * Note: this call is equivalent to {@link #update(String)} if operation caching is disabled.
	 *
	 * @param owner                the class of the client requesting the update, used to generate a cache key in
	 *                             combination with the operation name
	 * @param operationName        name of the operation that, within the scope of the client, identifies the update
	 * @param updateStringSupplier supplies the sparql of the update if needed
	 */
	public UpdateExecutionBuilder update(
			Class<?> owner, String operationName, Supplier<String> updateStringSupplier) {
		return applyToConnection(
				con -> new UpdateExecutionBuilder(
						operationInstantiator.getUpdate(
								con, owner, operationName, updateStringSupplier),
						this));
	}

	/**
	 * Reads the update from the specified resource and provides it through a {@link Supplier <String>} in
	 * {@link #update(Class, String, Supplier)}, using the <code>resourceName
	 * </code> as the <code>operationName</code>.
	 *
	 */
	public UpdateExecutionBuilder updateFromResource(Class<?> owner, String resourceName) {
		return update(
				owner,
				resourceName,
				() -> getStringSupplierFromResourceContent(resourceName).get());
	}

	/**
	 * Uses the provided {@link NamedSparqlSupplier} for calling {@link #update(Class, String, Supplier)}.
	 */
	public UpdateExecutionBuilder update(Class<?> owner, NamedSparqlSupplier namedSparqlSupplier) {
		return update(
				owner, namedSparqlSupplier.getName(), namedSparqlSupplier.getSparqlSupplier());
	}

	public UpdateExecutionBuilder updateWithoutCachingStatement(String updateString) {
		return applyToConnection(
				con -> new UpdateExecutionBuilder(con.prepareUpdate(updateString), this));
	}

	public UpdateWithModelBuilder updateWithBuilder() {
		return new UpdateWithModelBuilder(getRepositoryConnection());
	}

	/**
	 * Bypassing any caches, generates a new TupleQuery from the specified SPARQL string and returns a Builder for its
	 * evaluation. Should be avoided in favor of one of the methods that apply caching unless the query is not reusable.
	 */
	public TupleQueryEvaluationBuilder tupleQuery(String queryString) {
		return new TupleQueryEvaluationBuilder(
				applyToConnection(con -> operationInstantiator.getTupleQuery(con, queryString)),
				this);
	}

	/**
	 * Uses a cached {@link TupleQuery} if one is available under the specified <code>operationName
	 * </code> for the {@link RepositoryConnection} that is used, otherwise the query string is obtained from the
	 * specified supplier, a new TupleQuery is instantiated and cached for future calls to this method.
	 */
	public TupleQueryEvaluationBuilder tupleQuery(
			Class<?> owner, String operationName, Supplier<String> queryStringSupplier) {
		return new TupleQueryEvaluationBuilder(
				applyToConnection(
						con -> operationInstantiator.getTupleQuery(
								con, owner, operationName, queryStringSupplier)),
				this);
	}

	/**
	 * Reads the query from the specified resource and provides it through a {@link Supplier <String>} in
	 * {@link #tupleQuery(Class, String, Supplier)}, using the <code>
	 * resourceName</code> as the <code>operationName</code>.
	 *
	 */
	public TupleQueryEvaluationBuilder tupleQueryFromResource(Class<?> owner, String resourceName) {
		return tupleQuery(
				owner,
				resourceName,
				() -> getStringSupplierFromResourceContent(resourceName).get());
	}

	/**
	 * Uses the provided {@link NamedSparqlSupplier} for calling {@link #tupleQuery(Class, String, Supplier)}.
	 */
	public TupleQueryEvaluationBuilder tupleQuery(
			Class<?> owner, NamedSparqlSupplier namedSparqlSupplier) {
		return tupleQuery(
				owner, namedSparqlSupplier.getName(), namedSparqlSupplier.getSparqlSupplier());
	}

	/**
	 * Bypassing any caches, generates a new GraphQuery from the specified SPARQL string and returns a Builder for its
	 * evaluation. Should be avoided in favor of one of the methods that apply caching unless the query is not reusable.
	 */
	public GraphQueryEvaluationBuilder graphQuery(String graphQueryString) {
		return new GraphQueryEvaluationBuilder(
				applyToConnection(
						con -> operationInstantiator.getGraphQuery(con, graphQueryString)),
				this);
	}

	/**
	 * Uses a cached {@link GraphQuery} if one is available under the specified <code>operationName
	 * </code> for the {@link RepositoryConnection} that is used, otherwise the query string is obtained from the
	 * specified supplier, a new GraphQuery is instantiated and cached for future calls to this method.
	 */
	public GraphQueryEvaluationBuilder graphQuery(
			Class<?> owner, String operationName, Supplier<String> queryStringSupplier) {
		return new GraphQueryEvaluationBuilder(
				applyToConnection(
						con -> operationInstantiator.getGraphQuery(
								con, owner, operationName, queryStringSupplier)),
				this);
	}

	/**
	 * Reads the query from the specified resource and provides it through a {@link Supplier <String>} in
	 * {@link #graphQuery(Class, String, Supplier)}, using the <code>
	 * resourceName</code> as the <code>operationName</code>.
	 */
	public GraphQueryEvaluationBuilder graphQueryFromResource(Class<?> owner, String resourceName) {
		return graphQuery(
				owner,
				resourceName,
				() -> getStringSupplierFromResourceContent(resourceName).get());
	}

	/**
	 * Uses the provided {@link NamedSparqlSupplier} for calling {@link #graphQuery(Class, String, Supplier)}.
	 */
	public GraphQueryEvaluationBuilder graphQuery(
			Class<?> owner, NamedSparqlSupplier namedSparqlSupplier) {
		return graphQuery(
				owner, namedSparqlSupplier.getName(), namedSparqlSupplier.getSparqlSupplier());
	}

	public void deleteTriplesWithSubject(IRI id) {
		consumeConnection(
				con -> {
					con.remove(id, null, null);
				});
	}

	/**
	 * Deletes the specified resource: all triples are deleted in which <code>id</code> is the subject or the object.
	 *
	 * @param id
	 */
	public void delete(IRI id) {
		consumeConnection(
				con -> {
					con.remove(id, null, null);
					con.remove((Resource) null, null, id);
				});
	}

	/**
	 * Deletes the specified resource and all resources <code>R</code> reached via any of the specified property paths.
	 *
	 * Deletion means that all triples are removed in which <code>start</code> or any resource in <code>R</code> are the
	 * subject or the object.
	 *
	 * @param start         the initial resource to be deleted
	 * @param propertyPaths paths by which to reach more resources to be deleted.
	 */
	public void delete(IRI start, List<PropertyPath> propertyPaths) {
		List<Variable> targets = new ArrayList<>();
		int i = 0;
		Variable sp = SparqlBuilder.var("sp");
		Variable so = SparqlBuilder.var("so");
		Variable is = SparqlBuilder.var("is");
		Variable ip = SparqlBuilder.var("ip");
		ModifyQuery q = Queries.MODIFY()
				.delete(toIri(start).has(sp, so), is.has(ip, start))
				.where(toIri(start).has(sp, so).optional(), is.has(ip, start).optional());
		for (PropertyPath p : propertyPaths) {
			i++;
			Variable target = SparqlBuilder.var("target_" + i);
			Variable p1 = SparqlBuilder.var("p1_" + i);
			Variable o1 = SparqlBuilder.var("o2_" + i);
			Variable p2 = SparqlBuilder.var("p2_" + i);
			Variable s2 = SparqlBuilder.var("s2_" + i);
			q.delete(target.has(p1, o1), s2.has(p2, target))
					.where(toIri(start).has(p, target).optional(), target.has(p1, o1).optional(),
							s2.has(p2, target).optional());
		}
		update(q.getQueryString()).execute();
	}

	public void associate(
			IRI fromResource,
			IRI property,
			Collection<IRI> toResources,
			boolean deleteOtherOutgoing,
			boolean deleteOtherIcoming) {
		Variable from = SparqlBuilder.var("fromResource");
		Variable to = SparqlBuilder.var("toResource");
		if (deleteOtherOutgoing) {
			String query = Queries.MODIFY()
					.delete(from.has(property, to))
					.where(from.has(property, to))
					.getQueryString();
			update(query).withBinding(from, fromResource).execute();
		}
		if (deleteOtherIcoming) {
			String query = Queries.MODIFY()
					.delete(from.has(property, to))
					.where(
							from.has(property, to)
									.filter(
											Expressions.in(
													to,
													toResources.stream()
															.map(TypeMappingUtils::toIri)
															.collect(Collectors.toList())
															.toArray(
																	new RdfValue[toResources
																			.size()]))))
					.getQueryString();
			update(query).execute();
		}
		String query = Queries.INSERT_DATA(
				toResources.stream()
						.map(e -> toIri(fromResource).has(property, e))
						.collect(Collectors.toList())
						.toArray(new TriplePattern[toResources.size()]))
				.getQueryString();
		updateWithoutCachingStatement(query).execute();
	}

	/**
	 * Returns a {@link Supplier <String>} that returns the String content of the specified resource (as obtained by a
	 * {@link ResourceLoader}). The resource's content is read once when this method is called (revealing any problem
	 * reading the resource early on.
	 */
	public Supplier<String> getStringSupplierFromResourceContent(String resourceName) {
		Objects.requireNonNull(resourceName);
		try {
			org.springframework.core.io.Resource res = resourceLoader.getResource(resourceName);
			String contentAsString = new String(res.getInputStream().readAllBytes());
			return () -> contentAsString;
		} catch (IOException e) {
			throw new RDF4JDaoException(
					String.format("Cannot read String from resource %s", resourceName));
		}
	}

	private RepositoryConnection getRepositoryConnection() {
		return repositoryConnectionFactory.getConnection();
	}

	private void logIfShaclValidationFailure(Throwable t) {
		Throwable cause = t.getCause();
		if (cause instanceof ShaclSailValidationException) {
			logger.error("SHACL Validation failed!");
			Model report = ((ShaclSailValidationException) cause).validationReportAsModel();
			StringWriter out = new StringWriter();
			Rio.write(report, out, RDFFormat.TURTLE);
			logger.error("Validation report:\n{}", out.toString());
		}
	}

	/**
	 * Returns a UUID IRI (schema: 'urn:uuid'). Actual implementation depends on the {@link #uuidSource} that has been
	 * configured. See {@link UUIDSource} and {@link org.eclipse.rdf4j.spring.uuidsource} for details.
	 */
	public IRI getNewUUID() {
		return uuidSource.nextUUID();
	}
}
