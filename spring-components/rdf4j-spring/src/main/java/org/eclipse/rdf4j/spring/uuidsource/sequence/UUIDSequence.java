/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.uuidsource.sequence;

import static org.eclipse.rdf4j.spring.util.QueryResultUtils.getIRI;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;
import org.eclipse.rdf4j.spring.support.UUIDSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class UUIDSequence implements UUIDSource {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private @Autowired RDF4JTemplate rdf4JTemplate;
	private final int prefetchCount;
	private final Map<RepositoryConnection, Queue<IRI>> prefetchedUUIDs = Collections
			.synchronizedMap(new WeakHashMap<>());

	public UUIDSequence(UUIDSequenceProperties properties) {
		this.prefetchCount = properties.getPrefetchCount();
		logger.debug("UUIDSequence uses prefetchCount of {}", prefetchCount);
	}

	@Override
	public IRI nextUUID() {
		if (logger.isDebugEnabled()) {
			logger.debug("Obtaining UUID from UUIDSequence...");
		}
		return rdf4JTemplate.applyToConnection(
				con -> {
					Queue<IRI> uuids = prefetchedUUIDs.computeIfAbsent(con, this::prefetchUUIDs);
					IRI uuid = uuids.poll();
					if (uuid == null) {
						uuids = prefetchUUIDs(con);
						prefetchedUUIDs.put(con, uuids);
						uuid = uuids.poll();
					}
					if (uuid == null) {
						throw new IllegalStateException("Unable to produce next UUID in sequence");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Returning next UUID");
					}
					return uuid;
				});
	}

	private Queue<IRI> prefetchUUIDs(RepositoryConnection con) {
		double nd = Math.pow(prefetchCount, 1d / 3d);
		int n = (int) Math.ceil(nd);
		int exactPrefetchCount = (int) Math.ceil(Math.pow(n, 3d));
		if (logger.isDebugEnabled()) {
			logger.debug("prefetching {} uuids from the repostory", exactPrefetchCount);
		}
		String ints = IntStream.range(0, n).mapToObj(Integer::toString).collect(Collectors.joining(" "));
		TupleQuery query = con.prepareTupleQuery(
				"SELECT (UUID() as ?id) WHERE {"
						+ "VALUES ?index1 { "
						+ ints
						+ " } "
						+ "VALUES ?index2 { "
						+ ints
						+ " } "
						+ "VALUES ?index3 { "
						+ ints
						+ " } "
						+ "}");
		ArrayDeque<IRI> uuids = new ArrayDeque<>(exactPrefetchCount);
		try (TupleQueryResult result = query.evaluate()) {
			while (result.hasNext()) {
				BindingSet b = result.next();
				uuids.add(getIRI(b, "id"));
			}
		}
		return uuids;
	}
}
