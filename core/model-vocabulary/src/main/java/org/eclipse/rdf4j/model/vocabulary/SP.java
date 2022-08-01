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
public class SP {

	/**
	 * http://spinrdf.org/sp An RDF Schema to syntactically represent SPARQL queries (including SPARQL UPDATE) as RDF
	 * triples.
	 */
	public static final String NAMESPACE = "http://spinrdf.org/sp#";

	public static final String PREFIX = "sp";

	/**
	 * http://spinrdf.org/sp#Path The base class of SPARQL property path expressions. Paths are used by sp:TriplePath
	 * triple paths.
	 */
	public static final IRI PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#SystemClass An "artificial" root class that groups all SP classes. This makes them look
	 * much less overwhelming in UI tools. Typical end users don't need to see those classes anyway.
	 */
	public static final IRI SYSTEM_CLASS;

	/**
	 * http://spinrdf.org/sp#Asc Marker to indicate ascending order.
	 */
	public static final IRI ASC_CLASS;

	/**
	 * http://spinrdf.org/sp#OrderByCondition An abstract base class for ascending or descending order conditions.
	 * Instances of this class (typically bnodes) must have a value for expression to point to the actual values.
	 */
	public static final IRI ORDER_BY_CONDITION_CLASS;

	/**
	 * http://spinrdf.org/sp#Sum Represents sum aggregations, e.g. SELECT SUM(?varName)...
	 */
	public static final IRI SUM_CLASS;

	/**
	 * http://spinrdf.org/sp#Aggregation Base class of aggregation types (not part of the SPARQL 1.0 standard but
	 * supported by ARQ and other engines).
	 */
	public static final IRI AGGREGATION_CLASS;

	/**
	 * http://spinrdf.org/sp#Union A UNION group.
	 */
	public static final IRI UNION_CLASS;

	/**
	 * http://spinrdf.org/sp#ElementGroup Abstract base class of group patterns.
	 */
	public static final IRI ELEMENT_GROUP_CLASS;

	/**
	 * http://spinrdf.org/sp#TriplePattern A triple pattern used in the body of a query.
	 */
	public static final IRI TRIPLE_PATTERN_CLASS;

	/**
	 * http://spinrdf.org/sp#Element An abstract base class for all pattern elements.
	 */
	public static final IRI ELEMENT_CLASS;

	/**
	 * http://spinrdf.org/sp#Triple A base class for TriplePattern and TripleTemplate. This basically specifies that
	 * subject, predicate and object must be present.
	 */
	public static final IRI TRIPLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Load A LOAD Update operation. The document to load is specified using sp:document, and the
	 * (optional) target graph using sp:into.
	 */
	public static final IRI LOAD_CLASS;

	/**
	 * http://spinrdf.org/sp#Update Abstract base class to group the various SPARQL UPDATE commands.
	 */
	public static final IRI UPDATE_CLASS;

	/**
	 * http://spinrdf.org/sp#DeleteData An Update operation to delete specific triples. The graph triples are
	 * represented using sp:data, which points to an rdf:List of sp:Triples or sp:NamedGraphs.
	 */
	public static final IRI DELETE_DATA_CLASS;

	/**
	 * http://spinrdf.org/sp#Desc Marker to indicate descending order.
	 */
	public static final IRI DESC_CLASS;

	/**
	 * http://spinrdf.org/sp#TripleTemplate A prototypical triple used as template in the head of a Construct query. May
	 * contain variables.
	 */
	public static final IRI TRIPLE_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/sp#Max Represents MAX aggregations.
	 */
	public static final IRI MAX_CLASS;

	/**
	 * http://spinrdf.org/sp#Insert Deprecated - use sp:Modify instead. Represents a INSERT INTO (part of SPARQL UPDATE
	 * language). The graph IRIs are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:insertPattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static final IRI INSERT_CLASS;

	/**
	 * http://spinrdf.org/sp#Modify Represents a MODIFY (part of SPARQL UPDATE language). The graph IRIs are stored in
	 * sp:graphIRI. The template patterns are stored in sp:deletePattern and sp:insertPattern. The WHERE clause is
	 * represented using sp:where.
	 */
	public static final IRI MODIFY_CLASS;

	/**
	 * http://spinrdf.org/sp#Insert Deprecated - use sp:Modify instead. Represents a INSERT INTO (part of SPARQL UPDATE
	 * language). The graph IRIs are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:insertPattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static final IRI Insert;

	/**
	 * http://spinrdf.org/sp#Avg Represents AVG aggregations.
	 */
	public static final IRI AVG_CLASS;

	/**
	 * http://spinrdf.org/sp#TriplePath Similar to a TriplePattern, but with a path expression as its predicate. For
	 * example, this can be used to express transitive sub-class relationships (?subClass rdfs:subClassOf* ?superClass).
	 */
	public static final IRI TRIPLE_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Tuple Abstract base class for things that have subject and object.
	 */
	public static final IRI TUPLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Let Deprecated: use sp:Bind instead. A variable assignment (LET (?<varName> :=
	 * <expression>)). Not part of the SPARQL 1.0 standard, but (for example) ARQ.
	 */
	@Deprecated
	public static final IRI LET_CLASS;

	/**
	 * http://spinrdf.org/sp#Bind A BIND element.
	 */
	public static final IRI BIND_CLASS;

	/**
	 * http://spinrdf.org/sp#Let Deprecated: use sp:Bind instead. A variable assignment (LET (?<varName> :=
	 * <expression>)). Not part of the SPARQL 1.0 standard, but (for example) ARQ.
	 */
	@Deprecated
	public static final IRI Let;

	/**
	 * http://spinrdf.org/sp#ElementList A list of Elements. This class is never instantiated directly as SPIN will use
	 * plain rdf:Lists to store element lists.
	 */
	public static final IRI ELEMENT_LIST_CLASS;

	/**
	 * http://spinrdf.org/sp#SubQuery A nested SELECT query inside of an element list. The query is stored in sp:query.
	 */
	public static final IRI SUB_QUERY_CLASS;

	/**
	 * http://spinrdf.org/sp#Delete Deprecated - use sp:Modify instead. Represents a DELETE FROM (part of SPARQL UPDATE
	 * language). The graph IRIs are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:deletePattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static final IRI DELETE_CLASS;

	/**
	 * http://spinrdf.org/sp#Delete Deprecated - use sp:Modify instead. Represents a DELETE FROM (part of SPARQL UPDATE
	 * language). The graph IRIs are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:deletePattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static final IRI Delete;

	/**
	 * http://spinrdf.org/sp#Min Represents MIN aggregations.
	 */
	public static final IRI MIN_CLASS;

	/**
	 * http://spinrdf.org/sp#Optional An optional element in a query.
	 */
	public static final IRI OPTIONAL_CLASS;

	/**
	 * http://spinrdf.org/sp#AltPath An alternative path with the union of sp:path1 and sp:path2.
	 */
	public static final IRI ALT_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Count Counts the number of times a variable is used. The variable is stored in the variable
	 * property. This might be left blank to indicate COUNT(*).
	 */
	public static final IRI COUNT_CLASS;

	/**
	 * http://spinrdf.org/sp#ReversePath A path with reversed direction.
	 */
	public static final IRI REVERSE_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Construct A CONSTRUCT-type query that can be used to construct new triples from template
	 * triples (head) that use variable bindings from the match patterns (body).
	 */
	public static final IRI CONSTRUCT_CLASS;

	/**
	 * http://spinrdf.org/sp#Query Abstract base class of the various types of supported queries. Common to all types of
	 * queries is that they can have a body ("WHERE clause").
	 */
	public static final IRI QUERY_CLASS;

	/**
	 * http://spinrdf.org/sp#Variable A variable mentioned in a Triple or expression. Variables are often blank nodes
	 * with the variable name stored in ts:name. Variables can also be supplied with a IRI in which case the system will
	 * attempt to reuse the same variable instance across multiple query definitions.
	 */
	public static final IRI VARIABLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Ask An ASK query that returns true if the condition in the body is met by at least one
	 * result set.
	 */
	public static final IRI ASK_CLASS;

	/**
	 * http://spinrdf.org/sp#ModPath A modified path such as rdfs:subClassOf*.
	 */
	public static final IRI MOD_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Create An Update operation that creates a new empty graph with a name specified by
	 * sp:graphIRI. May have sp:silent set to true.
	 */
	public static final IRI CREATE_CLASS;

	/**
	 * http://spinrdf.org/sp#NamedGraph A named Graph element such as GRAPH <IRI> {...}.
	 */
	public static final IRI NAMED_GRAPH_CLASS;

	/**
	 * http://spinrdf.org/sp#Command A shared superclass for sp:Query and sp:Update that can be used to specify that the
	 * range of property can be either one.
	 */
	public static final IRI COMMAND_CLASS;

	public static final IRI REVERSE_LINK_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#NotExists A NOT EXISTS element group (ARQ only).
	 */
	public static final IRI NOT_EXISTS_CLASS;

	/**
	 * http://spinrdf.org/sp#Drop An Update operation that removes a specified graph from the Graph Store. Must specify
	 * the graph using sp:graphIRI, or sp:default, sp:named or sp:all. May have the SILENT flag, encoded using
	 * sp:silent.
	 */
	public static final IRI DROP_CLASS;

	/**
	 * http://spinrdf.org/sp#InsertData An Update operation to insert specific triples. The graph triples are
	 * represented using sp:data, which points to an rdf:List of sp:Triples or sp:NamedGraphs.
	 */
	public static final IRI INSERT_DATA_CLASS;

	/**
	 * http://spinrdf.org/sp#DeleteWhere An Update operation where the triples matched by the WHERE clause (sp:where)
	 * will be the triples deleted.
	 */
	public static final IRI DELETE_WHERE_CLASS;

	/**
	 * http://spinrdf.org/sp#Service A SERVICE call that matches a nested sub-pattern against a SPARQL end point
	 * specified by a IRI.
	 */
	public static final IRI SERVICE_CLASS;

	/**
	 * http://spinrdf.org/sp#Select A SELECT-type query that returns variable bindings as its result.
	 */
	public static final IRI SELECT_CLASS;

	/**
	 * http://spinrdf.org/sp#Filter A constraint element that evaluates a given expression to true or false.
	 */
	public static final IRI FILTER_CLASS;

	/**
	 * http://spinrdf.org/sp#Minus A MINUS element group.
	 */
	public static final IRI MINUS_CLASS;

	/**
	 * http://spinrdf.org/sp#Clear An Update operation that removes all triples from a specified graph. Must specify the
	 * graph using sp:graphIRI, or sp:default, sp:named or sp:all. May have the SILENT flag, encoded using sp:silent.
	 */
	public static final IRI CLEAR_CLASS;

	/**
	 * http://spinrdf.org/sp#Describe A DESCRIBE-type Query.
	 */
	public static final IRI DESCRIBE_CLASS;

	/**
	 * http://spinrdf.org/sp#SeqPath A sequence of multiple paths.
	 */
	public static final IRI SEQ_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#arg5 The fifth argument of a function call. Further arguments are not common in SPARQL,
	 * therefore no sp:arg6, etc are defined here. However, they can be created if needed.
	 */
	public static final IRI ARG5_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg Abstract superproperty for the enumerated arg1, arg2 etc.
	 */
	public static final IRI ARG_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path1 The first child path of a property path. Used by sp:AltPath and sp:SeqPath.
	 */
	public static final IRI PATH1_PROPERTY;

	/**
	 * http://spinrdf.org/sp#systemProperty An abstract base proprerty that groups together the SP system properties.
	 * Users typically don't need to see them anyway.
	 */
	public static final IRI SYSTEM_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg1 The first argument of a function call.
	 */
	public static final IRI ARG1_PROPERTY;

	/**
	 * http://spinrdf.org/sp#default Used in DROP and CLEAR.
	 */
	public static final IRI DEFAULT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#object An RDF Node or Variable describing the object of a triple.
	 */
	public static final IRI OBJECT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#graphNameNode The name (IRI or Variable) of a NamedGraph.
	 */
	public static final IRI GRAPH_NAME_NODE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#varName The name of a Variable.
	 */
	public static final IRI VAR_NAME_PROPERTY;

	/**
	 * http://spinrdf.org/sp#named Used in DROP and CLEAR.
	 */
	public static final IRI NAMED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#as Points to a Variable used in an AS statement such as COUNT aggregates.
	 */
	@Deprecated
	public static final IRI AS_PROPERTY;

	/**
	 * http://spinrdf.org/sp#distinct A marker property to indicate that a Select query is of type SELECT DISTINCT.
	 */
	public static final IRI DISTINCT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path2 The second child path of a property path. Used by sp:AltPath and sp:SeqPath.
	 */
	public static final IRI PATH2_PROPERTY;

	/**
	 * http://spinrdf.org/sp#orderBy Links a query with an ORDER BY clause where the values are rdf:List containing
	 * OrderByConditions or expressions. While the domain of this property is sp:Query, only Describe and Select queries
	 * can have values of it.
	 */
	public static final IRI ORDER_BY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#variable The variable of a Bind element.
	 */
	public static final IRI VARIABLE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg4 The forth argument of a function call.
	 */
	public static final IRI ARG4_PROPERTY;

	public static final IRI SILENT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#having Points from a SELECT query to a list of HAVING expressions.
	 */
	public static final IRI HAVING_PROPERTY;

	/**
	 * http://spinrdf.org/sp#query Links a SubQuery resource with the nested Query.
	 */
	public static final IRI QUERY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#groupBy Points from a Query to the list of GROUP BY expressions.
	 */
	public static final IRI GROUP_BY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#graphIRI Points to graph names (IRIs) in various sp:Update operations.
	 */
	public static final IRI GRAPH_IRI_PROPERTY;

	/**
	 * http://spinrdf.org/sp#limit The LIMIT solution modifier of a Query.
	 */
	public static final IRI LIMIT_PROPERTY;

	public static final IRI USING_PROPERTY;

	/**
	 * http://spinrdf.org/sp#templates Points to a list of TripleTemplates that form the head of a Construct query.
	 */
	public static final IRI TEMPLATES_PROPERTY;

	/**
	 * http://spinrdf.org/sp#resultNodes Contains the result nodes (IRI resources or Variables) of a Describe query.
	 */
	public static final IRI RESULT_NODES_PROPERTY;

	public static final IRI USING_NAMED_PROPERTY;

	public static final IRI DATA_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg3 The third argument of a function call.
	 */
	public static final IRI ARG3_PROPERTY;

	/**
	 * http://spinrdf.org/sp#reduced A property with true to indicate that a Select query has a REDUCED flag.
	 */
	public static final IRI REDUCED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#subPath The child path of a property path expression. This is used by ReversePath and
	 * ModPath.
	 */
	public static final IRI SUB_PATH_PROPERTY;

	/**
	 * http://spinrdf.org/sp#into The (optional) target of a LOAD Update operation.
	 */
	public static final IRI INTO_PROPERTY;

	public static final IRI WITH_PROPERTY;

	/**
	 * http://spinrdf.org/sp#serviceURI Used by sp:Service to specify the IRI of the SPARQL end point to invoke. Must
	 * point to a IRI node.
	 */
	public static final IRI SERVICE_URI_PROPERTY;

	/**
	 * http://spinrdf.org/sp#document The IRI of the document to load using a LOAD Update operation.
	 */
	public static final IRI DOCUMENT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#where The WHERE clause of a Query.
	 */
	public static final IRI WHERE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#resultVariables An rdf:List of variables that are returned by a Select query.
	 */
	public static final IRI RESULT_VARIABLES_PROPERTY;

	/**
	 * http://spinrdf.org/sp#text Can be attached to sp:Queries to store a textual representation of the query. This can
	 * be useful for tools that do not have a complete SPIN Syntax parser available.
	 */
	public static final IRI TEXT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path Points from a TriplePath to its path.
	 */
	public static final IRI PATH_PROPERTY;

	public static final IRI MOD_MAX_PROPERTY;

	/**
	 * http://spinrdf.org/sp#predicate A resource or Variable describing the predicate of a triple.
	 */
	public static final IRI PREDICATE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#elements Points to an ElementList, for example in an Optional element.
	 */
	public static final IRI ELEMENTS_PROPERTY;

	public static final IRI NODE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#fromNamed Specifies a named RDF Dataset used by a Query (FROM NAMED syntax in SPARQL).
	 * Values of this property must be IRI resources.
	 */
	public static final IRI FROM_NAMED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg2 The second argument of a function call.
	 */
	public static final IRI ARG2_PROPERTY;

	/**
	 * http://spinrdf.org/sp#subject A resource or Variable describing the subject of a triple.
	 */
	public static final IRI SUBJECT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#expression Points to an expression, for example in a Filter or Assignment.
	 */
	public static final IRI EXPRESSION_PROPERTY;

	/**
	 * http://spinrdf.org/sp#deletePattern Points to a list of sp:TripleTemplates and sp:NamedGraphs in a modify
	 * operation.
	 */
	public static final IRI DELETE_PATTERN_PROPERTY;

	/**
	 * http://spinrdf.org/sp#all Used in DROP and CLEAR.
	 */
	public static final IRI ALL_PROPERTY;

	/**
	 * http://spinrdf.org/sp#offset The OFFSET solution modifier of a Query.
	 */
	public static final IRI OFFSET_PROPERTY;

	/**
	 * http://spinrdf.org/sp#from Specifies an RDF Dataset used by a Query (FROM syntax in SPARQL). Values of this
	 * property must be IRI resources.
	 */
	public static final IRI FROM_PROPERTY;

	public static final IRI MOD_MIN_PROPERTY;

	/**
	 * http://spinrdf.org/sp#insertPattern Points to a list of sp:TripleTemplates or sp:NamedGraphs in a modify command.
	 */
	public static final IRI INSERT_PATTERN_PROPERTY;

	public static final IRI VALUES_CLASS;

	public static final IRI BINDINGS_PROPERTY;

	public static final IRI VAR_NAMES_PROPERTY;

	public static final IRI UNDEF;

	public static final IRI GROUP_CONCAT_CLASS;

	public static final IRI SAMPLE_CLASS;

	// "The SPIN RDF Syntax provides standard URIs for the built-in functions
	// and operators of the SPARQL language.
	// For example, sp:gt represents the > operator."
	public static final IRI AND;

	public static final IRI OR;

	public static final IRI ADD;

	public static final IRI SUB;

	public static final IRI MUL;

	public static final IRI DIVIDE;

	public static final IRI EQ;

	public static final IRI NE;

	public static final IRI LT;

	public static final IRI LE;

	public static final IRI GE;

	public static final IRI GT;

	public static final IRI NOT;

	public static final IRI EXISTS;

	public static final IRI NOT_EXISTS;

	public static final IRI BOUND;

	public static final IRI IF;

	public static final IRI COALESCE;

	public static final IRI IS_IRI;

	public static final IRI IS_URI;

	public static final IRI IS_BLANK;

	public static final IRI IS_LITERAL;

	public static final IRI IS_NUMERIC;

	public static final IRI STR;

	public static final IRI LANG;

	public static final IRI DATATYPE;

	public static final IRI BNODE;

	public static final IRI REGEX;

	public static final IRI IRI;

	public static final IRI URI;

	static {
		PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "Path");
		SYSTEM_CLASS = Vocabularies.createIRI(NAMESPACE, "SystemClass");
		ASC_CLASS = Vocabularies.createIRI(NAMESPACE, "Asc");
		ORDER_BY_CONDITION_CLASS = Vocabularies.createIRI(NAMESPACE, "OrderByCondition");
		SUM_CLASS = Vocabularies.createIRI(NAMESPACE, "Sum");
		AGGREGATION_CLASS = Vocabularies.createIRI(NAMESPACE, "Aggregation");
		UNION_CLASS = Vocabularies.createIRI(NAMESPACE, "Union");
		ELEMENT_GROUP_CLASS = Vocabularies.createIRI(NAMESPACE, "ElementGroup");
		TRIPLE_PATTERN_CLASS = Vocabularies.createIRI(NAMESPACE, "TriplePattern");
		ELEMENT_CLASS = Vocabularies.createIRI(NAMESPACE, "Element");
		TRIPLE_CLASS = Vocabularies.createIRI(NAMESPACE, "Triple");
		LOAD_CLASS = Vocabularies.createIRI(NAMESPACE, "Load");
		UPDATE_CLASS = Vocabularies.createIRI(NAMESPACE, "Update");
		DELETE_DATA_CLASS = Vocabularies.createIRI(NAMESPACE, "DeleteData");
		DESC_CLASS = Vocabularies.createIRI(NAMESPACE, "Desc");
		TRIPLE_TEMPLATE_CLASS = Vocabularies.createIRI(NAMESPACE, "TripleTemplate");
		MAX_CLASS = Vocabularies.createIRI(NAMESPACE, "Max");
		INSERT_CLASS = Vocabularies.createIRI(NAMESPACE, "Insert");
		Insert = INSERT_CLASS;
		MODIFY_CLASS = Vocabularies.createIRI(NAMESPACE, "Modify");
		AVG_CLASS = Vocabularies.createIRI(NAMESPACE, "Avg");
		TRIPLE_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "TriplePath");
		TUPLE_CLASS = Vocabularies.createIRI(NAMESPACE, "Tuple");
		LET_CLASS = Vocabularies.createIRI(NAMESPACE, "Let");
		BIND_CLASS = Vocabularies.createIRI(NAMESPACE, "Bind");
		Let = LET_CLASS;
		ELEMENT_LIST_CLASS = Vocabularies.createIRI(NAMESPACE, "ElementList");
		SUB_QUERY_CLASS = Vocabularies.createIRI(NAMESPACE, "SubQuery");
		DELETE_CLASS = Vocabularies.createIRI(NAMESPACE, "Delete");
		MIN_CLASS = Vocabularies.createIRI(NAMESPACE, "Min");
		OPTIONAL_CLASS = Vocabularies.createIRI(NAMESPACE, "Optional");
		Delete = DELETE_CLASS;
		ALT_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "AltPath");
		COUNT_CLASS = Vocabularies.createIRI(NAMESPACE, "Count");
		REVERSE_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "ReversePath");
		CONSTRUCT_CLASS = Vocabularies.createIRI(NAMESPACE, "Construct");
		QUERY_CLASS = Vocabularies.createIRI(NAMESPACE, "Query");
		VARIABLE_CLASS = Vocabularies.createIRI(NAMESPACE, "Variable");
		ASK_CLASS = Vocabularies.createIRI(NAMESPACE, "Ask");
		MOD_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "ModPath");
		CREATE_CLASS = Vocabularies.createIRI(NAMESPACE, "Create");
		NAMED_GRAPH_CLASS = Vocabularies.createIRI(NAMESPACE, "NamedGraph");
		COMMAND_CLASS = Vocabularies.createIRI(NAMESPACE, "Command");
		REVERSE_LINK_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "ReverseLinkPath");
		NOT_EXISTS_CLASS = Vocabularies.createIRI(NAMESPACE, "NotExists");
		DROP_CLASS = Vocabularies.createIRI(NAMESPACE, "Drop");
		INSERT_DATA_CLASS = Vocabularies.createIRI(NAMESPACE, "InsertData");
		DELETE_WHERE_CLASS = Vocabularies.createIRI(NAMESPACE, "DeleteWhere");
		SERVICE_CLASS = Vocabularies.createIRI(NAMESPACE, "Service");
		SELECT_CLASS = Vocabularies.createIRI(NAMESPACE, "Select");
		FILTER_CLASS = Vocabularies.createIRI(NAMESPACE, "Filter");
		MINUS_CLASS = Vocabularies.createIRI(NAMESPACE, "Minus");
		CLEAR_CLASS = Vocabularies.createIRI(NAMESPACE, "Clear");
		DESCRIBE_CLASS = Vocabularies.createIRI(NAMESPACE, "Describe");
		SEQ_PATH_CLASS = Vocabularies.createIRI(NAMESPACE, "SeqPath");
		ARG5_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg5");
		ARG_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg");
		PATH1_PROPERTY = Vocabularies.createIRI(NAMESPACE, "path1");
		SYSTEM_PROPERTY = Vocabularies.createIRI(NAMESPACE, "systemProperty");
		ARG1_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg1");
		DEFAULT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "default");
		OBJECT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "object");
		GRAPH_NAME_NODE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "graphNameNode");
		VAR_NAME_PROPERTY = Vocabularies.createIRI(NAMESPACE, "varName");
		NAMED_PROPERTY = Vocabularies.createIRI(NAMESPACE, "named");
		AS_PROPERTY = Vocabularies.createIRI(NAMESPACE, "as");
		DISTINCT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "distinct");
		PATH2_PROPERTY = Vocabularies.createIRI(NAMESPACE, "path2");
		ORDER_BY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "orderBy");
		VARIABLE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "variable");
		ARG4_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg4");
		SILENT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "silent");
		HAVING_PROPERTY = Vocabularies.createIRI(NAMESPACE, "having");
		QUERY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "query");
		GROUP_BY_PROPERTY = Vocabularies.createIRI(NAMESPACE, "groupBy");
		GRAPH_IRI_PROPERTY = Vocabularies.createIRI(NAMESPACE, "graphIRI");
		LIMIT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "limit");
		USING_PROPERTY = Vocabularies.createIRI(NAMESPACE, "using");
		TEMPLATES_PROPERTY = Vocabularies.createIRI(NAMESPACE, "templates");
		RESULT_NODES_PROPERTY = Vocabularies.createIRI(NAMESPACE, "resultNodes");
		USING_NAMED_PROPERTY = Vocabularies.createIRI(NAMESPACE, "usingNamed");
		DATA_PROPERTY = Vocabularies.createIRI(NAMESPACE, "data");
		ARG3_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg3");
		REDUCED_PROPERTY = Vocabularies.createIRI(NAMESPACE, "reduced");
		SUB_PATH_PROPERTY = Vocabularies.createIRI(NAMESPACE, "subPath");
		INTO_PROPERTY = Vocabularies.createIRI(NAMESPACE, "into");
		WITH_PROPERTY = Vocabularies.createIRI(NAMESPACE, "with");
		SERVICE_URI_PROPERTY = Vocabularies.createIRI(NAMESPACE, "serviceURI");
		DOCUMENT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "document");
		WHERE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "where");
		RESULT_VARIABLES_PROPERTY = Vocabularies.createIRI(NAMESPACE, "resultVariables");
		TEXT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "text");
		PATH_PROPERTY = Vocabularies.createIRI(NAMESPACE, "path");
		MOD_MAX_PROPERTY = Vocabularies.createIRI(NAMESPACE, "modMax");
		PREDICATE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "predicate");
		ELEMENTS_PROPERTY = Vocabularies.createIRI(NAMESPACE, "elements");
		NODE_PROPERTY = Vocabularies.createIRI(NAMESPACE, "node");
		FROM_NAMED_PROPERTY = Vocabularies.createIRI(NAMESPACE, "fromNamed");
		ARG2_PROPERTY = Vocabularies.createIRI(NAMESPACE, "arg2");
		SUBJECT_PROPERTY = Vocabularies.createIRI(NAMESPACE, "subject");
		EXPRESSION_PROPERTY = Vocabularies.createIRI(NAMESPACE, "expression");
		DELETE_PATTERN_PROPERTY = Vocabularies.createIRI(NAMESPACE, "deletePattern");
		ALL_PROPERTY = Vocabularies.createIRI(NAMESPACE, "all");
		OFFSET_PROPERTY = Vocabularies.createIRI(NAMESPACE, "offset");
		FROM_PROPERTY = Vocabularies.createIRI(NAMESPACE, "from");
		MOD_MIN_PROPERTY = Vocabularies.createIRI(NAMESPACE, "modMin");
		INSERT_PATTERN_PROPERTY = Vocabularies.createIRI(NAMESPACE, "insertPattern");

		VALUES_CLASS = Vocabularies.createIRI(NAMESPACE, "Values");
		BINDINGS_PROPERTY = Vocabularies.createIRI(NAMESPACE, "bindings");
		VAR_NAMES_PROPERTY = Vocabularies.createIRI(NAMESPACE, "varNames");
		UNDEF = Vocabularies.createIRI(NAMESPACE, "undef");

		GROUP_CONCAT_CLASS = Vocabularies.createIRI(NAMESPACE, "GroupConcat");
		SAMPLE_CLASS = Vocabularies.createIRI(NAMESPACE, "Sample");

		AND = Vocabularies.createIRI(NAMESPACE, "and");
		OR = Vocabularies.createIRI(NAMESPACE, "or");
		ADD = Vocabularies.createIRI(NAMESPACE, "add");
		SUB = Vocabularies.createIRI(NAMESPACE, "sub");
		MUL = Vocabularies.createIRI(NAMESPACE, "mul");
		DIVIDE = Vocabularies.createIRI(NAMESPACE, "divide");
		EQ = Vocabularies.createIRI(NAMESPACE, "eq");
		NE = Vocabularies.createIRI(NAMESPACE, "ne");
		LT = Vocabularies.createIRI(NAMESPACE, "lt");
		LE = Vocabularies.createIRI(NAMESPACE, "le");
		GE = Vocabularies.createIRI(NAMESPACE, "ge");
		GT = Vocabularies.createIRI(NAMESPACE, "gt");
		NOT = Vocabularies.createIRI(NAMESPACE, "not");

		EXISTS = Vocabularies.createIRI(NAMESPACE, "exists");
		NOT_EXISTS = Vocabularies.createIRI(NAMESPACE, "notExists");

		BOUND = Vocabularies.createIRI(NAMESPACE, "bound");
		IF = Vocabularies.createIRI(NAMESPACE, "if");
		COALESCE = Vocabularies.createIRI(NAMESPACE, "coalesce");
		IS_IRI = Vocabularies.createIRI(NAMESPACE, "isIRI");
		IS_URI = Vocabularies.createIRI(NAMESPACE, "isURI");
		IS_BLANK = Vocabularies.createIRI(NAMESPACE, "isBlank");
		IS_LITERAL = Vocabularies.createIRI(NAMESPACE, "isLiteral");
		IS_NUMERIC = Vocabularies.createIRI(NAMESPACE, "isNumeric");
		STR = Vocabularies.createIRI(NAMESPACE, "str");
		LANG = Vocabularies.createIRI(NAMESPACE, "lang");
		DATATYPE = Vocabularies.createIRI(NAMESPACE, "datatype");
		IRI = Vocabularies.createIRI(NAMESPACE, "iri");
		URI = Vocabularies.createIRI(NAMESPACE, "uri");
		BNODE = Vocabularies.createIRI(NAMESPACE, "bnode");
		REGEX = Vocabularies.createIRI(NAMESPACE, "regex");
	}
}
