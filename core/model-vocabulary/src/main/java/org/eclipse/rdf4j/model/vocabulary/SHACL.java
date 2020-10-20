/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

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
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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

		ABSTRACT_RESULT = createIRI(NAMESPACE, "AbstractResult");
		AND_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "AndConstraintComponent");
		AND_CONSTRAINT_COMPONENT_AND = createIRI(NAMESPACE, "AndConstraintComponent-and");
		BLANK_NODE = createIRI(NAMESPACE, "BlankNode");
		BLANK_NODE_OR_IRI = createIRI(NAMESPACE, "BlankNodeOrIRI");
		BLANK_NODE_OR_LITERAL = createIRI(NAMESPACE, "BlankNodeOrLiteral");
		CLASS_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "ClassConstraintComponent");
		CLASS_CONSTRAINT_COMPONENT_CLASS = createIRI(NAMESPACE, "ClassConstraintComponent-class");
		CLOSED_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "ClosedConstraintComponent");
		CLOSED_CONSTRAINT_COMPONENT_CLOSED = createIRI(NAMESPACE, "ClosedConstraintComponent-closed");
		CLOSED_CONSTRAINT_COMPONENT_IGNORED_PROPERTIES = createIRI(NAMESPACE,
				"ClosedConstraintComponent-ignoredProperties");
		CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "ConstraintComponent");
		DATATYPE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "DatatypeConstraintComponent");
		DATATYPE_CONSTRAINT_COMPONENT_DATATYPE = createIRI(NAMESPACE,
				"DatatypeConstraintComponent-datatype");
		DERIVED_VALUES_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "DerivedValuesConstraintComponent");
		DISJOINT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "DisjointConstraintComponent");
		DISJOINT_CONSTRAINT_COMPONENT_DISJOINT = createIRI(NAMESPACE,
				"DisjointConstraintComponent-disjoint");
		EQUALS_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "EqualsConstraintComponent");
		EQUALS_CONSTRAINT_COMPONENT_EQUALS = createIRI(NAMESPACE, "EqualsConstraintComponent-equals");
		FUNCTION = createIRI(NAMESPACE, "Function");
		HAS_VALUE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "HasValueConstraintComponent");
		HAS_VALUE_CONSTRAINT_COMPONENT_HAS_VALUE = createIRI(NAMESPACE,
				"HasValueConstraintComponent-hasValue");
		IRI = createIRI(NAMESPACE, "IRI");
		IRI_OR_LITERAL = createIRI(NAMESPACE, "IRIOrLiteral");
		IN_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "InConstraintComponent");
		IN_CONSTRAINT_COMPONENT_IN = createIRI(NAMESPACE, "InConstraintComponent-in");
		INFO = createIRI(NAMESPACE, "Info");
		LANGUAGE_IN_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "LanguageInConstraintComponent");
		LANGUAGE_IN_CONSTRAINT_COMPONENT_LANGUAGE_IN = createIRI(NAMESPACE,
				"LanguageInConstraintComponent-languageIn");
		LESS_THAN_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "LessThanConstraintComponent");
		LESS_THAN_CONSTRAINT_COMPONENT_LESS_THAN = createIRI(NAMESPACE,
				"LessThanConstraintComponent-lessThan");
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT = createIRI(NAMESPACE,
				"LessThanOrEqualsConstraintComponent");
		LESS_THAN_OR_EQUALS_CONSTRAINT_COMPONENT_LESS_THAN_OR_EQUALS = createIRI(NAMESPACE,
				"LessThanOrEqualsConstraintComponent-lessThanOrEquals");
		LITERAL = createIRI(NAMESPACE, "Literal");
		MAX_COUNT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MaxCountConstraintComponent");
		MAX_COUNT_CONSTRAINT_COMPONENT_MAX_COUNT = createIRI(NAMESPACE,
				"MaxCountConstraintComponent-maxCount");
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MaxExclusiveConstraintComponent");
		MAX_EXCLUSIVE_CONSTRAINT_COMPONENT_MAX_EXCLUSIVE = createIRI(NAMESPACE,
				"MaxExclusiveConstraintComponent-maxExclusive");
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MaxInclusiveConstraintComponent");
		MAX_INCLUSIVE_CONSTRAINT_COMPONENT_MAX_INCLUSIVE = createIRI(NAMESPACE,
				"MaxInclusiveConstraintComponent-maxInclusive");
		MAX_LENGTH_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MaxLengthConstraintComponent");
		MAX_LENGTH_CONSTRAINT_COMPONENT_MAX_LENGTH = createIRI(NAMESPACE,
				"MaxLengthConstraintComponent-maxLength");
		MIN_COUNT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MinCountConstraintComponent");
		MIN_COUNT_CONSTRAINT_COMPONENT_MIN_COUNT = createIRI(NAMESPACE,
				"MinCountConstraintComponent-minCount");
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MinExclusiveConstraintComponent");
		MIN_EXCLUSIVE_CONSTRAINT_COMPONENT_MIN_EXCLUSIVE = createIRI(NAMESPACE,
				"MinExclusiveConstraintComponent-minExclusive");
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MinInclusiveConstraintComponent");
		MIN_INCLUSIVE_CONSTRAINT_COMPONENT_MIN_INCLUSIVE = createIRI(NAMESPACE,
				"MinInclusiveConstraintComponent-minInclusive");
		MIN_LENGTH_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "MinLengthConstraintComponent");
		MIN_LENGTH_CONSTRAINT_COMPONENT_MIN_LENGTH = createIRI(NAMESPACE,
				"MinLengthConstraintComponent-minLength");
		NODE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "NodeConstraintComponent");
		NODE_CONSTRAINT_COMPONENT_NODE = createIRI(NAMESPACE, "NodeConstraintComponent-node");
		NODE_KIND = createIRI(NAMESPACE, "NodeKind");
		NODE_KIND_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "NodeKindConstraintComponent");
		NODE_KIND_CONSTRAINT_COMPONENT_NODE_KIND = createIRI(NAMESPACE,
				"NodeKindConstraintComponent-nodeKind");
		NODE_SHAPE = createIRI(NAMESPACE, "NodeShape");
		NOT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "NotConstraintComponent");
		NOT_CONSTRAINT_COMPONENT_NOT = createIRI(NAMESPACE, "NotConstraintComponent-not");
		OR_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "OrConstraintComponent");
		OR_CONSTRAINT_COMPONENT_OR = createIRI(NAMESPACE, "OrConstraintComponent-or");
		PARAMETER = createIRI(NAMESPACE, "Parameter");
		PARAMETERIZABLE = createIRI(NAMESPACE, "Parameterizable");
		PATTERN_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "PatternConstraintComponent");
		PATTERN_CONSTRAINT_COMPONENT_FLAGS = createIRI(NAMESPACE, "PatternConstraintComponent-flags");
		PATTERN_CONSTRAINT_COMPONENT_PATTERN = createIRI(NAMESPACE, "PatternConstraintComponent-pattern");
		PREFIX_DECLARATION = createIRI(NAMESPACE, "PrefixDeclaration");
		PROPERTY_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "PropertyConstraintComponent");
		PROPERTY_CONSTRAINT_COMPONENT_PROPERTY = createIRI(NAMESPACE,
				"PropertyConstraintComponent-property");
		PROPERTY_GROUP = createIRI(NAMESPACE, "PropertyGroup");
		PROPERTY_SHAPE = createIRI(NAMESPACE, "PropertyShape");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MAX_COUNT = createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedMaxCount");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE = createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedValueShape");
		QUALIFIED_MAX_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT = createIRI(NAMESPACE,
				"QualifiedMaxCountConstraintComponent-qualifiedValueShapesDisjoint");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT = createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_MIN_COUNT = createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedMinCount");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPE = createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedValueShape");
		QUALIFIED_MIN_COUNT_CONSTRAINT_COMPONENT_QUALIFIED_VALUE_SHAPES_DISJOINT = createIRI(NAMESPACE,
				"QualifiedMinCountConstraintComponent-qualifiedValueShapesDisjoint");
		RESULT_ANNOTATION = createIRI(NAMESPACE, "ResultAnnotation");
		SPARQL_ASK_EXECUTABLE = createIRI(NAMESPACE, "SPARQLAskExecutable");
		SPARQL_ASK_VALIDATOR = createIRI(NAMESPACE, "SPARQLAskValidator");
		SPARQL_CONSTRAINT = createIRI(NAMESPACE, "SPARQLConstraint");
		SPARQL_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "SPARQLConstraintComponent");
		SPARQL_CONSTRAINT_COMPONENT_SPARQL = createIRI(NAMESPACE, "SPARQLConstraintComponent-sparql");
		SPARQL_CONSTRUCT_EXECUTABLE = createIRI(NAMESPACE, "SPARQLConstructExecutable");
		SPARQL_EXECUTABLE = createIRI(NAMESPACE, "SPARQLExecutable");
		SPARQL_FUNCTION = createIRI(NAMESPACE, "SPARQLFunction");
		SPARQL_SELECT_EXECUTABLE = createIRI(NAMESPACE, "SPARQLSelectExecutable");
		SPARQL_SELECT_VALIDATOR = createIRI(NAMESPACE, "SPARQLSelectValidator");
		SPARQL_TARGET = createIRI(NAMESPACE, "SPARQLTarget");
		SPARQL_TARGET_TYPE = createIRI(NAMESPACE, "SPARQLTargetType");
		SPARQL_UPDATE_EXECUTABLE = createIRI(NAMESPACE, "SPARQLUpdateExecutable");
		SPARQL_VALUES_DERIVER = createIRI(NAMESPACE, "SPARQLValuesDeriver");
		SEVERITY = createIRI(NAMESPACE, "Severity");
		SHAPE = createIRI(NAMESPACE, "Shape");
		TARGET = createIRI(NAMESPACE, "Target");
		TARGET_TYPE = createIRI(NAMESPACE, "TargetType");
		UNIQUE_LANG_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "UniqueLangConstraintComponent");
		UNIQUE_LANG_CONSTRAINT_COMPONENT_UNIQUE_LANG = createIRI(NAMESPACE,
				"UniqueLangConstraintComponent-uniqueLang");
		VALIDATION_REPORT = createIRI(NAMESPACE, "ValidationReport");
		VALIDATION_RESULT = createIRI(NAMESPACE, "ValidationResult");
		VALIDATOR = createIRI(NAMESPACE, "Validator");
		VALUES_DERIVER = createIRI(NAMESPACE, "ValuesDeriver");
		VIOLATION = createIRI(NAMESPACE, "Violation");
		WARNING = createIRI(NAMESPACE, "Warning");
		XONE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "XoneConstraintComponent");
		XONE_CONSTRAINT_COMPONENT_XONE = createIRI(NAMESPACE, "XoneConstraintComponent-xone");

		ALTERNATIVE_PATH = createIRI(NAMESPACE, "alternativePath");
		AND = createIRI(NAMESPACE, "and");
		ANNOTATION_PROPERTY = createIRI(NAMESPACE, "annotationProperty");
		ANNOTATION_VALUE = createIRI(NAMESPACE, "annotationValue");
		ANNOTATION_VAR_NAME = createIRI(NAMESPACE, "annotationVarName");
		ASK = createIRI(NAMESPACE, "ask");
		CLASS = createIRI(NAMESPACE, "class");
		CLOSED = createIRI(NAMESPACE, "closed");
		CONFORMS = createIRI(NAMESPACE, "conforms");
		CONSTRUCT = createIRI(NAMESPACE, "construct");
		DATATYPE = createIRI(NAMESPACE, "datatype");
		DEACTIVATED = createIRI(NAMESPACE, "deactivated");
		DECLARE = createIRI(NAMESPACE, "declare");
		DEFAULT_VALUE = createIRI(NAMESPACE, "defaultValue");
		DERIVED_VALUES = createIRI(NAMESPACE, "derivedValues");
		DESCRIPTION = createIRI(NAMESPACE, "description");
		DETAIL = createIRI(NAMESPACE, "detail");
		DISJOINT = createIRI(NAMESPACE, "disjoint");
		EQUALS = createIRI(NAMESPACE, "equals");
		FLAGS = createIRI(NAMESPACE, "flags");
		FOCUS_NODE = createIRI(NAMESPACE, "focusNode");
		GROUP = createIRI(NAMESPACE, "group");
		HAS_VALUE = createIRI(NAMESPACE, "hasValue");
		IGNORED_PROPERTIES = createIRI(NAMESPACE, "ignoredProperties");
		IN = createIRI(NAMESPACE, "in");
		INVERSE_PATH = createIRI(NAMESPACE, "inversePath");
		LABEL_TEMPLATE = createIRI(NAMESPACE, "labelTemplate");
		LANGUAGE_IN = createIRI(NAMESPACE, "languageIn");
		LESS_THAN = createIRI(NAMESPACE, "lessThan");
		LESS_THAN_OR_EQUALS = createIRI(NAMESPACE, "lessThanOrEquals");
		MAX_COUNT = createIRI(NAMESPACE, "maxCount");
		MAX_EXCLUSIVE = createIRI(NAMESPACE, "maxExclusive");
		MAX_INCLUSIVE = createIRI(NAMESPACE, "maxInclusive");
		MAX_LENGTH = createIRI(NAMESPACE, "maxLength");
		MESSAGE = createIRI(NAMESPACE, "message");
		MIN_COUNT = createIRI(NAMESPACE, "minCount");
		MIN_EXCLUSIVE = createIRI(NAMESPACE, "minExclusive");
		MIN_INCLUSIVE = createIRI(NAMESPACE, "minInclusive");
		MIN_LENGTH = createIRI(NAMESPACE, "minLength");
		NAME = createIRI(NAMESPACE, "name");
		NAMESPACE_PROP = createIRI(NAMESPACE, "namespace");
		NODE = createIRI(NAMESPACE, "node");
		NODE_KIND_PROP = createIRI(NAMESPACE, "nodeKind");
		NODE_VALIDATOR = createIRI(NAMESPACE, "nodeValidator");
		NOT = createIRI(NAMESPACE, "not");
		ONE_OR_MORE_PATH = createIRI(NAMESPACE, "oneOrMorePath");
		OPTIONAL = createIRI(NAMESPACE, "optional");
		OR = createIRI(NAMESPACE, "or");
		ORDER = createIRI(NAMESPACE, "order");
		PARAMETER_PROP = createIRI(NAMESPACE, "parameter");
		PATH = createIRI(NAMESPACE, "path");
		PATTERN = createIRI(NAMESPACE, "pattern");
		PREFIX_PROP = createIRI(NAMESPACE, "prefix");
		PREFIXES = createIRI(NAMESPACE, "prefixes");
		PROPERTY = createIRI(NAMESPACE, "property");
		PROPERTY_VALIDATOR = createIRI(NAMESPACE, "propertyValidator");
		QUALIFIED_MAX_COUNT = createIRI(NAMESPACE, "qualifiedMaxCount");
		QUALIFIED_MIN_COUNT = createIRI(NAMESPACE, "qualifiedMinCount");
		QUALIFIED_VALUE_SHAPE = createIRI(NAMESPACE, "qualifiedValueShape");
		QUALIFIED_VALUE_SHAPES_DISJOINT = createIRI(NAMESPACE, "qualifiedValueShapesDisjoint");
		RESULT = createIRI(NAMESPACE, "result");
		RESULT_ANNOTATION_PROP = createIRI(NAMESPACE, "resultAnnotation");
		RESULT_MESSAGE = createIRI(NAMESPACE, "resultMessage");
		RESULT_PATH = createIRI(NAMESPACE, "resultPath");
		RESULT_SEVERITY = createIRI(NAMESPACE, "resultSeverity");
		RETURN_TYPE = createIRI(NAMESPACE, "returnType");
		SELECT = createIRI(NAMESPACE, "select");
		SEVERITY_PROP = createIRI(NAMESPACE, "severity");
		SHAPES_GRAPH = createIRI(NAMESPACE, "shapesGraph");
		SHAPES_GRAPH_WELL_FORMED = createIRI(NAMESPACE, "shapesGraphWellFormed");
		SOURCE_CONSTRAINT = createIRI(NAMESPACE, "sourceConstraint");
		SOURCE_CONSTRAINT_COMPONENT = createIRI(NAMESPACE, "sourceConstraintComponent");
		SOURCE_SHAPE = createIRI(NAMESPACE, "sourceShape");
		SPARQL = createIRI(NAMESPACE, "sparql");
		TARGET_PROP = createIRI(NAMESPACE, "target");
		TARGET_CLASS = createIRI(NAMESPACE, "targetClass");
		TARGET_NODE = createIRI(NAMESPACE, "targetNode");
		TARGET_OBJECTS_OF = createIRI(NAMESPACE, "targetObjectsOf");
		TARGET_SUBJECTS_OF = createIRI(NAMESPACE, "targetSubjectsOf");
		UNIQUE_LANG = createIRI(NAMESPACE, "uniqueLang");
		UPDATE = createIRI(NAMESPACE, "update");
		VALIDATOR_PROP = createIRI(NAMESPACE, "validator");
		VALUE = createIRI(NAMESPACE, "value");
		XONE = createIRI(NAMESPACE, "xone");
		ZERO_OR_MORE_PATH = createIRI(NAMESPACE, "zeroOrMorePath");
		ZERO_OR_ONE_PATH = createIRI(NAMESPACE, "zeroOrOnePath");
	}
}
