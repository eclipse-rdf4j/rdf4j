/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlods;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;

// Assume TupleQueryResultFormat.ODS exists or is defined elsewhere
// import static org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat.ODS;

/**
 * Render a SPARQL result set into an ODF spreadsheet file (.ods) by manually generating the XML content and ZIP
 * structure.
 *
 * NOTE: This implementation manually creates XML and does not use any ODF library. It is more complex and potentially
 * less robust than using a dedicated library. Auto-sizing columns is not implemented as it's typically handled by the
 * viewing application.
 *
 * @author Adapted from SPARQLResultsXLSXWriter by Jerven Bolleman
 */
public class SPARQLResultsODSWriter implements TupleQueryResultWriter {

	private final ZipOutputStream zos;
	// Replace PrintWriter with XMLWriter
	private XMLWriter contentXmlWriter;

	private final Map<String, Integer> columnIndexes = new HashMap<>();
	private final Map<String, String> prefixes = new HashMap<>();

	private int columnCount = 0;
	private boolean headerWritten = false;

	// ODF requires specific date/time format
	private static final DateTimeFormatter ODF_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final DateTimeFormatter ODF_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	// Style names (must match definitions in styles.xml)
	private static final String STYLE_DEFAULT = "DefaultStyle";
	private static final String STYLE_HEADER = "HeaderStyle";
	private static final String STYLE_IRI = "IriStyle";
	private static final String STYLE_ANY_IRI = "AnyIriStyle"; // Differentiate if needed
	private static final String STYLE_NUMERIC = "NumericStyle";
	private static final String STYLE_DATE = "DateStyle";
	private static final String STYLE_DATETIME = "DateTimeStyle";
	private static final String STYLE_BOOLEAN = "BooleanStyle";

	// ODS Namespaces
	private static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
	private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
	private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	private static final String STYLE_NS = "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
	private static final String FO_NS = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0";
	private static final String XLINK_NS = "http://www.w3.org/1999/xlink";
	private static final String DC_NS = "http://purl.org/dc/elements/1.1/";
	private static final String META_NS = "urn:oasis:names:tc:opendocument:xmlns:meta:1.0";
	private static final String NUMBER_NS = "urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0";
	private static final String SVG_NS = "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0";
	private static final String MANIFEST_NS = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0";

	// ODS Prefixes
	private static final String OFFICE_PRE = "office";
	private static final String TABLE_PRE = "table";
	private static final String TEXT_PRE = "text";
	private static final String STYLE_PRE = "style";
	private static final String FO_PRE = "fo";
	private static final String XLINK_PRE = "xlink";
	private static final String DC_PRE = "dc";
	private static final String META_PRE = "meta";
	private static final String NUMBER_PRE = "datastyle";
	private static final String SVG_PRE = "svg";
	private static final String MANIFEST_PRE = "manifest";

	public SPARQLResultsODSWriter(OutputStream out) {
		this.zos = new ZipOutputStream(out);
	}

	// --- Core TupleQueryResultWriter Methods ---

	@Override
	public void startDocument() throws QueryResultHandlerException {
		try {
			// 1. Write mimetype (must be first and uncompressed)
			ZipEntry mimetypeEntry = new ZipEntry("mimetype");
			mimetypeEntry.setMethod(ZipEntry.STORED);
			byte[] mimetype = "application/vnd.oasis.opendocument.spreadsheet".getBytes(StandardCharsets.US_ASCII);
			mimetypeEntry.setSize(mimetype.length); // Length of the mimetype string
			mimetypeEntry.setCompressedSize(mimetype.length);
//			// CRC-32 for "application/vnd.oasis.opendocument.spreadsheet" is 0xadc46ac
//			mimetypeEntry.setCrc(0xadc46acL);
			mimetypeEntry.setCrc(0x8a396c85L);
			zos.putNextEntry(mimetypeEntry);
			zos.write(mimetype);
			zos.closeEntry();
			zos.setMethod(ZipEntry.DEFLATED); // Use compression for subsequent entries

			// 2. Prepare XMLWriters for main XML files

			// styles.xml
			zos.putNextEntry(new ZipEntry("styles.xml"));
			// Instantiate XMLWriter
			XMLWriter stylesXmlWriter = new XMLWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
			stylesXmlWriter.setPrettyPrint(true); // Enable indentation for readability
			writeStylesXml(stylesXmlWriter); // Write boilerplate and style definitions using XMLWriter
			stylesXmlWriter.endDocument();
			// stylesXmlWriter.close(); // Don't close underlying stream
			zos.closeEntry(); // Close the styles.xml entry

			// --- Write meta.xml ---
			zos.putNextEntry(new ZipEntry("meta.xml"));
			// Use try-with-resources for the intermediate XMLWriter
			XMLWriter metaXmlWriter = new XMLWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
			metaXmlWriter.setPrettyPrint(true);
			writeMetaXml(metaXmlWriter); // Use XMLWriter methods
			metaXmlWriter.endDocument();
			// Don't close the underlying stream
			zos.closeEntry(); // close meta

			// --- Write META-INF/manifest.xml ---
			zos.putNextEntry(new ZipEntry("META-INF/manifest.xml"));

			XMLWriter manifestXmlWriter = new XMLWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
			manifestXmlWriter.setPrettyPrint(true);
			writeManifestXml(manifestXmlWriter); // Use XMLWriter methods
			manifestXmlWriter.endDocument();
			// Don't close the underlying stream
			zos.closeEntry();

			// content.xml
			zos.putNextEntry(new ZipEntry("content.xml"));
			// Instantiate XMLWriter
			contentXmlWriter = new XMLWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8));
			contentXmlWriter.setPrettyPrint(true); // Enable indentation
			writeContentXmlStart(); // Write boilerplate up to <office:spreadsheet> using XMLWriter

		} catch (IOException e) {
			throw new QueryResultHandlerException("Failed to initialize ODF document structure", e);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		prefixes.put(uri, prefix);
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		this.columnCount = bindingNames.size();
		int columnIndex = 0;
		columnIndexes.clear(); // Reset for potential multiple results
		for (String bindingName : bindingNames) {
			columnIndexes.put(bindingName, columnIndex++);
		}

		if (contentXmlWriter == null) {
			throw new TupleQueryResultHandlerException("startQueryResult called before startDocument");
		}

		// Write table structures only once
		if (!headerWritten) {
			try {
				// Write Table Definitions in content.xml for "nice" sheet
				// Note: Skipping the separate "raw" sheet for simplicity in this refactor
				writeTableStart(contentXmlWriter, "QueryResult", columnCount); // Use a descriptive name
				writeHeaderRow(contentXmlWriter, bindingNames, STYLE_HEADER);
				// Keep the table open for data rows

				headerWritten = true;
			} catch (IOException e) {
				throw new TupleQueryResultHandlerException("Failed to write table start/header", e);
			}
		} else {
			throw new TupleQueryResultHandlerException(
					"startQueryResult called more than once. ODF writer handles only one result set.");
		}
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		if (!headerWritten || contentXmlWriter == null) {
			throw new TupleQueryResultHandlerException(
					"handleSolution called before startQueryResult or after endDocument");
		}

		try {
			startTag(contentXmlWriter, TABLE_PRE, "table-row");
			// contentXmlWriter.attribute(TABLE_NS, "style-name", "ro1"); // Optional:
			// Assuming default row style

			Value[] values = new Value[columnCount]; // To hold values in correct column order
			for (Binding binding : bindingSet) {
				int colIdx = columnIndexes.getOrDefault(binding.getName(), -1);
				if (colIdx != -1) {
					values[colIdx] = binding.getValue();
				}
			}

			// Iterate through columns to ensure correct order and handle unbound variables
			for (int i = 0; i < columnCount; i++) {
				Value v = values[i];
				if (v == null) {
					// Write empty cell for unbound variable
					emptyTag(contentXmlWriter, TABLE_PRE, "table-cell");
				} else {
					// Write formatted cell based on value type
					writeCell(contentXmlWriter, v);
				}
			}
			endTag(contentXmlWriter, TABLE_PRE, "table-row");
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException("Failed to write data row", e);
		}
	}

	private void emptyTag(XMLWriter writer, String prefix, String element) throws IOException {
		writer.startTag(prefix + ':' + element);
		writer.endTag(prefix + ':' + element);
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		if (contentXmlWriter == null)
			return; // Nothing was started

		// Close the table if it was opened
		if (headerWritten) {
			try {
				writeTableEnd(contentXmlWriter); // Close the last opened table
			} catch (IOException e) {
				throw new TupleQueryResultHandlerException("Failed to write table end", e);
			}
		}
		endDocument();
	}

	private void endDocument() throws QueryResultHandlerException {
		try {
			// --- Finish content.xml ---
			if (contentXmlWriter != null) {
				writeContentXmlEnd(contentXmlWriter); // Write closing tags
				contentXmlWriter.endDocument(); // Finalize XML document
				// XMLWriter wraps an OutputStreamWriter which shouldn't be closed directly
				// here,
				// as closing the ZipEntry handles the underlying stream flushing.
				// contentXmlWriter.close(); // Don't close underlying stream
				zos.closeEntry(); // Close the content.xml entry
				contentXmlWriter = null; // Mark as finished
			}

			// --- Finish the ZIP archive ---
			zos.finish();
			zos.close(); // Close the main ZipOutputStream

		} catch (IOException e) {
			throw new QueryResultHandlerException("Failed to finalize or write ODF document components", e);
		}
	}

	// --- Helper Methods for ODS XML Generation using XMLWriter ---

	private void declareNamespaces(XMLWriter writer, String... prefixesAndUris) throws IOException {
		for (int i = 0; i < prefixesAndUris.length; i += 2) {
			writer.setAttribute("xmlns:" + prefixesAndUris[i], prefixesAndUris[i + 1]);
		}
	}

	private void writeContentXmlStart() throws IOException {
		contentXmlWriter.startDocument();
		declareNamespaces(contentXmlWriter, OFFICE_PRE, OFFICE_NS, TABLE_PRE, TABLE_NS, TEXT_PRE, TEXT_NS, FO_PRE,
				FO_NS, XLINK_PRE, XLINK_NS, DC_PRE, DC_NS, META_PRE, META_NS, NUMBER_PRE, NUMBER_NS, STYLE_PRE,
				STYLE_NS, SVG_PRE, SVG_NS);
		startTag(contentXmlWriter, OFFICE_PRE, "document-content");
		setAttribute(contentXmlWriter, OFFICE_PRE, "version", "1.2");

		emptyTag(contentXmlWriter, OFFICE_PRE, "scripts"); // Required

		startTag(contentXmlWriter, OFFICE_PRE, "font-face-decls");
		{ // font
			setAttribute(contentXmlWriter, STYLE_PRE, "name", "Liberation Sans");
			setAttribute(contentXmlWriter, SVG_PRE, "font-family", "Liberation Sans");
			setAttribute(contentXmlWriter, STYLE_PRE, "font-family-generic", "swiss");
			setAttribute(contentXmlWriter, STYLE_PRE, "font-pitch", "variable");
			emptyTag(contentXmlWriter, STYLE_PRE, "font-face");
		}
		endTag(contentXmlWriter, OFFICE_PRE, "font-face-decls"); // office:font-face-decls

		startTag(contentXmlWriter, OFFICE_PRE, "automatic-styles");
		{
			// Define basic column style (co1) and row style (ro1)
			setAttribute(contentXmlWriter, STYLE_PRE, "name", "co1");
			setAttribute(contentXmlWriter, STYLE_PRE, "family", "table-column");
			startTag(contentXmlWriter, STYLE_PRE, "style");
			{

				setAttribute(contentXmlWriter, FO_PRE, "break-before", "auto");
				setAttribute(contentXmlWriter, STYLE_PRE, "column-width", "2.257cm"); // Default width
				emptyTag(contentXmlWriter, STYLE_PRE, "table-column-properties");
			}
			endTag(contentXmlWriter, STYLE_PRE, "style"); // style:style co1

			setAttribute(contentXmlWriter, STYLE_PRE, "name", "ro1");
			setAttribute(contentXmlWriter, STYLE_PRE, "family", "table-row");
			startTag(contentXmlWriter, STYLE_PRE, "style");
			{
				setAttribute(contentXmlWriter, STYLE_PRE, "row-height", "0.453cm");
				setAttribute(contentXmlWriter, FO_PRE, "break-before", "auto");
				setAttribute(contentXmlWriter, STYLE_PRE, "use-optimal-row-height", "true");
				emptyTag(contentXmlWriter, STYLE_PRE, "table-row-properties");
			}
			endTag(contentXmlWriter, STYLE_PRE, "style"); // style:style ro1
			// Add automatic data styles if needed (e.g., N1, N2 for specific number
			// formats)
		}
		endTag(contentXmlWriter, OFFICE_PRE, "automatic-styles");

		startTag(contentXmlWriter, OFFICE_PRE, "body");
		startTag(contentXmlWriter, OFFICE_PRE, "spreadsheet");
		// Tables will be added here by startQueryResult/handleSolution
	}

	private void emptyElement(XMLWriter contentXmlWriter, String prefix, String element, String content)
			throws IOException {
		contentXmlWriter.startTag(prefix + ':' + element);
		contentXmlWriter.text(content);
		contentXmlWriter.endTag(prefix + ':' + element);
	}

	private void startTag(XMLWriter writer, String prefix, String element) throws IOException {
		writer.startTag(prefix + ':' + element);
	}

	private void writeContentXmlEnd(XMLWriter writer) throws IOException {
		endTag(writer, OFFICE_PRE, "spreadsheet"); // office:spreadsheet
		endTag(writer, OFFICE_PRE, "body"); // office:body
		endTag(writer, OFFICE_PRE, "document-content"); // office:
	}

	private void endTag(XMLWriter writer, String officePre, String element) throws IOException {
		writer.endTag(officePre + ":" + element);
	}

	private void writeStylesXml(XMLWriter stylesXmlWriter) throws IOException {
		stylesXmlWriter.startDocument();
		declareNamespaces(stylesXmlWriter, "office", OFFICE_NS, "style", STYLE_NS, "text", TEXT_NS, "table", TABLE_NS,
				// "draw", "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0",
				"fo", FO_NS, "xlink", XLINK_NS, "dc", DC_NS, "meta", META_NS, "number", NUMBER_NS, "svg", SVG_NS);
		setAttribute(stylesXmlWriter, OFFICE_PRE, "version", "1.2");
		startTag(stylesXmlWriter, OFFICE_PRE, "document-styles");
		{
			{
				startTag(stylesXmlWriter, OFFICE_PRE, "font-face-decls");
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Liberation Sans");
				setAttribute(stylesXmlWriter, SVG_PRE, "font-family", "'Liberation Sans'");
				setAttribute(stylesXmlWriter, STYLE_PRE, "font-family-generic", "swiss");
				setAttribute(stylesXmlWriter, STYLE_PRE, "font-pitch", "variable");

				emptyTag(stylesXmlWriter, STYLE_PRE, "font-face");

				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Liberation Mono");
				setAttribute(stylesXmlWriter, SVG_PRE, "font-family", "'Liberation Mono'");
				setAttribute(stylesXmlWriter, STYLE_PRE, "font-family-generic", "modern");
				setAttribute(stylesXmlWriter, STYLE_PRE, "font-pitch", "fixed");

				endTag(stylesXmlWriter, OFFICE_PRE, "font-face-decls"); // office:font-face-decls
			}
			startTag(stylesXmlWriter, OFFICE_PRE, "styles");
			{
				// --- Default Cell Style ---

				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_DEFAULT);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", "Default"); // Assumes "Default" is
																							// built-in
																							// or defined
				startTag(stylesXmlWriter, STYLE_PRE, "style");
				{
					setAttribute(stylesXmlWriter, FO_PRE, "padding", "0.097cm");
					setAttribute(stylesXmlWriter, FO_PRE, "border", "0.002cm solid #000000"); // Basic border
					emptyTag(stylesXmlWriter, STYLE_PRE, "table-cell-properties");

					setAttribute(stylesXmlWriter, STYLE_PRE, "font-name", "Liberation Sans");
					setAttribute(stylesXmlWriter, FO_PRE, "font-size", "10pt");
					emptyTag(stylesXmlWriter, STYLE_PRE, "text-properties");
				}
				endTag(stylesXmlWriter, STYLE_PRE, "style");

				// --- Header Style ---
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_HEADER);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				startTag(stylesXmlWriter, STYLE_PRE, "style");
				{
					setAttribute(stylesXmlWriter, FO_PRE, "background-color", "#cccccc");
					setAttribute(stylesXmlWriter, FO_PRE, "text-align", "center");
					setAttribute(stylesXmlWriter, STYLE_PRE, "vertical-align", "middle");
					setAttribute(stylesXmlWriter, FO_PRE, "border", "0.002cm solid #000000");

					emptyTag(stylesXmlWriter, STYLE_PRE, "table-cell-properties");

					setAttribute(stylesXmlWriter, FO_PRE, "font-weight", "bold");
					setAttribute(stylesXmlWriter, STYLE_PRE, "font-name", "Liberation Sans");
					setAttribute(stylesXmlWriter, FO_PRE, "font-size", "10pt");

					emptyTag(stylesXmlWriter, STYLE_PRE, "text-properties");
				}
				endTag(stylesXmlWriter, STYLE_PRE, "style"); // style:style HeaderStyle

				// --- IRI Hyperlink Style ---
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_IRI);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				startTag(stylesXmlWriter, STYLE_PRE, "style");
				{
					setAttribute(stylesXmlWriter, FO_PRE, "color", "#0000ff");
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-style", "solid");
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-width", "auto");
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-color", "font-color"); // Blue, underlined
					emptyTag(stylesXmlWriter, STYLE_PRE, "text-properties");
				}
				endTag(stylesXmlWriter, STYLE_PRE, "style"); // style:style IriStyle

				// --- Any IRI Hyperlink Style ---
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_ANY_IRI);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				startTag(stylesXmlWriter, STYLE_PRE, "style");
				{
					setAttribute(stylesXmlWriter, FO_PRE, "color", "#ff00ff"); // Magenta
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-style", "solid");
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-width", "auto");
					setAttribute(stylesXmlWriter, STYLE_PRE, "text-underline-color", "font-color");
					emptyTag(stylesXmlWriter, STYLE_PRE, "text-properties");
				}
				endTag(stylesXmlWriter, STYLE_PRE, "style");

				// --- Define Number/Date/Bool Data Styles First (referenced by cell styles) ---
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "N0");
				startTag(stylesXmlWriter, NUMBER_PRE, "number-style");
				{
					setAttribute(stylesXmlWriter, NUMBER_PRE, "min-integer-digits", "1");
					setAttribute(stylesXmlWriter, NUMBER_PRE, "decimal-places", "2");// 2 decimal places
					setAttribute(stylesXmlWriter, NUMBER_PRE, "grouping", "false");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "number");
				}
				endTag(stylesXmlWriter, NUMBER_PRE, "number-style");

				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Ndate");
				setAttribute(stylesXmlWriter, NUMBER_PRE, "automatic-order", "true");
				startTag(stylesXmlWriter, NUMBER_PRE, "date-style");
				{
					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "year");

					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "month");

					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "day");
				}
				endTag(stylesXmlWriter, NUMBER_PRE, "date-style");

				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Ndatetime");
				setAttribute(stylesXmlWriter, NUMBER_PRE, "automatic-order", "true");
				startTag(stylesXmlWriter, NUMBER_PRE, "date-style");
				{
					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "year");
					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "month");

					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");
					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "day");
					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", " "); // Separator

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "hours");

					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "minutes");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyElement(stylesXmlWriter, NUMBER_PRE, "text", "-");

					setAttribute(stylesXmlWriter, NUMBER_PRE, "style", "long");
					emptyTag(stylesXmlWriter, NUMBER_PRE, "seconds");
				}
				endTag(stylesXmlWriter, NUMBER_PRE, "date-style");

				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Nbool");
				emptyTag(stylesXmlWriter, NUMBER_PRE, "boolean-style"); // Displays TRUE/FALSE

				// --- Cell styles referencing data styles ---
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_NUMERIC);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				setAttribute(stylesXmlWriter, STYLE_PRE, "data-style-name", "N0");
				// Reference N0 number format
				emptyTag(stylesXmlWriter, STYLE_PRE, "style");

				// Reference Ndate
				// date
				// format
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_DATE);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				setAttribute(stylesXmlWriter, STYLE_PRE, "data-style-name", "Ndate");
				emptyTag(stylesXmlWriter, STYLE_PRE, "style");

				// Reference
				// Ndatetime
				// date
				// format
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_DATETIME);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				setAttribute(stylesXmlWriter, STYLE_PRE, "data-style-name", "Ndatetime");
				emptyTag(stylesXmlWriter, STYLE_PRE, "style");

				// Reference
				// Nbool
				// boolean
				// format
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", STYLE_BOOLEAN);
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table-cell");
				setAttribute(stylesXmlWriter, STYLE_PRE, "parent-style-name", STYLE_DEFAULT);
				setAttribute(stylesXmlWriter, STYLE_PRE, "data-style-name", "Nbool");
				emptyTag(stylesXmlWriter, STYLE_PRE, "style");
			}
			endTag(stylesXmlWriter, OFFICE_PRE, "styles"); // office:styles

			startTag(stylesXmlWriter, OFFICE_PRE, "automatic-styles");
			// Could define column/row styles here too if needed (ta1 example)
			{
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "ta1");
				setAttribute(stylesXmlWriter, STYLE_PRE, "family", "table");
				setAttribute(stylesXmlWriter, STYLE_PRE, "master-page-name", "Default"); // Link to master page
				startTag(stylesXmlWriter, STYLE_PRE, "style");
				{
					setAttribute(stylesXmlWriter, TABLE_PRE, "display", "true");
					setAttribute(stylesXmlWriter, STYLE_PRE, "writing-mode", "lr-tb");
					emptyTag(stylesXmlWriter, STYLE_PRE, "table-properties");
				}
				endTag(stylesXmlWriter, STYLE_PRE, "style");
			}
			endTag(stylesXmlWriter, OFFICE_PRE, "automatic-styles");

			startTag(stylesXmlWriter, OFFICE_PRE, "master-styles"); // Required structure
			{
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "Default");
				setAttribute(stylesXmlWriter, STYLE_PRE, "page-layout-name", "pm1"); // Needs corresponding page-layout
				emptyTag(stylesXmlWriter, STYLE_PRE, "master-page");

				// Define the page layout referenced above
				setAttribute(stylesXmlWriter, STYLE_PRE, "name", "pm1");
				startTag(stylesXmlWriter, STYLE_PRE, "page-layout");

				setAttribute(stylesXmlWriter, FO_PRE, "margin", "0.7874in"); // Example margins
				setAttribute(stylesXmlWriter, FO_PRE, "page-width", "8.5in");
				setAttribute(stylesXmlWriter, FO_PRE, "page-height", "11in");
				setAttribute(stylesXmlWriter, STYLE_PRE, "print-orientation", "portrait");
				emptyTag(stylesXmlWriter, STYLE_PRE, "page-layout-properties");
				// Header/Footer styles would go inside page-layout if used
				endTag(stylesXmlWriter, STYLE_PRE, "page-layout");
			}
			endTag(stylesXmlWriter, OFFICE_PRE, "master-styles"); // Required structure
		}
		endTag(stylesXmlWriter, OFFICE_PRE, "document-styles"); // office:document-styles
	}

	private void writeMetaXml(XMLWriter writer) throws IOException {
		setAttribute(writer, OFFICE_PRE, "version", "1.2");
		writer.startDocument();
		declareNamespaces(writer, "office", OFFICE_NS, "meta", META_NS, "dc", DC_NS, "xlink", XLINK_NS);
		startTag(writer, OFFICE_PRE, "document-meta");
		{
			startTag(writer, OFFICE_PRE, "meta");
			{
				startTag(writer, META_PRE, "generator");
				writer.text("Eclipse RDF4J SPARQLResultsODSWriter (ODSWriter)");
				endTag(writer, META_PRE, "generator");
			}
			{
				startTag(writer, META_PRE, "creation-date");
				writer.text(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
				endTag(writer, META_PRE, "creation-date");
			}
			endTag(writer, OFFICE_PRE, "meta");
		}
		endTag(writer, OFFICE_PRE, "document-meta");
	}

	private void writeManifestXml(XMLWriter writer) throws IOException {
		writer.startDocument();
		writer.setAttribute("xmlns:manifest", MANIFEST_NS);
		setAttribute(writer, MANIFEST_PRE, "version", "1.2");
		startTag(writer, MANIFEST_PRE, "manifest");
		{
			setAttribute(writer, MANIFEST_PRE, "full-path", "/");
			// Version of the ODF standard for this entry
			setAttribute(writer, MANIFEST_PRE, "version", "1.2");
			setAttribute(writer, MANIFEST_PRE, "media-type", "application/vnd.oasis.opendocument.spreadsheet");
			emptyTag(writer, MANIFEST_PRE, "file-entry");

			addFile(writer, "content.xml");
			addFile(writer, "styles.xml");
			addFile(writer, "meta.xml");
		}
		endTag(writer, MANIFEST_PRE, "manifest"); // manifest:manifest
	}

	private void addFile(XMLWriter writer, String f) throws IOException {
		setAttribute(writer, MANIFEST_PRE, "full-path", f);
		setAttribute(writer, MANIFEST_PRE, "media-type", "text/xml");
		emptyTag(writer, MANIFEST_PRE, "file-entry");
	}

	private void writeTableStart(XMLWriter writer, String name, int columnCount) throws IOException {
		setAttribute(writer, TABLE_PRE, "name", name); // Use name provided
		setAttribute(writer, TABLE_PRE, "style-name", "ta1"); // Use defined table style
		startTag(writer, TABLE_PRE, "table");

		// Define columns - using the automatic style 'co1' defined earlier
		for (int i = 0; i < columnCount; i++) {
			setAttribute(writer, TABLE_PRE, "style-name", "co1");
			emptyElement(writer, TABLE_PRE, "table-column", name);
		}
	}

	private void writeTableEnd(XMLWriter writer) throws IOException {
		endTag(writer, TABLE_PRE, "table"); // table:table
	}

	private void writeHeaderRow(XMLWriter writer, List<String> bindingNames, String headerStyleName)
			throws IOException {
		startTag(writer, TABLE_PRE, "table-row");
		{
			// writer.attribute(TABLE_NS, "style-name", "ro1"); // Optional: Assume default
			// row style

			for (String name : bindingNames) {
				setAttribute(writer, OFFICE_PRE, "value-type", "string");
				setAttribute(writer, TABLE_PRE, "style-name", headerStyleName); // Apply header style
				startTag(writer, TABLE_PRE, "table-cell");
				{
					startTag(writer, TEXT_PRE, "p");
					writer.text(name); // XMLWriter handles escaping
					endTag(writer, TEXT_PRE, "p");
				}
				endTag(writer, TABLE_PRE, "table-cell");
			}
		}
		endTag(writer, TABLE_PRE, "table-row"); // table:table-row
	}

	private void setAttribute(XMLWriter writer, String prefix, String element, String value) {
		writer.setAttribute(prefix + ":" + element, value);

	}

	// Main cell writing logic using XMLWriter
	private void writeCell(XMLWriter writer, Value value) throws IOException {
		if (value.isLiteral()) {
			handleLiteralCell(writer, (Literal) value);
		} else if (value.isIRI()) {
			handleIriCell(writer, (IRI) value, STYLE_IRI); // Default IRI style
		} else if (value.isBNode()) {
			writeStringCell(writer, value.stringValue(), STYLE_DEFAULT);
		} else if (value.isTriple()) {
			writeStringCell(writer, value.stringValue(), STYLE_DEFAULT); // Or a dedicated style?
		} else {
			writeStringCell(writer, value.stringValue(), STYLE_DEFAULT);
		}
	}

	// --- Cell Type Handling using XMLWriter ---

	private void handleLiteralCell(XMLWriter writer, Literal l) throws IOException {
		CoreDatatype cd = l.getCoreDatatype();
		Optional<String> lang = l.getLanguage();

		if (cd != null && cd.isXSDDatatype()) {
			handleXsdLiteral(writer, l, cd.asXSDDatatypeOrNull());
		} else if (lang.isPresent()) {
			writeStringCell(writer, l.getLabel(), STYLE_DEFAULT); // Add lang info in comment? maybe later
		} else if (cd != null && (cd.isRDFDatatype() || cd.isGEODatatype())) {
			writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
		} else {
			writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
		}
	}

	private void handleXsdLiteral(XMLWriter writer, Literal l, XSD xsdType) throws IOException {
		if (xsdType == null) {
			writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
			return;
		}

		try {
			switch (xsdType) {
			case BOOLEAN:
				writeBooleanCell(writer, l.booleanValue(), STYLE_BOOLEAN);
				break;

			case DECIMAL:
			case INTEGER:
			case NEGATIVE_INTEGER:
			case NON_NEGATIVE_INTEGER:
			case POSITIVE_INTEGER:
			case NON_POSITIVE_INTEGER:
			case LONG:
			case INT:
			case SHORT:
			case BYTE:
			case UNSIGNED_LONG:
			case UNSIGNED_INT:
			case UNSIGNED_SHORT:
			case UNSIGNED_BYTE:
				try {
					BigDecimal decVal = l.decimalValue();
					// Pass original label for display, double value for storage
					writeNumericCell(writer, decVal.doubleValue(), l.getLabel(), STYLE_NUMERIC);
				} catch (NumberFormatException nfe) {
					writeStringCell(writer, l.getLabel(), STYLE_NUMERIC); // Fallback
				}
				break;

			case DOUBLE:
				writeNumericCell(writer, l.doubleValue(), l.getLabel(), STYLE_NUMERIC);
				break;
			case FLOAT:
				writeNumericCell(writer, l.floatValue(), l.getLabel(), STYLE_NUMERIC);
				break;

			case DATETIME:
			case DATETIMESTAMP:
				// Use LocalDateTime (no timezone info in ODF value attribute)
				writeDateTimeCell(writer, l.calendarValue().toGregorianCalendar().toZonedDateTime().toLocalDateTime(),
						STYLE_DATETIME);
				break;
			case DATE:
				writeDateCell(writer, l.calendarValue().toGregorianCalendar().toZonedDateTime().toLocalDate(),
						STYLE_DATE);
				break;

			case TIME:
			case GYEAR:
			case GMONTH:
			case GDAY:
			case GYEARMONTH:
			case GMONTHDAY:
			case DURATION:
			case YEARMONTHDURATION:
			case DAYTIMEDURATION:
				writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
				break;

			case ANYURI:
				try {
					handleIriCell(writer, SimpleValueFactory.getInstance().createIRI(l.getLabel()), STYLE_ANY_IRI);
				} catch (IllegalArgumentException e) {
					writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
				}
				break;

			default:
				writeStringCell(writer, l.getLabel(), STYLE_DEFAULT);
				break;
			}
		} catch (Exception e) {
			System.err.println("Warn: Error converting literal '" + l.stringValue() + "' type " + xsdType
					+ ". Writing as string. Error: " + e.getMessage());
			writeStringCell(writer, l.getLabel(), STYLE_DEFAULT); // Fallback
		}
	}

	private void handleIriCell(XMLWriter writer, IRI iri, String styleName) throws IOException {
		String displayString = formatIri(iri);
		String url = iri.stringValue();

		setAttribute(writer, OFFICE_PRE, "value-type", "string"); // Hyperlinks are fundamentally text cells
		setAttribute(writer, TABLE_PRE, "style-name", styleName);
		startTag(writer, TABLE_PRE, "table-cell");
		{
			startTag(writer, TEXT_PRE, "p");
			{
				// Add hyperlink using text:a
				setAttribute(writer, XLINK_PRE, "type", "simple");
				setAttribute(writer, XLINK_PRE, "href", url); // XMLWriter handles attribute escaping
				startTag(writer, TEXT_PRE, "a");
				writer.text(displayString); // XMLWriter handles text escaping
				endTag(writer, TEXT_PRE, "a");
			}
			endTag(writer, TEXT_PRE, "p");
		}
		endTag(writer, TABLE_PRE, "table-cell");
	}

	private void writeStringCell(XMLWriter writer, String value, String styleName) throws IOException {
		setAttribute(writer, OFFICE_PRE, "value-type", "string");
		setAttribute(writer, TABLE_PRE, "style-name", styleName);
		startTag(writer, TABLE_PRE, "table-cell");

		startTag(writer, TEXT_NS, "p");
		writer.text(value); // XMLWriter handles escaping
		endTag(writer, TEXT_PRE, "p");

		endTag(writer, TABLE_PRE, "table-cell");
	}

	private void writeNumericCell(XMLWriter writer, double value, String displayValue, String styleName)
			throws IOException {
		setAttribute(writer, OFFICE_PRE, "value-type", "float"); // Use float for numbers
		setAttribute(writer, OFFICE_PRE, "value", String.valueOf(value)); // ODF requires string representation of
																			// number
		setAttribute(writer, TABLE_PRE, "style-name", styleName); // Style defines display format
		startTag(writer, TABLE_PRE, "table-cell");
		{
			startTag(writer, TEXT_PRE, "p");
			writer.text(displayValue); // Text content shows original or formatted string
			endTag(writer, TEXT_PRE, "p");
		}
		endTag(writer, TABLE_PRE, "table-cell");
	}

	private void writeBooleanCell(XMLWriter writer, boolean value, String styleName) throws IOException {
		setAttribute(writer, OFFICE_PRE, "value-type", "boolean");
		setAttribute(writer, OFFICE_PRE, "boolean-value", String.valueOf(value)); // "true" or "false"
		setAttribute(writer, TABLE_PRE, "style-name", styleName);
		startTag(writer, TABLE_PRE, "table-cell");
		{

			startTag(writer, TEXT_PRE, "p");
			writer.text(value ? "TRUE" : "FALSE"); // Text content for display
			endTag(writer, TEXT_PRE, "p");
		}
		endTag(writer, TABLE_PRE, "table-cell");
	}

	private void writeDateTimeCell(XMLWriter writer, java.time.LocalDateTime dateTime, String styleName)
			throws IOException {
		// ODF requires ISO 8601 format YYYY-MM-DDTHH:MM:SS
		String isoValue = dateTime.format(ODF_DATETIME_FORMATTER);

		setAttribute(writer, OFFICE_PRE, "value-type", "date"); // Type is "date" for both date and datetime
		setAttribute(writer, OFFICE_PRE, "date-value", isoValue); // Stores full date+time
		setAttribute(writer, TABLE_PRE, "style-name", styleName); // Style controls display
		startTag(writer, TABLE_PRE, "table-cell");

		startTag(writer, TEXT_PRE, "p");
		// Displayed text can be formatted differently by the style,
		// but putting the ISO value here ensures something is shown if style fails.
		writer.text(isoValue);
		endTag(writer, TEXT_PRE, "p");

		endTag(writer, TABLE_PRE, "table-cell");
	}

	private void writeDateCell(XMLWriter writer, java.time.LocalDate date, String styleName) throws IOException {
		// ODF requires ISO 8601 format YYYY-MM-DD
		String isoDateValue = date.format(ODF_DATE_FORMATTER);
		// ODF stores dates internally as datetime at midnight
		String isoDateTimeValue = date.atStartOfDay().format(ODF_DATETIME_FORMATTER);

		startTag(writer, TABLE_PRE, "table-cell");
		setAttribute(writer, OFFICE_PRE, "value-type", "date");
		setAttribute(writer, OFFICE_PRE, "date-value", isoDateTimeValue); // Store as full date+time
		setAttribute(writer, TABLE_PRE, "style-name", styleName); // Style controls display (shows date part)

		startTag(writer, TEXT_PRE, "p");
		writer.text(isoDateValue); // Display the date part
		endTag(writer, TEXT_PRE, "p");

		endTag(writer, TABLE_PRE, "table-cell");
	}

	// --- Formatting Helpers ---

	private String formatIri(IRI iri) {
		String iriStr = iri.stringValue();
		String namespace = iri.getNamespace();
		String localName = iri.getLocalName();

		if (prefixes.containsKey(namespace)) {
			String prefix = prefixes.get(namespace);
			if (!localName.isEmpty() && iriStr.equals(namespace + localName)) { // Check for clean split
				return prefix + ":" + localName;
			}
		}
		// If no prefix or local name is weird, return full IRI (or just local name if
		// sensible)
		// Let's prefer the full IRI for clarity in the spreadsheet unless prefixed
		// if (localName != null && !localName.isEmpty() && iriStr.endsWith(localName))
		// {
		// return localName; // Could use this, but full IRI might be less ambiguous
		// }
		return iriStr; // Fallback to full IRI string
	}

	// Remove escapeXml and escapeXmlAttribute methods as XMLWriter handles
	// escaping.
	// private String escapeXml(String s) { ... }
	// private String escapeXmlAttribute(String s) { ... }

	// --- Unimplemented/Simplified Methods from Interface ---

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		System.err.println("Warning: handleBoolean (SPARQL ASK result) not implemented for ODF writer.");
		startDocument();
		MapBindingSet result = new MapBindingSet();
		startQueryResult(List.of("result"));
		result.setBinding("result", SimpleValueFactory.getInstance().createLiteral(value));
		handleSolution(result);
		endQueryResult();
		endDocument();
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// Could store these in meta.xml meta:user-defined fields if needed
		System.err.println("Warning: handleLinks (document-level links) not implemented for ODF writer.");
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return TupleQueryResultFormat.ODS;
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.ODS;
	}

	// Keep other interface methods (setWriterConfig, getWriterConfig, etc.) as they
	// were

	@Override
	public void setWriterConfig(WriterConfig config) {
		// Configuration options could be added (e.g., date formats, default styles)
	}

	@Override
	public WriterConfig getWriterConfig() {
		return new WriterConfig(); // Return default/empty config
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList(); // No specific settings supported yet
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// Not applicable/supported for direct ODF generation
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		// Handled within startQueryResult for ODF table structure
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		// Handled within startQueryResult/endQueryResult
	}

}
