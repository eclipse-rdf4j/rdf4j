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

/**
 *
 *
 * <h1>Rdf4J-Spring Tx</h1>
 *
 * Automatically configures spring transaction handling via {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig}.
 *
 * <p>
 * To enable, set <code>rdf4j.spring.tx.enabled=true</code>
 *
 * <p>
 * If enabled, @{@link org.springframework.transaction.annotation.Transactional @Transactional} annotations and Spring's
 * {@link org.springframework.transaction.support.TransactionTemplate TransactionTemplate} can be used to configure
 * transactionality of Rdf4J repository accesses.
 *
 * <p>
 * <b>Beware:</b> suspending transactions is not supported.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 *
 */

package org.eclipse.rdf4j.spring.tx;
