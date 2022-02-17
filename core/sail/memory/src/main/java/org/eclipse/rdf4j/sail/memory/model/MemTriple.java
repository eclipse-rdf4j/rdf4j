/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.Triple;

import com.google.common.base.Objects;

/**
 * A MemoryStore-specific implementation of {@link Triple}.
 *
 * @author Jeen Broekstra
 */
public class MemTriple implements Triple, MemResource {

	private static final long serialVersionUID = -9085188980084028689L;

	transient private final Object creator;

	private final MemResource subject;
	private final MemIRI predicate;
	private final MemValue object;

	/**
	 * The list of statements for which this MemTriple is the subject.
	 */
	transient private final MemStatementList subjectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemTriple is the object.
	 */
	transient private final MemStatementList objectStatements = new MemStatementList();

	public MemTriple(Object creator, MemResource subject, MemIRI predicate, MemValue object) {
		this.creator = creator;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	@Override
	public String stringValue() {
		StringBuilder sb = new StringBuilder(256);

		sb.append("<<");
		sb.append(getSubject());
		sb.append(" ");
		sb.append(getPredicate());
		sb.append(" ");
		sb.append(getObject());
		sb.append(">>");

		return sb.toString();
	}

	@Override
	public String toString() {
		return stringValue();
	}

	@Override
	public Object getCreator() {
		return creator;
	}

	@Override
	public boolean hasStatements() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public MemStatementList getObjectStatementList() {
		return objectStatements;
	}

	@Override
	public int getObjectStatementCount() {
		return getObjectStatementList().size();
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
	public MemStatementList getSubjectStatementList() {

		return subjectStatements;
	}

	@Override
	public int getSubjectStatementCount() {
		return getSubjectStatementList().size();
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

	@Override
	public MemStatementList getContextStatementList() {
		return EMPTY_LIST;
	}

	@Override
	public int getContextStatementCount() {
		return 0;
	}

	@Override
	public void addContextStatement(MemStatement st) {
		throw new UnsupportedOperationException("RDF-star triples can not be used as context identifier");
	}

	@Override
	public void removeContextStatement(MemStatement st) {
		// no-op
	}

	@Override
	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		// no-op
	}

	@Override
	public MemResource getSubject() {
		return subject;
	}

	@Override
	public MemIRI getPredicate() {
		return predicate;
	}

	@Override
	public MemValue getObject() {
		return object;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(subject, predicate, object);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof Triple) {
			Triple that = (Triple) other;
			return this.subject.equals(that.getSubject()) && this.predicate.equals(that.getPredicate())
					&& this.object.equals(that.getObject());
		}

		return false;
	}

	@Override
	public boolean hasSubjectStatements() {
		return !subjectStatements.isEmpty();
	}

	@Override
	public boolean hasPredicateStatements() {
		return false;
	}

	@Override
	public boolean hasObjectStatements() {
		return !objectStatements.isEmpty();
	}

	@Override
	public boolean hasContextStatements() {
		return false;
	}
}
