/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.lang.ref.SoftReference;

import org.eclipse.rdf4j.model.IRI;

/**
 * A MemoryStore-specific implementation of URI that stores separated namespace and local name information to enable
 * reuse of namespace String objects (reducing memory usage) and that gives it node properties.
 */
public class MemIRI implements IRI, MemResource {

	private static final long serialVersionUID = 9118488004995852467L;

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * The URI's namespace.
	 */
	private final String namespace;

	/**
	 * The URI's local name.
	 */
	private final String localName;

	/**
	 * The object that created this MemURI.
	 */
	transient private final Object creator;

	/**
	 * The MemURI's hash code, 0 if not yet initialized.
	 */
	private volatile int hashCode = 0;

	/**
	 * The list of statements for which this MemURI is the subject.
	 */
	transient private final MemStatementList subjectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemURI is the predicate.
	 */
	transient private final MemStatementList predicateStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemURI is the object.
	 */
	transient private final MemStatementList objectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemURI represents the context.
	 */
	transient private final MemStatementList contextStatements = new MemStatementList();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemURI for a URI.
	 *
	 * @param creator   The object that is creating this MemURI.
	 * @param namespace namespace part of URI.
	 * @param localName localname part of URI.
	 */
	public MemIRI(Object creator, String namespace, String localName) {
		this.creator = creator;
		this.namespace = namespace;
		this.localName = localName;
	}

	/*---------*
	 * Methods *
	 *---------*/

	transient SoftReference<String> toStringCache = null;

	@Override
	public String toString() {
		String result;
		if (toStringCache == null) {
			result = namespace + localName;
			toStringCache = new SoftReference<>(result);
		} else {
			result = toStringCache.get();
			if (result == null) {
				result = namespace + localName;
				toStringCache = new SoftReference<>(result);
			}
		}
		return result;
	}

	@Override
	public String stringValue() {
		return toString();
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof MemIRI) {
			MemIRI o = (MemIRI) other;
			if (o.creator == creator) {
				// two different MemIRI from the same MemoryStore can not be equal.
				return false;
			}
			return namespace.equals(o.getNamespace()) && localName.equals(o.getLocalName());
		} else if (other instanceof IRI) {
			String otherStr = ((IRI) other).stringValue();

			return namespace.length() + localName.length() == otherStr.length() && otherStr.endsWith(localName)
					&& otherStr.startsWith(namespace);
		}

		return false;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = stringValue().hashCode();
		}

		return hashCode;
	}

	@Override
	public Object getCreator() {
		return creator;
	}

	@Override
	public boolean hasStatements() {
		return !subjectStatements.isEmpty() || !predicateStatements.isEmpty() || !objectStatements.isEmpty()
				|| !contextStatements.isEmpty();
	}

	@Override
	public MemStatementList getSubjectStatementList() {
		return subjectStatements;
	}

	@Override
	public int getSubjectStatementCount() {
		return subjectStatements.size();
	}

	@Override
	public void addSubjectStatement(MemStatement st) {
		subjectStatements.add(st);
	}

	@Override
	public void removeSubjectStatement(MemStatement st) {
		subjectStatements.remove(st);
	}

	@Override
	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) {
		subjectStatements.cleanSnapshots(currentSnapshot);
	}

	/**
	 * Gets the list of statements for which this MemURI is the predicate.
	 *
	 * @return a MemStatementList containing the statements.
	 */
	public MemStatementList getPredicateStatementList() {
		return predicateStatements;
	}

	/**
	 * Gets the number of Statements for which this MemURI is the predicate.
	 *
	 * @return An integer larger than or equal to 0.
	 */
	public int getPredicateStatementCount() {
		return predicateStatements.size();
	}

	/**
	 * Adds a statement to this MemURI's list of statements for which it is the predicate.
	 */
	public void addPredicateStatement(MemStatement st) {
		predicateStatements.add(st);
	}

	/**
	 * Removes a statement from this MemURI's list of statements for which it is the predicate.
	 */
	public void removePredicateStatement(MemStatement st) {
		predicateStatements.remove(st);
	}

	/**
	 * Removes statements from old snapshots (those that have expired at or before the specified snapshot version) from
	 * this MemValue's list of statements for which it is the predicate.
	 *
	 * @param currentSnapshot The current snapshot version.
	 */
	public void cleanSnapshotsFromPredicateStatements(int currentSnapshot) {
		predicateStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public MemStatementList getObjectStatementList() {
		return objectStatements;
	}

	@Override
	public int getObjectStatementCount() {
		return objectStatements.size();
	}

	@Override
	public void addObjectStatement(MemStatement st) {
		objectStatements.add(st);
	}

	@Override
	public void removeObjectStatement(MemStatement st) {
		objectStatements.remove(st);
	}

	@Override
	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) {
		objectStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public boolean hasSubjectStatements() {
		return !subjectStatements.isEmpty();
	}

	@Override
	public boolean hasPredicateStatements() {
		return !predicateStatements.isEmpty();
	}

	@Override
	public boolean hasObjectStatements() {
		return !objectStatements.isEmpty();
	}

	@Override
	public boolean hasContextStatements() {
		return !contextStatements.isEmpty();
	}

	@Override
	public MemStatementList getContextStatementList() {
		return contextStatements;
	}

	@Override
	public int getContextStatementCount() {
		return contextStatements.size();
	}

	@Override
	public void addContextStatement(MemStatement st) {
		contextStatements.add(st);
	}

	@Override
	public void removeContextStatement(MemStatement st) {
		contextStatements.remove(st);
	}

	@Override
	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		contextStatements.cleanSnapshots(currentSnapshot);
	}
}
