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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.spring.dao.exception.IncorrectResultSetSizeException;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.BindingsBuilder;
import org.eclipse.rdf4j.spring.dao.support.bindingsBuilder.MutableBindings;
import org.eclipse.rdf4j.spring.dao.support.key.CompositeKey;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.TupleQueryEvaluationBuilder;
import org.eclipse.rdf4j.spring.dao.support.opbuilder.UpdateExecutionBuilder;
import org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * Base class for DAOs providing CRUD functionality. The class allows for entities to be represented with different
 * classes for read (ENTITY type) vs write (INPUT type) operations. DAOs that do not require this distinction must use
 * the same class for both parameters.
 *
 * @param <ENTITY>
 * @param <INPUT>
 * @param <ID>
 *
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public abstract class RDF4JCRUDDao<ENTITY, INPUT, ID> extends RDF4JDao {
	private static final String KEY_READ_QUERY = "readQuery";
	public static final String KEY_PREFIX_INSERT = "insert";
	public static final String KEY_PREFIX_UPDATE = "update";
	private final Class<ID> idClass;

	/**
	 * Constructor that provides the type of the ID to the base implementation. This constructor has to be used if the
	 * ID is anything but IRI.
	 */
	public RDF4JCRUDDao(RDF4JTemplate rdf4JTemplate, Class<ID> idClass) {
		super(rdf4JTemplate);
		this.idClass = idClass;
	}

	/**
	 * Constructor to be used by implementations that use IRI for the ID type.
	 */
	public RDF4JCRUDDao(RDF4JTemplate rdf4JTemplate) {
		this(rdf4JTemplate, (Class<ID>) IRI.class);
	}

	/**
	 * Saves the entity, loads it again and returns it. If the modified entity is not required, clients should prefer
	 * {@link #saveAndReturnId(Object, Object)} or {@link #saveAndReturnId(Object)}
	 */
	public final ENTITY save(INPUT input) {
		ID id = getInputId(input);
		final ID finalId = saveAndReturnId(input, id);
		return getById(finalId);
	}

	public ID saveAndReturnId(INPUT input) {
		return saveAndReturnId(input, getInputId(input));
	}

	/**
	 * Saves the entity and returns its (possibly newly generated) ID.
	 *
	 * @param input the entity
	 * @param id    the id or null for a new entity.
	 * @return the id (a newly generated one if the specified <code>id</code> is null, otherwise just <code>id</code>.
	 */
	public ID saveAndReturnId(INPUT input, ID id) {
		if (id != null) {
			// delete triples for the modify case
			deleteForUpdate(id);
		}
		final ID finalId = getOrGenerateId(id);
		getInsertQueryOrUseCached(input)
				.withBindings(bindingsBuilder -> populateIdBindings(bindingsBuilder, finalId))
				.withBindings(bindingsBuilder -> populateBindingsForUpdate(bindingsBuilder, input))
				.execute(bindings -> postProcessUpdate(input, bindings));
		return finalId;
	}

	/**
	 * When updating an entity via {@link #save(Object)}, its triples are removed first using this method. The default
	 * implementation used {@link RDF4JTemplate#deleteTriplesWithSubject(IRI)}. If more complex deletion behaviour (e.g.
	 * cascading) is needed, this method should be overriden.
	 *
	 */
	protected void deleteForUpdate(ID id) {
		IRI iri = convertIdToIri(id);
		getRdf4JTemplate().deleteTriplesWithSubject(iri);
	}

	private ID getOrGenerateId(ID id) {
		boolean idPresent;
		if (id instanceof CompositeKey) {
			idPresent = ((CompositeKey) id).isPresent();
		} else {
			idPresent = id != null;
		}
		if (!idPresent) {
			id = generateNewId(id);
		}
		return id;
	}

	/**
	 * Converts the provided id to an IRI. The default implementation only works for DAOs that use IRI ids.
	 *
	 * @param id
	 * @return
	 */
	protected IRI convertIdToIri(ID id) {
		if (id == null) {
			return null;
		}
		if (idClass.equals(IRI.class)) {
			return (IRI) id;
		}
		throw new UnsupportedOperationException(
				"Cannot generically convert IDs to IRIs. The subclass must implement convertToIri(ID)");
	}

	/**
	 * Generates a new id for an entity. The default implementation only works for IRI ids.
	 *
	 * @param providedId
	 * @return a new id.
	 */
	protected ID generateNewId(ID providedId) {
		if (idClass.equals(IRI.class)) {
			return (ID) getRdf4JTemplate().getNewUUID();
		}
		throw new UnsupportedOperationException(
				"Cannot generically generate any other IDs than IRIs. The subclass must implement generateNewId(ID)");
	}

	private UpdateExecutionBuilder getInsertQueryOrUseCached(INPUT input) {
		final NamedSparqlSupplier cs = getInsertSparql(input);
		String key = KEY_PREFIX_INSERT + cs.getName();
		return getRdf4JTemplate().update(this.getClass(), key, cs.getSparqlSupplier());
	}

	public final List<ENTITY> list() {
		return getReadQueryOrUseCached()
				.evaluateAndConvert()
				.toList(this::mapSolution, this::postProcessMappedSolution);
	}

	private TupleQueryEvaluationBuilder getReadQueryOrUseCached() {
		return getRdf4JTemplate().tupleQuery(getClass(), KEY_READ_QUERY, this::getReadQuery);
	}

	/**
	 * Obtains the entity with the specified id, throwing an exception if none is found.
	 *
	 * @param id the id
	 * @throws IncorrectResultSetSizeException if no entity is found with the specified id
	 * @return the entity
	 */
	public final ENTITY getById(ID id) {
		return getByIdOptional(id)
				.orElseThrow(
						() -> new IncorrectResultSetSizeException(
								"Expected to find exactly one entity but found 0", 1, 0));
	}

	/**
	 * Obtains an optional entity with the specified id.
	 *
	 * @param id the id
	 * @return an Optional maybe containing the entity
	 */
	public final Optional<ENTITY> getByIdOptional(ID id) {
		return getReadQueryOrUseCached()
				.withBindings(bindingsBuilder -> populateIdBindings(bindingsBuilder, id))
				.evaluateAndConvert()
				.toSingletonOptional(this::mapSolution, this::postProcessMappedSolution);
	}

	/**
	 * Naive implementation using {@link RDF4JTemplate#delete(IRI)}. DAOs that need more complex deletion behaviour
	 * (e.g. cascading) should override this method.
	 *
	 */
	public void delete(ID id) {
		if (idClass.equals(IRI.class)) {
			getRdf4JTemplate().delete((IRI) id);
		} else {
			throw new UnsupportedOperationException(
					"Cannot generically delete instances that do not use IRI ids. The subclass must implement delete(ID)");
		}
	}

	/**
	 * Returns the SPARQL string used to read an instance of T from the database. The base implementation will cache the
	 * query string, so implementations should not try to cache the query.
	 */
	protected String getReadQuery() {
		throw new UnsupportedOperationException(
				"Cannot perform generic read operation: subclass does not override getReadQuery()");
	}

	/**
	 * Map one solution of the readQuery to the type of this DAO.
	 */
	protected ENTITY mapSolution(BindingSet querySolution) {
		throw new UnsupportedOperationException(
				"Cannot perform generic read operation: subclass does not override mapSolution()");
	}

	/**
	 * Callback invoked after mapping a solution to an entity, allowing subclasses to modify the entity before returning
	 * it to the client.
	 */
	protected ENTITY postProcessMappedSolution(ENTITY entity) {
		return entity;
	}

	/**
	 * Callback invoked after a successful insert/update.
	 */
	protected void postProcessUpdate(INPUT input, Map<String, Value> bindings) {
		// empty default implementation
	}

	/**
	 * Returns the SPARQL string used to write an instance of T to the database. The instance to be inserted is passed
	 * to the function so implementations can decide which query to use based on the instance.
	 */
	protected NamedSparqlSupplier getInsertSparql(INPUT input) {
		throw new UnsupportedOperationException(
				"Cannot perform generic write operation: subclass does not override getInsertQuery()");
	}

	/**
	 * Returns the SPARQL string used to update an instance of T in the database. The instance to be updated is passed
	 * to the function so implementations can decide which query to use based on the instance.
	 */
	protected NamedSparqlSupplier getUpdateSparql(INPUT input) {
		throw new UnsupportedOperationException(
				"Cannot perform generic write operation: subclass does not override getUpdateQuery()");
	}

	/**
	 * Binds the instance id to query variable(s).
	 */
	protected abstract void populateIdBindings(MutableBindings bindingsBuilder, ID id);

	/**
	 * Sets the non-id bindings on for the write query such that the instance of type I is written to the database. ID
	 * bindings are set through populateIdBindings()
	 */
	protected void populateBindingsForUpdate(MutableBindings bindingsBuilder, INPUT input) {
		throw new UnsupportedOperationException(
				"Cannot perform generic write operation: subclass does not override populateBindingsForUpdate()");
	}

	/**
	 * Obtains the id of the input instance or null if it is new (or a partially populated composite key).
	 */
	protected ID getInputId(INPUT input) {
		throw new UnsupportedOperationException(
				"Cannot perform generic write operation: subclass does not override getInputId()");
	}

	/**
	 * Returns a new BindingsBuilder for your convenience.
	 */
	protected static BindingsBuilder newBindingsBuilder() {
		return new BindingsBuilder();
	}
}
