/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@Deprecated
@InternalUseOnly
public class RdfsSubClassOfReasoner {

	private static final Logger logger = LoggerFactory.getLogger(RdfsSubClassOfReasoner.class);

	private final Collection<Statement> subClassOfStatements = new ArrayList<>();
	private final Collection<Resource> types = new ArrayList<>();

	private final Map<Resource, Set<Resource>> forwardChainCache = new HashMap<>();
	private final Map<Resource, Set<Resource>> backwardsChainCache = new HashMap<>();

	public Stream<Statement> forwardChain(Statement statement) {
		if (forwardChainCache.isEmpty()) {
			return Stream.of(statement);
		}

		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		if (statement.getPredicate().equals(RDF.TYPE)
				&& forwardChainCache.containsKey(((Resource) statement.getObject()))) {
			return forwardChainCache.get(statement.getObject())
					.stream()
					.map(r -> vf.createStatement(statement.getSubject(), RDF.TYPE, r, statement.getContext()));
		}
		return Stream.of(statement);
	}

	public Set<Resource> backwardsChain(Resource type) {
		if (backwardsChainCache.isEmpty()) {
			return Collections.singleton(type);
		}

		Set<Resource> resources = backwardsChainCache.get(type);
		if (resources != null) {
			return resources;
		}
		return Collections.singleton(type);
	}

	private void addSubClassOfStatement(Statement st) {
		subClassOfStatements.add(st);
		types.add(st.getSubject());
		types.add((Resource) st.getObject());
	}

	private void calculateSubClassOf(Collection<Statement> subClassOfStatements) {
		if (subClassOfStatements.isEmpty()) {
			return;
		}

		types.forEach(type -> {
			if (!forwardChainCache.containsKey(type)) {
				forwardChainCache.put(type, new HashSet<>());
			}
			if (!backwardsChainCache.containsKey(type)) {
				backwardsChainCache.put(type, new HashSet<>());
			}

			forwardChainCache.get(type).add(type);
			backwardsChainCache.get(type).add(type);

		});

		subClassOfStatements.forEach(s -> {
			Resource subClass = s.getSubject();
			Resource supClass = (Resource) s.getObject();
			if (!forwardChainCache.containsKey(subClass)) {
				forwardChainCache.put(subClass, new HashSet<>());
			}
			if (!backwardsChainCache.containsKey(supClass)) {
				backwardsChainCache.put(supClass, new HashSet<>());
			}

			forwardChainCache.get(subClass).add((Resource) s.getObject());
			backwardsChainCache.get(supClass).add((Resource) s.getSubject());

		});

		forwardChainUntilFixPoint(forwardChainCache);
		forwardChainUntilFixPoint(backwardsChainCache);

	}

	private void forwardChainUntilFixPoint(Map<Resource, Set<Resource>> forwardChainCache) {
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

			forwardChainCache.forEach((key, value) -> {
				List<Resource> temp = new ArrayList<>();
				value.forEach(superClass -> temp.addAll(resolveTypes(superClass, forwardChainCache)));

				value.addAll(temp);
				newSize[0] += value.size();
			});

		}
	}

	private Set<Resource> resolveTypes(Resource value, Map<Resource, Set<Resource>> forwardChainCache) {
		Set<Resource> iris = forwardChainCache.get(value);
		return iris != null ? iris : Collections.emptySet();
	}

	static RdfsSubClassOfReasoner createReasoner(ShaclSailConnection shaclSailConnection) {
		long before = 0;
		if (shaclSailConnection.sail.isPerformanceLogging()) {
			before = System.currentTimeMillis();
		}

		RdfsSubClassOfReasoner rdfsSubClassOfReasoner = new RdfsSubClassOfReasoner();

		try (Stream<? extends Statement> stream = shaclSailConnection.getStatements(null, RDFS.SUBCLASSOF, null, false)
				.stream()) {
			stream.forEach(rdfsSubClassOfReasoner::addSubClassOfStatement);
		}

		rdfsSubClassOfReasoner.calculateSubClassOf(rdfsSubClassOfReasoner.subClassOfStatements);
		if (shaclSailConnection.sail.isPerformanceLogging()) {
			logger.info("RdfsSubClassOfReasoner.createReasoner() took {} ms", System.currentTimeMillis() - before);
		}
		return rdfsSubClassOfReasoner;
	}

	public boolean isEmpty() {
		return subClassOfStatements.isEmpty() && forwardChainCache.isEmpty() && backwardsChainCache.isEmpty();
	}
}

/*

 */
