/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.util.iterators.Iterators;

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
	 * Retrieves an object {@link Value} from the statements in the given model. If more than one possible object value
	 * exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object value.
	 * @return an object value from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<Value> object(Model m) {
		return m.stream().map(st -> st.getObject()).findAny();
	}

	/**
	 * @deprecated since 4.0. Use {@link #object(Model)} instead.
	 */
	@Deprecated
	public static Value anyObject(Model m) {
		return object(m).orElse(null);
	}

	/**
	 * Retrieves an object {@link Literal} value from the statements in the given model. If more than one possible
	 * Literal value exists, any one Literal value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object Literal value.
	 * @return an object Literal value from the given model, or {@link Optional#empty()} if no such value exists.
	 */
	public static Optional<Literal> objectLiteral(Model m) {
		return m.stream().map(st -> st.getObject()).filter(o -> o instanceof Literal).map(l -> (Literal) l).findAny();
	}

	/**
	 * Retrieves all object {@link Literal} values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object {@link Literal} values.
	 * @return a {@link Set} containing object {@link Literal} values from the given model, which will be empty if no
	 *         such value exists.
	 * @see Model#objects()
	 */
	public static Set<Literal> objectLiterals(Model m) {
		return m.stream()
				.map(st -> st.getObject())
				.filter(o -> o instanceof Literal)
				.map(l -> (Literal) l)
				.collect(Collectors.toSet());
	}

	/**
	 * @deprecated since 4.0. Use {@link #objectLiteral(Model)} instead.
	 */
	@Deprecated
	public static Literal anyObjectLiteral(Model m) {
		return objectLiteral(m).orElse(null);
	}

	/**
	 * Retrieves an object {@link Resource} value from the statements in the given model. If more than one possible
	 * Resource value exists, any one Resource value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object Resource value.
	 * @return an {@link Optional} object Resource value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 */
	public static Optional<Resource> objectResource(Model m) {
		return m.stream().map(st -> st.getObject()).filter(o -> o instanceof Resource).map(r -> (Resource) r).findAny();
	}

	/**
	 * Retrieves all object {@link Resource} values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object {@link Resource} values.
	 * @return a {@link Set} containing object {@link Resource} values from the given model, which will be empty if no
	 *         such value exists.
	 * @see Model#objects()
	 */
	public static Set<Resource> objectResources(Model m) {
		return m.stream()
				.map(st -> st.getObject())
				.filter(o -> o instanceof Resource)
				.map(r -> (Resource) r)
				.collect(Collectors.toSet());
	}

	/**
	 * @deprecated since 4.0. Use {@link #objectResource(Model)} instead.
	 */
	@Deprecated
	public static Resource anyObjectResource(Model m) {
		return objectResource(m).orElse(null);
	}

	/**
	 * Retrieves an object {@link IRI} value from the statements in the given model. If more than one possible IRI value
	 * exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object IRI value.
	 * @return an {@link Optional} object IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 */
	public static Optional<IRI> objectIRI(Model m) {
		return m.stream().map(st -> st.getObject()).filter(o -> o instanceof IRI).map(r -> (IRI) r).findAny();
	}

	/**
	 * Retrieves all object {@link IRI} values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object IRI values.
	 * @return a {@link Set} containing object IRI values from the given model, which will be empty if no such value
	 *         exists.
	 * @see Model#objects()
	 */
	public static Set<IRI> objectIRIs(Model m) {
		return m.stream()
				.map(st -> st.getObject())
				.filter(o -> o instanceof IRI)
				.map(r -> (IRI) r)
				.collect(Collectors.toSet());
	}

	/**
	 * Retrieves an object value as a String from the statements in the given model. If more than one possible object
	 * value exists, any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve an object String value.
	 * @return an {@link Optional} object String value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 */
	public static Optional<String> objectString(Model m) {
		return m.stream().map(st -> st.getObject().stringValue()).findAny();
	}

	/**
	 * Retrieves all object String values from the statements in the given model.
	 *
	 * @param m the model from which to retrieve all object String values.
	 * @return a {@link Set} containing object String values from the given model, which will be empty if no such value
	 *         exists.
	 * @see Model#objects()
	 */
	public static Set<String> objectStrings(Model m) {
		return m.stream().map(st -> st.getObject().stringValue()).collect(Collectors.toSet());
	}

	/**
	 * @deprecated since 4.0. Use {@link #objectIRI(Model)} instead.
	 */
	@Deprecated
	public static URI anyObjectURI(Model m) {
		return objectIRI(m).orElse(null);
	}

	/**
	 * Retrieves a subject {@link Resource} from the statements in the given model. If more than one possible resource
	 * value exists, any one resource value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject Resource.
	 * @return an {@link Optional} subject resource from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 */
	public static Optional<Resource> subject(Model m) {
		return m.stream().map(st -> st.getSubject()).findAny();
	}

	/**
	 * @deprecated since 4.0. Use {@link #subject(Model)} instead.
	 */
	@Deprecated
	public static Resource anySubject(Model m) {
		return subject(m).orElse(null);
	}

	/**
	 * Retrieves a subject {@link IRI} from the statements in the given model. If more than one possible IRI value
	 * exists, any one IRI value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return an {@link Optional} subject IRI value from the given model, which will be {@link Optional#empty() empty}
	 *         if no such value exists.
	 */
	public static Optional<IRI> subjectIRI(Model m) {
		return m.stream().map(st -> st.getSubject()).filter(s -> s instanceof IRI).map(s -> (IRI) s).findAny();
	}

	/**
	 * Retrieves all subject {@link IRI}s from the statements in the given model.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject IRI values from the given model. The returned Set may be empty.
	 */
	public static Set<IRI> subjectIRIs(Model m) {
		return m.subjects().stream().filter(s -> s instanceof IRI).map(s -> (IRI) s).collect(Collectors.toSet());
	}

	/**
	 * @deprecated since 4.0. Use {@link #subjectIRI(Model)} instead.
	 */
	@Deprecated
	public static URI anySubjectURI(Model m) {
		return subjectIRI(m).orElse(null);
	}

	/**
	 * Retrieves a subject {@link BNode} from the statements in the given model. If more than one possible blank node
	 * value exists, any one blank node value is picked and returned.
	 *
	 * @param m the model from which to retrieve a subject BNode value.
	 * @return an {@link Optional} subject BNode value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 */
	public static Optional<BNode> subjectBNode(Model m) {
		return m.stream().map(st -> st.getSubject()).filter(s -> s instanceof BNode).map(s -> (BNode) s).findAny();
	}

	/**
	 * Retrieves all subject {@link BNode}s from the statements in the given model.
	 *
	 * @param m the model from which to retrieve a subject IRI value.
	 * @return a {@link Set} of subject {@link BNode} values from the given model. The returned Set may be empty.
	 */
	public static Set<BNode> subjectBNodes(Model m) {
		return m.subjects().stream().filter(s -> s instanceof BNode).map(s -> (BNode) s).collect(Collectors.toSet());
	}

	/**
	 * @deprecated since 4.0. Use {@link #subjectBNode(Model)} instead.
	 */
	@Deprecated
	public static BNode anySubjectBNode(Model m) {
		return subjectBNode(m).orElse(null);
	}

	/**
	 * Retrieves a predicate from the statements in the given model. If more than one possible predicate value exists,
	 * any one value is picked and returned.
	 *
	 * @param m the model from which to retrieve a predicate value.
	 * @return an {@link Optional} predicate value from the given model, which will be {@link Optional#empty() empty} if
	 *         no such value exists.
	 */
	public static Optional<IRI> predicate(Model m) {
		return m.stream().map(st -> st.getPredicate()).findAny();
	}

	/**
	 * @deprecated since 4.0. Use {@link #predicate(Model)} instead.
	 */
	@Deprecated
	public static URI anyPredicate(Model m) {
		return predicate(m).orElse(null);
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
		return object(m.filter(subject, property, null, contexts));
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
		return objectResource(m.filter(subject, property, null, contexts));
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
		return objectResources(m.filter(subject, property, null, contexts));
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
		return objectIRI(m.filter(subject, property, null, contexts));
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
		return objectIRIs(m.filter(subject, property, null, contexts));
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
		return objectLiteral(m.filter(subject, property, null, contexts));
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
		return objectLiterals(m.filter(subject, property, null, contexts));
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
		return objectString(m.filter(subject, property, null, contexts));
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
		return objectStrings(m.filter(subject, property, null, contexts));
	}

	/**
	 * Compares two RDF models, and returns <tt>true</tt> if they consist of isomorphic graphs and the isomorphic graph
	 * identifiers map 1:1 to each other. RDF graphs are isomorphic graphs if statements from one graphs can be mapped
	 * 1:1 on to statements in the other graphs. In this mapping, blank nodes are not considered mapped when having an
	 * identical internal id, but are mapped from one graph to the other by looking at the statements in which the blank
	 * nodes occur. A Model can consist of more than one graph (denoted by context identifiers). Two models are
	 * considered isomorphic if for each of the graphs in one model, an isomorphic graph exists in the other model, and
	 * the context identifiers of these graphs are either identical or (in the case of blank nodes) map 1:1 on each
	 * other.
	 *
	 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#graph-isomorphism">RDF Concepts &amp; Abstract Syntax, section
	 *      3.6 (Graph Comparison)</a>
	 */
	public static boolean isomorphic(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		Model set1 = toModel(model1);
		Model set2 = toModel(model2);
		// Compare the number of statements in both sets
		if (set1.size() != set2.size()) {
			return false;
		}

		return isSubsetInternal(set1, set2);
	}

	/**
	 * Compares two RDF models, defined by two statement collections, and returns <tt>true</tt> if they are equal.
	 * Models are equal if they contain the same set of statements. Blank node IDs are not relevant for model equality,
	 * they are mapped from one model to the other by using the attached properties.
	 *
	 * @deprecated since 2.8.0. Use {@link Models#isomorphic(Iterable, Iterable)} instead.
	 */
	@Deprecated
	public static boolean equals(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		return isomorphic(model1, model2);
	}

	/**
	 * Compares two RDF models, and returns <tt>true</tt> if the first model is a subset of the second model, using
	 * graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		// Filter duplicates
		Model set1 = toModel(model1);
		Model set2 = toModel(model2);

		return isSubset(set1, set2);
	}

	/**
	 * Compares two RDF models, and returns <tt>true</tt> if the first model is a subset of the second model, using
	 * graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Set<? extends Statement> model1, Set<? extends Statement> model2) {
		// Compare the number of statements in both sets
		if (model1.size() > model2.size()) {
			return false;
		}

		return isSubsetInternal(toModel(model1), toModel(model2));
	}

	private static boolean isSubsetInternal(Model model1, Model model2) {
		// try to create a full blank node mapping
		return matchModels(model1, model2);
	}

	private static boolean matchModels(Model model1, Model model2) {
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

		return matchModels(model1BNodes, model2, new HashMap<>(), 0);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in model1 and blank nodes in model2. The
	 * algorithm does a depth-first search trying to establish a mapping for each blank node occurring in model1.
	 *
	 * @param model1
	 * @param model2
	 * @param bNodeMapping
	 * @param idx
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchModels(List<? extends Statement> model1, Model model2,
			Map<Resource, Resource> bNodeMapping, int idx) {
		boolean result = false;

		if (idx < model1.size()) {
			Statement st1 = model1.get(idx);

			List<Statement> matchingStats = findMatchingStatements(st1, model2, bNodeMapping);

			for (Statement st2 : matchingStats) {
				// Map bNodes in st1 to bNodes in st2
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

				// FIXME: this recursive implementation has a high risk of
				// triggering a stack overflow

				// Enter recursion
				result = matchModels(model1, model2, newBNodeMapping, idx + 1);

				if (result == true) {
					// models match, look no further
					break;
				}
			}
		} else {
			// All statements have been mapped successfully
			result = true;
		}

		return result;
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

		return result;
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

		if (isBlank(subj1) && isBlank(subj2)) {
			Resource mappedBNode = bNodeMapping.get(subj1);

			if (mappedBNode != null) {
				// bNode 'subj1' was already mapped to some other bNode
				if (!subj2.equals(mappedBNode)) {
					// 'subj1' and 'subj2' do not match
					return false;
				}
			} else {
				// 'subj1' was not yet mapped. we need to check if 'subj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(subj2)) {
					// 'subj2' is already mapped to some other value.
					return false;
				}
			}
		} else {
			// subjects are not (both) bNodes
			if (!subj1.equals(subj2)) {
				return false;
			}
		}

		Value obj1 = st1.getObject();
		Value obj2 = st2.getObject();

		if (isBlank(obj1) && isBlank(obj2)) {
			Resource mappedBNode = bNodeMapping.get(obj1);

			if (mappedBNode != null) {
				// bNode 'obj1' was already mapped to some other bNode
				if (!obj2.equals(mappedBNode)) {
					// 'obj1' and 'obj2' do not match
					return false;
				}
			} else {
				// 'obj1' was not yet mapped. we need to check if 'obj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(obj2)) {
					// 'obj2' is already mapped to some other value.
					return false;
				}
			}
		} else {
			// objects are not (both) bNodes
			if (!obj1.equals(obj2)) {
				return false;
			}
		}

		Resource context1 = st1.getContext();
		Resource context2 = st2.getContext();

		// no match if in different contexts
		if (context1 == null) {
			return context2 == null;
		} else if (context2 == null) {
			return false;
		}

		if (isBlank(context1) && isBlank(context2)) {
			Resource mappedBNode = bNodeMapping.get(context1);

			if (mappedBNode != null) {
				// bNode 'context1' was already mapped to some other bNode
				if (!context2.equals(mappedBNode)) {
					// 'context1' and 'context2' do not match
					return false;
				}
			} else {
				// 'context1' was not yet mapped. we need to check if 'context2' is
				// a
				// possible mapping candidate
				if (bNodeMapping.containsValue(context2)) {
					// 'context2' is already mapped to some other value.
					return false;
				}
			}
		} else {
			// contexts are not (both) bNodes
			if (!context1.equals(context1)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isBlank(Value value) {
		if (value instanceof IRI) {
			return value.stringValue().indexOf("/.well-known/genid/") > 0;
		} else {
			return value instanceof BNode;
		}
	}

	private static Model toModel(Iterable<? extends Statement> iterable) {
		if (iterable instanceof Model) {
			return (Model) iterable;
		}
		final Model set = new TreeModel();
		StreamSupport.stream(iterable.spliterator(), false).filter(Objects::nonNull).forEach(st -> set.add(st));
		return set;
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
}