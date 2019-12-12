/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
	 * Compares two RDF models, and returns <tt>true</tt> if the first model is a subset of the second model, using
	 * graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		// Filter duplicates
		Set<Statement> set1 = toSet(model1);
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

		return isSubsetInternal(toSet(model1), toModel(model2));
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
}