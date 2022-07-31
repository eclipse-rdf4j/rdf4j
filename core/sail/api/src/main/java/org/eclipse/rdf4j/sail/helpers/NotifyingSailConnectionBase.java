/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

/**
 * @author Jeen Broekstra
 *
 * @deprecated since RDF4J 4.0. Use {@link AbstractNotifyingSailConnection} instead.
 */
@Deprecated(forRemoval = true)
public abstract class NotifyingSailConnectionBase extends AbstractNotifyingSailConnection {

	public NotifyingSailConnectionBase(AbstractSail sailBase) {
		super(sailBase);
	}

}
