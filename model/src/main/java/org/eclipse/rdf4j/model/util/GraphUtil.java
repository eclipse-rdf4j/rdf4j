/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.util.iterators.ConvertingIterator;
import org.eclipse.rdf4j.util.iterators.Iterators;

/**
 * Utility methods for working with {@link Graph} objects. Note that since release 2.7.0, most of the functionality here
 * is also available (in more convenient form) in the {@link org.eclipse.rdf4j.model.Model} interface, which extends
 * {@link Graph}.
 * 
 * @deprecated since 2.8.0. Use equivalent functionality in {@link Model} and {@link Models} instead.
 * @author Arjohn Kampman
 */
@Deprecated
public class GraphUtil {

	/**
	 * Gets the subject of the statements with the specified predicate, object and (optionally) contexts from the
	 * supplied graph. Calling this method is equivalent to calling <tt>graph.match(null, pred, obj, contexts)</tt> and
	 * extracting the subjects of the matching statements from the returned iterator. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and {@link Model#subjects()}
	 *             instead.
	 */
	@Deprecated
	public static Iterator<Resource> getSubjectIterator(Graph graph, IRI pred, Value obj, Resource... contexts) {
		Iterator<Statement> iter = graph.match(null, pred, obj, contexts);

		return new ConvertingIterator<Statement, Resource>(iter) {

			@Override
			protected Resource convert(Statement st) throws RuntimeException {
				return st.getSubject();
			}

		};
	}

	/**
	 * Gets the subject of the statements with the specified predicate, object and (optionally) contexts from the
	 * supplied graph. Calling this method is equivalent to calling <tt>graph.match(null, pred, obj, contexts)</tt> and
	 * adding the subjects of the matching statements to a set. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and {@link Model#subjects()}
	 *             instead.
	 */
	@Deprecated
	public static Set<Resource> getSubjects(Graph graph, IRI pred, Value obj, Resource... contexts) {
		Iterator<Resource> iter = getSubjectIterator(graph, pred, obj, contexts);
		return Iterators.addAll(iter, new LinkedHashSet<>());
	}

	/**
	 * Gets the subject of the statement(s) with the specified predicate and object from the specified contexts in the
	 * supplied graph. The combination of predicate, object and contexts must match at least one statement. In case more
	 * than one statement matches -- for example statements from multiple contexts -- all these statements should have
	 * the same subject. A {@link GraphUtilException} is thrown if these conditions are not met. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @return The subject of the matched statement(s).
	 * @throws GraphUtilException If the statements matched by the specified parameters do not have exactly one unique
	 *                            subject.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and
	 *             {@link Model#subjectResource()} instead.
	 */
	@Deprecated
	public static Resource getUniqueSubject(Graph graph, IRI pred, Value obj, Resource... contexts)
			throws GraphUtilException {
		Set<Resource> subjects = getSubjects(graph, pred, obj, contexts);

		if (subjects.size() == 1) {
			return subjects.iterator().next();
		} else if (subjects.isEmpty()) {
			throw new GraphUtilException("Missing property: " + pred);
		} else {
			throw new GraphUtilException("Multiple " + pred + " properties found");
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getUniqueSubject(Graph, IRI, Value, Resource[])} to a URI,
	 * or throws a GraphUtilException if that value is not a URI.
	 * 
	 * @return The subject of the matched statement(s).
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getUniqueSubject(Graph, IRI, Value, Resource[])} or if its return value is not
	 *                            a URI.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and
	 *             {@link Model#subjectIRI()} instead.
	 */
	@Deprecated
	public static IRI getUniqueSubjectURI(Graph graph, IRI pred, Value obj, Resource... contexts)
			throws GraphUtilException {
		Resource subject = getUniqueSubject(graph, pred, obj, contexts);

		if (subject instanceof IRI) {
			return (IRI) subject;
		} else {
			throw new GraphUtilException("Expected URI for subject " + subject);
		}
	}

	/**
	 * Gets the subject of the statement(s) with the specified predicate and object from the specified contexts in the
	 * supplied graph. If the combination of predicate, object and contexts matches one or more statements, all these
	 * statements should have the same subject. A {@link GraphUtilException} is thrown if this is not the case. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @return The subject of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If the statements matched by the specified parameters have more than one unique
	 *                            subject.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and
	 *             {@link Model#subjectResource()} instead.
	 */
	@Deprecated
	public static Resource getOptionalSubject(Graph graph, IRI pred, Value obj, Resource... contexts)
			throws GraphUtilException {
		Set<Resource> subjects = getSubjects(graph, pred, obj, contexts);

		if (subjects.isEmpty()) {
			return null;
		} else if (subjects.size() == 1) {
			return subjects.iterator().next();
		} else {
			throw new GraphUtilException("Multiple " + pred + " properties found");
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getOptionalSubject(Graph, IRI, Value, Resource[])} to a
	 * URI, or throws a GraphUtilException if that value is not a URI.
	 * 
	 * @return The subject of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getOptionalSubject(Graph, IRI, Value, Resource[])} or if its return value is
	 *                            not a URI.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and
	 *             {@link Model#subjectIRI()} instead.
	 */
	@Deprecated
	public static IRI getOptionalSubjectURI(Graph graph, IRI pred, Value obj, Resource... contexts)
			throws GraphUtilException {
		Resource subject = getOptionalSubject(graph, pred, obj, contexts);

		if (subject == null || subject instanceof IRI) {
			return (IRI) subject;
		} else {
			throw new GraphUtilException("Expected URI for subject " + subject);
		}
	}

	/**
	 * Gets the objects of the statements with the specified subject, predicate and (optionally) contexts from the
	 * supplied graph. Calling this method is equivalent to calling <tt>graph.match(subj, pred, null, contexts)</tt> and
	 * extracting the objects of the matching statements from the returned iterator. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and {@link Model#objects()}
	 *             instead.
	 */
	@Deprecated
	public static Iterator<Value> getObjectIterator(Graph graph, Resource subj, IRI pred, Resource... contexts) {
		Iterator<Statement> iter = graph.match(subj, pred, null, contexts);

		return new ConvertingIterator<Statement, Value>(iter) {

			@Override
			protected Value convert(Statement st) throws RuntimeException {
				return st.getObject();
			}

		};
	}

	/**
	 * Gets the objects of the statements with the specified subject, predicate and (optionally) contexts from the
	 * supplied graph. Calling this method is equivalent to calling <tt>graph.match(subj, pred, null, contexts)</tt> and
	 * adding the objects of the matching statements to a set. See {@link Graph#match(Resource, IRI, Value, Resource[])}
	 * for a description of the parameter values.
	 * 
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and {@link Model#objects()}
	 *             instead.
	 */
	@Deprecated
	public static Set<Value> getObjects(Graph graph, Resource subj, IRI pred, Resource... contexts) {
		Iterator<Value> iter = getObjectIterator(graph, subj, pred, contexts);
		return Iterators.addAll(iter, new LinkedHashSet<>());
	}

	/**
	 * Gets the object of the statement(s) with the specified subject and predicate from the specified contexts in the
	 * supplied graph. The combination of subject, predicate and contexts must match at least one statement. In case
	 * more than one statement matches -- for example statements from multiple contexts -- all these statements should
	 * have the same object. A {@link GraphUtilException} is thrown if these conditions are not met. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @return The object of the matched statement(s).
	 * @throws GraphUtilException If the statements matched by the specified parameters do not have exactly one unique
	 *                            object.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...)} and
	 *             {@link Model#objectValue()} instead.
	 */
	@Deprecated
	public static Value getUniqueObject(Graph graph, Resource subj, IRI pred, Resource... contexts)
			throws GraphUtilException {
		Set<Value> objects = getObjects(graph, subj, pred, contexts);

		if (objects.size() == 1) {
			return objects.iterator().next();
		} else if (objects.isEmpty()) {
			throw new GraphUtilException("Missing property: " + pred);
		} else {
			throw new GraphUtilException("Multiple " + pred + " properties found");
		}
	}

	/**
	 * Adds the specified statement and makes sure that no other statements are present in the Graph with the same
	 * subject and predicate. When contexts are specified, the (subj, pred) pair will occur exactly once in each
	 * context, else the (subj, pred) pair will occur exactly once in the entire Graph.
	 * 
	 * @deprecated since 2.8.0. Use {@link Models#setProperty(Model, Resource, IRI, Value, Resource...) } instead.
	 */
	@Deprecated
	public static void setUniqueObject(Graph graph, Resource subj, IRI pred, Value obj, Resource... contexts) {
		Iterator<Statement> iter = graph.match(subj, pred, null, contexts);

		while (iter.hasNext()) {
			iter.next();
			iter.remove();
		}

		graph.add(subj, pred, obj, contexts);
	}

	/**
	 * Utility method that casts the return value of {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} to a
	 * Resource, or throws a GraphUtilException if that value is not a Resource.
	 * 
	 * @return The object of the matched statement(s).
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a Resource.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectResource() } instead.
	 */
	@Deprecated
	public static Resource getUniqueObjectResource(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getUniqueObject(graph, subj, pred);

		if (obj instanceof Resource) {
			return (Resource) obj;
		} else {
			throw new GraphUtilException("Expected URI or blank node for property " + pred);
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} to a
	 * URI, or throws a GraphUtilException if that value is not a URI.
	 * 
	 * @return The object of the matched statement(s).
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a URI.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectIRI() } instead.
	 */
	@Deprecated
	public static IRI getUniqueObjectURI(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getUniqueObject(graph, subj, pred);

		if (obj instanceof IRI) {
			return (IRI) obj;
		} else {
			throw new GraphUtilException("Expected URI for property " + pred);
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} to a
	 * Literal, or throws a GraphUtilException if that value is not a Literal.
	 * 
	 * @return The object of the matched statement(s).
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getUniqueObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a Literal.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectLiteral() } instead.
	 */
	@Deprecated
	public static Literal getUniqueObjectLiteral(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getUniqueObject(graph, subj, pred);

		if (obj instanceof Literal) {
			return (Literal) obj;
		} else {
			throw new GraphUtilException("Expected literal for property " + pred);
		}
	}

	/**
	 * Gets the object of the statement(s) with the specified subject and predicate from the specified contexts in the
	 * supplied graph. If the combination of subject, predicate and contexts matches one or more statements, all these
	 * statements should have the same object. A {@link GraphUtilException} is thrown if this is not the case. See
	 * {@link Graph#match(Resource, IRI, Value, Resource[])} for a description of the parameter values.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If the statements matched by the specified parameters have more than one unique
	 *                            object.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectValue() } instead.
	 */
	@Deprecated
	public static Value getOptionalObject(Graph graph, Resource subj, IRI pred, Resource... contexts)
			throws GraphUtilException {
		Set<Value> objects = getObjects(graph, subj, pred, contexts);

		if (objects.isEmpty()) {
			return null;
		} else if (objects.size() == 1) {
			return objects.iterator().next();
		} else {
			throw new GraphUtilException("Multiple " + pred + " properties found");
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} to a
	 * Resource, or throws a GraphUtilException if that value is not a Resource.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a Resource.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectResource() } instead.
	 */
	@Deprecated
	public static Resource getOptionalObjectResource(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getOptionalObject(graph, subj, pred);

		if (obj == null || obj instanceof Resource) {
			return (Resource) obj;
		} else {
			throw new GraphUtilException("Expected URI or blank node for property " + pred);
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} to a
	 * URI, or throws a GraphUtilException if that value is not a URI.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a URI.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectIRI() } instead.
	 */
	@Deprecated
	public static IRI getOptionalObjectURI(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getOptionalObject(graph, subj, pred);

		if (obj == null || obj instanceof IRI) {
			return (IRI) obj;
		} else {
			throw new GraphUtilException("Expected URI for property " + pred);
		}
	}

	/**
	 * Utility method that casts the return value of {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} to a
	 * Literal, or throws a GraphUtilException if that value is not a Literal.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no matching statements were found.
	 * @throws GraphUtilException If such an exception is thrown by
	 *                            {@link #getOptionalObject(Graph, Resource, IRI, Resource[])} or if its return value is
	 *                            not a Literal.
	 * @deprecated since 2.8.0. Use {@link Model#filter(Resource, IRI, Value, Resource...) } and
	 *             {@link Model#objectLiteral() } instead.
	 */
	@Deprecated
	public static Literal getOptionalObjectLiteral(Graph graph, Resource subj, IRI pred) throws GraphUtilException {
		Value obj = getOptionalObject(graph, subj, pred);

		if (obj == null || obj instanceof Literal) {
			return (Literal) obj;
		} else {
			throw new GraphUtilException("Expected literal for property " + pred);
		}
	}

	/**
	 * Utility method that removes all statements matching the specified criteria from a graph.
	 * 
	 * @param graph    The graph to remove the statements from.
	 * @param subj     The subject of the statements to match, <tt>null</tt> to match statements with any subject.
	 * @param pred     The predicate of the statements to match, <tt>null</tt> to match statements with any predicate.
	 * @param obj      The object of the statements to match, <tt>null</tt> to match statements with any object.
	 * @param contexts The contexts of the statements to match. If no contexts are specified, statements will match
	 *                 disregarding their context. If one or more contexts are specified, statements with a context
	 *                 matching one of these will match.
	 * @throws IllegalArgumentException If a <tt>null</tt>-array is specified as the value for <tt>contexts</tt>. See
	 *                                  {@link OpenRDFUtil#verifyContextNotNull(Resource[])} for more info.
	 * @deprecated since 2.8.0. Use {@link Model#remove(Resource, IRI, Value, Resource...) } instead.
	 */
	@Deprecated
	public static void remove(Graph graph, Resource subj, IRI pred, Value obj, Resource... contexts) {
		Iterator<Statement> statements = graph.match(subj, pred, obj, contexts);
		while (statements.hasNext()) {
			statements.next();
			statements.remove();
		}
	}
}
