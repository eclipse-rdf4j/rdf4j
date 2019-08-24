/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for the <a href="https://www.w3.org/TR/skos-reference/#xl">SKOS eXtension for Labels
 * (SKOS-XL)</a>.
 * 
 * @see <a href="https://www.w3.org/TR/skos-reference/#xl">Appendix B of SKOS Simple Knowledge Organization System
 *      Reference</a>
 * @author Manuel Fiorelli
 */
public class SKOSXL {

	/**
	 * The SKOS-XL namespace: http://www.w3.org/2008/05/skos-xl#
	 */
	public static final String NAMESPACE = "http://www.w3.org/2008/05/skos-xl#";

	/**
	 * The recommended prefix for the SKOS-XL namespace: "skosxl"
	 */
	public static final String PREFIX = "skosxl";

	/**
	 * An immutable {@link Namespace} constant that represents the SKOS-XL namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/* classes */

	/**
	 * The skosxl:Label class.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-Label">The skosxl:Label Class</a>
	 */
	public static final IRI LABEL;

	/* literal form */

	/**
	 * The skosxl:literalForm property.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-Label">The skosxl:Label Class</a>
	 */
	public static final IRI LITERAL_FORM;

	/* lexical labels */

	/**
	 * The skosxl:prefLabel property.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-labels">Preferred, Alternate and Hidden skosxl:Labels</a>
	 */
	public static final IRI PREF_LABEL;

	/**
	 * The skosxl:altLabel property.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-labels">Preferred, Alternate and Hidden skosxl:Labels</a>
	 */
	public static final IRI ALT_LABEL;

	/**
	 * The skosxl:hiddenLabel property.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-labels">Preferred, Alternate and Hidden skosxl:Labels</a>
	 */
	public static final IRI HIDDEN_LABEL;

	/* label relations */

	/**
	 * The skosxl:labelRelation relation.
	 * 
	 * @see <a href="https://www.w3.org/TR/skos-reference/#xl-label-relations">Links Between skosxl:Labels</a>
	 */
	public static final IRI LABEL_RELATION;

	static {
		final ValueFactory f = SimpleValueFactory.getInstance();

		LABEL = f.createIRI(NAMESPACE, "Label");
		LITERAL_FORM = f.createIRI(NAMESPACE, "literalForm");
		PREF_LABEL = f.createIRI(NAMESPACE, "prefLabel");
		ALT_LABEL = f.createIRI(NAMESPACE, "altLabel");
		HIDDEN_LABEL = f.createIRI(NAMESPACE, "hiddenLabel");
		LABEL_RELATION = f.createIRI(NAMESPACE, "labelRelation");
	}
}
