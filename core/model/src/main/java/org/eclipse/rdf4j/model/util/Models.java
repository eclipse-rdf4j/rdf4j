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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.util.iterators.Iterators;

import com.google.common.base.Optional;

/**
 * Utility functions for working with {@link Model}s and other {@link Statement} collections.
 * 
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 * @see org.eclipse.rdf4j.model.Model
 */
public class Models {

	/*
	 * hidden constructor to avoid instantiation
	 */
	protected Models() {
	}

	/**
	 * Retrieves an object {@link Value} from the statements in the given model. If more than one possible
	 * object value exists, any one value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve an object value.
	 * @return an object value from the given model, or null if no such value exists.
	 */
	public static Value object(Model m) {
		Value result = null;
		final Set<Value> objects = m.objects();
		if (objects != null && !objects.isEmpty()) {
			result = objects.iterator().next();
		}

		return result;

	}

	/**
	 * @deprecated since 4.0. Use {@link #object(Model)} instead.
	 */
	@Deprecated
	public static Value anyObject(Model m) {
		return object(m);
	}

	/**
	 * Retrieves an object {@link Literal} value from the statements in the given model. If more than one
	 * possible Literal value exists, any one Literal value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve an object Literal value.
	 * @return an object Literal value from the given model, or null if no such value exists.
	 */
	public static Literal objectLiteral(Model m) {
		Literal result = null;
		final Set<Value> objects = m.objects();
		if (objects != null && !objects.isEmpty()) {
			for (Value v : objects) {
				if (v instanceof Literal) {
					result = (Literal)v;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * @deprecated since 4.0. Use {@link #objectLiteral(Model)} instead.
	 */
	@Deprecated
	public static Literal anyObjectLiteral(Model m) {
		return objectLiteral(m);
	}

	/**
	 * Retrieves an object {@link Resource} value from the statements in the given model. If more than one
	 * possible Resource value exists, any one Resource value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve an object Resource value.
	 * @return an object Resource value from the given model, which will be null if no such value exists.
	 */
	public static Resource objectResource(Model m) {
		Resource result = null;
		final Set<Value> objects = m.objects();
		if (objects != null && !objects.isEmpty()) {
			for (Value v : objects) {
				if (v instanceof Resource) {
					result = (Resource)v;
					break;
				}
			}
		}

		return result;

	}

	/**
	 * @deprecated since 4.0. Use {@link #objectResource(Model)} instead.
	 */
	@Deprecated
	public static Resource anyObjectResource(Model m) {
		return objectResource(m);
	}

	/**
	 * Retrieves an object {@link IRI} value from the statements in the given model. If more than one possible
	 * IRI value exists, any one value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve an object IRI value.
	 * @return an object IRI value from the given model, which will be null if no such value exists.
	 */
	public static IRI objectIRI(Model m) {
		IRI result = null;
		final Set<Value> objects = m.objects();
		if (objects != null && !objects.isEmpty()) {
			for (Value v : objects) {
				if (v instanceof IRI) {
					result = (IRI)v;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * Retrieves an object value as a String from the statements in the given model. If more than one possible
	 * object value exists, any one value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve an object String value.
	 * @return an object String value from the given model, which will be null if no such value exists.
	 */
	public static String objectString(Model m) {
		for (Value object : m.objects()) {
			return object.stringValue();
		}

		return null;
	}

	/**
	 * @deprecated since 4.0. Use {@link #objectIRI(Model)} instead.
	 */
	@Deprecated
	public static URI anyObjectURI(Model m) {
		return objectIRI(m);
	}

	/**
	 * Retrieves a subject {@link Resource} from the statements in the given model. If more than one possible
	 * resource value exists, any one resource value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve a subject Resource.
	 * @return a subject resource from the given model, which will be null if no such value exists.
	 */
	public static Resource subject(Model m) {
		Resource result = null;
		final Set<Resource> subjects = m.subjects();
		if (subjects != null && !subjects.isEmpty()) {
			result = subjects.iterator().next();
		}

		return result;
	}

	/**
	 * @deprecated since 4.0. Use {@link #subject(Model)} instead.
	 */
	@Deprecated
	public static Resource anySubject(Model m) {
		return subject(m);
	}

	/**
	 * Retrieves a subject {@link IRI} from the statements in the given model. If more than one possible IRI
	 * value exists, any one IRI value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve a subject IRI value.
	 * @return a subject IRI value from the given model, which will be null if no such value exists.
	 */
	public static IRI subjectIRI(Model m) {
		IRI result = null;
		final Set<Resource> objects = m.subjects();
		if (objects != null && !objects.isEmpty()) {
			for (Value v : objects) {
				if (v instanceof IRI) {
					result = (IRI)v;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * @deprecated since 4.0. Use {@link #subjectIRI(Model)} instead.
	 */
	@Deprecated
	public static URI anySubjectURI(Model m) {
		return subjectIRI(m);
	}

	/**
	 * Retrieves a subject {@link BNode} from the statements in the given model. If more than one possible
	 * blank node value exists, any one blank node value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve a subject BNode value.
	 * @return a subject BNode value from the given model, which will be null if no such value exists.
	 */
	public static BNode subjectBNode(Model m) {
		BNode result = null;
		final Set<Resource> objects = m.subjects();
		if (objects != null && !objects.isEmpty()) {
			for (Value v : objects) {
				if (v instanceof BNode) {
					result = (BNode)v;
					break;
				}
			}
		}

		return result;
	}

	/**
	 * @deprecated since 4.0. Use {@link #subjectBNode(Model)} instead.
	 */
	@Deprecated
	public static BNode anySubjectBNode(Model m) {
		return subjectBNode(m);
	}

	/**
	 * Retrieves a predicate from the statements in the given model. If more than one possible predicate value
	 * exists, any one value is picked and returned.
	 * 
	 * @param m
	 *        the model from which to retrieve a predicate value.
	 * @return an {@link Optional} predicate value from the given model, which will be {@link Optional#empty()
	 *         empty} if no such value exists.
	 */
	public static IRI predicate(Model m) {
		IRI result = null;
		final Set<IRI> predicates = m.predicates();
		if (predicates != null && !predicates.isEmpty()) {
			result = predicates.iterator().next();
		}
		return result;
	}

	/**
	 * @deprecated since 4.0. Use {@link #predicate(Model)} instead.
	 */
	@Deprecated
	public static URI anyPredicate(Model m) {
		return predicate(m);
	}

	/**
	 * Sets the property value for the given subject to the given object value, replacing any existing
	 * value(s) for the subject's property. This method updates the original input Model and then returns that
	 * same Model object.
	 * 
	 * @param m
	 *        the model in which to set the property value. May not be null.
	 * @param subject
	 *        the subject for which to set/replace the property value. May not be null.
	 * @param property
	 *        the property for which to set/replace the value. May not be null.
	 * @param value
	 *        the value to set for the given subject and property. May not be null.
	 * @param contexts
	 *        the context(s) in which to set/replace the property value. Optional vararg argument. If not
	 *        specified the operations works on the entire Model.
	 * @return the Model object, containing the updated property value.
	 */
	public static Model setProperty(Model m, Resource subject, IRI property, Value value,
			Resource... contexts)
	{
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
	 * Compares two RDF models, and returns <tt>true</tt> if they consist of isomorphic graphs and the
	 * isomorphic graph identifiers map 1:1 to each other. RDF graphs are isomorphic graphs if statements from
	 * one graphs can be mapped 1:1 on to statements in the other graphs. In this mapping, blank nodes are not
	 * considered mapped when having an identical internal id, but are mapped from one graph to the other by
	 * looking at the statements in which the blank nodes occur.
	 * <p>
	 * A Model can consist of more than one graph (denoted by context identifiers). Two models are considered
	 * isomorphic if for each of the graphs in one model, an isomorphic graph exists in the other model, and
	 * the context identifiers of these graphs are either identical or (in the case of blank nodes) map 1:1 on
	 * each other.
	 * 
	 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#graph-isomorphism">RDF Concepts &amp; Abstract
	 *      Syntax, section 3.6 (Graph Comparison)</a>
	 */
	public static boolean isomorphic(Iterable<? extends Statement> model1,
			Iterable<? extends Statement> model2)
	{
		Set<? extends Statement> set1 = toSet(model1);
		Set<? extends Statement> set2 = toSet(model2);
		// Compare the number of statements in both sets
		if (set1.size() != set2.size()) {
			return false;
		}

		return isSubsetInternal(set1, set2);
	}

	/**
	 * Compares two RDF models, defined by two statement collections, and returns <tt>true</tt> if they are
	 * equal. Models are equal if they contain the same set of statements. Blank node IDs are not relevant for
	 * model equality, they are mapped from one model to the other by using the attached properties.
	 * 
	 * @deprecated since 2.8.0. Use {@link Models#isomorphic(Iterable, Iterable)} instead.
	 */
	@Deprecated
	public static boolean equals(Iterable<? extends Statement> model1, Iterable<? extends Statement> model2) {
		return isomorphic(model1, model2);
	}

	/**
	 * Compares two RDF models, and returns <tt>true</tt> if the first model is a subset of the second model,
	 * using graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Iterable<? extends Statement> model1,
			Iterable<? extends Statement> model2)
	{
		// Filter duplicates
		Set<? extends Statement> set1 = toSet(model1);
		Set<? extends Statement> set2 = toSet(model2);

		return isSubset(set1, set2);
	}

	/**
	 * Compares two RDF models, and returns <tt>true</tt> if the first model is a subset of the second model,
	 * using graph isomorphism to map statements between models.
	 */
	public static boolean isSubset(Set<? extends Statement> model1, Set<? extends Statement> model2) {
		// Compare the number of statements in both sets
		if (model1.size() > model2.size()) {
			return false;
		}

		return isSubsetInternal(model1, model2);
	}

	private static boolean isSubsetInternal(Set<? extends Statement> model1,
			Set<? extends Statement> model2)
	{
		// try to create a full blank node mapping
		return matchModels(model1, model2);
	}

	private static boolean matchModels(Set<? extends Statement> model1, Set<? extends Statement> model2) {
		// Compare statements without blank nodes first, save the rest for later
		List<Statement> model1BNodes = new ArrayList<Statement>(model1.size());

		for (Statement st : model1) {
			if (st.getSubject() instanceof BNode || st.getObject() instanceof BNode
					|| st.getContext() instanceof BNode)
			{
				model1BNodes.add(st);
			}
			else {
				if (!model2.contains(st)) {
					return false;
				}
			}
		}

		return matchModels(model1BNodes, model2, new HashMap<BNode, BNode>(), 0);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in model1 and blank nodes in
	 * model2. The algorithm does a depth-first search trying to establish a mapping for each blank node
	 * occurring in model1.
	 * 
	 * @param model1
	 * @param model2
	 * @param bNodeMapping
	 * @param idx
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchModels(List<? extends Statement> model1, Iterable<? extends Statement> model2,
			Map<BNode, BNode> bNodeMapping, int idx)
	{
		boolean result = false;

		if (idx < model1.size()) {
			Statement st1 = model1.get(idx);

			List<Statement> matchingStats = findMatchingStatements(st1, model2, bNodeMapping);

			for (Statement st2 : matchingStats) {
				// Map bNodes in st1 to bNodes in st2
				Map<BNode, BNode> newBNodeMapping = new HashMap<BNode, BNode>(bNodeMapping);

				if (st1.getSubject() instanceof BNode && st2.getSubject() instanceof BNode) {
					newBNodeMapping.put((BNode)st1.getSubject(), (BNode)st2.getSubject());
				}

				if (st1.getObject() instanceof BNode && st2.getObject() instanceof BNode) {
					newBNodeMapping.put((BNode)st1.getObject(), (BNode)st2.getObject());
				}

				if (st1.getContext() instanceof BNode && st2.getContext() instanceof BNode) {
					newBNodeMapping.put((BNode)st1.getContext(), (BNode)st2.getContext());
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
		}
		else {
			// All statements have been mapped successfully
			result = true;
		}

		return result;
	}

	private static List<Statement> findMatchingStatements(Statement st, Iterable<? extends Statement> model,
			Map<BNode, BNode> bNodeMapping)
	{
		List<Statement> result = new ArrayList<Statement>();

		for (Statement modelSt : model) {
			if (statementsMatch(st, modelSt, bNodeMapping)) {
				// All components possibly match
				result.add(modelSt);
			}
		}

		return result;
	}

	private static boolean statementsMatch(Statement st1, Statement st2, Map<BNode, BNode> bNodeMapping) {
		IRI pred1 = st1.getPredicate();
		IRI pred2 = st2.getPredicate();

		if (!pred1.equals(pred2)) {
			// predicates don't match
			return false;
		}

		Resource subj1 = st1.getSubject();
		Resource subj2 = st2.getSubject();

		if (subj1 instanceof BNode && subj2 instanceof BNode) {
			BNode mappedBNode = bNodeMapping.get(subj1);

			if (mappedBNode != null) {
				// bNode 'subj1' was already mapped to some other bNode
				if (!subj2.equals(mappedBNode)) {
					// 'subj1' and 'subj2' do not match
					return false;
				}
			}
			else {
				// 'subj1' was not yet mapped. we need to check if 'subj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(subj2)) {
					// 'subj2' is already mapped to some other value.
					return false;
				}
			}
		}
		else {
			// subjects are not (both) bNodes
			if (!subj1.equals(subj2)) {
				return false;
			}
		}

		Value obj1 = st1.getObject();
		Value obj2 = st2.getObject();

		if (obj1 instanceof BNode && obj2 instanceof BNode) {
			BNode mappedBNode = bNodeMapping.get(obj1);

			if (mappedBNode != null) {
				// bNode 'obj1' was already mapped to some other bNode
				if (!obj2.equals(mappedBNode)) {
					// 'obj1' and 'obj2' do not match
					return false;
				}
			}
			else {
				// 'obj1' was not yet mapped. we need to check if 'obj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(obj2)) {
					// 'obj2' is already mapped to some other value.
					return false;
				}
			}
		}
		else {
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
		}
		else if (context2 == null) {
			return false;
		}

		if (context1 instanceof BNode && context2 instanceof BNode) {
			BNode mappedBNode = bNodeMapping.get(context1);

			if (mappedBNode != null) {
				// bNode 'context1' was already mapped to some other bNode
				if (!context2.equals(mappedBNode)) {
					// 'context1' and 'context2' do not match
					return false;
				}
			}
			else {
				// 'context1' was not yet mapped. we need to check if 'context2' is
				// a
				// possible mapping candidate
				if (bNodeMapping.containsValue(context2)) {
					// 'context2' is already mapped to some other value.
					return false;
				}
			}
		}
		else {
			// contexts are not (both) bNodes
			if (!context1.equals(context1)) {
				return false;
			}
		}

		return true;
	}

	private static <S extends Statement> Set<S> toSet(Iterable<S> iterable) {
		Set<S> set = null;
		if (iterable instanceof Set) {
			set = (Set<S>)iterable;
		}
		else {
			// Filter duplicates
			set = new HashSet<S>();
			Iterators.addAll(iterable.iterator(), set);
		}
		return set;
	}

}