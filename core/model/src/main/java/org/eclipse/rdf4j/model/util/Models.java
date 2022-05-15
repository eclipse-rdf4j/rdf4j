/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Utility functions for working with {@link Model}s and other {@link Statement} collections.
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 * @see org.eclipse.rdf4j.model.Model
 * @see org.eclipse.rdf4j.model.util.ModelBuilder
 */
public class Models {

	/*
	 * hidden constructor to avoid instantiation
	 */
	protected Models() {
	}

	/**
	 * Retrieves an object {@link Value} from the supplied statements. If more than one possible object value exists,
	 * any one value is picked and returned.
	 *
	 * @param statements the {@link Statement } {@link Iterable} from which to retrieve an object value.
	 * @return an object value from the given statement collection, or {@link Optional#empty()} if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #object(Model)}.
	 */
	public static Optional<Value> object(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false).map(st -> st.getObject()).findAny();
	}

	/**
	 * Retrieves an object {@link Value} from the statements in the given model. If more than one possible object value
	 * exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object value.
	 * @return an object value from the given model, or {@link Optional#empty()} if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #object(Iterable)}. This method signature kept for binary
	 *          compatibility.
	 *
	 */
	public static Optional<Value> object(Model m) {
		return object((Iterable<Statement>) m);
	}

	/**
	 * Retrieves an object {@link Literal} value from the supplied statements. If more than one possible Literal value
	 * exists, any one Literal value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve an object Literal value.
	 * @return an object Literal value from the given model, or {@link Optional#empty()} if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectLiteral(Model)}.
	 */
	public static Optional<Literal> objectLiteral(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof Literal)
				.map(l -> (Literal) l)
				.findAny();
	}

	/**
	 * Retrieves an object {@link Literal} value from the statements in the given model. If more than one possible
	 * Literal value exists, any one Literal value is picked and returned.
	 *
	 * @param m the {@link Model} from which to retrieve an object Literal value.
	 * @return an object Literal value from the given model, or {@link Optional#empty()} if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectLiteral(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<Literal> objectLiteral(Model m) {
		return objectLiteral((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all object {@link Literal} values from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve all object {@link Literal}
	 *                   values.
	 * @return a {@link Set} containing object {@link Literal} values from the given model, which will be empty if no
	 *         such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectLiterals(Model)}.
	 * @see Model#objects()
	 */
	public static Set<Literal> objectLiterals(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof Literal)
				.map(l -> (Literal) l)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all object {@link Literal} values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object {@link Literal} values.
	 * @return a {@link Set} containing object {@link Literal} values from the given model, which will be empty if no
	 *         such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectLiterals(Iterable)}. This method signature kept
	 *          for binary compatibility.
	 *
	 * @see Model#objects()
	 */
	public static Set<Literal> objectLiterals(Model m) {
		return objectLiterals((Iterable<Statement>) m);
	}

	/**
	 * Retrieves an object {@link Resource} value from the supplied statements. If more than one possible Resource value
	 * exists, any one Resource value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve an object Resource value.
	 * @return an {@link Optional} object Resource value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectResource(Model)}.
	 */
	public static Optional<Resource> objectResource(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof Resource)
				.map(r -> (Resource) r)
				.findAny();
	}

	/**
	 * Retrieves an object {@link Resource} value from the statements in the given model. If more than one possible
	 * Resource value exists, any one Resource value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object Resource value.
	 * @return an {@link Optional} object Resource value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectResource(Iterable)}. This method signature kept
	 *          for binary compatibility.
	 */
	public static Optional<Resource> objectResource(Model m) {
		return objectResource((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all object {@link Resource} values from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve all object {@link Resource}
	 *                   values.
	 * @return a {@link Set} containing object {@link Resource} values from the given model, which will be empty if no
	 *         such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectResources(Model)}.
	 * @see Model#objects()
	 */
	public static Set<Resource> objectResources(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof Resource)
				.map(r -> (Resource) r)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all object {@link Resource} values from the supplied model.
	 *
	 * @param m the {@link Model} from which to retrieve all object {@link Resource} values.
	 * @return a {@link Set} containing object {@link Resource} values from the given model, which will be empty if no
	 *         such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectResources(Iterable)}. This method signature kept
	 *          for binary compatibility.
	 * @see Model#objects()
	 */
	public static Set<Resource> objectResources(Model m) {
		return objectResources((Iterable<Statement>) m);
	}

	/**
	 * Retrieves an object {@link IRI} value from the supplied statements. If more than one possible IRI value exists,
	 * any one value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve an object IRI value.
	 * @return an {@link Optional} object IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectIRI(Model)}.
	 */
	public static Optional<IRI> objectIRI(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof IRI)
				.map(r -> (IRI) r)
				.findAny();
	}

	/**
	 * Retrieves an object {@link IRI} value from the supplied statements in the given model. If more than one possible
	 * IRI value exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object IRI value.
	 * @return an {@link Optional} object IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectIRI(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<IRI> objectIRI(Model m) {
		return objectIRI((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all object {@link IRI} values from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve all object IRI values.
	 * @return a {@link Set} containing object IRI values from the given model, which will be empty if no such value
	 *         exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectIRIs(Model)}.
	 * @see Model#objects()
	 */
	public static Set<IRI> objectIRIs(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject())
				.filter(o -> o instanceof IRI)
				.map(r -> (IRI) r)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all object {@link IRI} values from the statements in the given model.
	 *
	 * @param m the {@link Model} from which to retrieve all object IRI values.
	 * @return a {@link Set} containing object IRI values from the given model, which will be empty if no such value
	 *         exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectIRIs(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 * @see Model#objects()
	 */
	public static Set<IRI> objectIRIs(Model m) {
		return objectIRIs((Iterable<Statement>) m);
	}

	/**
	 * Retrieves an object value as a String from the supplied statements. If more than one possible object value
	 * exists, any one value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve an object String value.
	 * @return an {@link Optional} object String value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectString(Model)}.
	 */
	public static Optional<String> objectString(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false).map(st -> st.getObject().stringValue()).findAny();
	}

	/**
	 * Retrieves an object value as a String from the statements in the given model. If more than one possible object
	 * value exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object String value.
	 * @return an {@link Optional} object String value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectString(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<String> objectString(Model m) {
		return objectString((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all object String values from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve all object String values.
	 * @return a {@link Set} containing object String values from the given model, which will be empty if no such value
	 *         exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #objectStrings(Model)}.
	 * @see Model#objects()
	 */
	public static Set<String> objectStrings(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getObject().stringValue())
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all object String values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object String values.
	 * @return a {@link Set} containing object String values from the given model, which will be empty if no such value
	 *         exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #objectStrings(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 * @see Model#objects()
	 */
	public static Set<String> objectStrings(Model m) {
		return objectStrings((Iterable<Statement>) m);
	}

	/**
	 * Retrieves a subject {@link Resource} from the supplied statements. If more than one possible resource value
	 * exists, any one resource value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a subject Resource.
	 * @return an {@link Optional} subject resource from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #subject(Model)}.
	 */
	public static Optional<Resource> subject(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false).map(st -> st.getSubject()).findAny();
	}

	/**
	 * Retrieves a subject {@link Resource} from the statements in the given model. If more than one possible resource
	 * value exists, any one resource value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject Resource.
	 * @return an {@link Optional} subject resource from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #subject(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<Resource> subject(Model m) {
		return subject((Iterable<Statement>) m);
	}

	/**
	 * Retrieves a subject {@link IRI} from the supplied statements. If more than one possible IRI value exists, any one
	 * IRI value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a subject IRI value.
	 * @return an {@link Optional} subject IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #subjectIRI(Model)}.
	 */
	public static Optional<IRI> subjectIRI(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getSubject())
				.filter(s -> s instanceof IRI)
				.map(s -> (IRI) s)
				.findAny();
	}

	/**
	 * Retrieves a subject {@link IRI} from the statements in the given model. If more than one possible IRI value
	 * exists, any one IRI value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return an {@link Optional} subject IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #subjectIRI(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<IRI> subjectIRI(Model m) {
		return subjectIRI((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all subject {@link IRI}s from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject IRI values from the given model. The returned Set may be empty.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #subjectIRIs(Model)}.
	 */
	public static Set<IRI> subjectIRIs(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getSubject())
				.filter(o -> o instanceof IRI)
				.map(r -> (IRI) r)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all subject {@link IRI}s from the statements in the given model.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject IRI values from the given model. The returned Set may be empty.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #subjectIRIs(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Set<IRI> subjectIRIs(Model m) {
		return subjectIRIs((Iterable<Statement>) m);
	}

	/**
	 * Retrieves a subject {@link BNode} from the supplied statements. If more than one possible blank node value
	 * exists, any one blank node value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a subject BNode value.
	 * @return an {@link Optional} subject BNode value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #subjectBNode(Model)}.
	 */
	public static Optional<BNode> subjectBNode(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getSubject())
				.filter(s -> s instanceof BNode)
				.map(s -> (BNode) s)
				.findAny();
	}

	/**
	 * Retrieves a subject {@link BNode} from the statements in the given model. If more than one possible blank node
	 * value exists, any one blank node value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject BNode value.
	 * @return an {@link Optional} subject BNode value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #subjectBNode(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<BNode> subjectBNode(Model m) {
		return subjectBNode((Iterable<Statement>) m);
	}

	/**
	 * Retrieves all subject {@link BNode}s from the supplied statements.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject {@link BNode} values from the given model. The returned Set may be empty.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #subjectBNodes(Model)}.
	 */
	public static Set<BNode> subjectBNodes(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false)
				.map(st -> st.getSubject())
				.filter(o -> o instanceof BNode)
				.map(r -> (BNode) r)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves all subject {@link BNode}s from the statements in the given model.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject {@link BNode} values from the given model. The returned Set may be empty.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #subjectBNodes(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Set<BNode> subjectBNodes(Model m) {
		return subjectBNodes((Iterable<Statement>) m);
	}

	/**
	 * Retrieves a predicate from the supplied statements. If more than one possible predicate value exists, any one
	 * value is picked and returned.
	 *
	 * @param statements the {@link Statement} {@link Iterable} from which to retrieve a predicate value.
	 * @return an {@link Optional} predicate value from the given model, which will be {@link Optional#empty() empty} if
	 *         no such value exists.
	 * @apiNote this method signature is new in 3.2.0, and is a generalization of {@link #predicate(Model)}.
	 */
	public static Optional<IRI> predicate(Iterable<Statement> statements) {
		return StreamSupport.stream(statements.spliterator(), false).map(st -> st.getPredicate()).findAny();
	}

	/**
	 * Retrieves a predicate from the statements in the given model. If more than one possible predicate value exists,
	 * any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve a predicate value.
	 * @return an {@link Optional} predicate value from the given model, which will be {@link Optional#empty() empty} if
	 *         no such value exists.
	 * @apiNote replaced in 3.2.0 with the more generic {@link #predicate(Iterable)}. This method signature kept for
	 *          binary compatibility.
	 */
	public static Optional<IRI> predicate(Model m) {
		return predicate((Iterable<Statement>) m);
	}

	/**
	 * Sets the property value for the given subject to the given object value, replacing any existing value(s) for the
	 * subject's property. This method updates the original input Model and then returns that same Model object.
	 *
	 * @param m        the model in which to set the property value. May not be null.
	 * @param subject  the subject for which to set/replace the property value. May not be null.
	 * @param property the property for which to set/replace the value. May not be null.
	 * @param value    the value to set for the given subject and property. May not be null.
	 * @param contexts the context(s) in which to set/replace the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return the Model object, containing the updated property value.
	 */
	public static Model setProperty(Model m, Resource subject, IRI property, Value value, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		Objects.requireNonNull(value, "value may not be null");

		if (m.contains(subject, property, null, contexts)) {
			m.remove(subject, property, null, contexts);
		}
		m.add(subject, property, value, contexts);
		return m;
	}

	/**
	 * Retrieve a property value for the supplied subject from the given model. If more than one property value exists,
	 * any one value is picked and returned.
	 *
	 * @param m        the model from which to retrieve an object value.
	 * @param subject  the subject resource for which to retrieve a property value.
	 * @param property the property for which to retrieve a value.
	 * @param contexts the contexts from which to retrieve the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a property value from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<Value> getProperty(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return object(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve all property values for the supplied subject and property from the given model.
	 *
	 * @param m        the model from which to retrieve the property values.
	 * @param subject  the subject resource for which to retrieve all property values.
	 * @param property the property for which to retrieve all values.
	 * @param contexts the contexts from which to retrieve the property values. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a Set of all property values for the supplied input. The resulting set may be empty.
	 */
	public static Set<Value> getProperties(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return m.filter(subject, property, null, contexts).objects();
	}

	/**
	 * Retrieve a property value as an IRI for the supplied subject from the given model. If more than one property
	 * value exists, any one value is picked and returned.
	 *
	 * @param m        the model from which to retrieve an object value.
	 * @param subject  the subject resource for which to retrieve a property value.
	 * @param property the property for which to retrieve a value.
	 * @param contexts the contexts from which to retrieve the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a property value Resource from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<Resource> getPropertyResource(Model m, Resource subject, IRI property,
			Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectResource(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve all property Resource values for the supplied subject and property from the given model.
	 *
	 * @param m        the model from which to retrieve the property Resource values.
	 * @param subject  the subject resource for which to retrieve all property Resource values.
	 * @param property the property for which to retrieve all Resource values.
	 * @param contexts the contexts from which to retrieve the property values. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a Set of all property Resource values for the supplied input. The resulting set may be empty.
	 */
	public static Set<Resource> getPropertyResources(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectResources(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve a property value as an IRI for the supplied subject from the given model. If more than one property
	 * value exists, any one value is picked and returned.
	 *
	 * @param m        the model from which to retrieve an object value.
	 * @param subject  the subject resource for which to retrieve a property value.
	 * @param property the property for which to retrieve a value.
	 * @param contexts the contexts from which to retrieve the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a property value IRI from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<IRI> getPropertyIRI(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectIRI(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve all property IRI values for the supplied subject and property from the given model.
	 *
	 * @param m        the model from which to retrieve the property IRI values.
	 * @param subject  the subject resource for which to retrieve all property IRI values.
	 * @param property the property for which to retrieve all IRI values.
	 * @param contexts the contexts from which to retrieve the property values. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a Set of all property IRI values for the supplied input. The resulting set may be empty.
	 */
	public static Set<IRI> getPropertyIRIs(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectIRIs(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve a property value as a {@link Literal} for the supplied subject from the given model. If more than one
	 * property value exists, any one value is picked and returned.
	 *
	 * @param m        the model from which to retrieve an object value.
	 * @param subject  the subject resource for which to retrieve a property literal value.
	 * @param property the property for which to retrieve a value.
	 * @param contexts the contexts from which to retrieve the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a property value Literal from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<Literal> getPropertyLiteral(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectLiteral(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve all property Literal values for the supplied subject and property from the given model.
	 *
	 * @param m        the model from which to retrieve the property Literal values.
	 * @param subject  the subject resource for which to retrieve all property Literal values.
	 * @param property the property for which to retrieve all Literal values.
	 * @param contexts the contexts from which to retrieve the property values. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a Set of all property IRI values for the supplied input. The resulting set may be empty.
	 */
	public static Set<Literal> getPropertyLiterals(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectLiterals(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve a property value as a String for the supplied subject from the given model. If more than one property
	 * value exists, any one value is picked and returned.
	 *
	 * @param m        the model from which to retrieve an object value.
	 * @param subject  the subject resource for which to retrieve a property literal value.
	 * @param property the property for which to retrieve a value.
	 * @param contexts the contexts from which to retrieve the property value. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a property value String from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<String> getPropertyString(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectString(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Retrieve all property values as Strings for the supplied subject and property from the given model.
	 *
	 * @param m        the model from which to retrieve the property values as Strings.
	 * @param subject  the subject resource for which to retrieve all property values as Strings.
	 * @param property the property for which to retrieve all values as Strings.
	 * @param contexts the contexts from which to retrieve the property values. Optional vararg argument. If not
	 *                 specified the operations works on the entire Model.
	 * @return a Set of all property values as Strings for the supplied input. The resulting set may be empty.
	 */
	public static Set<String> getPropertyStrings(Model m, Resource subject, IRI property, Resource... contexts) {
		Objects.requireNonNull(m, "model may not be null");
		Objects.requireNonNull(subject, "subject may not be null");
		Objects.requireNonNull(property, "property may not be null");
		return objectStrings(m.getStatements(subject, property, null, contexts));
	}

	/**
	 * Compares two RDF models, and returns <var>true</var> if they consist of isomorphic graphs and the isomorphic
	 * graph identifiers map 1:1 to each other. RDF graphs are isomorphic graphs if statements from one graphs can be
	 * mapped 1:1 on to statements in the other graphs. In this mapping, blank nodes are not considered mapped when
	 * having an identical internal id, but are mapped from one graph to the other by looking at the statements in which
	 * the blank nodes occur. A Model can consist of more than one graph (denoted by context identifiers). Two models
	 * are considered isomorphic if for each of the graphs in one model, an isomorphic graph exists in the other model,
	 * and the context identifiers of these graphs are either identical or (in the case of blank nodes) map 1:1 on each
	 * other.
	 *
	 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#graph-isomorphism">RDF Concepts &amp; Abstract Syntax, section
	 *      3.6 (Graph Comparison)</a>
	 */
	public static boolean isomorphic(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		if (model1 == model2) {
			return true;
		}

		Model set1 = toModel(model1);
		Model set2 = toModel(model2);

		return GraphComparisons.isomorphic(set1, set2);
	}

	/**
	 * Legacy implementation of {@link #isomorphic(Iterable, Iterable) isomorphic comparison}. This method is offered as
	 * a temporary fallback for corner cases where the newly introduced isomorphism algorithm (in release 3.6.0) has
	 * worse performance or an unexpected result.
	 *
	 * @apiNote This method is offered as a temporary fallback only, and will likely be removed again quite soon in a
	 *          future minor or major release.
	 * @implNote This uses an algorithm that has poor performance in many cases and can potentially get stuck in an
	 *           endless loop. We <strong>strongly recommend</strong> using the new algorithm available in the
	 *           {@link #isomorphic(Iterable, Iterable)} implementation.
	 *
	 * @deprecated since 3.6.0 - use {@link #isomorphic(Iterable, Iterable)} instead.
	 *
	 * @since 3.6.0
	 * @see #isomorphic(Iterable, Iterable)
	 */
	@Experimental
	@Deprecated
	public static boolean legacyIsomorphic(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		if (model1 == model2) {
			return true;
		}

		Set<Statement> set1 = toSet(model1);
		Model set2 = toModel(model2);
		// Compare the number of statements in both sets
		if (set1.size() != set2.size()) {
			return false;
		}

		return isSubsetInternal(set1, set2);
	}

	/**
	 * Compares two RDF models, and returns <var>true</var> if the first model is a subset of the second model, using
	 * graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		// Filter duplicates
		Set<Statement> set1 = toSet(model1);
		Model set2 = toModel(model2);

		return isSubset(set1, set2);
	}

	/**
	 * Compares two RDF models, and returns <var>true</var> if the first model is a subset of the second model, using
	 * graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Set<? extends Statement> model1, Set<? extends Statement> model2) {
		// Compare the number of statements in both sets
		if (model1.size() > model2.size()) {
			return false;
		}

		return isSubsetInternal(toSet(model1), toModel(model2));
	}

	/**
	 * Strips contexts from the input model. This method provides a new {@link Model} containing all statements from the
	 * input model, with the supplied contexts removed from those statements.
	 *
	 * @param model    the input model
	 * @param contexts the contexts to remove. This is a vararg and as such is optional. If not supplied, the method
	 *                 strips <i>all</i> contexts.
	 * @return a new {@link Model} object containg the same statements as the input model, with the supplied contexts
	 *         stripped.
	 */
	public static Model stripContexts(Model model, Resource... contexts) {
		final List<Resource> contextList = Arrays.asList(contexts);
		return model.stream().map(st -> {
			if (contextList.isEmpty() || contextList.contains(st.getContext())) {
				return Statements.stripContext(st);
			} else {
				return st;
			}
		}).collect(ModelCollector.toModel());
	}

	/**
	 * Creates a {@link Supplier} of {@link ModelException} objects that be passed to
	 * {@link Optional#orElseThrow(Supplier)} to generate exceptions as necessary.
	 *
	 * @param message The message to be used for the exception
	 * @return A {@link Supplier} that will create {@link ModelException} objects with the given message.
	 */
	public static Supplier<ModelException> modelException(String message) {
		return () -> new ModelException(message);
	}

	/**
	 * Make a model thread-safe by synchronizing all its methods. Iterators will still not be thread-safe!
	 *
	 * @param toSynchronize the model that should be synchronized
	 * @return Synchronized Model
	 */
	public static Model synchronizedModel(Model toSynchronize) {
		return new SynchronizedModel(toSynchronize);
	}

	/**
	 * Converts the supplied RDF-star model to RDF reification statements. The converted statements are sent to the
	 * supplied consumer function.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf       the {@link ValueFactory} to use for creating statements.
	 * @param model    the {@link Model} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDFStarToReification(ValueFactory vf, Model model, Consumer<Statement> consumer) {
		model.forEach(st -> Statements.convertRDFStarToReification(vf, st, consumer));
	}

	/**
	 * Converts the supplied RDF-star model to RDF reification statements. The converted statements are sent to the
	 * supplied consumer function.
	 *
	 * @param model    the {@link Model} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertRDFStarToReification(Model model, Consumer<Statement> consumer) {
		convertRDFStarToReification(SimpleValueFactory.getInstance(), model, consumer);
	}

	/**
	 * Converts the statements in supplied RDF-star model to a new RDF model using reificiation.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf    the {@link ValueFactory} to use for creating statements.
	 * @param model the {@link Model} to convert.
	 * @return a new {@link Model} with RDF-star statements converted to reified triples.
	 */
	@Experimental
	public static Model convertRDFStarToReification(ValueFactory vf, Model model) {
		Model reificationModel = new LinkedHashModel();
		convertRDFStarToReification(vf, model, (Consumer<Statement>) reificationModel::add);
		return reificationModel;
	}

	/**
	 * Converts the statements in supplied RDF-star model to a new RDF model using reificiation.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf           the {@link ValueFactory} to use for creating statements.
	 * @param model        the {@link Model} to convert.
	 * @param modelFactory the {@link ModelFactory} used to create the new output {@link Model}.
	 * @return a new {@link Model} with RDF-star statements converted to reified triples.
	 */
	@Experimental
	public static Model convertRDFStarToReification(ValueFactory vf, Model model, ModelFactory modelFactory) {
		Model reificationModel = modelFactory.createEmptyModel();
		convertRDFStarToReification(vf, model, (Consumer<Statement>) reificationModel::add);
		return reificationModel;
	}

	/**
	 * Converts the statements in the supplied RDF-star model to a new RDF model using reification.
	 *
	 * @param model the {@link Model} to convert.
	 * @return a new {@link Model} with RDF-star statements converted to reified triples.
	 */
	@Experimental
	public static Model convertRDFStarToReification(Model model) {
		return convertRDFStarToReification(SimpleValueFactory.getInstance(), model);
	}

	/**
	 * Converts the supplied RDF reification model to RDF-star statements. The converted statements are sent to the
	 * supplied consumer function.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf       the {@link ValueFactory} to use for creating statements.
	 * @param model    the {@link Model} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertReificationToRDFStar(ValueFactory vf, Model model, Consumer<Statement> consumer) {
		Map<Resource, Triple> convertedStatements = new HashMap<>();
		model.filter(null, RDF.TYPE, RDF.STATEMENT).forEach((s) -> {
			Value subject = object(model.filter(s.getSubject(), RDF.SUBJECT, null)).orElse(null);
			if (!(subject instanceof IRI) && !(subject instanceof BNode)) {
				return;
			}
			Value predicate = object(model.filter(s.getSubject(), RDF.PREDICATE, null)).orElse(null);
			if (!(predicate instanceof IRI)) {
				return;
			}
			Value object = object(model.filter(s.getSubject(), RDF.OBJECT, null)).orElse(null);
			if (!(object instanceof Value)) {
				return;
			}
			Triple t = vf.createTriple((Resource) subject, (IRI) predicate, object);
			convertedStatements.put(s.getSubject(), t);
		});

		for (Map.Entry<Resource, Triple> e : convertedStatements.entrySet()) {
			Triple t = e.getValue();
			Resource subject = convertedStatements.get(t.getSubject());
			Resource object = convertedStatements.get(t.getObject());
			if (subject != null || object != null) {
				// Triples within triples, replace them in the map
				Triple nt = vf.createTriple(subject != null ? subject : t.getSubject(), t.getPredicate(),
						object != null ? object : t.getObject());
				e.setValue(nt);
			}
		}

		model.forEach((s) -> {
			Resource subject = s.getSubject();
			IRI predicate = s.getPredicate();
			Value object = s.getObject();
			Triple subjectTriple = convertedStatements.get(subject);
			Triple objectTriple = convertedStatements.get(object);

			if (subjectTriple == null && objectTriple == null) {
				// Statement not part of detected reification, add it as is
				consumer.accept(s);
			} else if (subjectTriple == null || ((!RDF.TYPE.equals(predicate) || !RDF.STATEMENT.equals(object))
					&& !RDF.SUBJECT.equals(predicate) && !RDF.PREDICATE.equals(predicate)
					&& !RDF.OBJECT.equals(predicate))) {
				// Statement uses reified data and needs to be converted
				Statement ns = vf.createStatement(subjectTriple != null ? subjectTriple : s.getSubject(),
						s.getPredicate(), objectTriple != null ? objectTriple : s.getObject(), s.getContext());
				consumer.accept(ns);
			} // else: Statement part of reification and needs to be removed (skipped)
		});
	}

	/**
	 * Converts the supplied RDF reification model to RDF-star statements. The converted statements are sent to the
	 * supplied consumer function.
	 *
	 * @param model    the {@link Model} to convert.
	 * @param consumer the {@link Consumer} function for the produced statements.
	 */
	@Experimental
	public static void convertReificationToRDFStar(Model model, Consumer<Statement> consumer) {
		convertReificationToRDFStar(SimpleValueFactory.getInstance(), model, consumer);
	}

	/**
	 * Converts the statements in supplied RDF reification model to a new RDF-star model.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf           the {@link ValueFactory} to use for creating statements.
	 * @param model        the {@link Model} to convert.
	 * @param modelFactory the {@link ModelFactory} to use for creating a new Model object for the output.
	 * @return a new {@link Model} with reification statements converted to RDF-star {@link Triple}s.
	 */
	@Experimental
	public static Model convertReificationToRDFStar(ValueFactory vf, Model model, ModelFactory modelFactory) {
		Model rdfStarModel = modelFactory.createEmptyModel();
		convertReificationToRDFStar(vf, model, (Consumer<Statement>) rdfStarModel::add);
		return rdfStarModel;
	}

	/**
	 * Converts the statements in supplied RDF reification model to a new RDF-star model.
	 * <p>
	 * The supplied value factory is used to create all new statements.
	 *
	 * @param vf    the {@link ValueFactory} to use for creating statements.
	 * @param model the {@link Model} to convert.
	 * @return a new {@link Model} with reification statements converted to RDF-star {@link Triple}s.
	 */
	@Experimental
	public static Model convertReificationToRDFStar(ValueFactory vf, Model model) {
		return convertReificationToRDFStar(vf, model, new DynamicModelFactory());
	}

	/**
	 * Converts the supplied RDF reification model to a new RDF-star model.
	 *
	 * @param model the {@link Model} to convert.
	 * @return a new {@link Model} with reification statements converted to RDF-star {@link Triple}s.
	 */
	@Experimental
	public static Model convertReificationToRDFStar(Model model) {
		return convertReificationToRDFStar(SimpleValueFactory.getInstance(), model);
	}

	private static boolean isSubsetInternal(Set<Statement> model1, Model model2) {
		// try to create a full blank node mapping
		return matchModels(model1, model2);
	}

	private static boolean matchModels(Set<Statement> model1, Model model2) {
		// Compare statements without blank nodes first, save the rest for later
		List<Statement> model1BNodes = new ArrayList<>(model1.size());

		for (Statement st : model1) {
			if (isBlank(st.getSubject()) || isBlank(st.getObject()) || isBlank(st.getContext())) {
				model1BNodes.add(st);
			} else {
				if (!model2.contains(st)) {
					return false;
				}
			}
		}

		return matchModels(Collections.unmodifiableList(model1BNodes), model2);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in model1 and blank nodes in model2. The
	 * algorithm does a depth-first search trying to establish a mapping for each blank node occurring in model1.
	 *
	 * @param model1
	 * @param model2
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchModels(final List<? extends Statement> model1, final Model model2) {

		ArrayDeque<Iterator<Statement>> iterators = new ArrayDeque<>();
		ArrayDeque<Map<Resource, Resource>> bNodeMappings = new ArrayDeque<>();

		Map<Resource, Resource> bNodeMapping = Collections.emptyMap();
		int idx = 0;

		Iterator<Statement> iterator = null;
		while (true) {

			if (idx >= model1.size()) {
				return true;
			}

			Statement st1 = model1.get(idx);

			if (iterator == null) {

				List<Statement> matchingStats = findMatchingStatements(st1, model2, bNodeMapping);

				iterator = matchingStats.iterator();
			}

			if (iterator.hasNext()) {
				Statement st2 = iterator.next();

				// Map bNodes in st1 to bNodes in st2
				Map<Resource, Resource> newBNodeMapping = createNewBnodeMapping(bNodeMapping, st1, st2);

				iterators.addLast(iterator);
				bNodeMappings.addLast(bNodeMapping);

				iterator = null;

				bNodeMapping = newBNodeMapping;
				idx++;

			}

			if (iterator != null) {
				idx--;
				if (idx < 0) {
					return false;
				}
				iterator = iterators.removeLast();
				bNodeMapping = bNodeMappings.removeLast();
			}

		}

	}

	private static Map<Resource, Resource> createNewBnodeMapping(Map<Resource, Resource> bNodeMapping, Statement st1,
			Statement st2) {
		Map<Resource, Resource> newBNodeMapping = new HashMap<>(bNodeMapping);

		if (isBlank(st1.getSubject()) && isBlank(st2.getSubject())) {
			newBNodeMapping.put(st1.getSubject(), st2.getSubject());
		}

		if (isBlank(st1.getObject()) && isBlank(st2.getObject())) {
			newBNodeMapping.put((Resource) st1.getObject(), (Resource) st2.getObject());
		}

		if (isBlank(st1.getContext()) && isBlank(st2.getContext())) {
			newBNodeMapping.put(st1.getContext(), st2.getContext());
		}
		return newBNodeMapping;
	}

	private static List<Statement> findMatchingStatements(Statement st, Model model,
			Map<Resource, Resource> bNodeMapping) {
		Resource s = isBlank(st.getSubject()) ? null : st.getSubject();
		IRI p = st.getPredicate();
		Value o = isBlank(st.getObject()) ? null : st.getObject();
		Resource[] g = isBlank(st.getContext()) ? new Resource[0] : new Resource[] { st.getContext() };
		List<Statement> result = new ArrayList<>();

		for (Statement modelSt : model.filter(s, p, o, g)) {
			if (statementsMatch(st, modelSt, bNodeMapping)) {
				// All components possibly match
				result.add(modelSt);
			}
		}

		return Collections.unmodifiableList(result);
	}

	private static boolean statementsMatch(Statement st1, Statement st2, Map<Resource, Resource> bNodeMapping) {
		IRI pred1 = st1.getPredicate();
		IRI pred2 = st2.getPredicate();

		if (!pred1.equals(pred2)) {
			// predicates don't match
			return false;
		}

		Resource subj1 = st1.getSubject();
		Resource subj2 = st2.getSubject();

		if (bnodeValueMatching(bNodeMapping, subj1, subj2)) {
			return false;
		}

		Value obj1 = st1.getObject();
		Value obj2 = st2.getObject();

		if (bnodeValueMatching(bNodeMapping, obj1, obj2)) {
			return false;
		}

		Resource context1 = st1.getContext();
		Resource context2 = st2.getContext();

		// no match if in different contexts
		if (context1 == null) {
			return context2 == null;
		} else if (context2 == null) {
			return false;
		}

		if (bnodeValueMatching(bNodeMapping, context1, context2)) {
			return false;
		}

		return true;
	}

	private static boolean bnodeValueMatching(Map<Resource, Resource> bNodeMapping, Value obj1, Value obj2) {
		if (isBlank(obj1) && isBlank(obj2)) {
			Resource mappedBNode = bNodeMapping.get(obj1);

			if (mappedBNode != null) {
				// bNode 'obj1' was already mapped to some other bNode
				// 'obj1' and 'obj2' do not match
				return !obj2.equals(mappedBNode);
			} else {
				// 'obj1' was not yet mapped. we need to check if 'obj2' is a
				// possible mapping candidate
				// 'obj2' is already mapped to some other value.
				return bNodeMapping.containsValue(obj2);
			}
		} else {
			// objects are not (both) bNodes
			return !obj1.equals(obj2);
		}
	}

	private static boolean isBlank(Value value) {
		if (value instanceof BNode) {
			return true;
		}

		if (value instanceof IRI) {
			boolean skolemizedBNode = value.stringValue().contains("/.well-known/genid/");
			return skolemizedBNode;
		}
		return false;
	}

	private static Model toModel(Iterable<? extends Statement> iterable) {
		if (iterable instanceof Model) {
			return (Model) iterable;
		}

		final Model set;
		if (iterable instanceof Collection) {
			int size = ((Collection<? extends Statement>) iterable).size();
			set = new LinkedHashModel(size);
		} else {
			set = new LinkedHashModel();
		}

		StreamSupport.stream(iterable.spliterator(), false).filter(Objects::nonNull).forEach(set::add);
		return set;
	}

	private static Set<Statement> toSet(Iterable<? extends Statement> iterable) {
		if (iterable instanceof Set) {
			return (Set<Statement>) iterable;
		}

		if (iterable instanceof Collection) {
			return new HashSet<>((Collection<? extends Statement>) iterable);
		} else {
			HashSet<Statement> statements = new HashSet<>();
			StreamSupport.stream(iterable.spliterator(), false).filter(Objects::nonNull).forEach(statements::add);
			return statements;
		}

	}

}
