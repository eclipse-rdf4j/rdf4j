/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
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
 * Constants for OWL / OWL 2 primitives and for the OWL / OWL 2 namespace.
 * 
 * @see <a href="http://www.w3.org/TR/owl-ref/">OWL Web Ontology Language Reference</a>
 * @see <a href="https://www.w3.org/TR/owl2-overview/">OWL 2 Web Ontology Language Document Overview</a>
 */
public class OWL {

	/** The OWL namespace: http://www.w3.org/2002/07/owl# */
	public static final String NAMESPACE = "http://www.w3.org/2002/07/owl#";

	/**
	 * Recommended prefix for the OWL and the OWL 2 namespace: "owl"
	 */
	public static final String PREFIX = "owl";

	/**
	 * An immutable {@link Namespace} constant that represents the OWL namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// OWL Lite

	/** http://www.w3.org/2002/07/owl#Class */
	public final static IRI CLASS;

	/** http://www.w3.org/2002/07/owl#Individual */
	@Deprecated
	public final static IRI INDIVIDUAL;

	/** http://www.w3.org/2002/07/owl#Thing */
	public static final IRI THING;

	/** http://www.w3.org/2002/07/owl#Nothing */
	public static final IRI NOTHING;

	/** http://www.w3.org/2002/07/owl#equivalentClass */
	public final static IRI EQUIVALENTCLASS;

	/** http://www.w3.org/2002/07/owl#equivalentProperty */
	public final static IRI EQUIVALENTPROPERTY;

	/** http://www.w3.org/2002/07/owl#sameAs */
	public final static IRI SAMEAS;

	/** http://www.w3.org/2002/07/owl#differentFrom */
	public final static IRI DIFFERENTFROM;

	/** http://www.w3.org/2002/07/owl#AllDifferent */
	public final static IRI ALLDIFFERENT;

	/** http://www.w3.org/2002/07/owl#distinctMembers */
	public final static IRI DISTINCTMEMBERS;

	/** http://www.w3.org/2002/07/owl#ObjectProperty */
	public final static IRI OBJECTPROPERTY;

	/** http://www.w3.org/2002/07/owl#DatatypeProperty */
	public final static IRI DATATYPEPROPERTY;

	/** http://www.w3.org/2002/07/owl#inverseOf */
	public final static IRI INVERSEOF;

	/** http://www.w3.org/2002/07/owl#TransitiveProperty */
	public final static IRI TRANSITIVEPROPERTY;

	/** http://www.w3.org/2002/07/owl#SymmetricProperty */
	public final static IRI SYMMETRICPROPERTY;

	/** http://www.w3.org/2002/07/owl#FunctionalProperty */
	public final static IRI FUNCTIONALPROPERTY;

	/** http://www.w3.org/2002/07/owl#InverseFunctionalProperty */
	public final static IRI INVERSEFUNCTIONALPROPERTY;

	/** http://www.w3.org/2002/07/owl#Restriction */
	public final static IRI RESTRICTION;

	/** http://www.w3.org/2002/07/owl#onProperty */
	public final static IRI ONPROPERTY;

	/** http://www.w3.org/2002/07/owl#allValuesFrom */
	public final static IRI ALLVALUESFROM;

	/** http://www.w3.org/2002/07/owl#someValuesFrom */
	public final static IRI SOMEVALUESFROM;

	/** http://www.w3.org/2002/07/owl#minCardinality */
	public final static IRI MINCARDINALITY;

	/** http://www.w3.org/2002/07/owl#maxCardinality */
	public final static IRI MAXCARDINALITY;

	/** http://www.w3.org/2002/07/owl#cardinality */
	public final static IRI CARDINALITY;

	/** http://www.w3.org/2002/07/owl#Ontology */
	public final static IRI ONTOLOGY;

	/** http://www.w3.org/2002/07/owl#imports */
	public final static IRI IMPORTS;

	/** http://www.w3.org/2002/07/owl#intersectionOf */
	public final static IRI INTERSECTIONOF;

	/** http://www.w3.org/2002/07/owl#versionInfo */
	public final static IRI VERSIONINFO;

	/** http://www.w3.org/2002/07/owl#versionIRI */
	public final static IRI VERSIONIRI;

	/** http://www.w3.org/2002/07/owl#priorVersion */
	public final static IRI PRIORVERSION;

	/** http://www.w3.org/2002/07/owl#backwardCompatibleWith */
	public final static IRI BACKWARDCOMPATIBLEWITH;

	/** http://www.w3.org/2002/07/owl#incompatibleWith */
	public final static IRI INCOMPATIBLEWITH;

	/** http://www.w3.org/2002/07/owl#DeprecatedClass */
	public final static IRI DEPRECATEDCLASS;

	/** http://www.w3.org/2002/07/owl#DeprecatedProperty */
	public final static IRI DEPRECATEDPROPERTY;

	/** http://www.w3.org/2002/07/owl#AnnotationProperty */
	public final static IRI ANNOTATIONPROPERTY;

	/** http://www.w3.org/2002/07/owl#OntologyProperty */
	public final static IRI ONTOLOGYPROPERTY;

	// OWL DL and OWL Full

	/** http://www.w3.org/2002/07/owl#oneOf */
	public final static IRI ONEOF;

	/** http://www.w3.org/2002/07/owl#hasValue */
	public final static IRI HASVALUE;

	/** http://www.w3.org/2002/07/owl#disjointWith */
	public final static IRI DISJOINTWITH;

	/** http://www.w3.org/2002/07/owl#unionOf */
	public final static IRI UNIONOF;

	/** http://www.w3.org/2002/07/owl#complementOf */
	public final static IRI COMPLEMENTOF;

	// OWL 2

	/** http://www.w3.org/2002/07/owl#AllDisjointClasses */
	public final static IRI ALLDISJOINTCLASSES;

	/** http://www.w3.org/2002/07/owl#AllDisjointProperties */
	public final static IRI ALLDISJOINTPROPERTIES;

	/** http://www.w3.org/2002/07/owl#annotatedProperty */
	public final static IRI ANNOTATEDPROPERTY;

	/** http://www.w3.org/2002/07/owl#annotatedSource */
	public final static IRI ANNOTATEDSOURCE;

	/** http://www.w3.org/2002/07/owl#annotatedTarget */
	public final static IRI ANNOTATEDTARGET;

	/** http://www.w3.org/2002/07/owl#Annotation */
	public final static IRI ANNOTATION;

	/** http://www.w3.org/2002/07/owl#assertionProperty */
	public final static IRI ASSERTIONPROPERTY;

	/** http://www.w3.org/2002/07/owl#AsymmetricProperty */
	public final static IRI ASYMMETRICPROPERTY;

	/** http://www.w3.org/2002/07/owl#Axiom */
	public final static IRI AXIOM;

	/** http://www.w3.org/2002/07/owl#bottomDataProperty */
	public final static IRI BOTTOMDATAPROPERTY;

	/** http://www.w3.org/2002/07/owl#bottomObjectProperty */
	public final static IRI BOTTOMOBJECTPROPERTY;

	/** http://www.w3.org/2002/07/owl#DataRange */
	public final static IRI DATARANGE;

	/** http://www.w3.org/2002/07/owl#datatypeComplementOf */
	public final static IRI DATATYPECOMPLEMENTOF;

	/** http://www.w3.org/2002/07/owl#deprecated */
	public final static IRI DEPRECATED;

	/** http://www.w3.org/2002/07/owl#disjointUnionOf */
	public final static IRI DISJOINTUNIONOF;

	/** http://www.w3.org/2002/07/owl#hasKey */
	public final static IRI HASKEY;

	/** http://www.w3.org/2002/07/owl#hasSelf */
	public final static IRI HASSELF;

	/** http://www.w3.org/2002/07/owl#IrreflexiveProperty */
	public final static IRI IRREFLEXIVEPROPERTY;

	/** http://www.w3.org/2002/07/owl#maxQualifiedCardinality */
	public final static IRI MAXQUALIFIEDCARDINALITY;

	/** http://www.w3.org/2002/07/owl#members */
	public final static IRI MEMBERS;

	/** http://www.w3.org/2002/07/owl#minQualifiedCardinality */
	public final static IRI MINQUALIFIEDCARDINALITY;

	/** http://www.w3.org/2002/07/owl#NamedIndividual */
	public final static IRI NAMEDINDIVIDUAL;

	/** http://www.w3.org/2002/07/owl#NegativePropertyAssertion */
	public final static IRI NEGATIVEPROPERTYASSERTION;

	/** http://www.w3.org/2002/07/owl#onClass */
	public final static IRI ONCLASS;

	/** http://www.w3.org/2002/07/owl#onDataRange */
	public final static IRI ONDATARANGE;

	/** http://www.w3.org/2002/07/owl#onDatatype */
	public final static IRI ONDATATYPE;

	/** http://www.w3.org/2002/07/owl#onProperties */
	public final static IRI ONPROPERTIES;

	/** http://www.w3.org/2002/07/owl#propertyChainAxiom */
	public final static IRI PROPERTYCHAINAXIOM;

	/** http://www.w3.org/2002/07/owl#propertyDisjointWith */
	public final static IRI PROPERTYDISJOINTWITH;

	/** http://www.w3.org/2002/07/owl#qualifiedCardinality */
	public final static IRI QUALIFIEDCARDINALITY;

	/** http://www.w3.org/2002/07/owl#ReflexiveProperty */
	public final static IRI REFLEXIVEPROPERTY;

	/** http://www.w3.org/2002/07/owl#sourceIndividual */
	public final static IRI SOURCEINDIVIDUAL;

	/** http://www.w3.org/2002/07/owl#targetIndividual */
	public final static IRI TARGETINDIVIDUAL;

	/** http://www.w3.org/2002/07/owl#targetValue */
	public final static IRI TARGETVALUE;

	/** http://www.w3.org/2002/07/owl#topDataProperty */
	public final static IRI TOPDATAPROPERTY;

	/** http://www.w3.org/2002/07/owl#topObjectProperty */
	public final static IRI TOPOBJECTPROPERTY;

	/** http://www.w3.org/2002/07/owl#withRestrictions */
	public final static IRI WITHRESTRICTIONS;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		CLASS = factory.createIRI(OWL.NAMESPACE, "Class");
		INDIVIDUAL = factory.createIRI(OWL.NAMESPACE, "Individual");

		THING = factory.createIRI(OWL.NAMESPACE, "Thing");
		NOTHING = factory.createIRI(NAMESPACE, "Nothing");

		EQUIVALENTCLASS = factory.createIRI(OWL.NAMESPACE, "equivalentClass");
		EQUIVALENTPROPERTY = factory.createIRI(OWL.NAMESPACE, "equivalentProperty");
		SAMEAS = factory.createIRI(OWL.NAMESPACE, "sameAs");
		DIFFERENTFROM = factory.createIRI(OWL.NAMESPACE, "differentFrom");
		ALLDIFFERENT = factory.createIRI(OWL.NAMESPACE, "AllDifferent");
		DISTINCTMEMBERS = factory.createIRI(OWL.NAMESPACE, "distinctMembers");
		OBJECTPROPERTY = factory.createIRI(OWL.NAMESPACE, "ObjectProperty");
		DATATYPEPROPERTY = factory.createIRI(OWL.NAMESPACE, "DatatypeProperty");
		INVERSEOF = factory.createIRI(OWL.NAMESPACE, "inverseOf");
		TRANSITIVEPROPERTY = factory.createIRI(OWL.NAMESPACE, "TransitiveProperty");
		SYMMETRICPROPERTY = factory.createIRI(OWL.NAMESPACE, "SymmetricProperty");
		FUNCTIONALPROPERTY = factory.createIRI(OWL.NAMESPACE, "FunctionalProperty");
		INVERSEFUNCTIONALPROPERTY = factory.createIRI(OWL.NAMESPACE, "InverseFunctionalProperty");
		RESTRICTION = factory.createIRI(OWL.NAMESPACE, "Restriction");
		ONPROPERTY = factory.createIRI(OWL.NAMESPACE, "onProperty");
		ALLVALUESFROM = factory.createIRI(OWL.NAMESPACE, "allValuesFrom");
		SOMEVALUESFROM = factory.createIRI(OWL.NAMESPACE, "someValuesFrom");
		MINCARDINALITY = factory.createIRI(OWL.NAMESPACE, "minCardinality");
		MAXCARDINALITY = factory.createIRI(OWL.NAMESPACE, "maxCardinality");
		CARDINALITY = factory.createIRI(OWL.NAMESPACE, "cardinality");
		ONTOLOGY = factory.createIRI(OWL.NAMESPACE, "Ontology");
		IMPORTS = factory.createIRI(OWL.NAMESPACE, "imports");
		INTERSECTIONOF = factory.createIRI(OWL.NAMESPACE, "intersectionOf");
		VERSIONINFO = factory.createIRI(OWL.NAMESPACE, "versionInfo");
		VERSIONIRI = factory.createIRI(OWL.NAMESPACE, "versionIRI");
		PRIORVERSION = factory.createIRI(OWL.NAMESPACE, "priorVersion");
		BACKWARDCOMPATIBLEWITH = factory.createIRI(OWL.NAMESPACE, "backwardCompatibleWith");
		INCOMPATIBLEWITH = factory.createIRI(OWL.NAMESPACE, "incompatibleWith");
		DEPRECATEDCLASS = factory.createIRI(OWL.NAMESPACE, "DeprecatedClass");
		DEPRECATEDPROPERTY = factory.createIRI(OWL.NAMESPACE, "DeprecatedProperty");
		ANNOTATIONPROPERTY = factory.createIRI(OWL.NAMESPACE, "AnnotationProperty");
		ONTOLOGYPROPERTY = factory.createIRI(OWL.NAMESPACE, "OntologyProperty");

		// OWL DL and OWL Full

		ONEOF = factory.createIRI(OWL.NAMESPACE, "oneOf");
		HASVALUE = factory.createIRI(OWL.NAMESPACE, "hasValue");
		DISJOINTWITH = factory.createIRI(OWL.NAMESPACE, "disjointWith");
		UNIONOF = factory.createIRI(OWL.NAMESPACE, "unionOf");
		COMPLEMENTOF = factory.createIRI(OWL.NAMESPACE, "complementOf");

		// OWL 2

		ALLDISJOINTCLASSES = factory.createIRI(OWL.NAMESPACE, "AllDisjointClasses");
		ALLDISJOINTPROPERTIES = factory.createIRI(OWL.NAMESPACE, "AllDisjointProperties");
		ANNOTATEDPROPERTY = factory.createIRI(OWL.NAMESPACE, "annotatedProperty");
		ANNOTATEDSOURCE = factory.createIRI(OWL.NAMESPACE, "annotatedSource");
		ANNOTATEDTARGET = factory.createIRI(OWL.NAMESPACE, "annotatedTarget");
		ANNOTATION = factory.createIRI(OWL.NAMESPACE, "Annotation");
		ASSERTIONPROPERTY = factory.createIRI(OWL.NAMESPACE, "assertionProperty");
		ASYMMETRICPROPERTY = factory.createIRI(OWL.NAMESPACE, "AsymmetricProperty");
		AXIOM = factory.createIRI(OWL.NAMESPACE, "Axiom");
		BOTTOMDATAPROPERTY = factory.createIRI(OWL.NAMESPACE, "bottomDataProperty");
		BOTTOMOBJECTPROPERTY = factory.createIRI(OWL.NAMESPACE, "bottomObjectProperty");
		DATARANGE = factory.createIRI(OWL.NAMESPACE, "DataRange");
		DATATYPECOMPLEMENTOF = factory.createIRI(OWL.NAMESPACE, "datatypeComplementOf");
		DEPRECATED = factory.createIRI(OWL.NAMESPACE, "deprecated");
		DISJOINTUNIONOF = factory.createIRI(OWL.NAMESPACE, "disjointUnionOf");
		HASKEY = factory.createIRI(OWL.NAMESPACE, "hasKey");
		HASSELF = factory.createIRI(OWL.NAMESPACE, "hasSelf");
		IRREFLEXIVEPROPERTY = factory.createIRI(OWL.NAMESPACE, "IrreflexiveProperty");
		MAXQUALIFIEDCARDINALITY = factory.createIRI(OWL.NAMESPACE, "maxQualifiedCardinality");
		MEMBERS = factory.createIRI(OWL.NAMESPACE, "members");
		MINQUALIFIEDCARDINALITY = factory.createIRI(OWL.NAMESPACE, "minQualifiedCardinality");
		NAMEDINDIVIDUAL = factory.createIRI(OWL.NAMESPACE, "NamedIndividual");
		NEGATIVEPROPERTYASSERTION = factory.createIRI(OWL.NAMESPACE, "NegativePropertyAssertion");
		ONCLASS = factory.createIRI(OWL.NAMESPACE, "onClass");
		ONDATARANGE = factory.createIRI(OWL.NAMESPACE, "onDataRange");
		ONDATATYPE = factory.createIRI(OWL.NAMESPACE, "onDatatype");
		ONPROPERTIES = factory.createIRI(OWL.NAMESPACE, "onProperties");
		PROPERTYCHAINAXIOM = factory.createIRI(OWL.NAMESPACE, "propertyChainAxiom");
		PROPERTYDISJOINTWITH = factory.createIRI(OWL.NAMESPACE, "propertyDisjointWith");
		QUALIFIEDCARDINALITY = factory.createIRI(OWL.NAMESPACE, "qualifiedCardinality");
		REFLEXIVEPROPERTY = factory.createIRI(OWL.NAMESPACE, "ReflexiveProperty");
		SOURCEINDIVIDUAL = factory.createIRI(OWL.NAMESPACE, "sourceIndividual");
		TARGETINDIVIDUAL = factory.createIRI(OWL.NAMESPACE, "targetIndividual");
		TARGETVALUE = factory.createIRI(OWL.NAMESPACE, "targetValue");
		TOPDATAPROPERTY = factory.createIRI(OWL.NAMESPACE, "topDataProperty");
		TOPOBJECTPROPERTY = factory.createIRI(OWL.NAMESPACE, "topObjectProperty");
		WITHRESTRICTIONS = factory.createIRI(OWL.NAMESPACE, "withRestrictions");
	}
}