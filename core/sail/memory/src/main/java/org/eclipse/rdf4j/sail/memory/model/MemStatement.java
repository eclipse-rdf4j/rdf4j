/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;

import org.eclipse.rdf4j.model.impl.GenericStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MemStatement is a Statement which contains context information and a flag indicating whether the statement is
 * explicit or inferred.
 */
public class MemStatement extends GenericStatement<MemResource, MemIRI, MemValue> {

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
	private final int sinceSnapshot;

	/**
	 * Identifies the snapshot in which this statement was revoked, defaults to {@link Integer#MAX_VALUE}.
	 */
	private volatile int tillSnapshot = Integer.MAX_VALUE;
	private static final VarHandle TILL_SNAPSHOT;

	static {
		try {
			TILL_SNAPSHOT = MethodHandles.lookup()
					.in(MemStatement.class)
					.findVarHandle(MemStatement.class, "tillSnapshot", int.class);
		} catch (ReflectiveOperationException e) {
			throw new Error(e);
		}
	}
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
		this.sinceSnapshot = sinceSnapshot;
	}

	public int getSinceSnapshot() {
		return sinceSnapshot;
	}

	public void setTillSnapshot(int snapshot) {
		TILL_SNAPSHOT.setRelease(this, snapshot);
	}

	public int getTillSnapshot() {
		return (int) TILL_SNAPSHOT.getAcquire(this);
	}

	public boolean isInSnapshot(int snapshot) {
		return snapshot >= sinceSnapshot && snapshot < ((int) TILL_SNAPSHOT.getAcquire(this));
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
	public void addToComponentLists() throws InterruptedException {
		getSubject().addSubjectStatement(this);
		getPredicate().addPredicateStatement(this);
		getObject().addObjectStatement(this);
		MemResource context = getContext();
		if (context != null) {
			context.addContextStatement(this);
		}
	}

	public boolean matchesSPO(MemResource subject, MemIRI predicate, MemValue object) {
		return (object == null || object == this.object) && (subject == null || subject == this.subject) &&
				(predicate == null || predicate == this.predicate);
	}

	public boolean matchesContext(MemResource[] memContexts) {
		if (memContexts != null && memContexts.length > 0) {
			for (MemResource context : memContexts) {
				if (context == this.context) {
					return true;
				}
			}
			return false;
		} else {
			// there is no context to check so we can return this statement
			return true;
		}
	}

	public boolean exactMatch(MemResource subject, MemIRI predicate, MemValue object, MemResource context) {
		return this.subject == subject && this.predicate == predicate && this.object == object
				&& this.context == context;
	}

}
