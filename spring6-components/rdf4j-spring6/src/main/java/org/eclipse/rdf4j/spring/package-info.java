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
 * <H1>Rdf4J-Spring</H1>
 *
 * Configures beans for Rdf4J access. Uses Spring's autoconfiguration mechanism at startup to determine which subsystems
 * to use. The following example shows a spring configuration class enabling all features and using an in-memory
 * repository. The DAO classes (subclasses of {@link org.eclipse.rdf4j.spring.dao.RDF4JDao Rdf4JDao}), assumed to be
 * under <code>com.example.your.app
 * </code>, are autodetected.
 *
 * <pre>
 *
 * &#64Configuration
 * &#64Import(Rdf4JConfig.class)
 * &#64ComponentScan(
 *         value = "com.example.your.app",
 *         includeFilters =
 *                 &#64ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Rdf4JDao.class)
 * &#64TestPropertySource(
 *         properties = {
 *             "rdf4j.spring.repository.inmemory.enabled=true",
 *             "rdf4j.spring.pool.enabled=true",
 *             "rdf4j.spring.operationcache.enabled=true",
 *             "rdf4j.spring.operationlog.enabled=true",
 *             "rdf4j.spring.resultcache.enabled=true",
 *             "rdf4j.spring.tx.enabled=true",
 *         })
 * public class Rdf4JStorageConfiguration {
 *
 *     // beans, if any (you may not need any - all your DAOs could be autodetected and all other beans may
 *     // be configured elsewhere)
 *
 * }
 * </pre>
 *
 * <p>
 * For more information on the subsystems, please refer to their package-infos:
 *
 * <ul>
 * <li>{@link org.eclipse.rdf4j.spring.operationcache Rdf4J-Spring OperationCache}
 * <li>{@link org.eclipse.rdf4j.spring.operationlog Rdf4J-Spring OperationLog}
 * <li>{@link org.eclipse.rdf4j.spring.pool Rdf4J-Spring Pool}
 * <li>{@link org.eclipse.rdf4j.spring.repository Rdf4J-Spring Repository}
 * <li>{@link org.eclipse.rdf4j.spring.resultcache Rdf4J-Spring ResultCache}
 * <li>{@link org.eclipse.rdf4j.spring.tx Rdf4J-Spring Tx}
 * </ul>
 * </p>
 *
 * <p>
 * This software has been developed in the project 'BIM-Interoperables Merkmalservice', funded by the Austrian Research
 * Promotion Agency and Österreichische Bautechnik Veranstaltungs GmbH.
 * </p>
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring;
