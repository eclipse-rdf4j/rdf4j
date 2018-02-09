/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * An expression that executes an update.
 */
public interface UpdateExpr extends QueryModelNode {

	public UpdateExpr clone();

	/**
	 * @return Returns the silent.
	 */
	public boolean isSilent();
}
