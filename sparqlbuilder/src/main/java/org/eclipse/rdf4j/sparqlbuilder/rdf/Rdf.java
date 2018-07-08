/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.AnonymousBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.LabeledBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.BooleanLiteral;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.NumericLiteral;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.StringLiteral;

/**
 * A class with static methods to create basic RDF objects
 */
public class Rdf {
	// not sure if other protocols are generally used in RDF iri's?
	private static final Set<String> IRI_PROTOCOLS = Stream.of("http://", "https://", "mailto:").collect(Collectors.toSet());

	private Rdf() { }

	/**
	 * Create a SparqlBuilder Iri instance from a String iri
	 * 
	 * @param iriString the String representing the iri
	 * @return the {@link Iri} instance
	 */
	public static Iri iri(String iriString) {
		return () -> IRI_PROTOCOLS.stream().anyMatch(iriString.toLowerCase()::startsWith) ?
			"<" + iriString + ">" : iriString;
	}
	
	/**
	 * Create a SparqlBuilder Iri instance from a namespace and local name
	 * @param namespace
	 * 		the namespace of the Iri
	 * @param localName
	 * 		the local name of the Iri
	 * @return a {@link Iri} instance
	 */
	public static Iri iri(String namespace, String localName) {
		return iri(namespace + localName);
	}
	
	/**
	 * creates a labeled blank node
	 * 
	 * @param label the label of the blank node
	 * 
	 * @return a new {@link LabeledBlankNode} instance
	 */
	public static LabeledBlankNode bNode(String label) {
		return new LabeledBlankNode(label);
	}

	/**
	 * creates a label-less blank node, identified by the supplied predicate-object lists
	 * 
	 * @param predicate the predicate of the initial predicate-object list to populate this blank node with
	 * @param objects the objects of the initial predicate-object list to populate this blank node with
	 * 
	 * @return a new {@link PropertiesBlankNode} instance
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
	 * 		Blank node syntax</a>
	 */
	public static PropertiesBlankNode bNode(RdfPredicate predicate, RdfObject... objects) {
		return new PropertiesBlankNode(predicate, objects);
	}
	
	/**
	 * create an empty anonymous blank node
	 * @return an empty {@link AnonymousBlankNode} instance
	 */
	public static AnonymousBlankNode bNode() {
		return new AnonymousBlankNode();
	}
	
	/**
	 * create an RDF string literal
	 * 
	 * @param stringValue the String instance to create a literal from
	 * @return a {@link StringLiteral} instance representing the given String
	 */
	public static StringLiteral literalOf(String stringValue) {
		return new StringLiteral(stringValue);
	}
	
	/**
	 * create a literal with a datatype
	 * 
	 * @param stringValue the literal string
	 * @param dataType the datatype tag
	 * 
	 * @return a {@link StringLiteral} instance representing the given String and datatype
	 */
	public static StringLiteral literalOfType(String stringValue, Iri dataType) {
		return new StringLiteral(stringValue, dataType);
	}
	
	/**
	 * create a literal with a language tag
	 * 
	 * @param stringValue the literal string
	 * @param language the language tag
	 * 
	 * @return a {@link StringLiteral} instance representing the given String and language
	 */
	public static StringLiteral literalOfLanguage(String stringValue, String language) {
		return new StringLiteral(stringValue, language);
	}
	
	/**
	 * create an RDF numeric literal
	 * 
	 * @param numberValue the Number instance to create a literal from
	 * @return a {@link NumericLiteral} instance representing the given Number
	 */
	public static NumericLiteral literalOf(Number numberValue) {
		return new NumericLiteral(numberValue);
	}
	
	/**
	 * create an RDF boolean literal
	 * 
	 * @param boolValue the boolean to create a literal from
	 * @return a {@link BooleanLiteral} instance representing the given boolean
	 */
	public static BooleanLiteral literalOf(Boolean boolValue) {
		return new BooleanLiteral(boolValue);
	}
	
	/**
	 * Create a {@link RdfPredicateObjectList}
	 * 
	 * @param predicate the {@link RdfPredicate} of the predicate-object list
	 * @param objects the {@link RdfObject}(s) of the list
	 * 
	 * @return a new {@link RdfPredicateObjectList}
	 */
	public static RdfPredicateObjectList predicateObjectList(RdfPredicate predicate, RdfObject... objects) {
		return new RdfPredicateObjectList(predicate, objects);
	}

	/**
	 * Create a {@link RdfPredicateObjectListCollection} with an initial {@link RdfPredicateObjectList}
	 * 
	 * @param predicate the {@link RdfPredicate} of the initial {@link RdfPredicateObjectList}
	 * @param objects the {@link RdfObject}(s) of the initial {@link RdfPredicateObjectList}
	 * 
	 * @return a new {@link RdfPredicateObjectListCollection}
	 */
	public static RdfPredicateObjectListCollection predicateObjectListCollection(RdfPredicate predicate, RdfObject... objects) {
		return new RdfPredicateObjectListCollection().andHas(predicate, objects);
	}

	/**
	 * Create a {@link RdfPredicateObjectListCollection} with the given {@link RdfPredicateObjectList}(s)
	 * 
	 * @param predicateObjectLists the {@link RdfPredicateObjectList}(s) to add to the collection
	 * 
	 * @return a new {@link RdfPredicateObjectListCollection}
	 */
	public static RdfPredicateObjectListCollection predicateObjectListCollection(RdfPredicateObjectList... predicateObjectLists) {
		return new RdfPredicateObjectListCollection().andHas(predicateObjectLists);
	}
	
	/**
	 * Convert an array of {@link String}s to an array of {@link StringLiteral}s
	 * 
	 * @param literals the {@link String}s to convert
	 * 
	 * @return an array of the corresponding {@link StringLiteral}s
	 */
	public static StringLiteral[] toRdfLiteralArray(String... literals) {
		return  Arrays.stream(literals).map(Rdf::literalOf).toArray(StringLiteral[]::new);
	}
	
	/**
	 * Convert an array of {@link Boolean}s to an array of {@link BooleanLiteral}s
	 * 
	 * @param literals the {@link Boolean}s to convert
	 * 
	 * @return an array of the corresponding {@link BooleanLiteral}s
	 */
	public static BooleanLiteral[] toRdfLiteralArray(Boolean... literals) {
		return  Arrays.stream(literals).map(Rdf::literalOf).toArray(BooleanLiteral[]::new);
	}
	
	/**
	 * Convert an array of {@link Number}s to an array of {@link NumericLiteral}s
	 * 
	 * @param literals the {@link Number}s to convert
	 * 
	 * @return an array of the corresponding {@link NumericLiteral}s
	 */
	public static NumericLiteral[] toRdfLiteralArray(Number... literals) {
		return  Arrays.stream(literals).map(Rdf::literalOf).toArray(NumericLiteral[]::new);
	}
}
