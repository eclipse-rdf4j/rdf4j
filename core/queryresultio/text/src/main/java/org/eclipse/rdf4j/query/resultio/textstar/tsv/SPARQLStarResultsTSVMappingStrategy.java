/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.textstar.tsv;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVMappingStrategy;

/**
 * Extends {@link SPARQLResultsTSVMappingStrategy} with support for parsing a {@link org.eclipse.rdf4j.model.Triple}.
 *
 * @author Pavel Mihaylov
 * @deprecated since 3.4.0 - functionality has been folded into {@link SPARQLResultsTSVMappingStrategy}
 */
@Deprecated
public class SPARQLStarResultsTSVMappingStrategy extends SPARQLResultsTSVMappingStrategy {
	public SPARQLStarResultsTSVMappingStrategy(ValueFactory valueFactory) {
		super(valueFactory);
	}
}
