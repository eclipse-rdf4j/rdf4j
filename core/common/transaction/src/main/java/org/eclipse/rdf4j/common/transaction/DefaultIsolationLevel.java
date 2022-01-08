/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

import java.util.Arrays;
import java.util.List;

/**
 * @author jeen
 *
 */
final class DefaultIsolationLevel implements IsolationLevel {

	private final List<? extends IsolationLevel> compatibleLevels;

	DefaultIsolationLevel(IsolationLevel... compatibleLevels) {
		this.compatibleLevels = Arrays.asList(compatibleLevels);
	}

	@Override
	public boolean isCompatibleWith(IsolationLevel otherLevel) {
		return this.equals(otherLevel) || compatibleLevels.contains(otherLevel);
	}

}
