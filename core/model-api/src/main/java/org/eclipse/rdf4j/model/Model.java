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
package org.eclipse.rdf4j.model;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An RDF Model, represented as a {@link java.util.Set} of {@link Statement}s with predictable iteration order.
 * <p>
 * Additional utility functionality for working with {@code Model} objects is available in the
 * {@code org.eclipse.rdf4j.model.util.Models} utility class.
 *
 * @author James Leigh
 * @author Jeen Broekstra
 */
public interface Model extends Set<Statement>, Serializable, NamespaceAware {

	/**
	 * Returns an unmodifiable view of this model. This method provides "read-only" access to this model. Query
	 * operations on the returned model "read through" to this model, and attempts to modify the returned model, whether
	 * direct or via its iterator, result in an {@link UnsupportedOperationException}.
	 * <p>
	 *
	 * @return an unmodifiable view of the specified set.
	 */
	Model unmodifiable();

	/**
	 * Sets the prefix for a namespace. This will replace any existing namespace associated to the prefix.
	 *
	 * @param prefix The new prefix.
	 * @param name   The namespace name that the prefix maps to.
	 * @return The {@link Namespace} object for the given namespace.
	 */
	default Namespace setNamespace(String prefix, String name) {
		Optional<? extends Namespace> result = getNamespace(prefix);
		if (!result.isPresent() || !result.get().getName().equals(name)) {
			result = Optional.of(new ModelNamespace(prefix, name));
			setNamespace(result.get());
		}
		return result.get();
	}

	/**
	 * Sets the prefix for a namespace. This will replace any existing namespace associated to the prefix.
	 *
	 * @param namespace A {@link Namespace} object to use in this Model.
	 */
	void setNamespace(Namespace namespace);

	/**
	 * Removes a namespace declaration by removing the association between a prefix and a namespace name.
	 *
	 * @param prefix The namespace prefix of which the assocation with a namespace name is to be removed.
	 * @return the previous namespace bound to the prefix or {@link Optional#empty()}
	 */
	Optional<Namespace> removeNamespace(String prefix);

	/**
	 * Determines if statements with the specified subject, predicate, object and (optionally) context exist in this
	 * model. The {@code subject}, {@code predicate} and {@code object} parameters can be {@code null} to indicate
	 * wildcards. The {@code contexts} parameter is a wildcard and accepts zero or more values. If no contexts are
	 * specified, statements will match disregarding their context. If one or more contexts are specified, statements
	 * with a context matching one of these will match. Note: to match statements without an associated context, specify
	 * the value {@code null} and explicitly cast it to type {@code Resource}.
	 * <p>
	 * Examples: {@code model.contains(s1, null, null)} is true if any statements in this model have subject
	 * {@code s1},<br>
	 * {@code model.contains(null, null, null, c1)} is true if any statements in this model have context {@code c1},<br>
	 * {@code model.contains(null, null, null, (Resource)null)} is true if any statements in this model have no
	 * associated context,<br>
	 * {@code model.contains(null, null, null, c1, c2, c3)} is true if any statements in this model have context
	 * {@code c1}, {@code c2} or {@code c3} .
	 *
	 * @param subj     The subject of the statements to match, {@code null} to match statements with any subject.
	 * @param pred     The predicate of the statements to match, {@code null} to match statements with any predicate.
	 * @param obj      The object of the statements to match, {@code null} to match statements with any object.
	 * @param contexts The contexts of the statements to match. If no contexts are specified, statements will match
	 *                 disregarding their context. If one or more contexts are specified, statements with a context
	 *                 matching one of these will match.
	 * @return <code>true</code> if statements match the specified pattern.
	 */
	boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * Adds one or more statements to the model. This method creates a statement for each specified context and adds
	 * those to the model. If no contexts are specified, a single statement with no associated context is added. If this
	 * Model is a filtered Model then null (if context empty) values are permitted and will use the corresponding
	 * filtered values.
	 *
	 * @param subj     The statement's subject.
	 * @param pred     The statement's predicate.
	 * @param obj      The statement's object.
	 * @param contexts The contexts to add statements to.
	 * @throws IllegalArgumentException      If This Model cannot store the given statement, because it is filtered out
	 *                                       of this view.
	 * @throws UnsupportedOperationException If this Model cannot accept any statements, because it is filtered to the
	 *                                       empty set.
	 */
	boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * Removes statements with the specified context exist in this model.
	 *
	 * @param context The context of the statements to remove.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	boolean clear(Resource... context);

	/**
	 * Removes statements with the specified subject, predicate, object and (optionally) context exist in this model.
	 * The {@code subject}, {@code predicate} and {@code object} parameters can be {@code null} to indicate wildcards.
	 * The {@code contexts} parameter is a wildcard and accepts zero or more values. If no contexts are specified,
	 * statements will be removed disregarding their context. If one or more contexts are specified, statements with a
	 * context matching one of these will be removed. Note: to remove statements without an associated context, specify
	 * the value {@code null} and explicitly cast it to type {@code Resource}.
	 * <p>
	 * Examples: {@code model.remove(s1, null, null)} removes any statements in this model have subject {@code s1},<br>
	 * {@code model.remove(null, null, null, c1)} removes any statements in this model have context {@code c1} ,<br>
	 * {@code model.remove(null, null, null, (Resource)null)} removes any statements in this model have no associated
	 * context,<br>
	 * {@code model.remove(null, null, null, c1, c2, c3)} removes any statements in this model have context {@code c1},
	 * {@code c2} or {@code c3}.
	 *
	 * @param subj     The subject of the statements to remove, {@code null} to remove statements with any subject.
	 * @param pred     The predicate of the statements to remove, {@code null} to remove statements with any predicate.
	 * @param obj      The object of the statements to remove, {@code null} to remove statements with any object.
	 * @param contexts The contexts of the statements to remove. If no contexts are specified, statements will be
	 *                 removed disregarding their context. If one or more contexts are specified, statements with a
	 *                 context matching one of these will be removed.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * Returns an {@link Iterable} over all {@link Statement}s in this Model that match the supplied criteria.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code model.getStatements(s1, null, null)} matches all statements that have subject {@code s1}</li>
	 * <li>{@code model.getStatements(s1, p1, null)} matches all statements that have subject {@code s1} and predicate
	 * {@code p1}</li>
	 * <li>{@code model.getStatements(null, null, null, c1)} matches all statements that have context {@code c1}</li>
	 * <li>{@code model.getStatements(null, null, null, (Resource)null)} matches all statements that have no associated
	 * context</li>
	 * <li>{@code model.getStatements(null, null, null, c1, c2, c3)} matches all statements that have context
	 * {@code c1}, {@code c2} or {@code c3}</li>
	 * </ul>
	 *
	 * @param subject   The subject of the statements to match, {@code null} to match statements with any subject.
	 * @param predicate The predicate of the statements to match, {@code null} to match statements with any predicate.
	 * @param object    The object of the statements to match, {@code null} to match statements with any object.
	 * @param contexts  The contexts of the statements to match. If no contexts are specified, statements will match
	 *                  disregarding their context. If one or more contexts are specified, statements with a context
	 *                  matching any one of these will match. To match statements without an associated context, specify
	 *                  the value {@code null} and explicitly cast it to type {@code Resource}.
	 * @return an {@link Iterable} over the statements in this Model that match the specified pattern.
	 *
	 * @since 3.2.0
	 *
	 * @see #filter(Resource, IRI, Value, Resource...)
	 */
	default Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object,
			Resource... contexts) {
		return () -> filter(subject, predicate, object, contexts).iterator();
	}

	/**
	 * Returns a filtered view of the statements with the specified subject, predicate, object and (optionally) context.
	 * The {@code subject}, {@code predicate} and {@code object} parameters can be {@code null} to indicate wildcards.
	 * The {@code contexts} parameter is a wildcard and accepts zero or more values. If no contexts are specified,
	 * statements will match disregarding their context. If one or more contexts are specified, statements with a
	 * context matching one of these will match. Note: to match statements without an associated context, specify the
	 * value {@code null} and explicitly cast it to type {@code Resource}.
	 * <p>
	 * The returned model is backed by this Model, so changes to this Model are reflected in the returned model, and
	 * vice-versa. If this Model is modified while an iteration over the returned model is in progress (except through
	 * the iterator's own {@code remove} operation), the results of the iteration are undefined. The model supports
	 * element removal, which removes the corresponding statement from this Model, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and {@code clear} operations. The statements passed to
	 * the {@code add} and {@code addAll} operations must match the parameter pattern.
	 * <p>
	 * Examples: {@code model.filter(s1, null, null)} matches all statements that have subject {@code s1},<br>
	 * {@code model.filter(null, null, null, c1)} matches all statements that have context {@code c1},<br>
	 * {@code model.filter(null, null, null, (Resource)null)} matches all statements that have no associated
	 * context,<br>
	 * {@code model.filter(null, null, null, c1, c2, c3)} matches all statements that have context {@code c1},
	 * {@code c2} or {@code c3}.
	 *
	 * @param subj     The subject of the statements to match, {@code null} to match statements with any subject.
	 * @param pred     The predicate of the statements to match, {@code null} to match statements with any predicate.
	 * @param obj      The object of the statements to match, {@code null} to match statements with any object.
	 * @param contexts The contexts of the statements to match. If no contexts are specified, statements will match
	 *                 disregarding their context. If one or more contexts are specified, statements with a context
	 *                 matching one of these will match.
	 * @return The statements that match the specified pattern.
	 *
	 * @see #getStatements(Resource, IRI, Value, Resource...)
	 */
	Model filter(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * Returns a {@link Set} view of the subjects contained in this model. The set is backed by the model, so changes to
	 * the model are reflected in the set, and vice-versa. If the model is modified while an iteration over the set is
	 * in progress (except through the iterator's own {@code remove} operation), the results of the iteration are
	 * undefined. The set supports element removal, which removes all statements from the model for which that element
	 * is a subject value, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll},
	 * and {@code clear} operations. It does not support the {@code add} or {@code addAll} operations if the parameters
	 * {@code pred} or {@code obj} are null.
	 *
	 * @return a set view of the subjects contained in this model
	 */
	Set<Resource> subjects();

	/**
	 * Returns a {@link Set} view of the predicates contained in this model. The set is backed by the model, so changes
	 * to the model are reflected in the set, and vice-versa. If the model is modified while an iteration over the set
	 * is in progress (except through the iterator's own {@code remove} operation), the results of the iteration are
	 * undefined. The set supports element removal, which removes all statements from the model for which that element
	 * is a predicate value, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll},
	 * and {@code clear} operations. It does not support the {@code add} or {@code addAll} operations if the parameters
	 * {@code subj} or {@code obj} are null.
	 *
	 * @return a set view of the predicates contained in this model
	 */
	Set<IRI> predicates();

	/**
	 * Returns a {@link Set} view of the objects contained in this model. The set is backed by the model, so changes to
	 * the model are reflected in the set, and vice-versa. If the model is modified while an iteration over the set is
	 * in progress (except through the iterator's own {@code remove} operation), the results of the iteration are
	 * undefined. The set supports element removal, which removes all statements from the model for which that element
	 * is an object value, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll},
	 * and {@code clear} operations. It does not support the {@code add} or {@code addAll} operations if the parameters
	 * {@code subj} or {@code pred} are null.
	 *
	 * @return a set view of the objects contained in this model
	 */
	Set<Value> objects();

	/**
	 * Returns a {@link Set} view of the contexts contained in this model. The set is backed by the model, so changes to
	 * the model are reflected in the set, and vice-versa. If the model is modified while an iteration over the set is
	 * in progress (except through the iterator's own {@code remove} operation), the results of the iteration are
	 * undefined. The set supports element removal, which removes all statements from the model for which that element
	 * is a context value, via the {@code Iterator.remove}, {@code Set.remove}, {@code removeAll}, {@code retainAll},
	 * and {@code clear} operations. It does not support the {@code add} or {@code addAll} operations if the parameters
	 * {@code subj} , {@code pred} or {@code obj} are null.
	 *
	 * @return a set view of the contexts contained in this model
	 */
	default Set<Resource> contexts() {
		Set<Resource> subjects = stream().map(st -> st.getContext()).collect(Collectors.toSet());
		return subjects;
	}

}
