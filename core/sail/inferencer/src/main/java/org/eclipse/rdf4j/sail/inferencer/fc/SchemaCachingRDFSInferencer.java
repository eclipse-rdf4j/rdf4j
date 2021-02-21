/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * <p>
 * The SchemaCachingRDFSInferencer is an RDFS reasoner that caches all schema (TBox) statements and calculates an
 * inference map to quickly determine inferred statements. The reasoner can also be instantiated with a predefined
 * schema for improved performance.
 * </p>
 * <p>
 * This reasoner is not a rule based reasoner and will be up to 80x faster than the
 * {@link ForwardChainingRDFSInferencer}, as well as being more complete.
 * </p>
 * <p>
 * The sail puts no limitations on isolation level for read transactions, however all write/delete/update transactions
 * are serializable with exclusive locks. This limits write/delete/update transactions to one transaction at a time.
 * </p>
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class SchemaCachingRDFSInferencer extends NotifyingSailWrapper {

	// An optional predifinedSchema that the user has provided
	Repository predefinedSchema;

	// exclusive lock for modifying the schema cache.
	private final ReentrantLock exclusiveWriteLock = new ReentrantLock(true);

	// If false, the inferencer will skip some RDFS rules.
	boolean useAllRdfsRules = true;

	// the SPIN sail will add inferred statements that it wants to be used for further inference.
	volatile protected boolean useInferredToCreateSchema;

	// Schema cache
	private final Collection<Resource> properties = new HashSet<>();

	private final Collection<Resource> types = new HashSet<>();

	private final Collection<Statement> subClassOfStatements = new HashSet<>();

	private final Collection<Statement> subPropertyOfStatements = new HashSet<>();

	private final Collection<Statement> rangeStatements = new HashSet<>();

	private final Collection<Statement> domainStatements = new HashSet<>();

	// Forward chained schema cache as lookup tables
	private final Map<Resource, Set<Resource>> calculatedTypes = new HashMap<>();

	private final Map<Resource, Set<Resource>> calculatedProperties = new HashMap<>();

	private final Map<Resource, Set<Resource>> calculatedRange = new HashMap<>();

	private final Map<Resource, Set<Resource>> calculatedDomain = new HashMap<>();

	// The inferencer has been instantiated from another inferencer and shares it's schema with that one
	private boolean sharedSchema;

	// Inferred statements can either be added to the default context
	// or to the context that the original inserted statement has
	private boolean addInferredStatementsToDefaultContext = false;

	/**
	 * Instantiate a new SchemaCachingRDFSInferencer
	 */
	public SchemaCachingRDFSInferencer() {
		super();
		predefinedSchema = null;
	}

	/**
	 * Instantiate a SchemaCachingRDFSInferencer.
	 *
	 * @param data Base sail for storing data.
	 */
	public SchemaCachingRDFSInferencer(NotifyingSail data) {
		super(data);
		predefinedSchema = null;

	}

	/**
	 * Instantiate a SchemaCachingRDFSInferencer with a predefined schema. The schema will be used for inference, all
	 * other schema statements added will be ignored and no schema statements can be removed. Using a predefined schema
	 * significantly improves performance.
	 *
	 * @param data             Base sail for storing data.
	 * @param predefinedSchema Repository containing the schema.
	 */
	public SchemaCachingRDFSInferencer(NotifyingSail data, Repository predefinedSchema) {
		super(data);

		this.predefinedSchema = predefinedSchema;

	}

	/**
	 * Instantiate a SchemaCachingRDFSInferencer.
	 *
	 * @param data            Base sail for storing data.
	 * @param useAllRdfsRules Usel all RDFS rules. If set to false rule rdf4a and rdfs4b will be ignore
	 */
	public SchemaCachingRDFSInferencer(NotifyingSail data, boolean useAllRdfsRules) {
		super(data);
		predefinedSchema = null;

		this.useAllRdfsRules = useAllRdfsRules;

	}

	/**
	 * Instantiate a SchemaCachingRDFSInferencer with a predefined schema. The schema will be used for inference, all
	 * other schema statements added will be ignored and no schema statements can be removed. Using a predefined schema
	 * significantly improves performance.
	 *
	 * @param data             Base sail for storing data.
	 * @param predefinedSchema Repository containing the schema.
	 * @param useAllRdfsRules  Usel all RDFS rules. If set to false rule rdf4a and rdfs4b will be ignore
	 */
	public SchemaCachingRDFSInferencer(NotifyingSail data, Repository predefinedSchema,
			boolean useAllRdfsRules) {
		super(data);

		this.predefinedSchema = predefinedSchema;
		this.useAllRdfsRules = useAllRdfsRules;

	}

	void clearInferenceTables() {
		acquireExclusiveWriteLock();
		properties.clear();
		types.clear();
		subClassOfStatements.clear();
		subPropertyOfStatements.clear();
		rangeStatements.clear();
		domainStatements.clear();

		calculatedTypes.clear();
		calculatedProperties.clear();
		calculatedRange.clear();
		calculatedDomain.clear();
	}

	/**
	 * Tries to obtain an exclusive write lock on this store. This method will block until either the lock is obtained
	 * or an interrupt signal is received.
	 *
	 * @throws SailException if the thread is interrupted while waiting to obtain the lock.
	 */
	void acquireExclusiveWriteLock() {
		if (exclusiveWriteLock.isHeldByCurrentThread()) {
			return;
		}

		try {
			exclusiveWriteLock.lockInterruptibly();
		} catch (InterruptedException e) {
			throw new SailException(e);
		}
	}

	/**
	 * Releases the exclusive write lock.
	 */
	void releaseExclusiveWriteLock() {
		while (exclusiveWriteLock.isHeldByCurrentThread()) {
			exclusiveWriteLock.unlock();
		}
	}

	@Override
	public void initialize()
			throws SailException {
		super.initialize();

		if (sharedSchema) {
			return;
		}

		try (final SchemaCachingRDFSInferencerConnection conn = getConnection()) {
			conn.begin();

			conn.addAxiomStatements();

			List<Statement> tboxStatments = new ArrayList<>();

			if (predefinedSchema != null) {

				try (RepositoryConnection schemaConnection = predefinedSchema.getConnection()) {
					schemaConnection.begin();
					try (Stream<Statement> stream = schemaConnection.getStatements(null, null, null).stream()) {
						tboxStatments = stream
								.peek(conn::processForSchemaCache)
								.collect(Collectors.toList());
					}
					schemaConnection.commit();
				}
			}

			calculateInferenceMaps(conn, true);

			if (predefinedSchema != null) {
				tboxStatments.forEach(statement -> conn.addStatement(statement.getSubject(),
						statement.getPredicate(), statement.getObject(), statement.getContext()));
			}

			conn.commit();
		}

	}

	@Override
	public SchemaCachingRDFSInferencerConnection getConnection()
			throws SailException {
		InferencerConnection e = (InferencerConnection) super.getConnection();
		return new SchemaCachingRDFSInferencerConnection(this, e);
	}

	@Override
	public ValueFactory getValueFactory() {
		return getBaseSail().getValueFactory();
	}

	/**
	 * Instantiate a new SchemaCachingRDFSInferencer from an existing one. Fast instantiation extracts the schema lookup
	 * tables generated by the existing sail and uses them to populate the lookup tables of a new reasoner. Schema
	 * triples can not be queried in the SchemaCachingRDFSInferencer returned by this method.
	 *
	 * @param sailToInstantiateFrom The SchemaCachingRDFSInferencer to extract the lookup tables from.
	 * @param store                 Base sail for storing data.
	 * @return inferencer
	 */
	static public SchemaCachingRDFSInferencer fastInstantiateFrom(
			SchemaCachingRDFSInferencer sailToInstantiateFrom, NotifyingSail store) {
		return fastInstantiateFrom(sailToInstantiateFrom, store, true);
	}

	/**
	 * Instantiate a new SchemaCachingRDFSInferencer from an existing one. Fast instantiation extracts the schema lookup
	 * tables generated by the existing sail and uses them to populate the lookup tables of a new reasoner. Schema
	 * triples can not be queried in the SchemaCachingRDFSInferencer returned by this method.
	 *
	 * @param sailToInstantiateFrom The SchemaCachingRDFSInferencer to extract the lookup tables from.
	 * @param store                 Base sail for storing data.
	 * @param useAllRdfsRules       Use all RDFS rules. If set to false rule rdf4a and rdfs4b will be ignore
	 * @return inferencer
	 */
	static public SchemaCachingRDFSInferencer fastInstantiateFrom(
			SchemaCachingRDFSInferencer sailToInstantiateFrom, NotifyingSail store,
			boolean useAllRdfsRules) {

		sailToInstantiateFrom.getConnection().close();

		SchemaCachingRDFSInferencer ret = new SchemaCachingRDFSInferencer(store,
				sailToInstantiateFrom.predefinedSchema, useAllRdfsRules);

		ret.sharedSchema = true;

		sailToInstantiateFrom.calculatedTypes.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedTypes.containsKey(key)) {
					ret.calculatedTypes.put(key, new HashSet<>());
				}
				ret.calculatedTypes.get(key).add(v);
			});
		});

		sailToInstantiateFrom.calculatedProperties.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedProperties.containsKey(key)) {
					ret.calculatedProperties.put(key, new HashSet<>());
				}
				ret.calculatedProperties.get(key).add(v);
			});
		});

		sailToInstantiateFrom.calculatedRange.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedRange.containsKey(key)) {
					ret.calculatedRange.put(key, new HashSet<>());
				}
				ret.calculatedRange.get(key).add(v);
			});
		});

		sailToInstantiateFrom.calculatedDomain.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedDomain.containsKey(key)) {
					ret.calculatedDomain.put(key, new HashSet<>());
				}
				ret.calculatedDomain.get(key).add(v);
			});
		});

		return ret;

	}

	long getSchemaSize() {
		return subClassOfStatements.size() + subPropertyOfStatements.size() + rangeStatements.size()
				+ domainStatements.size() + properties.size() + types.size();
	}

	void calculateInferenceMaps(SchemaCachingRDFSInferencerConnection conn, boolean addInferred) {
		calculateSubClassOf(subClassOfStatements);
		properties.forEach(predicate -> {
			if (addInferred) {
				conn.addInferredStatementInternal(predicate, RDF.TYPE, RDF.PROPERTY);
			}
			calculatedProperties.put(predicate, new HashSet<>());
		});
		calculateSubPropertyOf(subPropertyOfStatements);

		calculateRangeDomain(rangeStatements, calculatedRange);
		calculateRangeDomain(domainStatements, calculatedDomain);

		if (addInferred) {
			calculatedTypes.forEach((subClass, superClasses) -> {
				conn.addInferredStatementInternal(subClass, RDFS.SUBCLASSOF, subClass);

				superClasses.forEach(superClass -> {
					conn.addInferredStatementInternal(subClass, RDFS.SUBCLASSOF, superClass);
					conn.addInferredStatementInternal(superClass, RDFS.SUBCLASSOF, superClass);

				});
			});
		}
		if (addInferred) {
			calculatedProperties.forEach((sub, sups) -> {
				conn.addInferredStatementInternal(sub, RDFS.SUBPROPERTYOF, sub);

				sups.forEach(sup -> {
					conn.addInferredStatementInternal(sub, RDFS.SUBPROPERTYOF, sup);
					conn.addInferredStatementInternal(sup, RDFS.SUBPROPERTYOF, sup);

				});
			});
		}
	}

	void addSubClassOfStatement(Statement st) {
		subClassOfStatements.add(st);
		types.add(st.getSubject());
		types.add((Resource) st.getObject());
	}

	void addSubPropertyOfStatement(Statement st) {
		subPropertyOfStatements.add(st);
		properties.add(st.getSubject());
		properties.add((Resource) st.getObject());
	}

	void addRangeStatement(Statement st) {
		rangeStatements.add(st);
		properties.add(st.getSubject());
		types.add((Resource) st.getObject());
	}

	void addDomainStatement(Statement st) {
		domainStatements.add(st);
		properties.add(st.getSubject());
		types.add((Resource) st.getObject());
	}

	boolean hasType(Resource r) {
		return types.contains(r);
	}

	void addType(Resource r) {
		types.add(r);
	}

	boolean hasProperty(Resource property) {
		return properties.contains(property);
	}

	void addProperty(Resource property) {
		properties.add(property);
	}

	Set<Resource> resolveTypes(Resource value) {
		Set<Resource> iris = calculatedTypes.get(value);

		return iris != null ? iris : Collections.emptySet();
	}

	Set<Resource> resolveProperties(Resource predicate) {
		Set<Resource> iris = calculatedProperties.get(predicate);

		return iris != null ? iris : Collections.emptySet();
	}

	Set<Resource> resolveRangeTypes(IRI predicate) {
		Set<Resource> iris = calculatedRange.get(predicate);

		return iris != null ? iris : Collections.emptySet();
	}

	Set<Resource> resolveDomainTypes(IRI predicate) {
		Set<Resource> iris = calculatedDomain.get(predicate);

		return iris != null ? iris : Collections.emptySet();
	}

	private void calculateSubClassOf(Collection<Statement> subClassOfStatements) {
		types.forEach(type -> {
			if (!calculatedTypes.containsKey(type)) {
				calculatedTypes.put(type, new HashSet<>());
			}

			calculatedTypes.get(type).add(type);

		});

		subClassOfStatements.forEach(s -> {
			Resource subClass = s.getSubject();
			if (!calculatedTypes.containsKey(subClass)) {
				calculatedTypes.put(subClass, new HashSet<>());
			}

			calculatedTypes.get(subClass).add((Resource) s.getObject());

		});

		// Fixed point approach to finding all sub-classes.
		// prevSize is the size of the previous application of the function
		// newSize is the size of the current application of the function
		// Fixed point is reached when they are the same.
		// Eg. Two consecutive applications return the same number of subclasses
		long prevSize = 0;
		final long[] newSize = { -1 };
		while (prevSize != newSize[0]) {

			prevSize = newSize[0];

			newSize[0] = 0;

			calculatedTypes.forEach((key, value) -> {
				List<Resource> temp = new ArrayList<>();
				value.forEach(superClass -> temp.addAll(resolveTypes(superClass)));

				value.addAll(temp);
				newSize[0] += value.size();
			});

		}
	}

	private void calculateSubPropertyOf(Collection<Statement> subPropertyOfStatemenets) {

		subPropertyOfStatemenets.forEach(s -> {
			Resource subClass = s.getSubject();
			Resource superClass = (Resource) s.getObject();
			if (!calculatedProperties.containsKey(subClass)) {
				calculatedProperties.put(subClass, new HashSet<>());
			}

			if (!calculatedProperties.containsKey(superClass)) {
				calculatedProperties.put(superClass, new HashSet<>());
			}

			calculatedProperties.get(subClass).add((Resource) s.getObject());

		});

		// Fixed point approach to finding all sub-properties.
		// prevSize is the size of the previous application of the function
		// newSize is the size of the current application of the function
		// Fixed point is reached when they are the same.
		long prevSize = 0;
		final long[] newSize = { -1 };
		while (prevSize != newSize[0]) {

			prevSize = newSize[0];

			newSize[0] = 0;

			calculatedProperties.forEach((key, value) -> {
				List<Resource> temp = new ArrayList<>();
				value.forEach(superProperty -> temp.addAll(resolveProperties(superProperty)));

				value.addAll(temp);
				newSize[0] += value.size();
			});

		}
	}

	private void calculateRangeDomain(Collection<Statement> rangeOrDomainStatements,
			Map<Resource, Set<Resource>> calculatedRangeOrDomain) {

		rangeOrDomainStatements.forEach(s -> {
			Resource predicate = s.getSubject();
			if (!calculatedProperties.containsKey(predicate)) {
				calculatedProperties.put(predicate, new HashSet<>());
			}

			if (!calculatedRangeOrDomain.containsKey(predicate)) {
				calculatedRangeOrDomain.put(predicate, new HashSet<>());
			}

			calculatedRangeOrDomain.get(predicate).add((Resource) s.getObject());

			if (!calculatedTypes.containsKey(s.getObject())) {
				calculatedTypes.put((Resource) s.getObject(), new HashSet<>());
			}

		});

		calculatedProperties.keySet()
				.stream()
				.filter(
						key -> !calculatedRangeOrDomain.containsKey(key))
				.forEach(
						key -> calculatedRangeOrDomain.put(key, new HashSet<>()));

		// Fixed point approach to finding all ranges or domains.
		// prevSize is the size of the previous application of the function
		// newSize is the size of the current application of the function
		// Fixed point is reached when they are the same.
		long prevSize = 0;
		final long[] newSize = { -1 };
		while (prevSize != newSize[0]) {

			prevSize = newSize[0];

			newSize[0] = 0;

			calculatedRangeOrDomain.forEach((key, value) -> {
				List<Resource> resolvedBySubProperty = new ArrayList<>();
				resolveProperties(key).forEach(newPredicate -> {
					Set<Resource> iris = calculatedRangeOrDomain.get(newPredicate);
					if (iris != null) {
						resolvedBySubProperty.addAll(iris);
					}

				});

				List<Resource> resolvedBySubClass = new ArrayList<>();
				value.addAll(resolvedBySubProperty);

				value.stream().map(this::resolveTypes).forEach(resolvedBySubClass::addAll);

				value.addAll(resolvedBySubClass);

				newSize[0] += value.size();
			});

		}
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		IsolationLevel level = super.getDefaultIsolationLevel();
		if (level.isCompatibleWith(IsolationLevels.READ_COMMITTED)) {
			return level;
		} else {
			List<IsolationLevel> supported = this.getSupportedIsolationLevels();
			return IsolationLevels.getCompatibleIsolationLevel(IsolationLevels.READ_COMMITTED, supported);
		}
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		List<IsolationLevel> supported = super.getSupportedIsolationLevels();
		List<IsolationLevel> levels = new ArrayList<>(supported.size());
		for (IsolationLevel level : supported) {
			if (level.isCompatibleWith(IsolationLevels.READ_COMMITTED)) {
				levels.add(level);
			}
		}
		return levels;
	}

	/**
	 * <p>
	 * Inferred statements can either be added to the default context or to the context that the original inserted
	 * statement has.
	 * </p>
	 **/

	public boolean isAddInferredStatementsToDefaultContext() {
		return addInferredStatementsToDefaultContext;
	}

	/**
	 * <p>
	 * Inferred statements can either be added to the default context or to the context that the original inserted
	 * statement has. setAddInferredStatementsToDefaultContext(true) will add all inferred statements to the default
	 * context.
	 * </p>
	 * <p>
	 * Which context a tbox statement is added to is undefined.
	 * </p>
	 * <p>
	 * Before 3.0 default value for addInferredStatementsToDefaultContext was true. From 3.0 the default value is false.
	 * </p>
	 *
	 * @param addInferredStatementsToDefaultContext
	 */
	public void setAddInferredStatementsToDefaultContext(boolean addInferredStatementsToDefaultContext) {
		this.addInferredStatementsToDefaultContext = addInferredStatementsToDefaultContext;
	}

	boolean usesPredefinedSchema() {
		return predefinedSchema != null || sharedSchema;
	}
}
