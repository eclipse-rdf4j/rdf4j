/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @since 2.7.3
 * @version 1.4.0
 */
public class SP {

	/**
	 * http://spinrdf.org/sp An RDF Schema to syntactically represent SPARQL
	 * queries (including SPARQL UPDATE) as RDF triples.
	 */
	private static String NAMESPACE = "http://spinrdf.org/sp#";

	/**
	 * http://spinrdf.org/sp#Path The base class of SPARQL property path
	 * expressions. Paths are used by sp:TriplePath triple paths.
	 */
	public static IRI PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#SystemClass An "artificial" root class that groups
	 * all SP classes. This makes them look much less overwhelming in UI tools.
	 * Typical end users don't need to see those classes anyway.
	 */
	public static IRI SYSTEM_CLASS;

	/**
	 * http://spinrdf.org/sp#Asc Marker to indicate ascending order.
	 */
	public static IRI ASC_CLASS;

	/**
	 * http://spinrdf.org/sp#OrderByCondition An abstract base class for
	 * ascending or descending order conditions. Instances of this class
	 * (typically bnodes) must have a value for expression to point to the actual
	 * values.
	 */
	public static IRI ORDER_BY_CONDITION_CLASS;

	/**
	 * http://spinrdf.org/sp#Sum Represents sum aggregations, e.g. SELECT
	 * SUM(?varName)...
	 */
	public static IRI SUM_CLASS;

	/**
	 * http://spinrdf.org/sp#Aggregation Base class of aggregation types (not
	 * part of the SPARQL 1.0 standard but supported by ARQ and other engines).
	 */
	public static IRI AGGREGATION_CLASS;

	/**
	 * http://spinrdf.org/sp#Union A UNION group.
	 */
	public static IRI UNION_CLASS;

	/**
	 * http://spinrdf.org/sp#ElementGroup Abstract base class of group patterns.
	 */
	public static IRI ELEMENT_GROUP_CLASS;

	/**
	 * http://spinrdf.org/sp#TriplePattern A triple pattern used in the body of a
	 * query.
	 */
	public static IRI TRIPLE_PATTERN_CLASS;

	/**
	 * http://spinrdf.org/sp#Element An abstract base class for all pattern
	 * elements.
	 */
	public static IRI ELEMENT_CLASS;

	/**
	 * http://spinrdf.org/sp#Triple A base class for TriplePattern and
	 * TripleTemplate. This basically specifies that subject, predicate and
	 * object must be present.
	 */
	public static IRI TRIPLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Load A LOAD Update operation. The document to load
	 * is specified using sp:document, and the (optional) target graph using
	 * sp:into.
	 */
	public static IRI LOAD_CLASS;

	/**
	 * http://spinrdf.org/sp#Update Abstract base class to group the various
	 * SPARQL UPDATE commands.
	 */
	public static IRI UPDATE_CLASS;

	/**
	 * http://spinrdf.org/sp#DeleteData An Update operation to delete specific
	 * triples. The graph triples are represented using sp:data, which points to
	 * an rdf:List of sp:Triples or sp:NamedGraphs.
	 */
	public static IRI DELETE_DATA_CLASS;

	/**
	 * http://spinrdf.org/sp#Desc Marker to indicate descending order.
	 */
	public static IRI DESC_CLASS;

	/**
	 * http://spinrdf.org/sp#TripleTemplate A prototypical triple used as
	 * template in the head of a Construct query. May contain variables.
	 */
	public static IRI TRIPLE_TEMPLATE_CLASS;

	/**
	 * http://spinrdf.org/sp#Max Represents MAX aggregations.
	 */
	public static IRI MAX_CLASS;

	/**
	 * http://spinrdf.org/sp#Insert Deprecated - use sp:Modify instead.
	 * Represents a INSERT INTO (part of SPARQL UPDATE language). The graph IRIs
	 * are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:insertPattern. The WHERE clause is represented using sp:where.
	 */
	public static IRI INSERT_CLASS;

	/**
	 * http://spinrdf.org/sp#Modify Represents a MODIFY (part of SPARQL UPDATE
	 * language). The graph IRIs are stored in sp:graphIRI. The template patterns
	 * are stored in sp:deletePattern and sp:insertPattern. The WHERE clause is
	 * represented using sp:where.
	 */
	public static IRI MODIFY_CLASS;

	/**
	 * http://spinrdf.org/sp#Insert Deprecated - use sp:Modify instead.
	 * Represents a INSERT INTO (part of SPARQL UPDATE language). The graph IRIs
	 * are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:insertPattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static IRI Insert;

	/**
	 * http://spinrdf.org/sp#Avg Represents AVG aggregations.
	 */
	public static IRI AVG_CLASS;

	/**
	 * http://spinrdf.org/sp#TriplePath Similar to a TriplePattern, but with a
	 * path expression as its predicate. For example, this can be used to express
	 * transitive sub-class relationships (?subClass rdfs:subClassOf*
	 * ?superClass).
	 */
	public static IRI TRIPLE_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Tuple Abstract base class for things that have
	 * subject and object.
	 */
	public static IRI TUPLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Let Deprecated: use sp:Bind instead. A variable
	 * assignment (LET (?<varName> := <expression>)). Not part of the SPARQL 1.0
	 * standard, but (for example) ARQ.
	 */
	public static IRI LET_CLASS;

	/**
	 * http://spinrdf.org/sp#Bind A BIND element.
	 */
	public static IRI BIND_CLASS;

	/**
	 * http://spinrdf.org/sp#Let Deprecated: use sp:Bind instead. A variable
	 * assignment (LET (?<varName> := <expression>)). Not part of the SPARQL 1.0
	 * standard, but (for example) ARQ.
	 */
	@Deprecated
	public static IRI Let;

	/**
	 * http://spinrdf.org/sp#ElementList A list of Elements. This class is never
	 * instantiated directly as SPIN will use plain rdf:Lists to store element
	 * lists.
	 */
	public static IRI ELEMENT_LIST_CLASS;

	/**
	 * http://spinrdf.org/sp#SubQuery A nested SELECT query inside of an element
	 * list. The query is stored in sp:query.
	 */
	public static IRI SUB_QUERY_CLASS;

	/**
	 * http://spinrdf.org/sp#Delete Deprecated - use sp:Modify instead.
	 * Represents a DELETE FROM (part of SPARQL UPDATE language). The graph IRIs
	 * are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:deletePattern. The WHERE clause is represented using sp:where.
	 */
	public static IRI DELETE_CLASS;

	/**
	 * http://spinrdf.org/sp#Delete Deprecated - use sp:Modify instead.
	 * Represents a DELETE FROM (part of SPARQL UPDATE language). The graph IRIs
	 * are stored in sp:graphIRI. The template patterns to delete are stored in
	 * sp:deletePattern. The WHERE clause is represented using sp:where.
	 */
	@Deprecated
	public static IRI Delete;

	/**
	 * http://spinrdf.org/sp#Min Represents MIN aggregations.
	 */
	public static IRI MIN_CLASS;

	/**
	 * http://spinrdf.org/sp#Optional An optional element in a query.
	 */
	public static IRI OPTIONAL_CLASS;

	/**
	 * http://spinrdf.org/sp#AltPath An alternative path with the union of
	 * sp:path1 and sp:path2.
	 */
	public static IRI ALT_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Count Counts the number of times a variable is used.
	 * The variable is stored in the variable property. This might be left blank
	 * to indicate COUNT(*).
	 */
	public static IRI COUNT_CLASS;

	/**
	 * http://spinrdf.org/sp#ReversePath A path with reversed direction.
	 */
	public static IRI REVERSE_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Construct A CONSTRUCT-type query that can be used to
	 * construct new triples from template triples (head) that use variable
	 * bindings from the match patterns (body).
	 */
	public static IRI CONSTRUCT_CLASS;

	/**
	 * http://spinrdf.org/sp#Query Abstract base class of the various types of
	 * supported queries. Common to all types of queries is that they can have a
	 * body ("WHERE clause").
	 */
	public static IRI QUERY_CLASS;

	/**
	 * http://spinrdf.org/sp#Variable A variable mentioned in a Triple or
	 * expression. Variables are often blank nodes with the variable name stored
	 * in ts:name. Variables can also be supplied with a URI in which case the
	 * system will attempt to reuse the same variable instance across multiple
	 * query definitions.
	 */
	public static IRI VARIABLE_CLASS;

	/**
	 * http://spinrdf.org/sp#Ask An ASK query that returns true if the condition
	 * in the body is met by at least one result set.
	 */
	public static IRI ASK_CLASS;

	/**
	 * http://spinrdf.org/sp#ModPath A modified path such as rdfs:subClassOf*.
	 */
	public static IRI MOD_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#Create An Update operation that creates a new empty
	 * graph with a name specified by sp:graphIRI. May have sp:silent set to
	 * true.
	 */
	public static IRI CREATE_CLASS;

	/**
	 * http://spinrdf.org/sp#NamedGraph A named Graph element such as GRAPH <uri>
	 * {...}.
	 */
	public static IRI NAMED_GRAPH_CLASS;

	/**
	 * http://spinrdf.org/sp#Command A shared superclass for sp:Query and
	 * sp:Update that can be used to specify that the range of property can be
	 * either one.
	 */
	public static IRI COMMAND_CLASS;

	public static IRI REVERSE_LINK_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#NotExists A NOT EXISTS element group.
	 */
	public static IRI NOT_EXISTS_CLASS;

	/**
	 * http://spinrdf.org/sp#Drop An Update operation that removes a specified
	 * graph from the Graph Store. Must specify the graph using sp:graphIRI, or
	 * sp:default, sp:named or sp:all. May have the SILENT flag, encoded using
	 * sp:silent.
	 */
	public static IRI DROP_CLASS;

	/**
	 * http://spinrdf.org/sp#InsertData An Update operation to insert specific
	 * triples. The graph triples are represented using sp:data, which points to
	 * an rdf:List of sp:Triples or sp:NamedGraphs.
	 */
	public static IRI INSERT_DATA_CLASS;

	/**
	 * http://spinrdf.org/sp#DeleteWhere An Update operation where the triples
	 * matched by the WHERE clause (sp:where) will be the triples deleted.
	 */
	public static IRI DELETE_WHERE_CLASS;

	/**
	 * http://spinrdf.org/sp#Service A SERVICE call that matches a nested
	 * sub-pattern against a SPARQL end point specified by a URI.
	 */
	public static IRI SERVICE_CLASS;

	/**
	 * http://spinrdf.org/sp#Select A SELECT-type query that returns variable
	 * bindings as its result.
	 */
	public static IRI SELECT_CLASS;

	/**
	 * http://spinrdf.org/sp#Filter A constraint element that evaluates a given
	 * expression to true or false.
	 */
	public static IRI FILTER_CLASS;

	/**
	 * http://spinrdf.org/sp#Minus A MINUS element group.
	 */
	public static IRI MINUS_CLASS;

	/**
	 * http://spinrdf.org/sp#Clear An Update operation that removes all triples
	 * from a specified graph. Must specify the graph using sp:graphIRI, or
	 * sp:default, sp:named or sp:all. May have the SILENT flag, encoded using
	 * sp:silent.
	 */
	public static IRI CLEAR_CLASS;

	/**
	 * http://spinrdf.org/sp#Describe A DESCRIBE-type Query.
	 */
	public static IRI DESCRIBE_CLASS;

	/**
	 * http://spinrdf.org/sp#SeqPath A sequence of multiple paths.
	 */
	public static IRI SEQ_PATH_CLASS;

	/**
	 * http://spinrdf.org/sp#arg5 The fifth argument of a function call. Further
	 * arguments are not common in SPARQL, therefore no sp:arg6, etc are defined
	 * here. However, they can be created if needed.
	 */
	public static IRI ARG5_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg Abstract superproperty for the enumerated arg1,
	 * arg2 etc.
	 */
	public static IRI ARG_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path1 The first child path of a property path. Used
	 * by sp:AltPath and sp:SeqPath.
	 */
	public static IRI PATH1_PROPERTY;

	/**
	 * http://spinrdf.org/sp#systemProperty An abstract base proprerty that
	 * groups together the SP system properties. Users typically don't need to
	 * see them anyway.
	 */
	public static IRI SYSTEM_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg1 The first argument of a function call.
	 */
	public static IRI ARG1_PROPERTY;

	/**
	 * http://spinrdf.org/sp#default Used in DROP and CLEAR.
	 */
	public static IRI DEFAULT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#object An RDF Node or Variable describing the object
	 * of a triple.
	 */
	public static IRI OBJECT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#graphNameNode The name (URI or Variable) of a
	 * NamedGraph.
	 */
	public static IRI GRAPH_NAME_NODE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#varName The name of a Variable.
	 */
	public static IRI VAR_NAME_PROPERTY;

	/**
	 * http://spinrdf.org/sp#named Used in DROP and CLEAR.
	 */
	public static IRI NAMED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#as Points to a Variable used in an AS statement such
	 * as COUNT aggregates.
	 */
	public static IRI AS_PROPERTY;

	/**
	 * http://spinrdf.org/sp#distinct A marker property to indicate that a Select
	 * query is of type SELECT DISTINCT.
	 */
	public static IRI DISTINCT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path2 The second child path of a property path. Used
	 * by sp:AltPath and sp:SeqPath.
	 */
	public static IRI PATH2_PROPERTY;

	/**
	 * http://spinrdf.org/sp#orderBy Links a query with an ORDER BY clause where
	 * the values are rdf:List containing OrderByConditions or expressions. While
	 * the domain of this property is sp:Query, only Describe and Select queries
	 * can have values of it.
	 */
	public static IRI ORDER_BY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#variable The variable of a Bind element.
	 */
	public static IRI VARIABLE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg4 The forth argument of a function call.
	 */
	public static IRI ARG4_PROPERTY;

	public static IRI SILENT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#having Points from a SELECT query to a list of
	 * HAVING expressions.
	 */
	public static IRI HAVING_PROPERTY;

	/**
	 * http://spinrdf.org/sp#query Links a SubQuery resource with the nested
	 * Query.
	 */
	public static IRI QUERY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#groupBy Points from a Query to the list of GROUP BY
	 * expressions.
	 */
	public static IRI GROUP_BY_PROPERTY;

	/**
	 * http://spinrdf.org/sp#graphIRI Points to graph names (IRIs) in various
	 * sp:Update operations.
	 */
	public static IRI GRAPH_IRI_PROPERTY;

	/**
	 * http://spinrdf.org/sp#limit The LIMIT solution modifier of a Query.
	 */
	public static IRI LIMIT_PROPERTY;

	public static IRI USING_PROPERTY;

	/**
	 * http://spinrdf.org/sp#templates Points to a list of TripleTemplates that
	 * form the head of a Construct query.
	 */
	public static IRI TEMPLATES_PROPERTY;

	/**
	 * http://spinrdf.org/sp#resultNodes Contains the result nodes (URI resources
	 * or Variables) of a Describe query.
	 */
	public static IRI RESULT_NODES_PROPERTY;

	public static IRI USING_NAMED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg3 The third argument of a function call.
	 */
	public static IRI ARG3_PROPERTY;

	/**
	 * http://spinrdf.org/sp#reduced A property with true to indicate that a
	 * Select query has a REDUCED flag.
	 */
	public static IRI REDUCED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#subPath The child path of a property path
	 * expression. This is used by ReversePath and ModPath.
	 */
	public static IRI SUB_PATH_PROPERTY;

	/**
	 * http://spinrdf.org/sp#into The (optional) target of a LOAD Update
	 * operation.
	 */
	public static IRI INTO_PROPERTY;

	public static IRI WITH_PROPERTY;

	/**
	 * http://spinrdf.org/sp#serviceURI Used by sp:Service to specify the URI of
	 * the SPARQL end point to invoke. Must point to a URI node.
	 */
	public static IRI SERVICE_URI_PROPERTY;

	/**
	 * http://spinrdf.org/sp#document The URI of the document to load using a
	 * LOAD Update operation.
	 */
	public static IRI DOCUMENT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#where The WHERE clause of a Query.
	 */
	public static IRI WHERE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#resultVariables An rdf:List of variables that are
	 * returned by a Select query.
	 */
	public static IRI RESULT_VARIABLES_PROPERTY;

	/**
	 * http://spinrdf.org/sp#text Can be attached to sp:Queries to store a
	 * textual representation of the query. This can be useful for tools that do
	 * not have a complete SPIN Syntax parser available.
	 */
	public static IRI TEXT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#path Points from a TriplePath to its path.
	 */
	public static IRI PATH_PROPERTY;

	public static IRI MOD_MAX_PROPERTY;

	/**
	 * http://spinrdf.org/sp#predicate A resource or Variable describing the
	 * predicate of a triple.
	 */
	public static IRI PREDICATE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#elements Points to an ElementList, for example in an
	 * Optional element.
	 */
	public static IRI ELEMENTS_PROPERTY;

	public static IRI NODE_PROPERTY;

	/**
	 * http://spinrdf.org/sp#fromNamed Specifies a named RDF Dataset used by a
	 * Query (FROM NAMED syntax in SPARQL). Values of this property must be URI
	 * resources.
	 */
	public static IRI FROM_NAMED_PROPERTY;

	/**
	 * http://spinrdf.org/sp#arg2 The second argument of a function call.
	 */
	public static IRI ARG2_PROPERTY;

	/**
	 * http://spinrdf.org/sp#subject A resource or Variable describing the
	 * subject of a triple.
	 */
	public static IRI SUBJECT_PROPERTY;

	/**
	 * http://spinrdf.org/sp#expression Points to an expression, for example in a
	 * Filter or Assignment.
	 */
	public static IRI EXPRESSION_PROPERTY;

	/**
	 * http://spinrdf.org/sp#deletePattern Points to a list of sp:TripleTemplates
	 * and sp:NamedGraphs in a modify operation.
	 */
	public static IRI DELETE_PATTERN_PROPERTY;

	/**
	 * http://spinrdf.org/sp#all Used in DROP and CLEAR.
	 */
	public static IRI ALL_PROPERTY;

	/**
	 * http://spinrdf.org/sp#offset The OFFSET solution modifier of a Query.
	 */
	public static IRI OFFSET_PROPERTY;

	/**
	 * http://spinrdf.org/sp#from Specifies an RDF Dataset used by a Query (FROM
	 * syntax in SPARQL). Values of this property must be URI resources.
	 */
	public static IRI FROM_PROPERTY;

	public static IRI MOD_MIN_PROPERTY;

	/**
	 * http://spinrdf.org/sp#insertPattern Points to a list of sp:TripleTemplates
	 * or sp:NamedGraphs in a modify command.
	 */
	public static IRI INSERT_PATTERN_PROPERTY;
	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		PATH_CLASS = factory.createIRI(NAMESPACE, "Path");
		SYSTEM_CLASS = factory.createIRI(NAMESPACE, "SystemClass");
		ASC_CLASS = factory.createIRI(NAMESPACE, "Asc");
		ORDER_BY_CONDITION_CLASS = factory.createIRI(NAMESPACE, "OrderByCondition");
		SUM_CLASS = factory.createIRI(NAMESPACE, "Sum");
		AGGREGATION_CLASS = factory.createIRI(NAMESPACE, "Aggregation");
		UNION_CLASS = factory.createIRI(NAMESPACE, "Union");
		ELEMENT_GROUP_CLASS = factory.createIRI(NAMESPACE, "ElementGroup");
		TRIPLE_PATTERN_CLASS = factory.createIRI(NAMESPACE, "TriplePattern");
		ELEMENT_CLASS = factory.createIRI(NAMESPACE, "Element");
		TRIPLE_CLASS = factory.createIRI(NAMESPACE, "Triple");
		LOAD_CLASS = factory.createIRI(NAMESPACE, "Load");
		UPDATE_CLASS = factory.createIRI(NAMESPACE, "Update");
		DELETE_DATA_CLASS = factory.createIRI(NAMESPACE, "DeleteData");
		DESC_CLASS = factory.createIRI(NAMESPACE, "Desc");
		TRIPLE_TEMPLATE_CLASS = factory.createIRI(NAMESPACE, "TripleTemplate");
		MAX_CLASS = factory.createIRI(NAMESPACE, "Max");
		INSERT_CLASS = factory.createIRI(NAMESPACE, "Insert");
		MODIFY_CLASS = factory.createIRI(NAMESPACE, "Modify");
		AVG_CLASS = factory.createIRI(NAMESPACE, "Avg");
		TRIPLE_PATH_CLASS = factory.createIRI(NAMESPACE, "TriplePath");
		TUPLE_CLASS = factory.createIRI(NAMESPACE, "Tuple");
		LET_CLASS = factory.createIRI(NAMESPACE, "Let");
		BIND_CLASS = factory.createIRI(NAMESPACE, "Bind");
		ELEMENT_LIST_CLASS = factory.createIRI(NAMESPACE, "ElementList");
		SUB_QUERY_CLASS = factory.createIRI(NAMESPACE, "SubQuery");
		DELETE_CLASS = factory.createIRI(NAMESPACE, "Delete");
		MIN_CLASS = factory.createIRI(NAMESPACE, "Min");
		OPTIONAL_CLASS = factory.createIRI(NAMESPACE, "Optional");
		ALT_PATH_CLASS = factory.createIRI(NAMESPACE, "AltPath");
		COUNT_CLASS = factory.createIRI(NAMESPACE, "Count");
		REVERSE_PATH_CLASS = factory.createIRI(NAMESPACE, "ReversePath");
		CONSTRUCT_CLASS = factory.createIRI(NAMESPACE, "Construct");
		QUERY_CLASS = factory.createIRI(NAMESPACE, "Query");
		VARIABLE_CLASS = factory.createIRI(NAMESPACE, "Variable");
		ASK_CLASS = factory.createIRI(NAMESPACE, "Ask");
		MOD_PATH_CLASS = factory.createIRI(NAMESPACE, "ModPath");
		CREATE_CLASS = factory.createIRI(NAMESPACE, "Create");
		NAMED_GRAPH_CLASS = factory.createIRI(NAMESPACE, "NamedGraph");
		COMMAND_CLASS = factory.createIRI(NAMESPACE, "Command");
		REVERSE_LINK_PATH_CLASS = factory.createIRI(NAMESPACE, "ReverseLinkPath");
		NOT_EXISTS_CLASS = factory.createIRI(NAMESPACE, "NotExists");
		DROP_CLASS = factory.createIRI(NAMESPACE, "Drop");
		INSERT_DATA_CLASS = factory.createIRI(NAMESPACE, "InsertData");
		DELETE_WHERE_CLASS = factory.createIRI(NAMESPACE, "DeleteWhere");
		SERVICE_CLASS = factory.createIRI(NAMESPACE, "Service");
		SELECT_CLASS = factory.createIRI(NAMESPACE, "Select");
		FILTER_CLASS = factory.createIRI(NAMESPACE, "Filter");
		MINUS_CLASS = factory.createIRI(NAMESPACE, "Minus");
		CLEAR_CLASS = factory.createIRI(NAMESPACE, "Clear");
		DESCRIBE_CLASS = factory.createIRI(NAMESPACE, "Describe");
		SEQ_PATH_CLASS = factory.createIRI(NAMESPACE, "SeqPath");
		ARG5_PROPERTY = factory.createIRI(NAMESPACE, "arg5");
		ARG_PROPERTY = factory.createIRI(NAMESPACE, "arg");
		PATH1_PROPERTY = factory.createIRI(NAMESPACE, "path1");
		SYSTEM_PROPERTY = factory.createIRI(NAMESPACE, "systemProperty");
		ARG1_PROPERTY = factory.createIRI(NAMESPACE, "arg1");
		DEFAULT_PROPERTY = factory.createIRI(NAMESPACE, "default");
		OBJECT_PROPERTY = factory.createIRI(NAMESPACE, "object");
		GRAPH_NAME_NODE_PROPERTY = factory.createIRI(NAMESPACE, "graphNameNode");
		VAR_NAME_PROPERTY = factory.createIRI(NAMESPACE, "varName");
		NAMED_PROPERTY = factory.createIRI(NAMESPACE, "named");
		AS_PROPERTY = factory.createIRI(NAMESPACE, "as");
		DISTINCT_PROPERTY = factory.createIRI(NAMESPACE, "distinct");
		PATH2_PROPERTY = factory.createIRI(NAMESPACE, "path2");
		ORDER_BY_PROPERTY = factory.createIRI(NAMESPACE, "orderBy");
		VARIABLE_PROPERTY = factory.createIRI(NAMESPACE, "variable");
		ARG4_PROPERTY = factory.createIRI(NAMESPACE, "arg4");
		SILENT_PROPERTY = factory.createIRI(NAMESPACE, "silent");
		HAVING_PROPERTY = factory.createIRI(NAMESPACE, "having");
		QUERY_PROPERTY = factory.createIRI(NAMESPACE, "query");
		GROUP_BY_PROPERTY = factory.createIRI(NAMESPACE, "groupBy");
		GRAPH_IRI_PROPERTY = factory.createIRI(NAMESPACE, "graphIRI");
		LIMIT_PROPERTY = factory.createIRI(NAMESPACE, "limit");
		USING_PROPERTY = factory.createIRI(NAMESPACE, "using");
		TEMPLATES_PROPERTY = factory.createIRI(NAMESPACE, "templates");
		RESULT_NODES_PROPERTY = factory.createIRI(NAMESPACE, "resultNodes");
		USING_NAMED_PROPERTY = factory.createIRI(NAMESPACE, "usingNamed");
		ARG3_PROPERTY = factory.createIRI(NAMESPACE, "arg3");
		REDUCED_PROPERTY = factory.createIRI(NAMESPACE, "reduced");
		SUB_PATH_PROPERTY = factory.createIRI(NAMESPACE, "subPath");
		INTO_PROPERTY = factory.createIRI(NAMESPACE, "into");
		WITH_PROPERTY = factory.createIRI(NAMESPACE, "with");
		SERVICE_URI_PROPERTY = factory.createIRI(NAMESPACE, "serviceURI");
		DOCUMENT_PROPERTY = factory.createIRI(NAMESPACE, "document");
		WHERE_PROPERTY = factory.createIRI(NAMESPACE, "where");
		RESULT_VARIABLES_PROPERTY = factory.createIRI(NAMESPACE, "resultVariables");
		TEXT_PROPERTY = factory.createIRI(NAMESPACE, "text");
		PATH_PROPERTY = factory.createIRI(NAMESPACE, "path");
		MOD_MAX_PROPERTY = factory.createIRI(NAMESPACE, "modMax");
		PREDICATE_PROPERTY = factory.createIRI(NAMESPACE, "predicate");
		ELEMENTS_PROPERTY = factory.createIRI(NAMESPACE, "elements");
		NODE_PROPERTY = factory.createIRI(NAMESPACE, "node");
		FROM_NAMED_PROPERTY = factory.createIRI(NAMESPACE, "fromNamed");
		ARG2_PROPERTY = factory.createIRI(NAMESPACE, "arg2");
		SUBJECT_PROPERTY = factory.createIRI(NAMESPACE, "subject");
		EXPRESSION_PROPERTY = factory.createIRI(NAMESPACE, "expression");
		DELETE_PATTERN_PROPERTY = factory.createIRI(NAMESPACE, "deletePattern");
		ALL_PROPERTY = factory.createIRI(NAMESPACE, "all");
		OFFSET_PROPERTY = factory.createIRI(NAMESPACE, "offset");
		FROM_PROPERTY = factory.createIRI(NAMESPACE, "from");
		MOD_MIN_PROPERTY = factory.createIRI(NAMESPACE, "modMin");
		INSERT_PATTERN_PROPERTY = factory.createIRI(NAMESPACE, "insertPattern");
	}
}
