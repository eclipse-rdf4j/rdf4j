/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.resultcache;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;

import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.spring.support.query.DelegatingUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update that, upon each invocation of <code>execute()</code>, clears the result cache it is aware of.
 */
public class ClearableAwareUpdate extends DelegatingUpdate {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	WeakReference<Clearable> clearableRef;

	public ClearableAwareUpdate(Update delegate, Clearable clearable) {
		super(delegate);
		this.clearableRef = new WeakReference<>(clearable);
	}

	@Override
	public void execute() throws UpdateExecutionException {
		super.execute();
		Clearable clearable = clearableRef.get();
		if (clearable == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug(
					"marking Dirty: instance {} of type {}",
					hashCode(),
					clearable.getClass().getSimpleName());
		}
		clearable.markDirty();
	}

	public void renewClearable(Clearable clearable) {
		this.clearableRef = new WeakReference<>(clearable);
	}
}