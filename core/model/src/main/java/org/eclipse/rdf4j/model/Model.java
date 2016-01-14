/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;

/**
 * An RDF Model, represented as a {@link java.util.Set} of {@link Statement}s
 * with predictable iteration order.
 * <p>
 * Additional utility functionality for working with Model objects is available
 * in the {@link org.eclipse.rdf4j.model.util.Models Models} utility class.
 * 
 * @since 2.7.0
 * @author James Leigh
 * @author Jeen Broekstra
 * @see org.eclipse.rdf4j.model.util.Models the Models utility class
 */
@SuppressWarnings("deprecation")
public interface Model extends Graph, Set<Statement>, Serializable {

	/**
	 * Returns an unmodifiable view of this model. This method provides
	 * "read-only" access to this model. Query operations on the returned model
	 * "read through" to this model, and attempts to modify the returned model,
	 * whether direct or via its iterator, result in an
	 * {@link UnsupportedOperationException}.
	 * <p>
	 * 
	 * @return an unmodifiable view of the specified set.
	 */
	public Model unmodifiable();

	/**
	 * Gets the map that contains the assigned namespaces.
	 * 
	 * @return Map of prefix to namespace
	 */
	public Set<Namespace> getNamespaces();

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 * 
	 * @param prefix
	 *        A namespace prefix.
	 * @return The namespace name that is associated with the specified prefix,
	 *         or {@link Optional#empty()} if there is no such namespace.
	 */
	public default Optional<Namespace> getNamespace(String prefix) {
		return getNamespaces().stream().filter(t -> t.getPrefix().equals(prefix)).findAny();
	}

	/**
	 * Sets the prefix for a namespace. This will replace any existing namespace
	 * associated to the prefix.
	 * 
	 * @param prefix
	 *        The new prefix.
	 * @param name
	 *        The namespace name that the prefix maps to.
	 * @return The {@link Namespace} object for the given namespace.
	 */
	public default Namespace setNamespace(String prefix, String name) {
		Optional<? extends Namespace> result = getNamespace(prefix);
		if (!result.isPresent() || !result.get().getName().equals(name)) {
			result = Optional.of(new SimpleNamespace(prefix, name));
			setNamespace(result.get());
		}
		return result.get();
	}

	/**
	 * Sets the prefix for a namespace. This will replace any existing namespace
	 * associated to the prefix.
	 * 
	 * @param namespace
	 *        A {@link Namespace} object to use in this Model.
	 */
	public void setNamespace(Namespace namespace);

	/**
	 * Removes a namespace declaration by removing the association between a
	 * prefix and a namespace name.
	 * 
	 * @param prefix
	 *        The namespace prefix of which the assocation with a namespace name
	 *        is to be removed.
	 * @return the previous namespace bound to the prefix or
	 *         {@link Optional#empty()}
	 */
	public Optional<Namespace> removeNamespace(String prefix);

	/**
	 * Determines if statements with the specified subject, predicate, object and
	 * (optionally) context exist in this model. The {@code subject},
	 * {@code predicate} and {@code object} parameters can be {@code null} to
	 * indicate wildcards. The {@code contexts} parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements will
	 * match disregarding their context. If one or more contexts are specified,
	 * statements with a context matching one of these will match. Note: to match
	 * statements without an associated context, specify the value {@code null}
	 * and explicitly cast it to type {@code Resource}.
	 * <p>
	 * Examples: {@code model.contains(s1, null, null)} is true if any statements
	 * in this model have subject {@code s1},<br>
	 * {@code model.contains(null, null, null, c1)} is true if any statements in
	 * this model have context {@code c1},<br>
	 * {@code model.contains(null, null, null, (Resource)null)} is true if any
	 * statements in this model have no associated context,<br>
	 * {@code model.contains(null, null, null, c1, c2, c3)} is true if any
	 * statements in this model have context {@code c1}, {@code c2} or {@code c3}
	 * .
	 * 
	 * @param subj
	 *        The subject of the statements to match, {@code null} to match
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to match, {@code null} to match
	 *        statements with any predicate.
	 * @param obj
	 *        The object of the statements to match, {@code null} to match
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to match. If no contexts are
	 *        specified, statements will match disregarding their context. If one
	 *        or more contexts are specified, statements with a context matching
	 *        one of these will match.
	 * @return <code>true</code> if statements match the specified pattern.
	 */
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * @deprecated since 4.0. Use
	 *             {@link #contains(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	public default boolean contains(Resource subj, URI pred, Value obj, Resource... contexts) {
		return contains(subj, (IRI)pred, obj, contexts);
	}

	/**
	 * Adds one or more statements to the model. This method creates a statement
	 * for each specified context and adds those to the model. If no contexts are
	 * specified, a single statement with no associated context is added. If this
	 * Model is a filtered Model then null (if context empty) values are
	 * permitted and will use the corresponding filtered values.
	 * 
	 * @param subj
	 *        The statement's subject.
	 * @param pred
	 *        The statement's predicate.
	 * @param obj
	 *        The statement's object.
	 * @param contexts
	 *        The contexts to add statements to.
	 * @throws IllegalArgumentException
	 *         If This Model cannot store the given statement, because it is
	 *         filtered out of this view.
	 * @throws UnsupportedOperationException
	 *         If this Model cannot accept any statements, because it is filtered
	 *         to the empty set.
	 */
	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * @deprecated since 4.0. Use {@link #add(Resource, IRI, Value, Resource...)}
	 *             instead.
	 */
	@Deprecated
	public default boolean add(Resource subj, URI pred, Value obj, Resource... contexts) {
		return add(subj, (IRI)pred, obj, contexts);
	}

	/**
	 * Removes statements with the specified context exist in this model.
	 * 
	 * @param context
	 *        The context of the statements to remove.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	public boolean clear(Resource... context);

	/**
	 * Removes statements with the specified subject, predicate, object and
	 * (optionally) context exist in this model. The {@code subject},
	 * {@code predicate} and {@code object} parameters can be {@code null} to
	 * indicate wildcards. The {@code contexts} parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements will
	 * be removed disregarding their context. If one or more contexts are
	 * specified, statements with a context matching one of these will be
	 * removed. Note: to remove statements without an associated context, specify
	 * the value {@code null} and explicitly cast it to type {@code Resource}.
	 * <p>
	 * Examples: {@code model.remove(s1, null, null)} removes any statements in
	 * this model have subject {@code s1},<br>
	 * {@code model.remove(null, null, null, c1)} removes any statements in this
	 * model have context {@code c1},<br>
	 * {@code model.remove(null, null, null, (Resource)null)} removes any
	 * statements in this model have no associated context,<br>
	 * {@code model.remove(null, null, null, c1, c2, c3)} removes any statements
	 * in this model have context {@code c1}, {@code c2} or {@code c3}.
	 * 
	 * @param subj
	 *        The subject of the statements to remove, {@code null} to remove
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to remove, {@code null} to remove
	 *        statements with any predicate.
	 * @param obj
	 *        The object of the statements to remove, {@code null} to remove
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to remove. If no contexts are
	 *        specified, statements will be removed disregarding their context.
	 *        If one or more contexts are specified, statements with a context
	 *        matching one of these will be removed.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts);

	/**
	 * @deprecated since 4.0. Use
	 *             {@link #remove(Resource, IRI, Value, Resource...)} instead.
	 */
	@Deprecated
	public default boolean remove(Resource subj, URI pred, Value obj, Resource... contexts) {
		return remove(subj, (IRI)pred, obj, contexts);
	}

	// Views

	/**
	 * Returns a view of the statements with the specified subject, predicate,
	 * object and (optionally) context. The {@code subject}, {@code predicate}
	 * and {@code object} parameters can be {@code null} to indicate wildcards.
	 * The {@code contexts} parameter is a wildcard and accepts zero or more
	 * values. If no contexts are specified, statements will match disregarding
	 * their context. If one or more contexts are specified, statements with a
	 * context matching one of these will match. Note: to match statements
	 * without an associated context, specify the value {@code null} and
	 * explicitly cast it to type {@code Resource}.
	 * <p>
	 * The returned model is backed by this Model, so changes to this Model are
	 * reflected in the returned model, and vice-versa. If this Model is modified
	 * while an iteration over the returned model is in progress (except through
	 * the iterator's own {@code remove} operation), the results of the iteration
	 * are undefined. The model supports element removal, which removes the
	 * corresponding statement from this Model, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
	 * {@code clear} operations. The statements passed to the {@code add} and
	 * {@code addAll} operations must match the parameter pattern.
	 * <p>
	 * Examples: {@code model.filter(s1, null, null)} matches all statements that
	 * have subject {@code s1},<br>
	 * {@code model.filter(null, null, null, c1)} matches all statements that
	 * have context {@code c1},<br>
	 * {@code model.filter(null, null, null, (Resource)null)} matches all
	 * statements that have no associated context,<br>
	 * {@code model.filter(null, null, null, c1, c2, c3)} matches all statements
	 * that have context {@code c1}, {@code c2} or {@code c3}.
	 * 
	 * @param subj
	 *        The subject of the statements to match, {@code null} to match
	 *        statements with any subject.
	 * @param pred
	 *        The predicate of the statements to match, {@code null} to match
	 *        statements with any predicate.
	 * @param obj
	 *        The object of the statements to match, {@code null} to match
	 *        statements with any object.
	 * @param contexts
	 *        The contexts of the statements to match. If no contexts are
	 *        specified, statements will match disregarding their context. If one
	 *        or more contexts are specified, statements with a context matching
	 *        one of these will match.
	 * @return The statements that match the specified pattern.
	 */
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts);

	@Deprecated
	public default Model filter(Resource subj, URI pred, Value obj, Resource... contexts) {
		return filter(subj, (IRI)pred, obj, contexts);
	}

	/**
	 * Returns a {@link Set} view of the subjects contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own {@code remove}
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes all statements from the model for which
	 * that element is a subject value, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
	 * {@code clear} operations. It does not support the {@code add} or
	 * {@code addAll} operations if the parameters {@code pred} or {@code obj}
	 * are null.
	 * 
	 * @return a set view of the subjects contained in this model
	 */
	public Set<Resource> subjects();

	/**
	 * Gets the subject of the statement(s). If the model contains one or more
	 * statements, all these statements should have the same subject. A
	 * {@link ModelException} is thrown if this is not the case.
	 * 
	 * @return The subject of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If the statements matched by the specified parameters have more
	 *         than one unique subject.
	 * @since 2.8.0
	 * @deprecated since 4.0. Instead, use {@link Models#subject(Model)} to
	 *             retrieve a subject Resource, and/or use the size of the set
	 *             returned by {@link #subjects()} to verify if the subject is
	 *             unique.
	 */
	@Deprecated
	public default Optional<Resource> subjectResource()
		throws ModelException
	{
		Set<Resource> result = stream().map(st -> st.getSubject()).distinct().limit(2).collect(
				Collectors.toSet());
		if (result.isEmpty()) {
			return Optional.empty();
		}
		else if (result.size() > 1) {
			throw new ModelException("Did not find a unique subject resource");
		}
		else {
			return Optional.of(result.iterator().next());
		}
	}

	/**
	 * Utility method that casts the return value of {@link #subjectResource()}
	 * to a IRI, or throws a ModelException if that value is not an IRI.
	 * 
	 * @return The subject of the matched statement(s), or {@code null} if no
	 *         matching statements were found.
	 * @throws ModelException
	 *         If such an exception is thrown by {@link #subjectResource()} or if
	 *         its return value is not a IRI.
	 * @since 2.8.0
	 * @deprecated since 4.0. Instead, use {@link Models#subjectURI(Model)} to
	 *             retrieve a subject URI, and/or use the size of the set
	 *             returned by {@link #subjects()} to verify if the subject is
	 *             unique.
	 */
	@Deprecated
	public default Optional<IRI> subjectIRI()
		throws ModelException
	{
		Optional<Resource> subjectResource = subjectResource();
		if (subjectResource.isPresent()) {
			if (subjectResource.get() instanceof IRI) {
				return Optional.of((IRI)subjectResource.get());
			}
			else {
				throw new ModelException("Did not find a unique subject URI");
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Provided for backward-compatibility purposes only, this method executes
	 * {@link #subjectIRI} instead.
	 * 
	 * @deprecated use {@link #subjectIRI()} instead.
	 */
	@Deprecated
	public default Optional<IRI> subjectURI()
		throws ModelException
	{
		return subjectIRI();
	}

	/**
	 * Utility method that casts the return value of {@link #subjectResource()}
	 * to a BNode, or throws a ModelException if that value is not a BNode.
	 * 
	 * @return The subject of the matched statement(s), or {@code null} if no
	 *         matching statements were found.
	 * @throws ModelException
	 *         If such an exception is thrown by {@link #subjectResource()} or if
	 *         its return value is not a BNode.
	 * @since 2.8.0
	 * @deprecated since 4.0. Instead, use {@link Models#subjectBNode(Model)} to
	 *             retrieve a subject BNode, and/or use the size of the set
	 *             returned by {@link #subjects()} to verify if the subject is
	 *             unique.
	 */
	@Deprecated
	public default Optional<BNode> subjectBNode()
		throws ModelException
	{
		Optional<Resource> subjectResource = subjectResource();
		if (subjectResource.isPresent()) {
			if (subjectResource.get() instanceof BNode) {
				return Optional.of((BNode)subjectResource.get());
			}
			else {
				throw new ModelException("Did not find a unique subject URI");
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Returns a {@link Set} view of the predicates contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own {@code remove}
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes all statements from the model for which
	 * that element is a predicate value, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
	 * {@code clear} operations. It does not support the {@code add} or
	 * {@code addAll} operations if the parameters {@code subj} or {@code obj}
	 * are null.
	 * 
	 * @return a set view of the predicates contained in this model
	 */
	public Set<IRI> predicates();

	/**
	 * Returns a {@link Set} view of the objects contained in this model. The set
	 * is backed by the model, so changes to the model are reflected in the set,
	 * and vice-versa. If the model is modified while an iteration over the set
	 * is in progress (except through the iterator's own {@code remove}
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes all statements from the model for which
	 * that element is an object value, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
	 * {@code clear} operations. It does not support the {@code add} or
	 * {@code addAll} operations if the parameters {@code subj} or {@code pred}
	 * are null.
	 * 
	 * @return a set view of the objects contained in this model
	 */
	public Set<Value> objects();

	/**
	 * Returns a {@link Set} view of the contexts contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own {@code remove}
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes all statements from the model for which
	 * that element is a context value, via the {@code Iterator.remove},
	 * {@code Set.remove}, {@code removeAll}, {@code retainAll}, and
	 * {@code clear} operations. It does not support the {@code add} or
	 * {@code addAll} operations if the parameters {@code subj} , {@code pred} or
	 * {@code obj} are null.
	 * 
	 * @return a set view of the contexts contained in this model
	 */
	public default Set<Resource> contexts() {
		Set<Resource> subjects = stream().map(st -> st.getContext()).collect(Collectors.toSet());
		return subjects;
	};

	/**
	 * Gets the object of the statement(s). If the model contains one or more
	 * statements, all these statements should have the same object. A
	 * {@link ModelException} is thrown if this is not the case.
	 * 
	 * @return The object of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If the statements matched by the specified parameters have more
	 *         than one unique object.
	 * @deprecated since 4.0. Instead, use {@link Models#object(Model)} to
	 *             retrieve an object value, and/or use the size of the set
	 *             returned by {@link #objects()} to verify if the object is
	 *             unique.
	 */
	@Deprecated
	public default Optional<Value> objectValue()
		throws ModelException
	{
		Set<Value> result = stream().map(st -> st.getObject()).distinct().limit(2).collect(Collectors.toSet());
		if (result.isEmpty()) {
			return Optional.empty();
		}
		else if (result.size() > 1) {
			throw new ModelException("Did not find a unique object value");
		}
		else {
			return Optional.of(result.iterator().next());
		}
	};

	/**
	 * Utility method that casts the return value of {@link #objectValue()} to a
	 * Literal, or throws a ModelUtilException if that value is not a Literal.
	 * 
	 * @return The object of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If such an exception is thrown by {@link #objectValue()} or if its
	 *         return value is not a Literal.
	 * @deprecated since 4.0. Instead, use {@link Models#objectLiteral(Model)} to
	 *             retrieve an object Literal value, and/or use the size of the
	 *             set returned by {@link #objects()} to verify if the object is
	 *             unique.
	 */
	@Deprecated
	public default Optional<Literal> objectLiteral()
		throws ModelException
	{
		Optional<Value> objectValue = objectValue();
		if (objectValue.isPresent()) {
			if (objectValue.get() instanceof Literal) {
				return Optional.of((Literal)objectValue.get());
			}
			else {
				throw new ModelException("Did not find a unique object literal");
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Utility method that casts the return value of {@link #objectValue()} to a
	 * Resource, or throws a ModelUtilException if that value is not a Resource.
	 * 
	 * @return The object of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If such an exception is thrown by {@link #objectValue()} or if its
	 *         return value is not a Resource.
	 * @deprecated since 4.0. Instead, use {@link Models#objectResource(Model)}
	 *             to retrieve an object Resource value, and/or use the size of
	 *             the set returned by {@link #objects()} to verify if the object
	 *             is unique.
	 */
	@Deprecated
	public default Optional<Resource> objectResource()
		throws ModelException
	{
		Optional<Value> objectValue = objectValue();
		if (objectValue.isPresent()) {
			if (objectValue.get() instanceof Resource) {
				return Optional.of((Resource)objectValue.get());
			}
			else {
				throw new ModelException("Did not find a unique object resource");
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Utility method that casts the return value of {@link #objectValue()} to an
	 * IRI, or throws a ModelUtilException if that value is not an IRI.
	 * 
	 * @return The object of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If such an exception is thrown by {@link #objectValue()} or if its
	 *         return value is not an IRI.
	 * @deprecated since 4.0. Instead, use {@link Models#objectURI(Model)} to
	 *             retrieve an object URI value, and/or use the size of the set
	 *             returned by {@link #objects()} to verify if the object is
	 *             unique.
	 */
	@Deprecated
	public default Optional<IRI> objectIRI()
		throws ModelException
	{
		Optional<Value> objectValue = objectValue();
		if (objectValue.isPresent()) {
			if (objectValue.get() instanceof IRI) {
				return Optional.of((IRI)objectValue.get());
			}
			else {
				throw new ModelException("Did not find a unique object URI");
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Provided for backward-compatibility purposes only, this method executes
	 * {@link #objectIRI} instead.
	 * 
	 * @deprecated use {@link #objectIRI()} instead.
	 */
	@Deprecated
	public default Optional<IRI> objectURI()
		throws ModelException
	{
		return objectIRI();
	}

	/**
	 * Utility method that returns the string value of {@link #objectValue()}.
	 * 
	 * @return The object string value of the matched statement(s), or
	 *         {@link Optional#empty()} if no matching statements were found.
	 * @throws ModelException
	 *         If the statements matched by the specified parameters have more
	 *         than one unique object.
	 * @deprecated since 4.0. Instead, use {@link Models#objectString(Model)} to
	 *             retrieve an object string value, and/or use the size of the
	 *             set returned by {@link #objects()} to verify if the object is
	 *             unique.
	 */
	@Deprecated
	public default Optional<String> objectString()
		throws ModelException
	{
		Optional<Value> objectValue = objectValue();
		if (objectValue.isPresent()) {
			return Optional.of(objectValue.get().stringValue());
		}
		else {
			return Optional.empty();
		}
	}

}
