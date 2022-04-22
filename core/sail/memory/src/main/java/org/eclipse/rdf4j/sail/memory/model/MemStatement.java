/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.eclipse.rdf4j.model.impl.ContextStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MemStatement is a Statement which contains context information and a flag indicating whether the statement is
 * explicit or inferred.
 */
public class MemStatement extends ContextStatement {

	private static final Logger logger = LoggerFactory.getLogger(MemStatement.class);

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -3073275483628334134L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether or not this statement has been added explicitly or that it has been inferred.
	 */
	private final boolean explicit;

	/**
	 * Identifies the snapshot in which this statement was introduced.
	 */
	private volatile int sinceSnapshot;

	/**
	 * Identifies the snapshot in which this statement was revoked, defaults to {@link Integer#MAX_VALUE}.
	 */
	private volatile int tillSnapshot = Integer.MAX_VALUE;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemStatement with the supplied subject, predicate, object and context and marks it as 'explicit'.
	 */
	public MemStatement(MemResource subject, MemIRI predicate, MemValue object, MemResource context,
			int sinceSnapshot) {
		this(subject, predicate, object, context, true, sinceSnapshot);
	}

	/**
	 * Creates a new MemStatement with the supplied subject, predicate, object and context. The value of the
	 * <var>explicit</var> parameter determines if this statement is marked as 'explicit' or not.
	 */
	public MemStatement(MemResource subject, MemIRI predicate, MemValue object, MemResource context, boolean explicit,
			int sinceSnapshot) {
		super(subject, predicate, object, context);
		this.explicit = explicit;
		setSinceSnapshot(sinceSnapshot);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public MemResource getSubject() {
		return (MemResource) super.getSubject();
	}

	@Override
	public MemIRI getPredicate() {
		return (MemIRI) super.getPredicate();
	}

	@Override
	public MemValue getObject() {
		return (MemValue) super.getObject();
	}

	@Override
	public MemResource getContext() {
		return (MemResource) super.getContext();
	}

	public void setSinceSnapshot(int snapshot) {
		sinceSnapshot = snapshot;
	}

	public int getSinceSnapshot() {
		return sinceSnapshot;
	}

	public void setTillSnapshot(int snapshot) {
		tillSnapshot = snapshot;
	}

	public int getTillSnapshot() {
		return tillSnapshot;
	}

	public boolean isInSnapshot(int snapshot) {
		return snapshot >= sinceSnapshot && snapshot < tillSnapshot;
	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	public void setExplicit(boolean explicit) {
		logger.warn(
				"The explicit field has been set to final for improved performance. Java reflection will be used " +
						"to modify it. Take note that the MemorySailStore will not detect this change and may " +
						"assume that it doesn't have any inferred statements!");

		try {
			Field explicitField = MemStatement.class.getDeclaredField("explicit");
			explicitField.setAccessible(true);
			explicitField.set(this, explicit);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * Lets this statement add itself to the appropriate statement lists of its subject, predicate, object and context.
	 * The transaction status will be set to new.
	 */
	public void addToComponentLists() {
		getSubject().addSubjectStatement(this);
		getPredicate().addPredicateStatement(this);
		getObject().addObjectStatement(this);
		MemResource context = getContext();
		if (context != null) {
			context.addContextStatement(this);
		}
	}

	/**
	 * Lets this statement remove itself from the appropriate statement lists of its subject, predicate, object and
	 * context. The transaction status will be set to <var>null</var>.
	 */
	public void removeFromComponentLists() {
		getSubject().removeSubjectStatement(this);
		getPredicate().removePredicateStatement(this);
		getObject().removeObjectStatement(this);
		MemResource context = getContext();
		if (context != null) {
			context.removeContextStatement(this);
		}
	}

	public boolean matchesSPO(MemResource subject, MemIRI predicate, MemValue object) {
		return (subject == null || exactSameSubject(subject)) &&
				(predicate == null || exactSamePredicate(predicate)) &&
				(object == null || exactSameObject(object));
	}
}
