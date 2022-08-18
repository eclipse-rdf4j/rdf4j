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
/**
 * Parser/writer for the <a href="http://www.rdfhdt.org/hdt-binary-format/">HDT v1.0 format</a>.
 *
 * Unfortunately the draft specification is not entirely clear and probably slightly out of date, since the open source
 * reference implementation HDT-It seems to implement a slightly different version. This parser tries to be compatible
 * with HDT-It 1.0.
 *
 * File structure:
 *
 * <pre>
 * +---------------------+
 * | Global              |
 * | Header              |
 * | Dictionary (Shared) |
 * | Dictionary (S)      |
 * | Dictionary (P)      |
 * | Dictionary (O)      |
 * | Triples             |
 * +---------------------+
 * </pre>
 *
 * <h2>General structure for Global, Header, Dictionary and Triples</h2>
 *
 * These part all starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the format, and
 * optionally one or more <code>key=value;</code> properties.
 *
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included)
 *
 * <pre>
 * +------+------+--------+------+------------+------+-------+
 * | $HDT | type | format | NULL | key=value; | NULL | CRC16 |
 * +------+------+--------+------+------------+------+-------+
 * </pre>
 *
 * The <code>format</code> varies slightly: depending on the section, it can either be a string or a URI.
 *
 */
package org.eclipse.rdf4j.rio.hdt;
