/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Represents the concept of an RDF data serialization format. RDF formats are identified by a {@link #getName() name}
 * and can have one or more associated MIME types, zero or more associated file extensions and can specify a (default)
 * character encoding. Some formats are able to encode context information while other are not; this is indicated by the
 * value of {@link #supportsContexts}.
 *
 * @author Arjohn Kampman
 */
public class RDFFormat extends FileFormat {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Indicates that calls to {@link RDFHandler#handleNamespace(String, String)} may be serialised when serializing to
	 * this format.
	 */
	public static final boolean SUPPORTS_NAMESPACES = true;

	/**
	 * Indicates that all calls to {@link RDFHandler#handleNamespace(String, String)} will be ignored when serializing
	 * to this format.
	 */
	public static final boolean NO_NAMESPACES = false;

	/**
	 * Indicates that the {@link Statement#getContext()} URI may be serialized for this format.
	 */
	public static final boolean SUPPORTS_CONTEXTS = true;

	/**
	 * Indicates that the {@link Statement#getContext()} URI will NOT be serialized for this format.
	 */
	public static final boolean NO_CONTEXTS = false;

	/**
	 * Indicates that RDF-star triples can be serialized natively for this format.
	 */
	public static final boolean SUPPORTS_RDF_STAR = true;

	/**
	 * Indicates that RDF-star triples will NOT be serialized natively for this format.
	 */
	public static final boolean NO_RDF_STAR = false;

	/**
	 * The <a href="http://www.w3.org/TR/rdf-syntax-grammar/">RDF/XML</a> file format.
	 * <p>
	 * Several file extensions are accepted for RDF/XML documents, including <code>.rdf</code>, <code>.rdfs</code> (for
	 * RDF Schema files), <code>.owl</code> (for OWL ontologies), and <code>.xml</code>. The media type is
	 * <code>application/rdf+xml</code>, but <code>application/xml</code> and <code>text/xml</code> are also accepted.
	 * The character encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/rdf-syntax-grammar/">RDF/XML Syntax Specification (Revised)</a>
	 */
	public static final RDFFormat RDFXML = new RDFFormat("RDF/XML",
			Arrays.asList("application/rdf+xml", "application/xml", "text/xml"), StandardCharsets.UTF_8,
			Arrays.asList("rdf", "rdfs", "owl", "xml"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/RDF_XML"), SUPPORTS_NAMESPACES,
			NO_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/n-triples/">N-Triples</a> file format.
	 * <p>
	 * The file extension <code>.nt</code> is recommend for N-Triples documents. The media type is
	 * <code>application/n-triples</code> and encoding is in UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/n-triples/">N-Triples</a>
	 */
	public static final RDFFormat NTRIPLES = new RDFFormat("N-Triples",
			Arrays.asList("application/n-triples", "text/plain"), StandardCharsets.UTF_8, List.of("nt"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/N-Triples"), NO_NAMESPACES,
			NO_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/turtle/">Turtle</a> file format.
	 * <p>
	 * The file extension <code>.ttl</code> is recommend for Turtle documents. The media type is
	 * <code>text/turtle</code>, but <code>application/x-turtle</code> is also accepted. Character encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/turtle/">Turtle - Terse RDF Triple Language</a>
	 */
	public static final RDFFormat TURTLE = new RDFFormat("Turtle", Arrays.asList("text/turtle", "application/x-turtle"),
			StandardCharsets.UTF_8, List.of("ttl"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/Turtle"), SUPPORTS_NAMESPACES,
			NO_CONTEXTS, NO_RDF_STAR);

	/**
	 * The Turtle-star file format, a Turtle-based RDF serialization format that supports RDF-star triples.
	 * <p>
	 * The file extension <code>.ttls</code> is recommended for Turtle-star documents. The media type is
	 * <code>application/x-turtlestar</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="https://arxiv.org/pdf/1406.3399.pdf">Foundations of an Alternative Approach to Reification in
	 *      RDF</a>
	 * @see <a href="https://w3c.github.io/rdf-star/cg-spec/">RDF-star and SPARQL-star Draft Community Group Report</a>
	 */
	public static final RDFFormat TURTLESTAR = new RDFFormat("Turtle-star",
			Arrays.asList("text/x-turtlestar", "application/x-turtlestar"), StandardCharsets.UTF_8,
			List.of("ttls"), SUPPORTS_NAMESPACES, NO_CONTEXTS, SUPPORTS_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TeamSubmission/n3/">N3/Notation3</a> file format.
	 * <p>
	 * The file extension <code>.n3</code> is recommended for N3 documents. The media type is <code>text/n3</code>, but
	 * <code>text/rdf+n3</code> is also accepted. Character encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TeamSubmission/n3/">Notation3 (N3): A readable RDF syntax</a>
	 */
	public static final RDFFormat N3 = new RDFFormat("N3", Arrays.asList("text/n3", "text/rdf+n3"),
			StandardCharsets.UTF_8, List.of("n3"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/N3"), SUPPORTS_NAMESPACES,
			NO_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://swdev.nokia.com/trix/">TriX</a> file format, an XML-based RDF serialization format that
	 * supports recording of named graphs.
	 * <p>
	 * The file extension <code>.xml</code> is recommended for TriX documents, <code>.trix</code> is also accepted. The
	 * media type is <code>application/trix</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://swdev.nokia.com/trix/">TriX: RDF Triples in XML</a>
	 */
	public static final RDFFormat TRIX = new RDFFormat("TriX", List.of("application/trix"),
			StandardCharsets.UTF_8, Arrays.asList("xml", "trix"), null, SUPPORTS_NAMESPACES, SUPPORTS_CONTEXTS,
			NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/trig/">TriG</a> file format, a Turtle-based RDF serialization format that
	 * supports recording of named graphs.
	 * <p>
	 * The file extension <code>.trig</code> is recommend for TriG documents. The media type is
	 * <code>application/trig</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/trig/">The TriG Syntax</a>
	 */
	public static final RDFFormat TRIG = new RDFFormat("TriG", Arrays.asList("application/trig", "application/x-trig"),
			StandardCharsets.UTF_8, List.of("trig"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/TriG"), SUPPORTS_NAMESPACES,
			SUPPORTS_CONTEXTS, NO_RDF_STAR);

	/**
	 * The TriG-star file format, a TriG-based RDF serialization format that supports RDF-star triples. This builds upon
	 * the idea for the Turtle-star format but adds support for named graphs.
	 * <p>
	 * The file extension <code>.trigs</code> is recommended for TriG-star documents. The media type is
	 * <code>application/x-trigstar</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="https://arxiv.org/pdf/1406.3399.pdf">Foundations of an Alternative Approach to Reification in
	 *      RDF</a>
	 * @see <a href="https://w3c.github.io/rdf-star/cg-spec/">RDF-star and SPARQL-star Draft Community Group Report</a>
	 */
	public static final RDFFormat TRIGSTAR = new RDFFormat("TriG-star", "application/x-trigstar",
			StandardCharsets.UTF_8, "trigs", SUPPORTS_NAMESPACES, SUPPORTS_CONTEXTS, SUPPORTS_RDF_STAR);

	/**
	 * A binary RDF format.
	 * <p>
	 * The file extension <code>.brf</code> is recommend for binary RDF documents. The media type is
	 * <code>application/x-binary-rdf</code>.
	 * </p>
	 *
	 * @see <a href="http://rivuli-development.com/2011/11/binary-rdf-in-sesame/">Binary RDF in Sesame</a>
	 */
	public static final RDFFormat BINARY = new RDFFormat("BinaryRDF", List.of("application/x-binary-rdf"), null,
			List.of("brf"), null, SUPPORTS_NAMESPACES, SUPPORTS_CONTEXTS, SUPPORTS_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/n-quads/">N-Quads</a> file format, an RDF serialization format that supports
	 * recording of named graphs.
	 * <p>
	 * The file extension <code>.nq</code> is recommended for N-Quads documents. The media type is
	 * <code>application/n-quads</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/n-quads/">N-Quads: Extending N-Triples with Context</a>
	 */
	public static final RDFFormat NQUADS = new RDFFormat("N-Quads",
			Arrays.asList("application/n-quads", "text/x-nquads", "text/nquads"), StandardCharsets.UTF_8,
			List.of("nq"), SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/N-Quads"),
			NO_NAMESPACES, SUPPORTS_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/json-ld/">JSON-LD</a> file format, an RDF serialization format that supports
	 * recording of named graphs.
	 * <p>
	 * The file extension <code>.jsonld</code> is recommended for JSON-LD documents. The media type is
	 * <code>application/ld+json</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/json-ld/">JSON-LD 1.0</a>
	 */
	public static final RDFFormat JSONLD = new RDFFormat("JSON-LD", List.of("application/ld+json"),
			StandardCharsets.UTF_8, List.of("jsonld"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/JSON-LD"), SUPPORTS_NAMESPACES,
			SUPPORTS_CONTEXTS, NO_RDF_STAR);

	/**
	 * The NDJSON-LD is a Newline Delimited JSON-LD format.
	 * <p>
	 * The file extension <code>.ndjsonld</code> is recommended for NDJSON-LD documents. The media type is
	 * <code>application/x-ld+ndjson</code> and the encoding is UTF-8.
	 * </p>
	 */
	public static final RDFFormat NDJSONLD = new RDFFormat("NDJSON-LD", List.of("application/x-ld+ndjson"),
			StandardCharsets.UTF_8, Arrays.asList("ndjsonld", "jsonl", "ndjson"), null, SUPPORTS_NAMESPACES,
			SUPPORTS_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/rdf-json/" >RDF/JSON</a> file format, an RDF serialization format that supports
	 * recording of named graphs.
	 * <p>
	 * The file extension <code>.rj</code> is recommended for RDF/JSON documents. The media type is
	 * <code>application/rdf+json</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/rdf-json/">RDF 1.1 JSON Alternate Serialization (RDF/JSON)</a>
	 */
	public static final RDFFormat RDFJSON = new RDFFormat("RDF/JSON", List.of("application/rdf+json"),
			StandardCharsets.UTF_8, List.of("rj"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/RDF_JSON"), NO_NAMESPACES,
			SUPPORTS_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.w3.org/TR/rdfa-syntax/">RDFa</a> file format, an RDF serialization format.
	 * <p>
	 * The file extension <code>.xhtml</code> is recommended for RDFa documents. The preferred media type is
	 * <code>application/xhtml+xml</code> and the encoding is UTF-8.
	 * </p>
	 *
	 * @see <a href="http://www.w3.org/TR/rdfa-syntax/">XHTML+RDFa 1.1</a>
	 */
	public static final RDFFormat RDFA = new RDFFormat("RDFa",
			Arrays.asList("application/xhtml+xml", "application/html", "text/html"), StandardCharsets.UTF_8,
			Arrays.asList("xhtml", "html"),
			SimpleValueFactory.getInstance().createIRI("http://www.w3.org/ns/formats/RDFa"), SUPPORTS_NAMESPACES,
			NO_CONTEXTS, NO_RDF_STAR);

	/**
	 * The <a href="http://www.rdfhdt.org/hdt-binary-format/">HDT</a> file format, an RDF serialization format.
	 * <p>
	 * The file extension <code>.hdt</code> is recommended for HDT documents.
	 * </p>
	 *
	 * @see <a href="http://www.rdfhdt.org/hdt-binary-format/">HDT v1.0</a>
	 */
	public static final RDFFormat HDT = new RDFFormat("HDT",
			List.of("application/vnd.hdt"), null, List.of("hdt"), null,
			SUPPORTS_NAMESPACES, NO_CONTEXTS, NO_RDF_STAR);

	/*----------------*
	 * Static methods *
	 *----------------*/

	/**
	 * Processes the supplied collection of {@link RDFFormat}s and assigns quality values to each based on whether
	 * context must be supported and whether the format is preferred.
	 *
	 * @param rdfFormats      The {@link RDFFormat}s to process.
	 * @param requireContext  True to decrease the quality value for formats where {@link RDFFormat#supportsContexts()}
	 *                        returns false.
	 * @param preferredFormat The preferred RDFFormat. If it is not in the list then the quality of all formats will be
	 *                        processed as if they are not preferred.
	 * @return A list of strings containing the content types and an attached q-value specifying the quality for the
	 *         format for each type.
	 */
	public static List<String> getAcceptParams(Iterable<RDFFormat> rdfFormats, boolean requireContext,
			RDFFormat preferredFormat) {
		List<String> acceptParams = new ArrayList<>();

		for (RDFFormat format : rdfFormats) {
			// Determine a q-value that reflects the necessity of context
			// support and the user specified preference
			int qValue = 10;

			if (requireContext && !format.supportsContexts()) {
				// Prefer context-supporting formats over pure triple-formats
				qValue -= 5;
			}

			if (preferredFormat != null && !preferredFormat.equals(format)) {
				// Prefer specified format over other formats
				qValue -= 2;
			}

			if (!format.supportsNamespaces()) {
				// We like reusing namespace prefixes
				qValue -= 1;
			}

			if (RDFXML.equals(format)) {
				// We explicitly dislike RDF/XML as it has limitations in what it can serialize. See #299.
				qValue -= 4;
			}

			// ensure q-value does not go below 0.1.
			qValue = Math.max(1, qValue);

			for (String mimeType : format.getMIMETypes()) {
				String acceptParam = mimeType;

				if (qValue < 10) {
					acceptParam += ";q=0." + qValue;
				}

				acceptParams.add(acceptParam);
			}
		}

		return acceptParams;
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Flag indicating whether the RDFFormat can encode namespace information.
	 */
	private final boolean supportsNamespaces;

	/**
	 * Flag indicating whether the RDFFormat can encode context information.
	 */
	private final boolean supportsContexts;

	/**
	 * Flag indicating whether the RDFFormat can encode RDF-star triples natively.
	 */
	private final boolean supportsRDFStar;

	/**
	 * A standard URI published by the W3C or another standards body to uniquely denote this format.
	 *
	 * @see <a href="http://www.w3.org/ns/formats/">Unique URIs for File Formats</a>
	 */
	private final IRI standardURI;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeType           The MIME type of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtension      The (default) file extension for the RDF file format, e.g. <var>rdf</var> for RDF/XML
	 *                           files.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @deprecated since 3.2.0
	 */
	@Deprecated
	public RDFFormat(String name, String mimeType, Charset charset, String fileExtension, boolean supportsNamespaces,
			boolean supportsContexts) {
		this(name, mimeType, charset, fileExtension, supportsNamespaces, supportsContexts, NO_RDF_STAR);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeType           The MIME type of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtension      The (default) file extension for the RDF file format, e.g. <var>rdf</var> for RDF/XML
	 *                           files.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @param supportsRDFStar    <var>True</var> if the RDFFormat supports the encoding of RDF-star triples natively and
	 *                           <var>false</var> otherwise.
	 */
	public RDFFormat(String name, String mimeType, Charset charset, String fileExtension, boolean supportsNamespaces,
			boolean supportsContexts, boolean supportsRDFStar) {
		this(name, List.of(mimeType), charset, List.of(fileExtension), supportsNamespaces,
				supportsContexts, supportsRDFStar);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeType           The MIME type of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @deprecated since 3.2.0
	 */
	@Deprecated
	public RDFFormat(String name, String mimeType, Charset charset, Collection<String> fileExtensions,
			boolean supportsNamespaces, boolean supportsContexts) {
		this(name, mimeType, charset, fileExtensions, supportsNamespaces, supportsContexts, NO_RDF_STAR);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeType           The MIME type of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @param supportsRDFStar    <var>True</var> if the RDFFormat supports the encoding of RDF-star triples natively and
	 *                           <var>false</var> otherwise.
	 */
	public RDFFormat(String name, String mimeType, Charset charset, Collection<String> fileExtensions,
			boolean supportsNamespaces, boolean supportsContexts, boolean supportsRDFStar) {
		this(name, List.of(mimeType), charset, fileExtensions, supportsNamespaces, supportsContexts,
				supportsRDFStar);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeTypes          The MIME types of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format. The first item in the list is interpreted as the default MIME type
	 *                           for the format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @deprecated since 3.2.0
	 */
	@Deprecated
	public RDFFormat(String name, Collection<String> mimeTypes, Charset charset, Collection<String> fileExtensions,
			boolean supportsNamespaces, boolean supportsContexts) {
		this(name, mimeTypes, charset, fileExtensions, null, supportsNamespaces, supportsContexts, NO_RDF_STAR);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeTypes          The MIME types of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format. The first item in the list is interpreted as the default MIME type
	 *                           for the format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @param supportsRDFStar    <var>True</var> if the RDFFormat supports the encoding of RDF-star triples natively and
	 *                           <var>false</var> otherwise.
	 */
	public RDFFormat(String name, Collection<String> mimeTypes, Charset charset, Collection<String> fileExtensions,
			boolean supportsNamespaces, boolean supportsContexts, boolean supportsRDFStar) {
		this(name, mimeTypes, charset, fileExtensions, null, supportsNamespaces, supportsContexts,
				supportsRDFStar);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeTypes          The MIME types of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format. The first item in the list is interpreted as the default MIME type
	 *                           for the format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param standardURI        The standard URI that has been assigned to this format by a standards organisation or
	 *                           null if it does not currently have a standard URI.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @deprecated since 3.2.0
	 */
	@Deprecated
	public RDFFormat(String name, Collection<String> mimeTypes, Charset charset, Collection<String> fileExtensions,
			IRI standardURI, boolean supportsNamespaces, boolean supportsContexts) {
		this(name, mimeTypes, charset, fileExtensions, standardURI, supportsNamespaces, supportsContexts, NO_RDF_STAR);
	}

	/**
	 * Creates a new RDFFormat object.
	 *
	 * @param name               The name of the RDF file format, e.g. "RDF/XML".
	 * @param mimeTypes          The MIME types of the RDF file format, e.g. <var>application/rdf+xml</var> for the
	 *                           RDF/XML file format. The first item in the list is interpreted as the default MIME type
	 *                           for the format.
	 * @param charset            The default character encoding of the RDF file format. Specify <var>null</var> if not
	 *                           applicable.
	 * @param fileExtensions     The RDF format's file extensions, e.g. <var>rdf</var> for RDF/XML files. The first item
	 *                           in the list is interpreted as the default file extension for the format.
	 * @param standardURI        The standard URI that has been assigned to this format by a standards organisation or
	 *                           null if it does not currently have a standard URI.
	 * @param supportsNamespaces <var>True</var> if the RDFFormat supports the encoding of namespace/prefix information
	 *                           and <var>false</var> otherwise.
	 * @param supportsContexts   <var>True</var> if the RDFFormat supports the encoding of contexts/named graphs and
	 *                           <var>false</var> otherwise.
	 * @param supportsRDFStar    <var>True</var> if the RDFFormat supports the encoding of RDF-star triples natively and
	 *                           <var>false</var> otherwise.
	 */
	public RDFFormat(String name, Collection<String> mimeTypes, Charset charset, Collection<String> fileExtensions,
			IRI standardURI, boolean supportsNamespaces, boolean supportsContexts, boolean supportsRDFStar) {
		super(name, mimeTypes, charset, fileExtensions);

		this.standardURI = standardURI;
		this.supportsNamespaces = supportsNamespaces;
		this.supportsContexts = supportsContexts;
		this.supportsRDFStar = supportsRDFStar;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Return <var>true</var> if the RDFFormat supports the encoding of namespace/prefix information.
	 */
	public boolean supportsNamespaces() {
		return supportsNamespaces;
	}

	/**
	 * Return <var>true</var> if the RDFFormat supports the encoding of contexts/named graphs.
	 */
	public boolean supportsContexts() {
		return supportsContexts;
	}

	/**
	 * Return <var>true</var> if the RDFFormat supports the encoding of RDF-star triples natively.
	 */
	public boolean supportsRDFStar() {
		return supportsRDFStar;
	}

	/**
	 * @return True if a standard URI has been assigned to this format by a standards organisation.
	 */
	public boolean hasStandardURI() {
		return standardURI != null;
	}

	/**
	 * @return The standard URI that has been assigned to this format by a standards organisation or null if it does not
	 *         currently have a standard URI.
	 */
	public IRI getStandardURI() {
		return standardURI;
	}
}
