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
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;

/**
 * Magic properties are normally treated as {@link TupleFunction}s acting on the subject of a statement. However, there
 * are many cases where it makes more sense to treat a magic property as acting on the object of a statement instead.
 * Any TupleFunction implementing this interface will be given the object of the magic property as the argument and the
 * result will be bound to the subject of the magic property.
 */
public interface InverseMagicProperty extends TupleFunction {
}
