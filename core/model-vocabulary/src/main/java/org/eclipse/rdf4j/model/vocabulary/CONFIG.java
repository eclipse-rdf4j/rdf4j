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

import org.eclipse.rdf4j.model.IRI;

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
	 * Setting for linking a delegate config to a wrapper in a SAIL or Repository config stack.
	 *
	 * <var>tag:rdf4j.org,2023:config/delegate</var>
	 */
	public final static IRI delegate = Vocabularies.createIRI(NAMESPACE, "delegate");

	/**
	 * Repository config
	 */
	public static final class Rep {
		/**
		 * Type value for a RepositoryConfig.
		 *
		 * <var>tag:rdf4j.org,2023:config/Repository</var>
		 */
		public final static IRI Repository = Vocabularies.createIRI(NAMESPACE, "Repository");

		/**
		 * Setting for the repository ID.
		 *
		 * <var>tag:rdf4j.org,2023:config/repositoryID</var>
		 */
		public final static IRI repositoryID = Vocabularies.createIRI(NAMESPACE, "repositoryID");

		/**
		 * Setting for the repository implementation-specific configuration.
		 *
		 * <var>tag:rdf4j.org,2023:config/repositoryImpl</var>
		 */
		public final static IRI repositoryImpl = Vocabularies.createIRI(NAMESPACE, "repositoryImpl");

		/**
		 * Setting for the repository type.
		 *
		 * <var>tag:rdf4j.org,2023:config/repositoryType</var>
		 */
		public final static IRI repositoryType = Vocabularies.createIRI(NAMESPACE, "repositoryType");
	}

	/**
	 * HTTP Repository config
	 */
	public static final class Http {
		/**
		 * Setting for a (remote) RDF4J Repository URL.
		 *
		 * <var>tag:rdf4j.org,2023:config/repositoryURL</var>
		 */
		public static final IRI repositoryURL = Vocabularies.createIRI(NAMESPACE, "repositoryURL");

		/**
		 * Setting for a username to use for authentication.
		 *
		 * <var>tag:rdf4j.org,2023:config/username</var>
		 */
		public final static IRI username = Vocabularies.createIRI(NAMESPACE, "username");

		/**
		 * Setting for a password to use for authentication.
		 *
		 * <var>tag:rdf4j.org,2023:config/password</var>
		 */
		public final static IRI password = Vocabularies.createIRI(NAMESPACE, "password");
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
		public final static IRI includeInferred = Vocabularies.createIRI(NAMESPACE, "ca.includeInferred");

		/**
		 * Setting for the max query time.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.maxQueryTime</var>
		 */
		public final static IRI maxQueryTime = Vocabularies.createIRI(NAMESPACE, "ca.maxQueryTime");

		/**
		 * Setting for the query language to be used.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.queryLanguage</var>
		 */
		public final static IRI queryLanguage = Vocabularies.createIRI(NAMESPACE, "ca.queryLanguage");
		/**
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.readContext</var>
		 */
		public final static IRI readContext = Vocabularies.createIRI(NAMESPACE, "ca.readContext");

		/**
		 * <var>tag:rdf4j.org,2023:config/ca.removeContext</var>
		 */
		public final static IRI removeContext = Vocabularies.createIRI(NAMESPACE, "ca.removeContext");

		/**
		 * <var>tag:rdf4j.org,2023:config/ca.insertContext</var>
		 */
		public final static IRI insertContext = Vocabularies.createIRI(NAMESPACE, "ca.insertContext");

		/**
		 * Setting for a base URI.
		 *
		 * <var>tag:rdf4j.org,2023:config/ca.base</var>
		 */
		public final static IRI base = Vocabularies.createIRI(NAMESPACE, "ca.base");

	}

	/**
	 * ProxyRepository config
	 *
	 */
	public static final class Proxy {
		/**
		 * <var>tag:rdf4j.org,2023:config/proxy.proxiedID</var>
		 */
		public final static IRI proxiedID = Vocabularies.createIRI(NAMESPACE, "proxy.proxiedID");

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
		public static final IRI queryEndpoint = Vocabularies.createIRI(NAMESPACE, "sparq.queryEndpoint");

		/**
		 * Configuration setting for the SPARQL update endpoint.
		 *
		 * <var>tag:rdf4j.org,2023:config/sparql.updateEndpoint</var>
		 */
		public static final IRI updateEndpoint = Vocabularies
				.createIRI(NAMESPACE, "sparql.updateEndpoint");

		/**
		 * Configuration setting for enabling/disabling direct result pass-through.
		 *
		 * <var>tag:rdf4j.org,2023:config/sparql.passThroughEnabled</var>
		 *
		 * @see SPARQLProtocolSession#isPassThroughEnabled()
		 */
		public static final IRI passThroughEnabled = Vocabularies.createIRI(NAMESPACE, "sparql.passThroughEnabled");
	}

	/**
	 * Sail config
	 */
	public static final class Sail {
		/**
		 * <var>tag:rdf4j.org,2023:config/sail.type</var>
		 */
		public final static IRI type = Vocabularies.createIRI(NAMESPACE, "sail.type");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.impl</var>
		 */
		public final static IRI impl = Vocabularies.createIRI(NAMESPACE, "sail.impl");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.iterationCacheSyncTreshold</var>
		 */
		public final static IRI iterationCacheSyncThreshold = Vocabularies.createIRI(NAMESPACE,
				"sail.iterationCacheSyncThreshold");

		/**
		 * <var>tag:rdf4j.org,2023:config/sail.connectionTimeOut</var>
		 */
		public final static IRI connectionTimeOut = Vocabularies.createIRI(NAMESPACE, "sail.connectionTimeOut");

		/** <var>tag:rdf4j.org,2023:config/sail.evaluationStrategyFactory</var> */
		public final static IRI evaluationStrategyFactory = Vocabularies.createIRI(NAMESPACE,
				"sail.evaluationStrategyFactory");

		/** <var>tag:rdf4j.org,2023:config/sail.defaultQueryEvaluationMode</var> */
		public final static IRI defaultQueryEvaluationMode = Vocabularies.createIRI(NAMESPACE,
				"sail.defaultQueryEvaluationMode");
	}

	/**
	 * Memory Store config
	 */
	public static final class Mem {
		/** <var>tag:rdf4j.org,2023:config/mem.persist</var> */
		public final static IRI persist = Vocabularies.createIRI(NAMESPACE, "mem.persist");

		/** <var>tag:rdf4j.org,2023:config/mem.syncDelay</var> */
		public final static IRI syncDelay = Vocabularies.createIRI(NAMESPACE, "mem.syncDelay");
	}

	/**
	 * Native Store config
	 */
	public static final class Native {
		/**
		 * <var>tag:rdf4j.org,2023:config/native.tripleIndexes</var>
		 */
		public final static IRI tripleIndexes = Vocabularies.createIRI(NAMESPACE, "native.tripleIndexes");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.forceSync</var>
		 */
		public final static IRI forceSync = Vocabularies.createIRI(NAMESPACE, "native.forceSync");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.valueCacheSize</var>
		 */
		public final static IRI valueCacheSize = Vocabularies.createIRI(NAMESPACE, "native.valueCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.valueIDCacheSize</var>
		 */
		public final static IRI valueIDCacheSize = Vocabularies.createIRI(NAMESPACE, "native.valueIDCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.namespaceCacheSize</var>
		 */
		public final static IRI namespaceCacheSize = Vocabularies.createIRI(NAMESPACE, "native.namespaceCacheSize");

		/**
		 * <var>tag:rdf4j.org,2023:config/native.namespaceIDCacheSize</var>
		 */
		public final static IRI namespaceIDCacheSize = Vocabularies.createIRI(NAMESPACE, "native.namespaceIDCacheSize");
	}
}
