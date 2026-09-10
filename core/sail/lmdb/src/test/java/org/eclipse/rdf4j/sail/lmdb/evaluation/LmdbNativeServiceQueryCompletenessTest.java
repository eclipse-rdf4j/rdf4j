/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. Eclipse Distribution License v1.0.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedService;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;
import org.eclipse.rdf4j.repository.sparql.federation.RepositoryFederatedService;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Full-module SPARQL/parser/federation integration contracts. No public network is used: real SERVICE algebra calls
 * RepositoryFederatedService over a second LMDB repository with an independent dictionary. These tests MUST run with
 * actual module dependencies and Janino; the isolated IR harness does not execute this class.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class LmdbNativeServiceQueryCompletenessTest {
	private static final String NATIVE = "rdf4j.lmdb.nativeQueryEngine.enabled";
	private static final String CORPUS = "/org/eclipse/rdf4j/sail/lmdb/evaluation/service-query-corpus/";
	@TempDir
	File directory;
	private final Map<String, String> previous = new HashMap<>();
	private final List<FederatedService> services = new ArrayList<>();
	private SailRepository local, remote;

	private void set(String key, String value) {
		if (!previous.containsKey(key))
			previous.put(key, System.getProperty(key));
		System.setProperty(key, value);
	}

	@BeforeEach
	void open() throws Exception {
		set(NATIVE, "true");
		set("rdf4j.lmdb.janinoCodegen.enabled", "true");
		set("rdf4j.lmdb.janinoCodegen.synchronous", "true");
		set("rdf4j.lmdb.janinoCodegen.thresholdRows", "0");
		set("rdf4j.lmdb.kernelInterpreter.enabled", "true");
		set("rdf4j.lmdb.janinoCodegen.planBridge", "false");
		set("rdf4j.lmdb.janinoCodegen.scanSources", "true");
		set("rdf4j.lmdb.parallel.enabled", "false");
		set("rdf4j.lmdb.irAggregateParallel.enabled", "false");
		set("rdf4j.lmdb.irKernelParallel.enabled", "false");
		set("rdf4j.lmdb.costModel.persist.enabled", "false");
		Map<String, FederatedService> endpoints = new HashMap<>();
		FederatedServiceResolver resolver = url -> {
			FederatedService service = endpoints.get(url);
			if (service == null)
				throw new QueryEvaluationException("Unknown fixture endpoint: " + url);
			return service;
		};
		LmdbStore remoteStore = new LmdbStore(new File(directory, "remote"),
				new LmdbStoreConfig("spoc,posc,ospc").setDirectAdjacencyBuildOnStart(false));
		remoteStore.setEvaluationStrategyFactory(new StrictEvaluationStrategyFactory(resolver));
		remote = new SailRepository(remoteStore);
		load(remote, """
				@prefix : <urn:query-boundary:> .
				@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
				:k1 :q 10, 20; :tag "first"; :next :k2; :numeric "01"^^xsd:integer, "1"^^xsd:integer .
				:k2 :q 20; :tag "second"; :next :k3 .
				:g1 { :kg :q 30; :tag "same graph" . }
				:g2 { :kg :q 40 . }
				""");
		FederatedService remoteService = new RepositoryFederatedService(remote, false);
		remoteService.initialize();
		services.add(remoteService);
		endpoints.put("urn:query-boundary:remote", remoteService);
		endpoints.put("urn:query-boundary:remoteAlias", remoteService);
		Repository broken = (Repository) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { Repository.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getConnection" -> throw new RepositoryException("deliberately failed endpoint");
				case "isInitialized" -> true;
				case "getValueFactory" -> SimpleValueFactory.getInstance();
				case "toString" -> "deliberately failed endpoint";
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				case "init", "shutDown" -> null;
				default -> throw new AssertionError("unexpected repository operation " + method);
				});
		FederatedService brokenService = new RepositoryFederatedService(broken, false);
		brokenService.initialize();
		services.add(brokenService);
		endpoints.put("urn:query-boundary:broken", brokenService);
		LmdbStore localStore = new LmdbStore(new File(directory, "local"),
				new LmdbStoreConfig("spoc,posc,ospc").setDirectAdjacencyBuildOnStart(false));
		localStore.setEvaluationStrategyFactory(new LmdbNativeEvaluationStrategyFactory(resolver));
		local = new SailRepository(localStore);
		load(local, """
				@prefix : <urn:query-boundary:> .
				:a :p :k1; :edge :b . :b :p :k2; :edge :c . :c :p :k3 .
				""");
	}

	private static void load(SailRepository repository, String data) throws Exception {
		try (var connection = repository.getConnection()) {
			connection.begin();
			connection.add(new StringReader(data), "", RDFFormat.TRIG);
			connection.commit();
		}
	}

	@AfterEach
	void close() {
		try {
			for (FederatedService service : services)
				service.shutdown();
		} finally {
			try {
				if (local != null)
					local.shutDown();
			} finally {
				try {
					if (remote != null)
						remote.shutDown();
				} finally {
					previous.forEach((key, value) -> {
						if (value == null)
							System.clearProperty(key);
						else
							System.setProperty(key, value);
					});
				}
			}
		}
	}

	private static String resource(String name) throws IOException {
		try (var stream = LmdbNativeServiceQueryCompletenessTest.class.getResourceAsStream(CORPUS + name)) {
			if (stream == null)
				throw new IOException("missing query corpus resource " + name);
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	static Stream<Arguments> queries() throws IOException {
		return resource("manifest.tsv").lines()
				.filter(line -> !line.isBlank() && !line.startsWith("#"))
				.flatMap(line -> {
					String[] fields = line.split("\t");
					return Stream.of(false, true)
							.map(compiled -> Arguments.of(fields[0],
									Boolean.parseBoolean(fields[1]), fields[2], compiled));
				});
	}

	@ParameterizedTest(name = "{0} [aggregate={1}, compiled={3}]")
	@MethodSource("queries")
	void queryRunsWithRealKernelAndFederation(String name, boolean aggregate, String count, boolean compiled)
			throws Exception {
		String text = resource(name + ".rq");
		List<BindingSet> oracle = evaluate(text, false, null);
		if (!count.equals("-")) {
			assertEquals(1, oracle.size(), name);
			assertEquals(Long.parseLong(count), ((Literal) oracle.get(0).getValue("n")).longValue(), name);
		}
		long before = binds(compiled, aggregate);
		String route = aggregate ? "irAggregate" : "irKernel";
		if (!aggregate && name.equals("rows-distinct"))
			route = "irKernelDistinct";
		if (!compiled)
			route += "Interpreted";
		assertEquals(bag(oracle), bag(evaluate(text, true, route)), name);
		// LIMIT 0 is deliberately allowed to avoid binding/opening the producer at all.
		if (!name.equals("rows-limit-zero")) {
			assertTrue(binds(compiled, aggregate) > before,
					name + " must actually bind the requested kernel tier, not silently pass through generic fallback");
		}
	}

	@Test
	void nonSilentServiceFailureRemainsFatal() {
		String query = "PREFIX : <urn:query-boundary:> SELECT (COUNT(*) AS ?n) WHERE { "
				+ "?s :p ?k . SERVICE :broken { ?k :q ?v } }";
		for (String route : Arrays.asList(null, "irAggregateInterpreted", "irAggregate")) {
			assertThrows(QueryEvaluationException.class, () -> evaluate(query, route != null, route));
		}
	}

	private long binds(boolean compiled, boolean aggregate) {
		return aggregate ? (compiled ? LmdbNativeKernelExecution.AGG_COMPILED_BINDS.get()
				: LmdbNativeKernelExecution.AGG_INTERPRETED_BINDS.get())
				: (compiled ? LmdbNativeKernelExecution.COMPILED_BINDS.get()
						: LmdbNativeKernelExecution.INTERPRETED_BINDS.get());
	}

	private List<BindingSet> evaluate(String text, boolean nativeEnabled, String route) {
		set(NATIVE, Boolean.toString(nativeEnabled));
		try (var connection = local.getConnection()) {
			SailTupleQuery query = (SailTupleQuery) connection.prepareTupleQuery(text);
			if (route != null)
				query.setForcedLmdbExecutionStrategy(route);
			query.setMaxExecutionTime(15);
			return QueryResults.asList(query.evaluate());
		}
	}

	private static List<String> bag(List<BindingSet> results) {
		List<String> bag = new ArrayList<>();
		for (BindingSet row : results) {
			List<String> values = new ArrayList<>();
			for (String name : row.getBindingNames())
				values.add(name + "=" + row.getValue(name));
			values.sort(String::compareTo);
			bag.add(String.join(";", values));
		}
		bag.sort(String::compareTo); // sort for comparison only; preserve duplicate rows
		return bag;
	}
}
