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
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.rio.RioSetting;

import com.github.jsonldjava.core.DocumentLoader;

/**
 * Settings that can be passed to JSONLD Parsers and Writers.
 *
 * @author Peter Ansell
 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
 */
public class JSONLDSettings {

	/**
	 * If set to true, the JSON-LD processor replaces arrays with just one element with that element during compaction.
	 * If set to false, all arrays will remain arrays even if they have just one element.
	 * <p>
	 * Defaults to true.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.compact_arrays}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 */
	public static final RioSetting<Boolean> COMPACT_ARRAYS = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.compact_arrays", "Compact arrays", Boolean.TRUE);

	/**
	 * If specified, it is used to retrieve remote documents and contexts; otherwise the processor's built-in loader is
	 * used.
	 */
	public static final RioSetting<DocumentLoader> DOCUMENT_LOADER = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.document_loader", "Document loader", null);

	/**
	 * If set to true, the JSON-LD processor is allowed to optimize the output of the
	 * <a href= "http://json-ld.org/spec/latest/json-ld-api/#compaction-algorithm" >Compaction algorithm</a> to produce
	 * even compacter representations.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.optimize}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 */
	public static final RioSetting<Boolean> OPTIMIZE = new BooleanRioSetting("org.eclipse.rdf4j.rio.jsonld.optimize",
			"Optimize output", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor may emit blank nodes for triple predicates, otherwise they will be omitted.
	 *
	 * Note: the use of blank node identifiers to label properties is obsolete, and may be removed in a future version
	 * of JSON-LD,
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.produce_generalized_rdf}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 */
	public static final RioSetting<Boolean> PRODUCE_GENERALIZED_RDF = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.produce_generalized_rdf", "Produce generalized RDF", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will try to convert typed values to JSON native types instead of using the
	 * expanded object form when converting from RDF. xsd:boolean values will be converted to true or false. xsd:integer
	 * and xsd:double values will be converted to JSON numbers.
	 * <p>
	 * Defaults to false for RDF compatibility.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.use_native_types}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 */
	public static final RioSetting<Boolean> USE_NATIVE_TYPES = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.use_native_types", "Use Native JSON Types", Boolean.FALSE);

	/**
	 * If set to true, the JSON-LD processor will use the expanded rdf:type IRI as the property instead of @type when
	 * converting from RDF.
	 * <p>
	 * Defaults to false.
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.use_rdf_type}.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
	 */
	public static final RioSetting<Boolean> USE_RDF_TYPE = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.use_rdf_type", "Use RDF Type", Boolean.FALSE);

	/**
	 * The {@link JSONLDMode} that the writer will use to reorganise the JSONLD document after it is created.
	 * <p>
	 * Defaults to {@link JSONLDMode#EXPAND} to provide maximum RDF compatibility.
	 *
	 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#features">JSONLD Features</a>
	 */
	public static final RioSetting<JSONLDMode> JSONLD_MODE = new RioSettingImpl<>(
			"org.eclipse.rdf4j.rio.jsonld_mode", "JSONLD Mode", JSONLDMode.EXPAND);

	/**
	 * If set to true, the JSON-LD processor will try to represent the JSON-LD object in a hierarchical view.
	 * <p>
	 * Default to false
	 * <p>
	 * Can be overridden by setting system property {@code org.eclipse.rdf4j.rio.jsonld.hierarchical_view}.
	 */
	public static final RioSetting<Boolean> HIERARCHICAL_VIEW = new BooleanRioSetting(
			"org.eclipse.rdf4j.rio.jsonld.hierarchical_view", "Hierarchical representation of the JSON", Boolean.FALSE);

	/**
	 * Private default constructor.
	 */
	private JSONLDSettings() {
	}

}
