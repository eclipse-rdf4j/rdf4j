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

/**
 * @version 1.4.0
 */
public class SPIN {

	/**
	 * http://spinrdf.org/spin An RDF Schema that can be used to attach constraints and rules to RDFS classes, and to
	 * encapsulate reusable SPARQL queries into functions and templates.
	 */
	public static final String NAMESPACE = "http://spinrdf.org/spin#";

	public static final String PREFIX = "spin";

	/**
	 * http://spinrdf.org/spin#Function Metaclass for functions that can be used in SPARQL expressions (e.g. FILTER or
	 * BIND). The function themselves are classes that are instances of this metaclass. Function calls are instances of
	 * the function classes, with property values for the arguments.
	 */
	public static IRI FUNCTION_CLASS;

	/**
	 * http://spinrdf.org/spin#Module An abstract building block of a SPARQL system. A Module can take Arguments as
	 * input and applies them on an input RDF Graph. The Arguments should be declared as spin:constraints.
	 */
	public static IRI MODULE_CLASS;

	/**
	 * http://spinrdf.org/spin#body The body of a Function or Template. This points to a Query instance. For Functions,
	 * this is limited to either ASK or SELECT type queries. If the body is the ASK function then the return value is
	 * xsd:boolean. Otherwise, the SELECT query must have a single return variable. The first binding of this SELECT
	 * query will be returned as result of the function call.
	 */
	public static IRI BODY_PROPERTY;

	/**
	 * http://spinrdf.org/spin#TableDataProvider An abstraction of objects that can produce tabular data. This serves as
	 * a base class of spin:SelectTemplate, because SELECT queries can produce tables with columns for each result
	 * variable. However, other types of TableDataProviders are conceivable by other frameworks, and this class may
	 * prove as a useful shared foundation. TableDataProviders can link to definitions of columns via spin:column, and
	 * these definitions can inform rendering engines.
	 */
	public static IRI TABLE_DATA_PROVIDER_CLASS;

	public static IRI CONSTRUCT_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/spin#Template The metaclass of SPIN templates. Templates are classes that are instances of
	 * this class. A template represents a reusable SPARQL query or update request that can be parameterized with
	 * arguments. Templates can be instantiated in places where normally a SPARQL query or update request is used, in
	 * particular as spin:rules and spin:constraints.
	 */
	public static IRI TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/spin#Rule Groups together the kinds of SPARQL commands that can appear as SPIN rules and
	 * constructors: CONSTRUCT, DELETE WHERE and DELETE/INSERT. This class is never to be instantiated directly.
	 */
	public static IRI RULE_CLASS;

	/**
	 * http://spinrdf.org/spin#AskTemplate A SPIN template that wraps an ASK query.
	 */
	public static IRI ASK_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/spin#UpdateTemplate A SPIN template that has an UPDATE command as its body.
	 */
	public static IRI UPDATE_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/spin#RuleProperty The metaclass of spin:rule and its subproperties. spin:RuleProperties can
	 * have additional metadata attached to them.
	 */
	public static IRI RULE_PROPERTY_CLASS;

	/**
	 * http://spinrdf.org/spin#ConstraintViolation An object that can be created by spin:constraints to provide
	 * information about a constraint violation.
	 */
	public static IRI CONSTRAINT_VIOLATION_CLASS;

	/**
	 * http://spinrdf.org/spin#Modules An "artificial" parent class for all Functions and Templates.
	 */
	public static IRI MODULES_CLASS;

	/**
	 * http://spinrdf.org/spin#SelectTemplate A SPIN template that wraps a SELECT query.
	 */
	public static IRI SELECT_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/spin#Column Provides metadata about a column in the result set of a (SPARQL) query, for
	 * example of the body queries of SPIN templates. Columns can define human-readable labels that serve as column
	 * titles, using rdfs:label.
	 */
	public static IRI COLUMN_CLASS;

	/**
	 * http://spinrdf.org/spin#LibraryOntology A marker class that can be attached to base URIs (ontologies) to instruct
	 * SPIN engines that this ontology only contains a library of SPIN declarations. Library Ontologies should be
	 * ignored by SPIN inference engines even if they have been imported by a domain model. For example, a SPIN version
	 * of OWL RL may contain all the OWL RL axioms, attached to owl:Thing, but nothing else. However, when executed,
	 * these axioms should not be executed over themselves, because we don't want the system to reason about the SPIN
	 * triples to speed up things.
	 */
	public static IRI LIBRARY_ONTOLOGY_CLASS;

	public static IRI MAGIC_PROPERTY_CLASS;

	/**
	 * http://spinrdf.org/spin#update Can be used to point from any resource to an Update.
	 */
	public static IRI UPDATE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#command Can be used to link a resource with a SPARQL query or update request
	 * (sp:Command).
	 */
	public static IRI COMMAND_PROPERTY;

	/**
	 * http://spinrdf.org/spin#returnType The return type of a Function, e.g. xsd:string.
	 */
	public static IRI RETURN_TYPE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#systemProperty An "abstract" base property that groups together those system properties
	 * that the user will hardly ever need to see in property trees. This property may be dropped in future versions of
	 * this ontology - right now it's mainly here for convenience.
	 */
	public static IRI SYSTEM_PROPERTY_PROPERTY;

	/**
	 * http://spinrdf.org/spin#column Can link a TableDataProvider (esp. SelectTemplate) with one or more columns that
	 * provide metadata for rendering purposes. Columns can be sorted by their spin:columnIndex (which must align with
	 * the ordering of variables in the SELECT query starting with 0). Not all result variables of the underlying query
	 * need to have a matching spin:Column.
	 */
	public static IRI COLUMN_PROPERTY;

	/**
	 * http://spinrdf.org/spin#symbol The symbol of a function, e.g. "=" for the eq function.
	 */
	public static IRI SYMBOL_PROPERTY;

	/**
	 * http://spinrdf.org/spin#violationRoot The root resource of the violation (often ?this in the constraint body).
	 */
	public static IRI VIOLATION_ROOT_PROPERTY;

	/**
	 * http://spinrdf.org/spin#columnType The datatype or resource type of a spin:Column. For example this is useful as
	 * metadata to inform a rendering engine that numeric columns (e.g. xsd:float) need to be right-aligned.
	 */
	public static IRI COLUMN_TYPE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#nextRuleProperty Can be used to link two sub-properties of spin:rule (or spin:rule
	 * itself) to instruct the SPIN engine to execute one set of rules before another one. The values of the subject
	 * property will be executed before those of the object property.
	 */
	public static IRI NEXT_RULE_PROPERTY_PROPERTY;

	/**
	 * http://spinrdf.org/spin#private Can be set to true to indicate that a SPIN function is only meant to be used as a
	 * helper of other functions, but not directly. Among others, this allows user interfaces to filter out private
	 * functions. Furthermore, it tells potential users of this function that they should avoid using this function, as
	 * it may not be stable.
	 */
	public static IRI PRIVATE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#labelTemplate A template string for displaying instantiations of a module in
	 * human-readable form. The template may contain the argument variable names in curly braces to support
	 * substitution. For example, "The number of values of the {?arg1} property."
	 */
	public static IRI LABEL_TEMPLATE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#violationPath An optional attribute of ConstraintViolations to provide a path expression
	 * from the root resource to the value that is invalid. If this is a IRI then the path represents the predicate of a
	 * subject/predicate combination. Otherwise it should be a blank node of type sp:Path.
	 */
	public static IRI VIOLATION_PATH_PROPERTY;

	/**
	 * http://spinrdf.org/spin#constructor Can be used to attach a "constructor" to a class. A constructor is a SPARQL
	 * CONSTRUCT query or INSERT/DELETE Update operation that can add initial values to the current instance. At
	 * execution time, the variable ?this is bound to the current instance. Tools can call constructors of a class and
	 * its superclasses when an instance of a class has been created. Constructors will also be used to initialize
	 * resources that have received a new rdf:type triple as a result of spin:rules firing.
	 */
	public static IRI CONSTRUCTOR_PROPERTY;

	/**
	 * http://spinrdf.org/spin#abstract Can be set to true to indicate that this module shall not be instantiated.
	 * Abstract modules are only there to organize other modules into hierarchies.
	 */
	public static IRI ABSTRACT_PROPERTY;

	/**
	 * http://spinrdf.org/spin#constraint Links a class with constraints on its instances. The values of this property
	 * are "axioms" expressed as CONSTRUCT or ASK queries where the variable ?this refers to the instances of the
	 * surrounding class. ASK queries must evaluate to false for each member of this class - returning true means that
	 * the instance ?this violates the constraint. CONSTRUCT queries must create instances of spin:ConstraintViolation
	 * to provide details on the reason for the violation.
	 */
	public static IRI CONSTRAINT_PROPERTY;

	/**
	 * http://spinrdf.org/spin#query Can be used to point from any resource to a Query.
	 */
	public static IRI QUERY_PROPERTY;

	/**
	 * http://spinrdf.org/spin#fix Can be used to link a ConstraintViolation with one or more UPDATE Templates that
	 * would help fix the violation.
	 */
	public static IRI FIX_PROPERTY;

	/**
	 * http://spinrdf.org/spin#columnWidth The preferred width of the associated Column, for display purposes. Values in
	 * pixels (rendering engines may multiply the values depending on resolution).
	 */
	public static IRI COLUMN_WIDTH_PROPERTY;

	/**
	 * http://spinrdf.org/spin#violationSource Can be used to link a spin:ConstraintViolation with the query or template
	 * call that caused it. This property is typically filled in automatically by the constraint checking engine and
	 * does not need to be set manually. However, it can be useful to learn more about the origin of a violation.
	 */
	public static IRI VIOLATION_SOURCE_PROPERTY;

	/**
	 * http://spinrdf.org/spin#columnIndex The index of a column (from left to right) starting at 0.
	 */
	public static IRI COLUMN_INDEX_PROPERTY;

	/**
	 * http://spinrdf.org/spin#thisUnbound Can be set to true for SPIN rules and constraints that do not require
	 * pre-binding the variable ?this with all members of the associated class. This flag should only be set to true if
	 * the WHERE clause is sufficiently strong to only bind instances of the associated class, or its subclasses. In
	 * those cases, the engine can greatly improve performance of query execution, because it does not need to add
	 * clauses to narrow down the WHERE clause.
	 */
	public static IRI THIS_UNBOUND_PROPERTY;

	/**
	 * http://spinrdf.org/spin#rulePropertyMaxIterationCount Can be attached to spin:rule (or subclasses thereof) to
	 * instruct a SPIN rules engine that it shall only execute the rules max times. If no value is specified, then the
	 * rules will be executed with no specific limit.
	 */
	public static IRI RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY;

	/**
	 * http://spinrdf.org/spin#imports Can be used to link an RDF graph (usually the instance of owl:Ontology) with a
	 * SPIN library to define constraints. SPIN-aware tools should include the definitions from those libraries for
	 * constraint checking. Using such libraries is a simpler alternative than explicitly importing them using
	 * owl:imports, because it does not force all the SPIN triples into the RDF model.
	 */
	public static IRI IMPORTS_PROPERTY;

	/**
	 * http://spinrdf.org/spin#ConstructTemplates Suggested abstract base class for all ConstructTemplates.
	 */
	public static IRI CONSTRUCT_TEMPLATES_CLASS;

	/**
	 * http://spinrdf.org/spin#Templates Suggested abstract base class for all Templates.
	 */
	public static IRI TEMPLATES_CLASS;

	/**
	 * http://spinrdf.org/spin#eval Evaluates a given SPIN expression or SELECT or ASK query, and returns its result.
	 * The first argument must be the expression in SPIN RDF syntax. All other arguments must come in pairs: first a
	 * property name, and then a value. These name/value pairs will be pre-bound variables for the execution of the
	 * expression.
	 */
	public static final IRI EVAL_FUNCTION;

	@Deprecated
	public static final IRI EVAL_CLASS;

	/**
	 * http://spinrdf.org/spin#Functions An abstract base class for all defined functions. This class mainly serves as a
	 * shared root so that the various instances of the Function metaclass are grouped together.
	 */
	public static IRI FUNCTIONS_CLASS;

	/**
	 * http://spinrdf.org/spin#AskTemplates Suggested abstract base class for all AskTemplates.
	 */
	public static IRI ASK_TEMPLATES_CLASS;

	/**
	 * http://spinrdf.org/spin#SelectTemplates Suggested abstract base class for all SelectTemplates.
	 */
	public static IRI SELECT_TEMPLATES_CLASS;

	/**
	 * http://spinrdf.org/spin#MagicProperties An abstract superclass that can be used to group all spin:MagicProperty
	 * instances under a single parent class.
	 */
	public static IRI MAGIC_PROPERTIES_CLASS;

	/**
	 * http://spinrdf.org/spin#_this A system variable representing the current context instance in a rule or
	 * constraint.
	 */
	public static IRI THIS_CONTEXT_INSTANCE;

	/**
	 * http://spinrdf.org/spin#UpdateTemplates Suggested abstract base class for all UpdateTemplates.
	 */
	public static IRI UPDATE_TEMPLATES_CLASS;

	/**
	 * http://spinrdf.org/spin#rule An inferencing rule attached to a class. Rules are expressed as CONSTRUCT queries or
	 * INSERT/DELETE operations where the variable ?this will be bound to the current instance of the class. These
	 * inferences can be used to derive new values from existing values at the instance.
	 */
	public static IRI RULE_PROPERTY;

	public static final IRI VIOLATION_VALUE_PROPERTY;

	public static final IRI VIOLATION_LEVEL_PROPERTY;

	public static final IRI INFO_VIOLATION_LEVEL;

	public static final IRI WARNING_VIOLATION_LEVEL;

	public static final IRI ERROR_VIOLATION_LEVEL;

	public static final IRI FATAL_VIOLATION_LEVEL;

	public static final IRI ARG1_INSTANCE;

	public static final IRI ARG2_INSTANCE;

	public static final IRI ARG3_INSTANCE;

	public static final IRI ARG4_INSTANCE;

	public static final IRI ARG5_INSTANCE;

	public static final IRI ASK_FUNCTION;

	public static final IRI CONSTRUCT_PROPERTY;

	public static final IRI SELECT_PROPERTY;

	static {
		FUNCTION_CLASS = Vocabularies.createIRI(NAMESPACE, "Function");
		MODULE_CLASS = Vocabularies.createIRI(NAMESPACE, "Module");
		BODY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "body");
		TABLE_DATA_PROVIDER_CLASS = Vocabularies.createIRI(NAMESPACE, "TableDataProvider");
		CONSTRUCT_TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "ConstructTemplate");
		TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "Template");
		RULE_CLASS = Vocabularies.createIRI(NAMESPACE, "Rule");
		ASK_TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "AskTemplate");
		UPDATE_TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "UpdateTemplate");
		RULE_PROPERTY_CLASS = Vocabularies.createIRI(NAMESPACE, "RuleProperty");
		CONSTRAINT_VIOLATION_CLASS = Vocabularies.createIRI(NAMESPACE, "ConstraintViolation");
		MODULES_CLASS = Vocabularies.createIRI(NAMESPACE, "Modules");
		SELECT_TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "SelectTemplate");
		COLUMN_CLASS = Vocabularies.createIRI(NAMESPACE, "Column");
		LIBRARY_ONTOLOGY_CLASS = Vocabularies.createIRI(NAMESPACE, "LibraryOntology");
		MAGIC_PROPERTY_CLASS = Vocabularies.createIRI(NAMESPACE, "MagicProperty");
		UPDATE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "update");
		COMMAND_PROPERTY = Vocabularies.createIRI(NAMESPACE, "command");
		RETURN_TYPE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "returnType");
		SYSTEM_PROPERTY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "systemProperty");
		COLUMN_PROPERTY = Vocabularies.createIRI(NAMESPACE, "column");
		SYMBOL_PROPERTY = Vocabularies.createIRI(NAMESPACE, "symbol");
		VIOLATION_ROOT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "violationRoot");
		COLUMN_TYPE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "columnType");
		NEXT_RULE_PROPERTY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "nextRuleProperty");
		PRIVATE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "private");
		LABEL_TEMPLATE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "labelTemplate");
		VIOLATION_PATH_PROPERTY = Vocabularies.createIRI(NAMESPACE, "violationPath");
		CONSTRUCTOR_PROPERTY = Vocabularies.createIRI(NAMESPACE, "constructor");
		ABSTRACT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "abstract");
		CONSTRAINT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "constraint");
		QUERY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "query");
		FIX_PROPERTY = Vocabularies.createIRI(NAMESPACE, "fix");
		COLUMN_WIDTH_PROPERTY = Vocabularies.createIRI(NAMESPACE, "columnWidth");
		VIOLATION_SOURCE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "violationSource");
		COLUMN_INDEX_PROPERTY = Vocabularies.createIRI(NAMESPACE, "columnIndex");
		THIS_UNBOUND_PROPERTY = Vocabularies.createIRI(NAMESPACE, "thisUnbound");
		RULE_PROPERTY_MAX_ITERATION_COUNT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "rulePropertyMaxIterationCount");
		IMPORTS_PROPERTY = Vocabularies.createIRI(NAMESPACE, "imports");
		CONSTRUCT_TEMPLATES_CLASS = Vocabularies.createIRI(NAMESPACE, "ConstructTemplates");
		TEMPLATES_CLASS = Vocabularies.createIRI(NAMESPACE, "Templates");
		EVAL_CLASS = Vocabularies.createIRI(NAMESPACE, "eval");
		FUNCTIONS_CLASS = Vocabularies.createIRI(NAMESPACE, "Functions");
		ASK_TEMPLATES_CLASS = Vocabularies.createIRI(NAMESPACE, "AskTemplates");
		SELECT_TEMPLATES_CLASS = Vocabularies.createIRI(NAMESPACE, "SelectTemplates");
		MAGIC_PROPERTIES_CLASS = Vocabularies.createIRI(NAMESPACE, "MagicProperties");
		THIS_CONTEXT_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_this");
		UPDATE_TEMPLATES_CLASS = Vocabularies.createIRI(NAMESPACE, "UpdateTemplates");
		RULE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "rule");

		VIOLATION_VALUE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "violationValue");
		VIOLATION_LEVEL_PROPERTY = Vocabularies.createIRI(NAMESPACE, "violationLevel");

		INFO_VIOLATION_LEVEL = Vocabularies.createIRI(NAMESPACE, "Info");
		WARNING_VIOLATION_LEVEL = Vocabularies.createIRI(NAMESPACE, "Warning");
		ERROR_VIOLATION_LEVEL = Vocabularies.createIRI(NAMESPACE, "Error");
		FATAL_VIOLATION_LEVEL = Vocabularies.createIRI(NAMESPACE, "Fatal");

		ARG1_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_arg1");
		ARG2_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_arg2");
		ARG3_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_arg3");
		ARG4_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_arg4");
		ARG5_INSTANCE = Vocabularies.createIRI(NAMESPACE, "_arg5");

		EVAL_FUNCTION = Vocabularies.createIRI(NAMESPACE, "eval");
		ASK_FUNCTION = Vocabularies.createIRI(NAMESPACE, "ask");
		CONSTRUCT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "construct");
		SELECT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "select");
	}
}
