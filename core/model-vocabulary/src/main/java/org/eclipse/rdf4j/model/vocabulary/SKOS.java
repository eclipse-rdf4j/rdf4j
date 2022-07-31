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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary constants for the <a href="http://www.w3.org/2004/02/skos/">Simple Knowledge Organization System
 * (SKOS)</a>.
 *
 * @see <a href="http://www.w3.org/TR/skos-reference/">SKOS Simple Knowledge Organization System Reference</a>
 * @author Jeen Broekstra
 */
public class SKOS {

	/**
	 * The SKOS namespace: http://www.w3.org/2004/02/skos/core#
	 */
	public static final String NAMESPACE = "http://www.w3.org/2004/02/skos/core#";

	/**
	 * The recommended prefix for the SKOS namespace: "skos"
	 */
	public static final String PREFIX = "skos";

	/**
	 * An immutable {@link Namespace} constant that represents the SKOS namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/* classes */

	/**
	 * The skos:Concept class
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#concepts">The skos:Concept Class</a>
	 */
	public static final IRI CONCEPT;

	/**
	 * The skos:ConceptScheme class
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#schemes">Concept Schemes</a>
	 */
	public static final IRI CONCEPT_SCHEME;

	/**
	 * The skos:Collection class
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#collections">Concept Collections</a>
	 */
	public static final IRI COLLECTION;

	/**
	 * The skos:OrderedCollection class
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#collections">Concept Collections</a>
	 */
	public static final IRI ORDERED_COLLECTION;

	/* lexical labels */

	/**
	 * The skos:prefLabel lexical label property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#labels">Lexical Labels</a>
	 */
	public static final IRI PREF_LABEL;

	/**
	 * The skos:altLabel lexical label property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#labels">Lexical Labels</a>
	 */
	public static final IRI ALT_LABEL;

	/**
	 * The skos:hiddenLabel lexical label property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#labels">Lexical Labels</a>
	 */
	public static final IRI HIDDEN_LABEL;

	/* Concept Scheme properties */

	/**
	 * The skos:inScheme relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#schemes">Concept Schemes</a>
	 */
	public static final IRI IN_SCHEME;

	/**
	 * The skos:hasTopConcept relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#schemes">Concept Schemes</a>
	 */
	public static final IRI HAS_TOP_CONCEPT;

	/**
	 * The skos:topConceptOf relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#schemes">Concept Schemes</a>
	 */
	public static final IRI TOP_CONCEPT_OF;

	/* collection properties */

	/**
	 * The skos:member relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#collections">Concept Collections</a>
	 */
	public static final IRI MEMBER;

	/**
	 * The skos:memberList relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#collections">Concept Collections</a>
	 */
	public static final IRI MEMBER_LIST;

	/* notation properties */

	/**
	 * The skos:notation property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notations">Notations</a>
	 */
	public static final IRI NOTATION;

	/* documentation properties */

	/**
	 * The skos:changeNote property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI CHANGE_NOTE;

	/**
	 * The skos:definition property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI DEFINITION;

	/**
	 * The skos:editorialNote property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI EDITORIAL_NOTE;

	/**
	 * The skos:example property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */

	public static final IRI EXAMPLE;

	/**
	 * The skos:historyNote property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI HISTORY_NOTE;

	/**
	 * The skos:note property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI NOTE;

	/**
	 * The skos:scopeNote property.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#notes">Documentation Properties (Note Properties)</a>
	 */
	public static final IRI SCOPE_NOTE;

	/* semantic relations */

	/**
	 * The skos:broader relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI BROADER;

	/**
	 * The skos:broaderTransitive relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI BROADER_TRANSITIVE;

	/**
	 * The skos:narrower relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI NARROWER;

	/**
	 * The skos:narrowerTransitive relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI NARROWER_TRANSITIVE;

	/**
	 * The skos:related relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI RELATED;

	/**
	 * The skos:semanticRelation relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#semantic-relations">SKOS Simple Knowledge Organization System
	 *      Reference - Semantic Relations Section</a>
	 */
	public static final IRI SEMANTIC_RELATION;

	/* mapping properties */

	/**
	 * The skos:broadMatch relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI BROAD_MATCH;

	/**
	 * The skos:closeMatch relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI CLOSE_MATCH;

	/**
	 * The skos:exactMatch relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI EXACT_MATCH;

	/**
	 * The skos:mappingRelation relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI MAPPING_RELATION;

	/**
	 * The skos:narrowMatch relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI NARROW_MATCH;

	/**
	 * The skos:relatedMatch relation.
	 *
	 * @see <a href="http://www.w3.org/TR/skos-reference/#mapping">SKOS Simple Knowledge Organization System Reference -
	 *      Mapping Properties Section</a>
	 */
	public static final IRI RELATED_MATCH;

	static {

		CONCEPT = Vocabularies.createIRI(NAMESPACE, "Concept");
		CONCEPT_SCHEME = Vocabularies.createIRI(NAMESPACE, "ConceptScheme");
		COLLECTION = Vocabularies.createIRI(NAMESPACE, "Collection");
		ORDERED_COLLECTION = Vocabularies.createIRI(NAMESPACE, "OrderedCollection");

		PREF_LABEL = Vocabularies.createIRI(NAMESPACE, "prefLabel");
		ALT_LABEL = Vocabularies.createIRI(NAMESPACE, "altLabel");

		BROADER = Vocabularies.createIRI(NAMESPACE, "broader");
		NARROWER = Vocabularies.createIRI(NAMESPACE, "narrower");

		HAS_TOP_CONCEPT = Vocabularies.createIRI(NAMESPACE, "hasTopConcept");
		MEMBER = Vocabularies.createIRI(NAMESPACE, "member");

		HIDDEN_LABEL = Vocabularies.createIRI(NAMESPACE, "hiddenLabel");

		IN_SCHEME = Vocabularies.createIRI(NAMESPACE, "inScheme");

		TOP_CONCEPT_OF = Vocabularies.createIRI(NAMESPACE, "topConceptOf");

		MEMBER_LIST = Vocabularies.createIRI(NAMESPACE, "memberList");
		NOTATION = Vocabularies.createIRI(NAMESPACE, "notation");
		CHANGE_NOTE = Vocabularies.createIRI(NAMESPACE, "changeNote");
		DEFINITION = Vocabularies.createIRI(NAMESPACE, "definition");
		EDITORIAL_NOTE = Vocabularies.createIRI(NAMESPACE, "editorialNote");
		EXAMPLE = Vocabularies.createIRI(NAMESPACE, "example");
		HISTORY_NOTE = Vocabularies.createIRI(NAMESPACE, "historyNote");
		NOTE = Vocabularies.createIRI(NAMESPACE, "note");
		SCOPE_NOTE = Vocabularies.createIRI(NAMESPACE, "scopeNote");
		BROADER_TRANSITIVE = Vocabularies.createIRI(NAMESPACE, "broaderTransitive");
		NARROWER_TRANSITIVE = Vocabularies.createIRI(NAMESPACE, "narrowerTransitive");
		RELATED = Vocabularies.createIRI(NAMESPACE, "related");
		SEMANTIC_RELATION = Vocabularies.createIRI(NAMESPACE, "semanticRelation");
		BROAD_MATCH = Vocabularies.createIRI(NAMESPACE, "broadMatch");
		CLOSE_MATCH = Vocabularies.createIRI(NAMESPACE, "closeMatch");
		EXACT_MATCH = Vocabularies.createIRI(NAMESPACE, "exactMatch");
		MAPPING_RELATION = Vocabularies.createIRI(NAMESPACE, "mappingRelation");
		NARROW_MATCH = Vocabularies.createIRI(NAMESPACE, "narrowMatch");
		RELATED_MATCH = Vocabularies.createIRI(NAMESPACE, "relatedMatch");

	}
}
