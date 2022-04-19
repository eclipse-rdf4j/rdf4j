/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.rdf;

import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * Denotes an RDF Blank Node
 */
public interface RdfBlankNode extends RdfResource {
	/**
	 * a labeled blank node, of the form "_:<code>label</code>"
	 */
	class LabeledBlankNode implements RdfBlankNode {
		private final String label;

		LabeledBlankNode(String label) {
			this.label = label;
		}

		@Override
		public String getQueryString() {
			return "_:" + label;
		}
	}

	/**
	 * an anonymous blank node
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> Blank node syntax</a>
	 */
	class AnonymousBlankNode implements RdfBlankNode {
		@Override
		public String getQueryString() {
			return SparqlBuilderUtils.getBracketedString("");
		}
	}

	/**
	 * A blank node representing a resource that matches the contained set of predicate-object lists
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> Blank node syntax</a>
	 */
	class PropertiesBlankNode implements RdfBlankNode {
		private final RdfPredicateObjectListCollection predicateObjectLists = Rdf.predicateObjectListCollection();

		PropertiesBlankNode(RdfPredicate predicate, RdfObject... objects) {
			andHas(predicate, objects);
		}

		PropertiesBlankNode(IRI predicate, RdfObject... objects) {
			andHas(predicate, objects);
		}

		/**
		 * Using the predicate-object and object list mechanisms, expand this blank node's pattern to include triples
		 * consisting of this blank node as the subject, and the given predicate and object(s)
		 *
		 * @param predicate the predicate of the triple to add
		 * @param objects   the object or objects of the triple to add
		 * @return this blank node
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> Predicate-Object
		 *      Lists</a>
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists"> Object Lists</a>
		 */
		public PropertiesBlankNode andHas(RdfPredicate predicate, RdfObject... objects) {
			predicateObjectLists.andHas(predicate, objects);
			return this;
		}

		/**
		 * Using the predicate-object and object list mechanisms, expand this blank node's pattern to include triples
		 * consisting of this blank node as the subject, and the configured predicate path and object(s)
		 *
		 * @param propertyPathConfigurer
		 * @param objects
		 * @return
		 */
		public PropertiesBlankNode andHas(Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer,
				RdfObject... objects) {
			EmptyPropertyPathBuilder pathBuilder = new EmptyPropertyPathBuilder();
			propertyPathConfigurer.accept(pathBuilder);
			predicateObjectLists.andHas(pathBuilder.build(), objects);
			return this;
		}

		/**
		 * Using the predicate-object and object list mechanisms, expand this blank node's pattern to include triples
		 * consisting of this blank node as the subject, and the given predicate and object(s)
		 *
		 * @param predicate the predicate of the triple to add
		 * @param objects   the object or objects of the triple to add
		 *
		 * @return this blank node
		 *
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> Predicate-Object
		 *      Lists</a>
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists"> Object Lists</a>
		 */
		public PropertiesBlankNode andHas(IRI predicate, RdfObject... objects) {
			return andHas(Rdf.iri(predicate), objects);
		}

		/**
		 * Add predicate-object lists to this blank node's pattern
		 *
		 * @param lists the {@link RdfPredicateObjectList}(s) to add
		 * @return this blank node
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> Predicate-Object
		 *      Lists</a>
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists"> Object Lists</a>
		 */
		public PropertiesBlankNode andHas(RdfPredicateObjectList... lists) {
			predicateObjectLists.andHas(lists);
			return this;
		}

		/**
		 * convert this blank node to a triple pattern
		 *
		 * @return the triple pattern identified by this blank node
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> blank node syntax</a>
		 */
		public TriplePattern toTp() {
			return GraphPatterns.tp(this);
		}

		@Override
		public String getQueryString() {
			return SparqlBuilderUtils.getBracketedString(predicateObjectLists.getQueryString());
		}
	}
}
