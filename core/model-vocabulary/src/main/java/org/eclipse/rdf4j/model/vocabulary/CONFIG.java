/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.vocabulary.Vocabularies.createIRI;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Shared vocabulary for configuration of RDF4J components: Repositories, SAILs, and so on.
 *
 * @author Jeen Broekstra
 *
 * @since 4.3.0
 */
public class CONFIG {

	/**
	 * The RDF4J config namespace (<var>tag:rdf4j.org,2023:config/</var>).
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4151">the 'tag' URI Scheme (RFC 4151)</a>
	 */
	public static final String NAMESPACE = "tag:rdf4j.org,2023:config/";

	/**
	 * The recommended prefix for the RDF4J config namespace: "config"
	 */
	public static final String PREFIX = "config";

	/**
	 * The RDF4J config namespace (<var>tag:rdf4j.org,2023:config/</var>) as a {@link Namespace} object.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4151">the 'tag' URI Scheme (RFC 4151)</a>
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/**
	 * Setting for linking a delegate config to a wrapper in a SAIL or Repository config stack.
	 *
	 * <var>tag:rdf4j.org,2023:config/delegate</var>
	 */
	public final static IRI delegate = createIRI(NAMESPACE, "delegate");

	/**
	 * Repository config
	 */
	public static final class Rep {
		/**
		 * Type value for a RepositoryConfig.
		 *
		 * <var>tag:rdf4j.org,2023:config/Repository</var>
		 */
		public final static IRI Repository = createIRI(NAMESPACE, "Repository");

		/**
		 * Setting for the repository ID.
		 *
		 * <var>tag:rdf4j.org,2023:config/rep.id</var>
		 */
		public final static IRI id = createIRI(NAMESPACE, "rep.id");

		/**
		 * Setting for the repository implementation-specific configuration.
		 *
		 * <var>tag:rdf4j.org,2023:config/rep.impl</var>
		 */
		public final static IRI impl = createIRI(NAMESPACE, "rep.impl");

		/**
		 * Setting for the repository type.
		 *
		 * <var>tag:rdf4j.org,2023:config/rep.type</var>
		 */
		public final static IRI type = createIRI(NAMESPACE, "rep.type");
	}

	/**
	 * HTTP Repository config
	 */
	public static final class Http {
		/**
		 * Setting for a RDF4J HTTP Repository URL.
		 *
		 * <var>tag:rdf4j.org,2023:config/http.url</var>
		 */
		public static final IRI url = createIRI(NAMESPACE, "http.url");

		/**
		 * Setting for a username to use for authentication.
		 *
		 * <var>tag:rdf4j.org,2023:config/http.username</var>
		 */
		public final static IRI username = createIRI(NAMESPACE, "http.username");

		/**
		 * Setting for a password to use for authentication.
		 *
		 * <var>tag:rdf4j.org,2023:config/http.password</var>
		 */
		public final static IRI password = createIRI(NAMESPACE, "http.password");
	}

	/**
	 * ContextAwareRepository config
	 */
	public static final class ContextAware {
		/**
		 * Setting for including inferred statements by default.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.includeInferred</var>
		 */
		public final static IRI includeInferred = createIRI(NAMESPACE, "ca.includeInferred");

		/**
		 * Setting for the max query time.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.maxQueryTime</var>
		 */
		public final static IRI maxQueryTime = createIRI(NAMESPACE, "ca.maxQueryTime");

		/**
		 * Setting for the query language to be used.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.queryLanguage</var>
		 */
		public final static IRI queryLanguage = createIRI(NAMESPACE, "ca.queryLanguage");
		/**
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.readContext</var>
		 */
		public final static IRI readContext = createIRI(NAMESPACE, "ca.readContext");

		/**
		 * <var>tag:rdf4j.org,2023:config/ca.removeContext</var>
		 */
		public final static IRI removeContext = createIRI(NAMESPACE, "ca.removeContext");

		/**
		 * <var>tag:rdf4j.org,2023:config/ca.insertContext</var>
		 */
		public final static IRI insertContext = createIRI(NAMESPACE, "ca.insertContext");

		/**
		 * Setting for a base URI.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.base</var>
		 */
		public final static IRI base = createIRI(NAMESPACE, "ca.base");

	}

	/**
	 * ProxyRepository config
	 *
	 */
	public static final class Proxy {
		/**
		 * <var>tag:rdf4j.org,2023:config/proxy.proxiedID</var>
		 */
		public final static IRI proxiedID = createIRI(NAMESPACE, "proxy.proxiedID");

	}

	/**
	 * SPARQLRepository config
	 */
	public static final class Sparql {
		/**
		 * Configuration setting for the SPARQL query endpoint.
		 *
		 * <var>tag:rdf4j.org,2023:config/sparql.queryEndpoint</var>
		 */
		public static final IRI queryEndpoint = createIRI(NAMESPACE, "sparql.queryEndpoint");

		/**
		 * Configuration setting for the SPARQL update endpoint.
		 *
		 * <var>tag:rdf4j.org,2023:config/sparql.updateEndpoint</var>
		 */
		public static final IRI updateEndpoint = createIRI(NAMESPACE, "sparql.updateEndpoint");

		/**
		 * Configuration setting for enabling/disabling direct result pass-through.
		 *
		 * <var>tag:rdf4j.org,2023:config/sparql.passThroughEnabled</var>
		 *
		 * @see SPARQLProtocolSession#isPassThroughEnabled()
		 */
		public static final IRI passThroughEnabled = createIRI(NAMESPACE, "sparql.passThroughEnabled");
	}

	/**
	 * Sail config
	 */
	public static final class Sail {
		/**
		 * <var>tag:rdf4j.org,2023:config/sail.type</var>
		 */
		public final static IRI type = createIRI(NAMESPACE, "sail.type");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.impl</var>
		 */
		public final static IRI impl = createIRI(NAMESPACE, "sail.impl");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.iterationCacheSyncTreshold</var>
		 */
		public final static IRI iterationCacheSyncThreshold = createIRI(NAMESPACE,
				"sail.iterationCacheSyncThreshold");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.connectionTimeOut</var>
		 */
		public final static IRI connectionTimeOut = createIRI(NAMESPACE, "sail.connectionTimeOut");

		/** <var>tag:rdf4j.org,2023:config/sail.evaluationStrategyFactory</var> */
		public final static IRI evaluationStrategyFactory = createIRI(NAMESPACE,
				"sail.evaluationStrategyFactory");

		/** <var>tag:rdf4j.org,2023:config/sail.defaultQueryEvaluationMode</var> */
		public final static IRI defaultQueryEvaluationMode = createIRI(NAMESPACE,
				"sail.defaultQueryEvaluationMode");
	}

	/**
	 * Memory Store config
	 */
	public static final class Mem {
		/** <var>tag:rdf4j.org,2023:config/mem.persist</var> */
		public final static IRI persist = createIRI(NAMESPACE, "mem.persist");

		/** <var>tag:rdf4j.org,2023:config/mem.syncDelay</var> */
		public final static IRI syncDelay = createIRI(NAMESPACE, "mem.syncDelay");
	}

	/**
	 * Native Store config
	 */
	public static final class Native {
		/**
		 * <var>tag:rdf4j.org,2023:config/native.tripleIndexes</var>
		 */
		public final static IRI tripleIndexes = createIRI(NAMESPACE, "native.tripleIndexes");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.forceSync</var>
		 */
		public final static IRI forceSync = createIRI(NAMESPACE, "native.forceSync");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.valueCacheSize</var>
		 */
		public final static IRI valueCacheSize = createIRI(NAMESPACE, "native.valueCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.valueIDCacheSize</var>
		 */
		public final static IRI valueIDCacheSize = createIRI(NAMESPACE, "native.valueIDCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.namespaceCacheSize</var>
		 */
		public final static IRI namespaceCacheSize = createIRI(NAMESPACE, "native.namespaceCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.namespaceIDCacheSize</var>
		 */
		public final static IRI namespaceIDCacheSize = createIRI(NAMESPACE, "native.namespaceIDCacheSize");
	}

	/**
	 * SHACL Sail config
	 */
	public static final class Shacl {
		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.parallelValidation</code>
		 */
		public final static IRI parallelValidation = createIRI(NAMESPACE, "shacl.parallelValidation");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.logValidationPlans</code>
		 */
		public final static IRI logValidationPlans = createIRI(NAMESPACE, "shacl.logValidationPlans");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.logValidationViolations</code>
		 */
		public final static IRI logValidationViolations = createIRI(NAMESPACE,
				"shacl.logValidationViolations");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.validationEnabled</code>
		 */
		public final static IRI validationEnabled = createIRI(NAMESPACE, "shacl.validationEnabled");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.cacheSelectNodes</code>
		 */
		public final static IRI cacheSelectNodes = createIRI(NAMESPACE, "shacl.cacheSelectNodes");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.globalLogValidationExecution</code>
		 */
		public final static IRI globalLogValidationExecution = createIRI(NAMESPACE,
				"shacl.globalLogValidationExecution");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.rdfsSubClassReasoning</code>
		 */
		public final static IRI rdfsSubClassReasoning = createIRI(NAMESPACE,
				"shacl.rdfsSubClassReasoning");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.performanceLogging</code>
		 */
		public final static IRI performanceLogging = createIRI(NAMESPACE, "shacl.performanceLogging");

		/**
		 * <code>tag:rdf4j.org,2023:config/shacl.serializableValidation</code>
		 */
		public final static IRI serializableValidation = createIRI(NAMESPACE,
				"shacl.serializableValidation");

		public final static IRI eclipseRdf4jShaclExtensions = createIRI(NAMESPACE,
				"shacl.eclipseRdf4jShaclExtensions");

		public final static IRI dashDataShapes = createIRI(NAMESPACE, "shacl.dashDataShapes");

		public final static IRI validationResultsLimitTotal = createIRI(NAMESPACE,
				"shacl.validationResultsLimitTotal");
		public final static IRI validationResultsLimitPerConstraint = createIRI(NAMESPACE,
				"shacl.validationResultsLimitPerConstraint");
		public final static IRI transactionalValidationLimit = createIRI(NAMESPACE,
				"shacl.transactionalValidationLimit");

		public final static IRI shapesGraph = createIRI(NAMESPACE, "shacl.shapesGraph");
	}

	/**
	 * Lucene Sail config
	 *
	 */
	public static final class Lucene {
		public final static IRI indexDir = createIRI(NAMESPACE, "lucene.indexDir");
	}

	/**
	 * Elasticsearch Store config
	 */
	public static final class Ess {

		public final static IRI hostname = createIRI(NAMESPACE, "ess.hostname");
		public final static IRI port = createIRI(NAMESPACE, "ess.port");
		public final static IRI index = createIRI(NAMESPACE, "ess.index");
		public final static IRI clusterName = createIRI(NAMESPACE, "ess.clusterName");
	}

	/**
	 * Custom Graph Query Inferencer config
	 */
	public static final class Cgqi {
		public final static IRI queryLanguage = createIRI(NAMESPACE, "cgqi.queryLanguage");

		public final static IRI ruleQuery = createIRI(NAMESPACE, "cgqi.ruleQuery");

		public final static IRI matcherQuery = createIRI(NAMESPACE, "cgqi.matcherQuery");
	}

}
