/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;

/**
 * Configuration schema URI's for {@link CustomGraphQueryInferencer}.
 *
 * @author Dale Visser
 */
public class CustomGraphQueryInferencerSchema {

	/**
	 * The CustomGraphQueryInferencer schema namespace (
	 * <var>http://www.openrdf.org/config/sail/customGraphQueryInferencer#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/customGraphQueryInferencer#";

	/** <var>http://www.openrdf.org/config/sail/customGraphQueryInferencer#queryLanguage</var> */
	public final static IRI QUERY_LANGUAGE;

	/** <var>http://www.openrdf.org/config/sail/customGraphQueryInferencer#ruleQuery</var> */
	public final static IRI RULE_QUERY;

	/** <var>http://www.openrdf.org/config/sail/customGraphQueryInferencer#matcherQuery</var> */
	public final static IRI MATCHER_QUERY;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		QUERY_LANGUAGE = factory.createIRI(NAMESPACE, "queryLanguage");
		RULE_QUERY = factory.createIRI(NAMESPACE, "ruleQuery");
		MATCHER_QUERY = factory.createIRI(NAMESPACE, "matcherQuery");
	}
}
