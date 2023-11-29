/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.eclipse.rdf4j.rio.helpers.ClassRioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;

import no.hasmac.jsonld.document.Document;

/**
 * Settings that can be passed to JSONLD Parsers and Writers.
 *
 * @author Peter Ansell
 * @see <a href="http://json-ld.org/spec/latest/json-ld-api/#data-structures">JSONLD Data Structures</a>
 *
 * @since 4.3.0
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
	 * The JSON-LD context to use when expanding JSON-LD
	 *
	 * @see <a href=
	 *      "https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext">https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext</a>.
	 */
	public static final RioSetting<Document> EXPAND_CONTEXT = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.expand_context",
			"A no.hasmac.jsonld.document.Document that contains the expanded context as specified in https://www.w3.org/TR/json-ld11-api/#dom-jsonldoptions-expandcontext",
			null);

	public static final RioSetting<Document> FRAME = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.frame_document",
			"A no.hasmac.jsonld.document.Document that contains the frame used for framing as specified in https://www.w3.org/TR/json-ld11-framing/",
			null);;

	/**
	 * The JSON-LD processor will throw an exception if a warning is encountered during processing.
	 *
	 */
	public static final RioSetting<Boolean> EXCEPTION_ON_WARNING = new ClassRioSetting<>(
			"org.eclipse.rdf4j.rio.jsonld.exception_on_warning",
			"Throw an exception when logging a warning.",
			Boolean.FALSE);

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
