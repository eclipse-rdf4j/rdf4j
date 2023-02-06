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
	 * The RDF4J config namespace (<var>tag:rdf4j.org:2023:config/</var>).
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4151">the 'tag' URI Scheme (RFC 4151)</a>
	 */
	public static final String NAMESPACE = "tag:rdf4j.org:2023:config/";

	/**
	 * The recommended prefix for the RDF4J config namespace: "config"
	 */
	public static final String PREFIX = "config";

	/**
	 * <var>tag:rdf4j.org:2023:config/Repository</var>
	 */
	public final static IRI Repository = Vocabularies.createIRI(NAMESPACE, "Repository");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryID</var>
	 */
	public final static IRI repositoryID = Vocabularies.createIRI(NAMESPACE, "repositoryID");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryImpl</var>
	 */
	public final static IRI repositoryImpl = Vocabularies.createIRI(NAMESPACE, "repositoryImpl");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryType</var>
	 */
	public final static IRI repositoryType = Vocabularies.createIRI(NAMESPACE, "repositoryType");

	/**
	 * <var>tag:rdf4j.org:2023:config/delegate</var>
	 */
	public final static IRI delegate = Vocabularies.createIRI(NAMESPACE, "delegate");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryURL</var>
	 */
	public static final IRI repositoryURL = Vocabularies.createIRI(NAMESPACE, "repositoryURL");

	/**
	 * <var>tag:rdf4j.org:2023:config/username</var>
	 */
	public final static IRI username = Vocabularies.createIRI(NAMESPACE, "username");

	/**
	 * <var>tag:rdf4j.org:2023:config/password</var>
	 */
	public final static IRI password = Vocabularies.createIRI(NAMESPACE, "password");

	/**
	 * <var>tag:rdf4j.org:2023:config/includeInferred</var>
	 */
	public final static IRI includeInferred = Vocabularies.createIRI(NAMESPACE, "includeInferred");

	/**
	 * <var>tag:rdf4j.org:2023:config/maxQueryTime</var>
	 */
	public final static IRI maxQueryTime = Vocabularies.createIRI(NAMESPACE, "maxQueryTime");

	/**
	 * <var>tag:rdf4j.org:2023:config/queryLanguage</var>
	 */
	public final static IRI queryLanguage = Vocabularies.createIRI(NAMESPACE, "queryLanguage");

	/**
	 * <var>tag:rdf4j.org:2023:config/base</var>
	 */
	public final static IRI base = Vocabularies.createIRI(NAMESPACE, "base");

	/**
	 * <var>tag:rdf4j.org:2023:config/readContext</var>
	 */
	public final static IRI readContext = Vocabularies.createIRI(NAMESPACE, "readContext");

	/**
	 * <var>tag:rdf4j.org:2023:config/removeContext</var>
	 */
	public final static IRI removeContext = Vocabularies.createIRI(NAMESPACE, "removeContext");

	/**
	 * <var>tag:rdf4j.org:2023:config/insertContext</var>
	 */
	public final static IRI insertContext = Vocabularies.createIRI(NAMESPACE, "insertContext");

	/**
	 * <var>tag:rdf4j.org:2023:config/proxiedID</var>
	 */
	public final static IRI proxiedID = Vocabularies.createIRI(NAMESPACE, "proxiedID");

	/**
	 * <var>tag:rdf4j.org:2023:config/sailImpl</var>
	 */
	public final static IRI sailImpl = Vocabularies.createIRI(NAMESPACE, "sailImpl");

	/**
	 * <var>tag:rdf4j.org:2023:config/sailType</var>
	 */
	public final static IRI sailType = Vocabularies.createIRI(NAMESPACE, "sailType");

	/**
	 * <var>tag:rdf4j.org:2023:config/iterationCacheSyncTreshold</var>
	 */
	public final static IRI iterationCacheSyncThreshold = Vocabularies.createIRI(NAMESPACE,
			"iterationCacheSyncThreshold");

	/**
	 * <var>tag:rdf4j.org:2023:config/connectionTimeOut</var>
	 */
	public final static IRI connectionTimeOut = Vocabularies.createIRI(NAMESPACE, "connectionTimeOut");

	/** <var>tag:rdf4j.org:2023:config/evaluationStrategyFactory</var> */
	public final static IRI evaluationStrategyFactory = Vocabularies.createIRI(NAMESPACE,
			"evaluationStrategyFactory");

	/** <var>tag:rdf4j.org:2023:config/defaultQueryEvaluationMode</var> */
	public final static IRI defaultQueryEvaluationMode = Vocabularies.createIRI(NAMESPACE,
			"defaultQueryEvaluationMode");
}
