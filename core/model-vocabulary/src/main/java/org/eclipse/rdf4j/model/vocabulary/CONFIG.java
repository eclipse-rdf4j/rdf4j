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
	 * @see https://tools.ietf.org/html/rfc4151
	 */
	public static final String NAMESPACE = "tag:rdf4j.org:2023:config/";

	/**
	 * The recommended prefix for the RDF4J config namespace: "config"
	 */
	public static final String PREFIX = "config";

	/**
	 * <var>tag:rdf4j.org:2023:config/Repository</var>
	 */
	public final static IRI REPOSITORY = Vocabularies.createIRI(NAMESPACE, "Repository");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryID</var>
	 */
	public final static IRI REPOSITORY_ID = Vocabularies.createIRI(NAMESPACE, "repositoryID");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryImpl</var>
	 */
	public final static IRI REPOSITORY_IMPL = Vocabularies.createIRI(NAMESPACE, "repositoryImpl");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryType</var>
	 */
	public final static IRI REPOSITORY_TYPE = Vocabularies.createIRI(NAMESPACE, "repositoryType");

	/**
	 * <var>tag:rdf4j.org:2023:config/delegate</var>
	 */
	public final static IRI DELEGATE = Vocabularies.createIRI(NAMESPACE, "delegate");

	/**
	 * <var>tag:rdf4j.org:2023:config/repositoryURL</var>
	 */
	public static final IRI REPOSITORY_URL = Vocabularies.createIRI(NAMESPACE, "repositoryURL");

	/**
	 * <var>tag:rdf4j.org:2023:config/username</var>
	 */
	public final static IRI USERNAME = Vocabularies.createIRI(NAMESPACE, "username");

	/**
	 * <var>tag:rdf4j.org:2023:config/password</var>
	 */
	public final static IRI PASSWORD = Vocabularies.createIRI(NAMESPACE, "password");

	/**
	 * <var>tag:rdf4j.org:2023:config/includeInferred</var>
	 */
	public final static IRI INCLUDE_INFERRED = Vocabularies.createIRI(NAMESPACE, "includeInferred");

	/**
	 * <var>tag:rdf4j.org:2023:config/maxQueryTime</var>
	 */
	public final static IRI MAX_QUERY_TIME = Vocabularies.createIRI(NAMESPACE, "maxQueryTime");

	/**
	 * <var>tag:rdf4j.org:2023:config/queryLanguage</var>
	 */
	public final static IRI QUERY_LANGUAGE = Vocabularies.createIRI(NAMESPACE, "queryLanguage");

	/**
	 * <var>tag:rdf4j.org:2023:config/base</var>
	 */
	public final static IRI BASE_URI = Vocabularies.createIRI(NAMESPACE, "base");

	/**
	 * <var>tag:rdf4j.org:2023:config/readContext</var>
	 */
	public final static IRI READ_CONTEXT = Vocabularies.createIRI(NAMESPACE, "readContext");

	/**
	 * <var>tag:rdf4j.org:2023:config/removeContext</var>
	 */
	public final static IRI REMOVE_CONTEXT = Vocabularies.createIRI(NAMESPACE, "removeContext");

	/**
	 * <var>tag:rdf4j.org:2023:config/insertContext</var>
	 */
	public final static IRI INSERT_CONTEXT = Vocabularies.createIRI(NAMESPACE, "insertContext");
}

