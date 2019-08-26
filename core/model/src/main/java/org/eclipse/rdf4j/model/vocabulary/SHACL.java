/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Shapes Constraint Language.
 *
 * @see <a href="https://www.w3.org/TR/2017/WD-shacl-20170303/">Shapes Constraint Language</a>
 *
 * @author Bart Hanssens
 */
public class SHACL {
	/**
	 * The SHACL namespace: http://www.w3.org/ns/shacl#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/shacl#";

	/**
	 * Recommended prefix for the namespace: "sh"
	 */
	public static final String PREFIX = "sh";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	// Classes
	/** sh:AbstractResult */
	public static final IRI ABSTRACT_RESULT;

	/** sh:AndConstraintComponent */
	public static final IRI AND_CONSTRAINT_COMPONENT;

	/** sh:AndConstraintComponent-and */
	public static final IRI AND_CONSTRAINT_COMPONENT_AND;

	/** sh:BlankNode */
	public static final IRI BLANK_NODE;

	/** sh:BlankNodeOrIRI */
	public static final IRI BLANK_NODE_OR_IRI;

	/** sh:BlankNodeOrLiteral */
	public static final IRI BLANK_NODE_OR_LITERAL;

	/** sh:ClassConstraintComponent */
	public static final IRI CLASS_CONSTRAINT_COMPONENT;

	/** sh:ClassConstraintComponent-class */
	public static final IRI CLASS_CONSTRAINT_COMPONENT_CLASS;

	/** sh:ClosedConstraintComponent */
	public static final IRI CLOSED_CONSTRAINT_COMPONENT;

	/** sh:ClosedConstraintComponent-closed */
	public static final IRI CLOSED_CONSTRAINT_COMPONENT_CLOSED;

	/** sh:ClosedConstraintComponent-ignoredProperties */
	public static final IRI CLOSED_CONSTRAINT_COMPONENT_IGNORED_PROPERTIES;

	/** sh:ConstraintComponent */
	public static final IRI CONSTRAINT_COMPONENT;

	/** sh:DatatypeConstraintComponent */
	public static final IRI DATATYPE_CONSTRAINT_COMPONENT;

	/** sh:DatatypeConstraintComponent-datatype */
	public static final IRI DATATYPE_CONSTRAINT_COMPONENT_DATATYPE;

	/** sh:DerivedValuesConstraintComponent */
	public static final IRI DERIVED_VALUES_CONSTRAINT_COMPONENT;

	/** sh:DisjointConstraintComponent */
	public static final IRI DISJOINT_CONSTRAINT_COMPONENT;

	/** sh:DisjointConstraintComponent-disjoint */
	public static final IRI DISJOINT_CONSTRAINT_COMPONENT_DISJOINT;

	/** sh:EqualsConstraintComponent */
	public static final IRI EQUALS_CONSTRAINT_COMPONENT;

	/** sh:EqualsConstraintComponent-equals */
	public static final IRI EQUALS_CONSTRAINT_COMPONENT_EQUALS;

	/** sh:Function */
	public static final IRI FUNCTION;

	/** sh:HasValueConstraintComponent */
	public static final IRI HAS_VALUE_CONSTRAINT_COMPONENT;

	/** sh:HasValueConstraintComponent-hasValue */
	public static final IRI HAS_VALUE_CONSTRAINT_COMPONENT_HAS_VALUE;

	/** sh:IRI */
	public static final IRI IRI;

	/** sh:IRIOrLiteral */
	public static final IRI IRI_OR_LITERAL;

	/** sh:InConstraintComponent */
	public static final IRI IN_CONSTRAINT_COMPONENT;

	/** sh:InConstraintComponent-in */
	public static final IRI IN_CONSTRAINT_COMPONENT_IN;

	/** sh:Info */
	public static final IRI INFO;

	/** sh:LanguageInConstraintComponent */
	public static final IRI LANGUAGE_IN_CONSTRAINT_COMPONENT;

	/** sh:LanguageInConstraintComponent-languageIn */
	public static final IRI LANGUAGE_IN_CONSTRAINT_COMPONENT_LANGUAGE_IN;

	/** sh:LessThanConstraintComponent */
	public static final IRI LESS_THAN_CONSTRAINT_COMPONENT;

	/** sh:LessThanConstraintComponent-lessThan */
	public static final IRI LESS_THAN_CONSTRAINT_COMPONENT_LESS_THAN;

	/** sh:LessThanOrEqualsConstraintComponent */
	public static final IRI LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT;

	/** sh:LessThanOrEqualsConstraintComponent-lessThanOrEquals */
	public static final IRI LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT_LESS_THAN_OR_EQUALS;

	/** sh:Literal */
	public static final IRI LITERAL;

	/** sh:MaxCountConstraintComponent */
	public static final IRI MAX_COUNT_CONSTRAINT_COMPONENT;

	/** sh:MaxCountConstraintComponent-maxCount */
	public static final IRI MAX_COUNT_CONSTRAINT_COMPONENT_MAX_COUNT;

	/** sh:MaxExclusiveConstraintComponent */
	public static final IRI MAX_EXCLUSIVE_CONSTRAINT_COMPONENT;

	/** sh:MaxExclusiveConstraintComponent-maxExclusive */
	public static final IRI MAX_EXCLUSIVE_CONSTRAINT_COMPONENT_MAX_EXCLUSIVE;

	/** sh:MaxInclusiveConstraintComponent */
	public static final IRI MAX_INCLUSIVE_CONSTRAINT_COMPONENT;

	/** sh:MaxInclusiveConstraintComponent-maxInclusive */
	public static final IRI MAX_INCLUSIVE_CONSTRAINT_COMPONENT_MAX_INCLUSIVE;

	/** sh:MaxLengthConstraintComponent */
	public static final IRI MAX_LENGTH_CONSTRAINT_COMPONENT;

	/** sh:MaxLengthConstraintComponent-maxLength */
	public static final IRI MAX_LENGTH_CONSTRAINT_COMPONENT_MAX_LENGTH;

	/** sh:MinCountConstraintComponent */
	public static final IRI MIN_COUNT_CONSTRAINT_COMPONENT;

	/** sh:MinCountConstraintComponent-minCount */
	public static final IRI MIN_COUNT_CONSTRAINT_COMPONENT_MIN_COUNT;

	/** sh:MinExclusiveConstraintComponent */
	public static final IRI MIN_EXCLUSIVE_CONSTRAINT_COMPONENT;

	/** sh:MinExclusiveConstraintComponent-minExclusive */
	public static final IRI MIN_EXCLUSIVE_CONSTRAINT_COMPONENT_MIN_EXCLUSIVE;

	/** sh:MinInclusiveConstraintComponent */
	public static final IRI MIN_INCLUSIVE_CONSTRAINT_COMPONENT;

	/** sh:MinInclusiveConstraintComponent-minInclusive */
	public static final IRI MIN_INCLUSIVE_CONSTRAINT_COMPONENT_MIN_INCLUSIVE;

	/** sh:MinLengthConstraintComponent */
	public static final IRI MIN_LENGTH_CONSTRAINT_COMPONENT;

	/** sh:MinLengthConstraintComponent-minLength */
	public static final IRI MIN_LENGTH_CONSTRAINT_COMPONENT_MIN_LENGTH;

	/** sh:NodeConstraintComponent */
	public static final IRI NODE_CONSTRAINT_COMPONENT;

	/** sh:NodeConstraintComponent-node */
	public static final IRI NODE_CONSTRAINT_COMPONENT_NODE;

	/** sh:NodeKind */
	public static final IRI NODE_KIND;

	/** sh:NodeKindConstraintComponent */
	public static final IRI NODE_KIND_CONSTRAINT_COMPONENT;

	/** sh:NodeKindConstraintComponent-nodeKind */
	public static final IRI NODE_KIND_CONSTRAINT_COMPONENT_NODE_KIND;

	/** sh:NodeShape */
	public static final IRI NODE_SHAPE;

	/** sh:NotConstraintComponent */
	public static final IRI NOT_CONSTRAINT_COMPONENT;

	/** sh:NotConstraintComponent-not */
	public static final IRI NOT_CONSTRAINT_COMPONENT_NOT;

	/** sh:OrConstraintComponent */
	public static final IRI OR_CONSTRAINT_COMPONENT;

	/** sh:OrConstraintComponent-or */
	public static final IRI OR_CONSTRAINT_COMPONENT_OR;

	/** sh:Parameter */
	public static final IRI PARAMETER;

	/** sh:Parameterizable */
	public static final IRI PARAMETERIZABLE;

	/** sh:PatternConstraintComponent */
	public static final IRI PATTERN_CONSTRAINT_COMPONENT;

	/** sh:PatternConstraintComponent-flags */
	public static final IRI PATTERN_CONSTRAINT_COMPONENT_FLAGS;

	/** sh:PatternConstraintComponent-pattern */
	public static final IRI PATTERN_CONSTRAINT_COMPONENT_PATTERN;

	/** sh:PrefixDeclaration */
	public static final IRI PREFIX_DECLARATION;

	/** sh:PropertyConstraintComponent */
	public static final IRI PROPERTY_CONSTRAINT_COMPONENT;

	/** sh:PropertyConstraintComponent-property */
	public static final IRI PROPERTY_CONSTRAINT_COMPONENT_PROPERTY;

	/** sh:PropertyGroup */
	public static final IRI PROPERTY_GROUP;

	/** sh:PropertyShape */
	public static final IRI PROPERTY_SHAPE;

	/** sh:QualifiedMaxCountConstraintComponent */
	public static final IRI QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT;

	/** sh:QualifiedMaxCountConstraintComponent-qualifiedMaxCount */
	public static final IRI QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MAX_COUNT;

	/** sh:QualifiedMaxCountConstraintComponent-qualifiedValueShape */
	public static final IRI QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE;

	/** sh:QualifiedMaxCountConstraintComponent-qualifiedValueShapesDisjoint */
	public static final IRI QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT;

	/** sh:QualifiedMinCountConstraintComponent */
	public static final IRI QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT;

	/** sh:QualifiedMinCountConstraintComponent-qualifiedMinCount */
	public static final IRI QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MIN_COUNT;

	/** sh:QualifiedMinCountConstraintComponent-qualifiedValueShape */
	public static final IRI QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE;

	/** sh:QualifiedMinCountConstraintComponent-qualifiedValueShapesDisjoint */
	public static final IRI QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT;

	/** sh:ResultAnnotation */
	public static final IRI RESULT_ANNOTATION;

	/** sh:SPARQLAskExecutable */
	public static final IRI SPARQL_ASK_EXECUTABLE;

	/** sh:SPARQLAskValidator */
	public static final IRI SPARQL_ASK_VALIDATOR;

	/** sh:SPARQLConstraint */
	public static final IRI SPARQL_CONSTRAINT;

	/** sh:SPARQLConstraintComponent */
	public static final IRI SPARQL_CONSTRAINT_COMPONENT;

	/** sh:SPARQLConstraintComponent-sparql */
	public static final IRI SPARQL_CONSTRAINT_COMPONENT_SPARQL;

	/** sh:SPARQLConstructExecutable */
	public static final IRI SPARQL_CONSTRUCT_EXECUTABLE;

	/** sh:SPARQLExecutable */
	public static final IRI SPARQL_EXECUTABLE;

	/** sh:SPARQLFunction */
	public static final IRI SPARQL_FUNCTION;

	/** sh:SPARQLSelectExecutable */
	public static final IRI SPARQL_SELECT_EXECUTABLE;

	/** sh:SPARQLSelectValidator */
	public static final IRI SPARQL_SELECT_VALIDATOR;

	/** sh:SPARQLTarget */
	public static final IRI SPARQL_TARGET;

	/** sh:SPARQLTargetType */
	public static final IRI SPARQL_TARGET_TYPE;

	/** sh:SPARQLUpdateExecutable */
	public static final IRI SPARQL_UPDATE_EXECUTABLE;

	/** sh:SPARQLValuesDeriver */
	public static final IRI SPARQL_VALUES_DERIVER;

	/** sh:Severity */
	public static final IRI SEVERITY;

	/** sh:Shape */
	public static final IRI SHAPE;

	/** sh:Target */
	public static final IRI TARGET;

	/** sh:TargetType */
	public static final IRI TARGET_TYPE;

	/** sh:UniqueLangConstraintComponent */
	public static final IRI UNIQUE_LANG_CONSTRAINT_COMPONENT;

	/** sh:UniqueLangConstraintComponent-uniqueLang */
	public static final IRI UNIQUE_LANG_CONSTRAINT_COMPONENT_UNIQUE_LANG;

	/** sh:ValidationReport */
	public static final IRI VALIDATION_REPORT;

	/** sh:ValidationResult */
	public static final IRI VALIDATION_RESULT;

	/** sh:Validator */
	public static final IRI VALIDATOR;

	/** sh:ValuesDeriver */
	public static final IRI VALUES_DERIVER;

	/** sh:Violation */
	public static final IRI VIOLATION;

	/** sh:Warning */
	public static final IRI WARNING;

	/** sh:XoneConstraintComponent */
	public static final IRI XONE_CONSTRAINT_COMPONENT;

	/** sh:XoneConstraintComponent-xone */
	public static final IRI XONE_CONSTRAINT_COMPONENT_XONE;

	// Properties
	/** sh:alternativePath */
	public static final IRI ALTERNATIVE_PATH;

	/** sh:and */
	public static final IRI AND;

	/** sh:annotationProperty */
	public static final IRI ANNOTATION_PROPERTY;

	/** sh:annotationValue */
	public static final IRI ANNOTATION_VALUE;

	/** sh:annotationVarName */
	public static final IRI ANNOTATION_VAR_NAME;

	/** sh:ask */
	public static final IRI ASK;

	/** sh:class */
	public static final IRI CLASS;

	/** sh:closed */
	public static final IRI CLOSED;

	/** sh:conforms */
	public static final IRI CONFORMS;

	/** sh:construct */
	public static final IRI CONSTRUCT;

	/** sh:datatype */
	public static final IRI DATATYPE;

	/** sh:deactivated */
	public static final IRI DEACTIVATED;

	/** sh:declare */
	public static final IRI DECLARE;

	/** sh:defaultValue */
	public static final IRI DEFAULT_VALUE;

	/** sh:derivedValues */
	public static final IRI DERIVED_VALUES;

	/** sh:description */
	public static final IRI DESCRIPTION;

	/** sh:detail */
	public static final IRI DETAIL;

	/** sh:disjoint */
	public static final IRI DISJOINT;

	/** sh:equals */
	public static final IRI EQUALS;

	/** sh:flags */
	public static final IRI FLAGS;

	/** sh:focusNode */
	public static final IRI FOCUS_NODE;

	/** sh:group */
	public static final IRI GROUP;

	/** sh:hasValue */
	public static final IRI HAS_VALUE;

	/** sh:ignoredProperties */
	public static final IRI IGNORED_PROPERTIES;

	/** sh:in */
	public static final IRI IN;

	/** sh:inversePath */
	public static final IRI INVERSE_PATH;

	/** sh:labelTemplate */
	public static final IRI LABEL_TEMPLATE;

	/** sh:languageIn */
	public static final IRI LANGUAGE_IN;

	/** sh:lessThan */
	public static final IRI LESS_THAN;

	/** sh:lessThanOrEquals */
	public static final IRI LESS_THAN_OR_EQUALS;

	/** sh:maxCount */
	public static final IRI MAX_COUNT;

	/** sh:maxExclusive */
	public static final IRI MAX_EXCLUSIVE;

	/** sh:maxInclusive */
	public static final IRI MAX_INCLUSIVE;

	/** sh:maxLength */
	public static final IRI MAX_LENGTH;

	/** sh:message */
	public static final IRI MESSAGE;

	/** sh:minCount */
	public static final IRI MIN_COUNT;

	/** sh:minExclusive */
	public static final IRI MIN_EXCLUSIVE;

	/** sh:minInclusive */
	public static final IRI MIN_INCLUSIVE;

	/** sh:minLength */
	public static final IRI MIN_LENGTH;

	/** sh:name */
	public static final IRI NAME;

	/** sh:namespace */
	public static final IRI NAMESPACE_PROP;

	/** sh:node */
	public static final IRI NODE;

	/** sh:nodeKind */
	public static final IRI NODE_KIND_PROP;

	/** sh:nodeValidator */
	public static final IRI NODE_VALIDATOR;

	/** sh:not */
	public static final IRI NOT;

	/** sh:oneOrMorePath */
	public static final IRI ONE_OR_MORE_PATH;

	/** sh:optional */
	public static final IRI OPTIONAL;

	/** sh:or */
	public static final IRI OR;

	/** sh:order */
	public static final IRI ORDER;

	/** sh:parameter */
	public static final IRI PARAMETER_PROP;

	/** sh:path */
	public static final IRI PATH;

	/** sh:pattern */
	public static final IRI PATTERN;

	/** sh:prefix */
	public static final IRI PREFIX_PROP;

	/** sh:prefixes */
	public static final IRI PREFIXES;

	/** sh:property */
	public static final IRI PROPERTY;

	/** sh:propertyValidator */
	public static final IRI PROPERTY_VALIDATOR;

	/** sh:qualifiedMaxCount */
	public static final IRI QUALIFIED_MAX_COUNT;

	/** sh:qualifiedMinCount */
	public static final IRI QUALIFIED_MIN_COUNT;

	/** sh:qualifiedValueShape */
	public static final IRI QUALIFIED_VALUE_SHAPE;

	/** sh:qualifiedValueShapesDisjoint */
	public static final IRI QUALIFIED_VALUE_SHAPES_DISJOINT;

	/** sh:result */
	public static final IRI RESULT;

	/** sh:resultAnnotation */
	public static final IRI RESULT_ANNOTATION_PROP;

	/** sh:resultMessage */
	public static final IRI RESULT_MESSAGE;

	/** sh:resultPath */
	public static final IRI RESULT_PATH;

	/** sh:resultSeverity */
	public static final IRI RESULT_SEVERITY;

	/** sh:returnType */
	public static final IRI RETURN_TYPE;

	/** sh:select */
	public static final IRI SELECT;

	/** sh:severity */
	public static final IRI SEVERITY_PROP;

	/** sh:shapesGraph */
	public static final IRI SHAPES_GRAPH;

	/** sh:shapesGraphWellFormed */
	public static final IRI SHAPES_GRAPH_WELL_FORMED;

	/** sh:sourceConstraint */
	public static final IRI SOURCE_CONSTRAINT;

	/** sh:sourceConstraintComponent */
	public static final IRI SOURCE_CONSTRAINT_COMPONENT;

	/** sh:sourceShape */
	public static final IRI SOURCE_SHAPE;

	/** sh:sparql */
	public static final IRI SPARQL;

	/** sh:target */
	public static final IRI TARGET_PROP;

	/** sh:targetClass */
	public static final IRI TARGET_CLASS;

	/** sh:targetNode */
	public static final IRI TARGET_NODE;

	/** sh:targetObjectsOf */
	public static final IRI TARGET_OBJECTS_OF;

	/** sh:targetSubjectsOf */
	public static final IRI TARGET_SUBJECTS_OF;

	/** sh:uniqueLang */
	public static final IRI UNIQUE_LANG;

	/** sh:update */
	public static final IRI UPDATE;

	/** sh:validator */
	public static final IRI VALIDATOR_PROP;

	/** sh:value */
	public static final IRI VALUE;

	/** sh:xone */
	public static final IRI XONE;

	/** sh:zeroOrMorePath */
	public static final IRI ZERO_OR_MORE_PATH;

	/** sh:zeroOrOnePath */
	public static final IRI ZERO_OR_ONE_PATH;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();

		ABSTRACT_RESULT = factory.createIRI(NAMESPACE, "AbstractResult");
		AND_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "AndConstraintComponent");
		AND_CONSTRAINT_COMPONENT_AND = factory.createIRI(NAMESPACE, "AndConstraintComponent-and");
		BLANK_NODE = factory.createIRI(NAMESPACE, "BlankNode");
		BLANK_NODE_OR_IRI = factory.createIRI(NAMESPACE, "BlankNodeOrIRI");
		BLANK_NODE_OR_LITERAL = factory.createIRI(NAMESPACE, "BlankNodeOrLiteral");
		CLASS_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "ClassConstraintComponent");
		CLASS_CONSTRAINT_COMPONENT_CLASS = factory.createIRI(NAMESPACE, "ClassConstraintComponent-class");
		CLOSED_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "ClosedConstraintComponent");
		CLOSED_CONSTRAINT_COMPONENT_CLOSED = factory.createIRI(NAMESPACE, "ClosedConstraintComponent-closed");
		CLOSED_CONSTRAINT_COMPONENT_IGNORED_PROPERTIES = factory.createIRI(NAMESPACE,
				"ClosedConstraintComponent-ignoredProperties");
		CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "ConstraintComponent");
		DATATYPE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "DatatypeConstraintComponent");
		DATATYPE_CONSTRAINT_COMPONENT_DATATYPE = factory.createIRI(NAMESPACE, "DatatypeConstraintComponent-datatype");
		DERIVED_VALUES_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "DerivedValuesConstraintComponent");
		DISJOINT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "DisjointConstraintComponent");
		DISJOINT_CONSTRAINT_COMPONENT_DISJOINT = factory.createIRI(NAMESPACE, "DisjointConstraintComponent-disjoint");
		EQUALS_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "EqualsConstraintComponent");
		EQUALS_CONSTRAINT_COMPONENT_EQUALS = factory.createIRI(NAMESPACE, "EqualsConstraintComponent-equals");
		FUNCTION = factory.createIRI(NAMESPACE, "Function");
		HAS_VALUE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "HasValueConstraintComponent");
		HAS_VALUE_CONSTRAINT_COMPONENT_HAS_VALUE = factory.createIRI(NAMESPACE, "HasValueConstraintComponent-hasValue");
		IRI = factory.createIRI(NAMESPACE, "IRI");
		IRI_OR_LITERAL = factory.createIRI(NAMESPACE, "IRIOrLiteral");
		IN_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "InConstraintComponent");
		IN_CONSTRAINT_COMPONENT_IN = factory.createIRI(NAMESPACE, "InConstraintComponent-in");
		INFO = factory.createIRI(NAMESPACE, "Info");
		LANGUAGE_IN_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "LanguageInConstraintComponent");
		LANGUAGE_IN_CONSTRAINT_COMPONENT_LANGUAGE_IN = factory.createIRI(NAMESPACE,
				"LanguageInConstraintComponent-languageIn");
		LESS_THAN_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "LessThanConstraintComponent");
		LESS_THAN_CONSTRAINT_COMPONENT_LESS_THAN = factory.createIRI(NAMESPACE, "LessThanConstraintComponent-lessThan");
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "LessThanOrEqualsConstraintComponent");
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT_LESS_THAN_OR_EQUALS = factory.createIRI(NAMESPACE,
				"LessThanOrEqualsConstraintComponent-lessThanOrEquals");
		LITERAL = factory.createIRI(NAMESPACE, "Literal");
		MAX_COUNT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MaxCountConstraintComponent");
		MAX_COUNT_CONSTRAINT_COMPONENT_MAX_COUNT = factory.createIRI(NAMESPACE, "MaxCountConstraintComponent-maxCount");
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MaxExclusiveConstraintComponent");
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT_MAX_EXCLUSIVE = factory.createIRI(NAMESPACE,
				"MaxExclusiveConstraintComponent-maxExclusive");
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MaxInclusiveConstraintComponent");
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT_MAX_INCLUSIVE = factory.createIRI(NAMESPACE,
				"MaxInclusiveConstraintComponent-maxInclusive");
		MAX_LENGTH_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MaxLengthConstraintComponent");
		MAX_LENGTH_CONSTRAINT_COMPONENT_MAX_LENGTH = factory.createIRI(NAMESPACE,
				"MaxLengthConstraintComponent-maxLength");
		MIN_COUNT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MinCountConstraintComponent");
		MIN_COUNT_CONSTRAINT_COMPONENT_MIN_COUNT = factory.createIRI(NAMESPACE, "MinCountConstraintComponent-minCount");
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MinExclusiveConstraintComponent");
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT_MIN_EXCLUSIVE = factory.createIRI(NAMESPACE,
				"MinExclusiveConstraintComponent-minExclusive");
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MinInclusiveConstraintComponent");
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT_MIN_INCLUSIVE = factory.createIRI(NAMESPACE,
				"MinInclusiveConstraintComponent-minInclusive");
		MIN_LENGTH_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "MinLengthConstraintComponent");
		MIN_LENGTH_CONSTRAINT_COMPONENT_MIN_LENGTH = factory.createIRI(NAMESPACE,
				"MinLengthConstraintComponent-minLength");
		NODE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "NodeConstraintComponent");
		NODE_CONSTRAINT_COMPONENT_NODE = factory.createIRI(NAMESPACE, "NodeConstraintComponent-node");
		NODE_KIND = factory.createIRI(NAMESPACE, "NodeKind");
		NODE_KIND_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "NodeKindConstraintComponent");
		NODE_KIND_CONSTRAINT_COMPONENT_NODE_KIND = factory.createIRI(NAMESPACE, "NodeKindConstraintComponent-nodeKind");
		NODE_SHAPE = factory.createIRI(NAMESPACE, "NodeShape");
		NOT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "NotConstraintComponent");
		NOT_CONSTRAINT_COMPONENT_NOT = factory.createIRI(NAMESPACE, "NotConstraintComponent-not");
		OR_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "OrConstraintComponent");
		OR_CONSTRAINT_COMPONENT_OR = factory.createIRI(NAMESPACE, "OrConstraintComponent-or");
		PARAMETER = factory.createIRI(NAMESPACE, "Parameter");
		PARAMETERIZABLE = factory.createIRI(NAMESPACE, "Parameterizable");
		PATTERN_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "PatternConstraintComponent");
		PATTERN_CONSTRAINT_COMPONENT_FLAGS = factory.createIRI(NAMESPACE, "PatternConstraintComponent-flags");
		PATTERN_CONSTRAINT_COMPONENT_PATTERN = factory.createIRI(NAMESPACE, "PatternConstraintComponent-pattern");
		PREFIX_DECLARATION = factory.createIRI(NAMESPACE, "PrefixDeclaration");
		PROPERTY_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "PropertyConstraintComponent");
		PROPERTY_CONSTRAINT_COMPONENT_PROPERTY = factory.createIRI(NAMESPACE, "PropertyConstraintComponent-property");
		PROPERTY_GROUP = factory.createIRI(NAMESPACE, "PropertyGroup");
		PROPERTY_SHAPE = factory.createIRI(NAMESPACE, "PropertyShape");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "QualifiedMaxCountConstraintComponent");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MAX_COUNT = factory.createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedMaxCount");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE = factory.createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedValueShape");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT = factory.createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedValueShapesDisjoint");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "QualifiedMinCountConstraintComponent");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MIN_COUNT = factory.createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedMinCount");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE = factory.createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedValueShape");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT = factory.createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedValueShapesDisjoint");
		RESULT_ANNOTATION = factory.createIRI(NAMESPACE, "ResultAnnotation");
		SPARQL_ASK_EXECUTABLE = factory.createIRI(NAMESPACE, "SPARQLAskExecutable");
		SPARQL_ASK_VALIDATOR = factory.createIRI(NAMESPACE, "SPARQLAskValidator");
		SPARQL_CONSTRAINT = factory.createIRI(NAMESPACE, "SPARQLConstraint");
		SPARQL_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "SPARQLConstraintComponent");
		SPARQL_CONSTRAINT_COMPONENT_SPARQL = factory.createIRI(NAMESPACE, "SPARQLConstraintComponent-sparql");
		SPARQL_CONSTRUCT_EXECUTABLE = factory.createIRI(NAMESPACE, "SPARQLConstructExecutable");
		SPARQL_EXECUTABLE = factory.createIRI(NAMESPACE, "SPARQLExecutable");
		SPARQL_FUNCTION = factory.createIRI(NAMESPACE, "SPARQLFunction");
		SPARQL_SELECT_EXECUTABLE = factory.createIRI(NAMESPACE, "SPARQLSelectExecutable");
		SPARQL_SELECT_VALIDATOR = factory.createIRI(NAMESPACE, "SPARQLSelectValidator");
		SPARQL_TARGET = factory.createIRI(NAMESPACE, "SPARQLTarget");
		SPARQL_TARGET_TYPE = factory.createIRI(NAMESPACE, "SPARQLTargetType");
		SPARQL_UPDATE_EXECUTABLE = factory.createIRI(NAMESPACE, "SPARQLUpdateExecutable");
		SPARQL_VALUES_DERIVER = factory.createIRI(NAMESPACE, "SPARQLValuesDeriver");
		SEVERITY = factory.createIRI(NAMESPACE, "Severity");
		SHAPE = factory.createIRI(NAMESPACE, "Shape");
		TARGET = factory.createIRI(NAMESPACE, "Target");
		TARGET_TYPE = factory.createIRI(NAMESPACE, "TargetType");
		UNIQUE_LANG_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "UniqueLangConstraintComponent");
		UNIQUE_LANG_CONSTRAINT_COMPONENT_UNIQUE_LANG = factory.createIRI(NAMESPACE,
				"UniqueLangConstraintComponent-uniqueLang");
		VALIDATION_REPORT = factory.createIRI(NAMESPACE, "ValidationReport");
		VALIDATION_RESULT = factory.createIRI(NAMESPACE, "ValidationResult");
		VALIDATOR = factory.createIRI(NAMESPACE, "Validator");
		VALUES_DERIVER = factory.createIRI(NAMESPACE, "ValuesDeriver");
		VIOLATION = factory.createIRI(NAMESPACE, "Violation");
		WARNING = factory.createIRI(NAMESPACE, "Warning");
		XONE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "XoneConstraintComponent");
		XONE_CONSTRAINT_COMPONENT_XONE = factory.createIRI(NAMESPACE, "XoneConstraintComponent-xone");

		ALTERNATIVE_PATH = factory.createIRI(NAMESPACE, "alternativePath");
		AND = factory.createIRI(NAMESPACE, "and");
		ANNOTATION_PROPERTY = factory.createIRI(NAMESPACE, "annotationProperty");
		ANNOTATION_VALUE = factory.createIRI(NAMESPACE, "annotationValue");
		ANNOTATION_VAR_NAME = factory.createIRI(NAMESPACE, "annotationVarName");
		ASK = factory.createIRI(NAMESPACE, "ask");
		CLASS = factory.createIRI(NAMESPACE, "class");
		CLOSED = factory.createIRI(NAMESPACE, "closed");
		CONFORMS = factory.createIRI(NAMESPACE, "conforms");
		CONSTRUCT = factory.createIRI(NAMESPACE, "construct");
		DATATYPE = factory.createIRI(NAMESPACE, "datatype");
		DEACTIVATED = factory.createIRI(NAMESPACE, "deactivated");
		DECLARE = factory.createIRI(NAMESPACE, "declare");
		DEFAULT_VALUE = factory.createIRI(NAMESPACE, "defaultValue");
		DERIVED_VALUES = factory.createIRI(NAMESPACE, "derivedValues");
		DESCRIPTION = factory.createIRI(NAMESPACE, "description");
		DETAIL = factory.createIRI(NAMESPACE, "detail");
		DISJOINT = factory.createIRI(NAMESPACE, "disjoint");
		EQUALS = factory.createIRI(NAMESPACE, "equals");
		FLAGS = factory.createIRI(NAMESPACE, "flags");
		FOCUS_NODE = factory.createIRI(NAMESPACE, "focusNode");
		GROUP = factory.createIRI(NAMESPACE, "group");
		HAS_VALUE = factory.createIRI(NAMESPACE, "hasValue");
		IGNORED_PROPERTIES = factory.createIRI(NAMESPACE, "ignoredProperties");
		IN = factory.createIRI(NAMESPACE, "in");
		INVERSE_PATH = factory.createIRI(NAMESPACE, "inversePath");
		LABEL_TEMPLATE = factory.createIRI(NAMESPACE, "labelTemplate");
		LANGUAGE_IN = factory.createIRI(NAMESPACE, "languageIn");
		LESS_THAN = factory.createIRI(NAMESPACE, "lessThan");
		LESS_THAN_OR_EQUALS = factory.createIRI(NAMESPACE, "lessThanOrEquals");
		MAX_COUNT = factory.createIRI(NAMESPACE, "maxCount");
		MAX_EXCLUSIVE = factory.createIRI(NAMESPACE, "maxExclusive");
		MAX_INCLUSIVE = factory.createIRI(NAMESPACE, "maxInclusive");
		MAX_LENGTH = factory.createIRI(NAMESPACE, "maxLength");
		MESSAGE = factory.createIRI(NAMESPACE, "message");
		MIN_COUNT = factory.createIRI(NAMESPACE, "minCount");
		MIN_EXCLUSIVE = factory.createIRI(NAMESPACE, "minExclusive");
		MIN_INCLUSIVE = factory.createIRI(NAMESPACE, "minInclusive");
		MIN_LENGTH = factory.createIRI(NAMESPACE, "minLength");
		NAME = factory.createIRI(NAMESPACE, "name");
		NAMESPACE_PROP = factory.createIRI(NAMESPACE, "namespace");
		NODE = factory.createIRI(NAMESPACE, "node");
		NODE_KIND_PROP = factory.createIRI(NAMESPACE, "nodeKind");
		NODE_VALIDATOR = factory.createIRI(NAMESPACE, "nodeValidator");
		NOT = factory.createIRI(NAMESPACE, "not");
		ONE_OR_MORE_PATH = factory.createIRI(NAMESPACE, "oneOrMorePath");
		OPTIONAL = factory.createIRI(NAMESPACE, "optional");
		OR = factory.createIRI(NAMESPACE, "or");
		ORDER = factory.createIRI(NAMESPACE, "order");
		PARAMETER_PROP = factory.createIRI(NAMESPACE, "parameter");
		PATH = factory.createIRI(NAMESPACE, "path");
		PATTERN = factory.createIRI(NAMESPACE, "pattern");
		PREFIX_PROP = factory.createIRI(NAMESPACE, "prefix");
		PREFIXES = factory.createIRI(NAMESPACE, "prefixes");
		PROPERTY = factory.createIRI(NAMESPACE, "property");
		PROPERTY_VALIDATOR = factory.createIRI(NAMESPACE, "propertyValidator");
		QUALIFIED_MAX_COUNT = factory.createIRI(NAMESPACE, "qualifiedMaxCount");
		QUALIFIED_MIN_COUNT = factory.createIRI(NAMESPACE, "qualifiedMinCount");
		QUALIFIED_VALUE_SHAPE = factory.createIRI(NAMESPACE, "qualifiedValueShape");
		QUALIFIED_VALUE_SHAPES_DISJOINT = factory.createIRI(NAMESPACE, "qualifiedValueShapesDisjoint");
		RESULT = factory.createIRI(NAMESPACE, "result");
		RESULT_ANNOTATION_PROP = factory.createIRI(NAMESPACE, "resultAnnotation");
		RESULT_MESSAGE = factory.createIRI(NAMESPACE, "resultMessage");
		RESULT_PATH = factory.createIRI(NAMESPACE, "resultPath");
		RESULT_SEVERITY = factory.createIRI(NAMESPACE, "resultSeverity");
		RETURN_TYPE = factory.createIRI(NAMESPACE, "returnType");
		SELECT = factory.createIRI(NAMESPACE, "select");
		SEVERITY_PROP = factory.createIRI(NAMESPACE, "severity");
		SHAPES_GRAPH = factory.createIRI(NAMESPACE, "shapesGraph");
		SHAPES_GRAPH_WELL_FORMED = factory.createIRI(NAMESPACE, "shapesGraphWellFormed");
		SOURCE_CONSTRAINT = factory.createIRI(NAMESPACE, "sourceConstraint");
		SOURCE_CONSTRAINT_COMPONENT = factory.createIRI(NAMESPACE, "sourceConstraintComponent");
		SOURCE_SHAPE = factory.createIRI(NAMESPACE, "sourceShape");
		SPARQL = factory.createIRI(NAMESPACE, "sparql");
		TARGET_PROP = factory.createIRI(NAMESPACE, "target");
		TARGET_CLASS = factory.createIRI(NAMESPACE, "targetClass");
		TARGET_NODE = factory.createIRI(NAMESPACE, "targetNode");
		TARGET_OBJECTS_OF = factory.createIRI(NAMESPACE, "targetObjectsOf");
		TARGET_SUBJECTS_OF = factory.createIRI(NAMESPACE, "targetSubjectsOf");
		UNIQUE_LANG = factory.createIRI(NAMESPACE, "uniqueLang");
		UPDATE = factory.createIRI(NAMESPACE, "update");
		VALIDATOR_PROP = factory.createIRI(NAMESPACE, "validator");
		VALUE = factory.createIRI(NAMESPACE, "value");
		XONE = factory.createIRI(NAMESPACE, "xone");
		ZERO_OR_MORE_PATH = factory.createIRI(NAMESPACE, "zeroOrMorePath");
		ZERO_OR_ONE_PATH = factory.createIRI(NAMESPACE, "zeroOrOnePath");
	}
}
