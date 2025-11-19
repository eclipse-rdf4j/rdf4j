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
 * This package contains different approaches for generating UUIDs. One of them always generates the same sequence of
 * UUIDs after bean initialization, which is useful for tests. Three of them obtain them from a
 * {@link org.eclipse.rdf4j.repository.Repository Repository}, guaranteeing their uniqueness. Due to the <b>very, very,
 * very</b> low probability of a collision, it is recommended not to use any of the latter and instead rely on the one
 * instantiated by default, {@link org.eclipse.rdf4j.spring.support.DefaultUUIDSource DefaultUUIDSource}.
 *
 * <ol>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.predictable.PredictableUUIDSource PredictableUUIDSource}: Always
 * generate the same sequence of UUIDs.</li>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.noveltychecking.NoveltyCheckingUUIDSource NoveltyCheckingUUIDSource}:
 * Generate a {@link java.util.UUID UUID} locally using {@link java.util.UUID#randomUUID() UUID.randomUUID()} and then
 * ask the repository if the UUID is unique. Enable with
 * <code>rdf4j.spring.uuidsource.noveltychecking.enabled=true</code>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.simple.SimpleRepositoryUUIDSource SimpleRepositoryUUIDSource}: Ask the
 * repository for a new UUID each time one is needed. Enable with
 * <code>rdf4j.spring.uuidsource.simple.enabled=true</code>
 * <li>{@link org.eclipse.rdf4j.spring.uuidsource.sequence.UUIDSequence UUIDSequence}: When a UUID is needed, ask the
 * repository for N >> 1 UUIDs and answer one at a time. Enable with <code>
 *       rdf4j.spring.uuidsource.sequence.enabled=true</code>
 * </ol>
 *
 * Only one of these approaches can be activated.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring.uuidsource;
