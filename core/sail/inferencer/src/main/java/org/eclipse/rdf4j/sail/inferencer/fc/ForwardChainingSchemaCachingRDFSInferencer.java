/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.inferencer.fc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;

/**
 * @author Håvard Mikkelsen Ottestad
 */

public class ForwardChainingSchemaCachingRDFSInferencer extends AbstractForwardChainingInferencer {

	// The schema, or null
	Repository schema;

	private static final Random random = new Random();

	private StampedLock readWriteLock = new StampedLock();

	private AtomicInteger numberOfThreadsWaitingForWriteLock = new AtomicInteger(0);

	// If false, the inferencer will skip some RDFS rules.
	boolean useAllRdfsRules = true;

	// Schema caches
	private Collection<Resource> properties = new HashSet<>();

	private Collection<Resource> types = new HashSet<>();

	private final Model schemaStatements = new LinkedHashModel();

	// Forward chained Schema cache as lookup tables
	private Map<Resource, Set<Resource>> calculatedTypes = new HashMap<>();

	private Map<Resource, Set<Resource>> calculatedProperties = new HashMap<>();

	private Map<Resource, Set<Resource>> calculatedRange = new HashMap<>();

	private Map<Resource, Set<Resource>> calculatedDomain = new HashMap<>();

	// The inferencer has been instantiated from another inferencer and shares it's schema with that one
	private boolean sharedSchema;

	void clearInferenceTables() {
		properties = new HashSet<>();
		types = new HashSet<>();
		schemaStatements.clear();

		calculatedTypes = new HashMap<>();
		calculatedProperties = new HashMap<>();
		calculatedRange = new HashMap<>();
		calculatedDomain = new HashMap<>();
	}

	void addSubClassOfStatement(Statement st) {
		schemaStatements.add(st);
		types.add(st.getSubject());
		types.add((Resource)st.getObject());
	}

	void addSubPropertyOfStatement(Statement st) {
		schemaStatements.add(st);
		properties.add(st.getSubject());
		properties.add((Resource)st.getObject());
	}

	void addRangeStatement(Statement st) {
		schemaStatements.add(st);
		properties.add(st.getSubject());
		types.add((Resource)st.getObject());
	}

	void addDomainStatement(Statement st) {
		schemaStatements.add(st);
		properties.add(st.getSubject());
		types.add((Resource)st.getObject());
	}

	void addType(Resource r) {
		types.add(r);
	}

	boolean hasType(Resource r) {
		return types.contains(r);
	}

	boolean hasProperty(Resource r) {
		return properties.contains(r);
	}

	void addProperty(Resource r) {
		properties.add(r);
	}

	void readLock(ForwardChainingSchemaCachingRDFSInferencerConnection connection) {

		//        if (numberOfThreadsWaitingForWriteLock.get() > 0) {
		//            System.err.println("starve reads");
		//        }

		while (numberOfThreadsWaitingForWriteLock.get() > 0) {
			Thread.yield();
		}

		if (connection.lockStamp != 0) {
			throw new SailException(
					"Connection already has a lock! Might be a begin() without a commit() or rollback() for previous transaction().");
		}
		connection.lockStamp = readWriteLock.readLock();
		//        System.err.println("readLock: " + connection.lockStamp);

	}

	void releaseLock(ForwardChainingSchemaCachingRDFSInferencerConnection connection) {
		if (connection.lockStamp == 0) {
			throw new SailException(
					"Expected connection to have lock. Might be a commit() without a begin().");
		}
		readWriteLock.unlock(connection.lockStamp);
		//        System.err.println("Released lock: " + connection.lockStamp);

		connection.lockStamp = 0;
	}

	void upgradeLock(ForwardChainingSchemaCachingRDFSInferencerConnection connection) {

		//        System.err.println("Attempt writelock: "+connection.lockStamp);

		numberOfThreadsWaitingForWriteLock.incrementAndGet();

		try {
			while (true) {
				long l = readWriteLock.tryConvertToWriteLock(connection.lockStamp);

				if (l != 0) {
					//                    long temp = connection.lockStamp;
					connection.lockStamp = l;
					//                    if (temp != l) {
					//                        System.err.println("readLock: " + temp + " writeLock: " + connection.lockStamp);
					//                    }

					return;
				}

				try {
					Thread.sleep(random.nextInt(2));
				}
				catch (InterruptedException e) {
					// ignore interrupted exception
				}

				// detect potential deadlock scenario
				if (numberOfThreadsWaitingForWriteLock.get() > 1 //More than 1 connection waiting for a write lock
						&& numberOfThreadsWaitingForWriteLock.get() <= readWriteLock.getReadLockCount() // no connection only using a read lock
						&& random.nextBoolean())
				{ // randomly kick this connection out
					break;
				}

			}

			//            System.err.println("isReadLocked: " + readWriteLock.isReadLocked());
			//            System.err.println("isWriteLocked(): " + readWriteLock.isWriteLocked());
			//            System.err.println("read lock count: " + readWriteLock.getReadLockCount());

			releaseLock(connection);
			throw new SailConflictException("Concurrent modification of schema, could not acquire the lock.");
		}
		finally {

			numberOfThreadsWaitingForWriteLock.decrementAndGet();
		}

	}

	public ForwardChainingSchemaCachingRDFSInferencer(NotifyingSail data) {
		super(data);
		schema = null;

	}

	public ForwardChainingSchemaCachingRDFSInferencer(NotifyingSail data, Repository schema) {
		super(data);

		this.schema = schema;

	}

	public ForwardChainingSchemaCachingRDFSInferencer(NotifyingSail data, boolean useAllRdfsRules) {
		super(data);
		schema = null;

		this.useAllRdfsRules = useAllRdfsRules;

	}

	public ForwardChainingSchemaCachingRDFSInferencer(NotifyingSail data, Repository schema,
			boolean useAllRdfsRules)
	{
		super(data);

		this.schema = schema;
		this.useAllRdfsRules = useAllRdfsRules;

	}

	public void initialize()
		throws SailException
	{
		super.initialize();

		if (sharedSchema) {
			return;
		}

		try (final ForwardChainingSchemaCachingRDFSInferencerConnection connection = getConnection()) {
			connection.begin();
			connection.addAxiomStatements();
			Model tbox = null;
			if (schema != null) {
				try (RepositoryConnection schemaConnection = schema.getConnection()) {
					schemaConnection.begin();
					tbox = QueryResults.asModel(schemaConnection.getStatements(null, null, null));
					tbox.forEach(connection::processForSchemaCache);
					schemaConnection.commit();
				}
			}

			calculateInferenceMaps(connection);
			if (schema != null) {
				tbox.forEach(statement -> connection.addStatement(statement.getSubject(),
						statement.getPredicate(), statement.getObject(), statement.getContext()));
			}
			connection.commit();
		}
	}

	public void setDataDir(File file) {
		throw new UnsupportedOperationException();
	}

	public File getDataDir() {
		throw new UnsupportedOperationException();
	}

	public ForwardChainingSchemaCachingRDFSInferencerConnection getConnection()
		throws SailException
	{
		InferencerConnection e = (InferencerConnection)super.getConnection();
		return new ForwardChainingSchemaCachingRDFSInferencerConnection(this, e);
	}

	public ValueFactory getValueFactory() {
		return this.getBaseSail().getValueFactory();
	}

	public static ForwardChainingSchemaCachingRDFSInferencer fastInstantiateFrom(
			ForwardChainingSchemaCachingRDFSInferencer baseSail, NotifyingSail store)
	{
		return fastInstantiateFrom(baseSail, store, true);
	}

	public static ForwardChainingSchemaCachingRDFSInferencer fastInstantiateFrom(
			ForwardChainingSchemaCachingRDFSInferencer baseSail, NotifyingSail store,
			boolean useAllRdfsRules)
	{

		baseSail.getConnection().close();

		ForwardChainingSchemaCachingRDFSInferencer ret = new ForwardChainingSchemaCachingRDFSInferencer(store,
				baseSail.schema, useAllRdfsRules);

		ret.sharedSchema = true;

		baseSail.calculatedTypes.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedTypes.containsKey(key)) {
					ret.calculatedTypes.put(key, new HashSet<>());
				}
				ret.calculatedTypes.get(key).add(v);
			});
		});

		baseSail.calculatedProperties.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedProperties.containsKey(key)) {
					ret.calculatedProperties.put(key, new HashSet<>());
				}
				ret.calculatedProperties.get(key).add(v);
			});
		});

		baseSail.calculatedRange.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedRange.containsKey(key)) {
					ret.calculatedRange.put(key, new HashSet<>());
				}
				ret.calculatedRange.get(key).add(v);
			});
		});

		baseSail.calculatedDomain.forEach((key, value) -> {
			value.forEach(v -> {
				if (!ret.calculatedDomain.containsKey(key)) {
					ret.calculatedDomain.put(key, new HashSet<>());
				}
				ret.calculatedDomain.get(key).add(v);
			});
		});

		return ret;

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

			calculatedTypes.get(subClass).add((Resource)s.getObject());

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

	protected void calculateInferenceMaps(ForwardChainingSchemaCachingRDFSInferencerConnection conn) {

		calculateSubClassOf(schemaStatements.filter(null, RDFS.SUBCLASSOF, null));
		properties.forEach(predicate -> {
			conn.addInferredStatement(predicate, RDF.TYPE, RDF.PROPERTY);
			calculatedProperties.put(predicate, new HashSet<>());
		});
		calculateSubPropertyOf(schemaStatements.filter(null, RDFS.SUBPROPERTYOF, null));

		calculateRangeDomain(schemaStatements.filter(null, RDFS.RANGE, null), calculatedRange);
		calculateRangeDomain(schemaStatements.filter(null, RDFS.DOMAIN, null), calculatedDomain);

		calculatedTypes.forEach((subClass, superClasses) -> {
			conn.addInferredStatement(subClass, RDFS.SUBCLASSOF, subClass);

			superClasses.forEach(superClass -> {
				conn.addInferredStatement(subClass, RDFS.SUBCLASSOF, superClass);
				conn.addInferredStatement(superClass, RDFS.SUBCLASSOF, superClass);

			});
		});

		calculatedProperties.forEach((sub, sups) -> {
			conn.addInferredStatement(sub, RDFS.SUBPROPERTYOF, sub);

			sups.forEach(sup -> {
				conn.addInferredStatement(sub, RDFS.SUBPROPERTYOF, sup);
				conn.addInferredStatement(sup, RDFS.SUBPROPERTYOF, sup);
			});
		});

	}

	private void calculateRangeDomain(Collection<Statement> rangeOrDomainStatements,
			Map<Resource, Set<Resource>> calculatedRangeOrDomain)
	{

		rangeOrDomainStatements.forEach(s -> {
			Resource predicate = s.getSubject();
			if (!calculatedProperties.containsKey(predicate)) {
				calculatedProperties.put(predicate, new HashSet<>());
			}

			if (!calculatedRangeOrDomain.containsKey(predicate)) {
				calculatedRangeOrDomain.put(predicate, new HashSet<>());
			}

			calculatedRangeOrDomain.get(predicate).add((Resource)s.getObject());

			if (!calculatedTypes.containsKey(s.getObject())) {
				calculatedTypes.put((Resource)s.getObject(), new HashSet<>());
			}

		});

		calculatedProperties.keySet().stream().filter(
				key -> !calculatedRangeOrDomain.containsKey(key)).forEach(
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

	private void calculateSubPropertyOf(Collection<Statement> subPropertyOfStatemenets) {

		subPropertyOfStatemenets.forEach(s -> {
			Resource subClass = s.getSubject();
			Resource superClass = (Resource)s.getObject();
			if (!calculatedProperties.containsKey(subClass)) {
				calculatedProperties.put(subClass, new HashSet<>());
			}

			if (!calculatedProperties.containsKey(superClass)) {
				calculatedProperties.put(superClass, new HashSet<>());
			}

			calculatedProperties.get(subClass).add((Resource)s.getObject());

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
}
