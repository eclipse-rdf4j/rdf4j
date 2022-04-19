/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;

/**
 * Static methods for parsing and writing RDF for all available syntaxes.
 * <p>
 * It includes methods for searching for {@link RDFFormat}s based on MIME types and file extensions, creating
 * {@link RDFParser}s and {@link RDFWriter}s, and directly parsing and writing.
 *
 * @author Arjohn Kampman
 * @author Peter Ansell
 */
public class Rio {

	/**
	 * Tries to match a MIME type against the list of RDF formats that can be parsed.
	 *
	 * @param mimeType A MIME type, e.g. "application/rdf+xml".
	 * @return An RDFFormat object if a match was found, or {@link Optional#empty()} otherwise.
	 */
	public static Optional<RDFFormat> getParserFormatForMIMEType(String mimeType) {
		return RDFFormat.matchMIMEType(mimeType, RDFParserRegistry.getInstance().getKeys());
	}

	/**
	 * Tries to match the extension of a file name against the list of RDF formats that can be parsed.
	 *
	 * @param fileName A file name.
	 * @return An RDFFormat object if a match was found, or {@link Optional#empty()} otherwise.
	 */
	public static Optional<RDFFormat> getParserFormatForFileName(String fileName) {
		return RDFFormat.matchFileName(fileName, RDFParserRegistry.getInstance().getKeys());
	}

	/**
	 * Tries to match a MIME type against the list of RDF formats that can be written.
	 *
	 * @param mimeType A MIME type, e.g. "application/rdf+xml".
	 * @return An RDFFormat object if a match was found, or {@link Optional#empty()} otherwise.
	 */
	public static Optional<RDFFormat> getWriterFormatForMIMEType(String mimeType) {
		return RDFFormat.matchMIMEType(mimeType, RDFWriterRegistry.getInstance().getKeys());
	}

	/**
	 * Tries to match the extension of a file name against the list of RDF formats that can be written.
	 *
	 * @param fileName A file name.
	 * @return An RDFFormat object if a match was found, or {@link Optional#empty()} otherwise.
	 */
	public static Optional<RDFFormat> getWriterFormatForFileName(String fileName) {
		return RDFFormat.matchFileName(fileName, RDFWriterRegistry.getInstance().getKeys());
	}

	/**
	 * Convenience methods for creating RDFParser objects.This method uses the registry returned by
	 * {@link RDFParserRegistry#getInstance()} to get a factory for the specified format and uses this factory to create
	 * the appropriate parser.
	 *
	 * @param format
	 * @return RDF Parser
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 */
	public static RDFParser createParser(RDFFormat format) throws UnsupportedRDFormatException {
		RDFParserFactory factory = RDFParserRegistry.getInstance()
				.get(format)
				.orElseThrow(Rio.unsupportedFormat(format));

		return factory.getParser();
	}

	/**
	 * Convenience methods for creating RDFParser objects that use the specified ValueFactory to create RDF model
	 * objects.
	 *
	 * @param format
	 * @param valueFactory
	 * @return RDF Parser
	 * @throws UnsupportedRDFormatException If no parser is available for the specified RDF format.
	 * @see #createParser(RDFFormat)
	 * @see RDFParser#setValueFactory(ValueFactory)
	 */
	public static RDFParser createParser(RDFFormat format, ValueFactory valueFactory)
			throws UnsupportedRDFormatException {
		RDFParser rdfParser = createParser(format);
		rdfParser.setValueFactory(valueFactory);
		return rdfParser;
	}

	/**
	 * Convenience methods for creating RDFWriter objects.This method uses the registry returned by
	 * {@link RDFWriterRegistry#getInstance()} to get a factory for the specified format and uses this factory to create
	 * the appropriate writer.
	 *
	 * @param format
	 * @param out
	 * @return RDF Writer
	 * @throws UnsupportedRDFormatException If no writer is available for the specified RDF format.
	 */
	public static RDFWriter createWriter(RDFFormat format, OutputStream out) throws UnsupportedRDFormatException {
		RDFWriterFactory factory = RDFWriterRegistry.getInstance()
				.get(format)
				.orElseThrow(Rio.unsupportedFormat(format));

		return factory.getWriter(out);
	}

	/**
	 * Convenience methods for creating RDFWriter objects.This method uses the registry returned by
	 * {@link RDFWriterRegistry#getInstance()} to get a factory for the specified format and uses this factory to create
	 * the appropriate writer.
	 *
	 * @param format
	 * @param out
	 * @param baseURI
	 * @return RDF Writer
	 * @throws UnsupportedRDFormatException If no writer is available for the specified RDF format.
	 * @throws URISyntaxException           If the baseURI is invalid
	 */
	public static RDFWriter createWriter(RDFFormat format, OutputStream out, String baseURI)
			throws UnsupportedRDFormatException, URISyntaxException {
		RDFWriterFactory factory = RDFWriterRegistry.getInstance()
				.get(format)
				.orElseThrow(Rio.unsupportedFormat(format));

		return factory.getWriter(out, baseURI);
	}

	/**
	 * Convenience methods for creating RDFWriter objects.This method uses the registry returned by
	 * {@link RDFWriterRegistry#getInstance()} to get a factory for the specified format and uses this factory to create
	 * the appropriate writer.
	 *
	 * @param format
	 * @param writer
	 * @return RDF Writer
	 * @throws UnsupportedRDFormatException If no writer is available for the specified RDF format.
	 */
	public static RDFWriter createWriter(RDFFormat format, Writer writer) throws UnsupportedRDFormatException {
		RDFWriterFactory factory = RDFWriterRegistry.getInstance()
				.get(format)
				.orElseThrow(Rio.unsupportedFormat(format));

		return factory.getWriter(writer);
	}

	/**
	 * Convenience methods for creating RDFWriter objects.This method uses the registry returned by
	 * {@link RDFWriterRegistry#getInstance()} to get a factory for the specified format and uses this factory to create
	 * the appropriate writer.
	 *
	 * @param format
	 * @param writer
	 * @param baseURI
	 * @return RDF Writer
	 * @throws UnsupportedRDFormatException If no writer is available for the specified RDF format.
	 * @throws URISyntaxException           If the baseURI is invalid
	 */
	public static RDFWriter createWriter(RDFFormat format, Writer writer, String baseURI)
			throws UnsupportedRDFormatException, URISyntaxException {
		RDFWriterFactory factory = RDFWriterRegistry.getInstance()
				.get(format)
				.orElseThrow(Rio.unsupportedFormat(format));

		return factory.getWriter(writer, baseURI);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 *
	 * @since 3.5.0
	 */
	public static Model parse(InputStream in, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(in, null, dataFormat, new ParserConfig(), SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param settings   The {@link ParserConfig} containing settings for configuring the parser.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 *
	 * @since 4.0.0
	 */
	public static Model parse(InputStream in, RDFFormat dataFormat, ParserConfig settings, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(in, null, dataFormat, settings, SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. May be
	 *                   <code>null</code>.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(InputStream in, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(in, baseURI, dataFormat, new ParserConfig(), SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in         An InputStream from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. May be
	 *                   <code>null</code>.
	 * @param dataFormat The serialization format of the data.
	 * @param settings   The {@link ParserConfig} containing settings for configuring the parser.
	 * @param contexts   The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                   contextual information in the actual data. If no contexts are supplied the contextual
	 *                   information in the input stream is used, if no context information is available the data is
	 *                   added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 *
	 * @since 4.0.0
	 */
	public static Model parse(InputStream in, String baseURI, RDFFormat dataFormat, ParserConfig settings,
			Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(in, baseURI, dataFormat, settings, SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in           An InputStream from which RDF data can be read.
	 * @param baseURI      The base URI to resolve any relative URIs that are in the data against. May be
	 *                     <code>null</code>.
	 * @param dataFormat   The serialization format of the data.
	 * @param settings     The {@link ParserConfig} containing settings for configuring the parser.
	 * @param valueFactory The {@link ValueFactory} used by the parser to create statements.
	 * @param errors       The {@link ParseErrorListener} used by the parser to signal errors, including errors that do
	 *                     not generate an {@link RDFParseException}.
	 * @param contexts     The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                     contextual information in the actual data. If no contexts are supplied the contextual
	 *                     information in the input stream is used, if no context information is available the data is
	 *                     added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(InputStream in, String baseURI, RDFFormat dataFormat, ParserConfig settings,
			ValueFactory valueFactory, ParseErrorListener errors, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {

		return parse(in, baseURI, dataFormat, settings, valueFactory, errors, new DynamicModelFactory(), contexts);
	}

	/**
	 * Adds RDF data from an {@link InputStream} to a {@link Model}, optionally to one or more named contexts.
	 *
	 * @param in           An InputStream from which RDF data can be read.
	 * @param baseURI      The base URI to resolve any relative URIs that are in the data against. May be
	 *                     <code>null</code>.
	 * @param dataFormat   The serialization format of the data.
	 * @param settings     The {@link ParserConfig} containing settings for configuring the parser.
	 * @param valueFactory The {@link ValueFactory} used by the parser to create statements.
	 * @param errors       The {@link ParseErrorListener} used by the parser to signal errors, including errors that do
	 *                     not generate an {@link RDFParseException}.
	 * @param modelFactory the ModelFactory used to instantiate the model that gets returned.
	 * @param contexts     The contexts to add the data to. If one or more contexts are supplied the method ignores
	 *                     contextual information in the actual data. If no contexts are supplied the contextual
	 *                     information in the input stream is used, if no context information is available the data is
	 *                     added without any context.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the input stream.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(InputStream in, String baseURI, RDFFormat dataFormat, ParserConfig settings,
			ValueFactory valueFactory, ParseErrorListener errors, ModelFactory modelFactory, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		Model result = modelFactory.createEmptyModel();
		RDFParser parser = createParser(dataFormat, valueFactory);
		parser.setParserConfig(settings);
		parser.setParseErrorListener(errors);
		parser.setRDFHandler(new ContextStatementCollector(result, valueFactory, contexts));
		// DynamicModel and ContextStatementCollector should not throw
		// RDFHandlerException exceptions
		parser.parse(in, baseURI);
		return result;
	}

	/**
	 * Adds RDF data from a {@link Reader} to a {@link Model}, optionally to one or more named contexts. <b>Note: using
	 * a Reader to upload byte-based data means that you have to be careful not to destroy the data's character encoding
	 * by enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is
	 * to be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 *
	 * @since 3.5.0
	 */
	public static Model parse(Reader reader, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(reader, null, dataFormat, new ParserConfig(), SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from a {@link Reader} to a {@link Model}, optionally to one or more named contexts. <b>Note: using
	 * a Reader to upload byte-based data means that you have to be careful not to destroy the data's character encoding
	 * by enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is
	 * to be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param dataFormat The serialization format of the data.
	 * @param settings   The {@link ParserConfig} containing settings for configuring the parser.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 *
	 * @since 4.0.0
	 */
	public static Model parse(Reader reader, RDFFormat dataFormat, ParserConfig settings, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(reader, null, dataFormat, settings, SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from a {@link Reader} to a {@link Model}, optionally to one or more named contexts. <b>Note: using
	 * a Reader to upload byte-based data means that you have to be careful not to destroy the data's character encoding
	 * by enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is
	 * to be preferred.</b>
	 *
	 * @param reader     A Reader from which RDF data can be read.
	 * @param baseURI    The base URI to resolve any relative URIs that are in the data against. May be
	 *                   <code>null</code>.
	 * @param dataFormat The serialization format of the data.
	 * @param contexts   The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                   these contexts, ignoring any context information in the data itself.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(Reader reader, String baseURI, RDFFormat dataFormat, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		return parse(reader, baseURI, dataFormat, new ParserConfig(), SimpleValueFactory.getInstance(),
				new ParseErrorLogger(), contexts);
	}

	/**
	 * Adds RDF data from a {@link Reader} to a {@link Model}, optionally to one or more named contexts. <b>Note: using
	 * a Reader to upload byte-based data means that you have to be careful not to destroy the data's character encoding
	 * by enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is
	 * to be preferred.</b>
	 *
	 * @param reader       A Reader from which RDF data can be read.
	 * @param baseURI      The base URI to resolve any relative URIs that are in the data against. May be
	 *                     <code>null</code>.
	 * @param dataFormat   The serialization format of the data.
	 * @param settings     The {@link ParserConfig} containing settings for configuring the parser.
	 * @param valueFactory The {@link ValueFactory} used by the parser to create statements.
	 * @param errors       The {@link ParseErrorListener} used by the parser to signal errors, including errors that do
	 *                     not generate an {@link RDFParseException}.
	 * @param contexts     The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                     these contexts, ignoring any context information in the data itself.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(Reader reader, String baseURI, RDFFormat dataFormat, ParserConfig settings,
			ValueFactory valueFactory, ParseErrorListener errors, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {

		return parse(reader, baseURI, dataFormat, settings, valueFactory, errors, new DynamicModelFactory(), contexts);
	}

	/**
	 * Adds RDF data from a {@link Reader} to a {@link Model}, optionally to one or more named contexts. <b>Note: using
	 * a Reader to upload byte-based data means that you have to be careful not to destroy the data's character encoding
	 * by enforcing a default character encoding upon the bytes. If possible, adding such data using an InputStream is
	 * to be preferred.</b>
	 *
	 * @param reader       A Reader from which RDF data can be read.
	 * @param baseURI      The base URI to resolve any relative URIs that are in the data against. May be
	 *                     <code>null</code>.
	 * @param dataFormat   The serialization format of the data.
	 * @param settings     The {@link ParserConfig} containing settings for configuring the parser.
	 * @param valueFactory The {@link ValueFactory} used by the parser to create statements.
	 * @param errors       The {@link ParseErrorListener} used by the parser to signal errors, including errors that do
	 *                     not generate an {@link RDFParseException}.
	 * @param modelFactory the ModelFactory used to instantiate the model that gets returned.
	 * @param contexts     The contexts to add the data to. If one or more contexts are specified the data is added to
	 *                     these contexts, ignoring any context information in the data itself.
	 * @return A {@link Model} containing the parsed statements.
	 * @throws IOException                  If an I/O error occurred while reading from the reader.
	 * @throws UnsupportedRDFormatException If no {@link RDFParser} is available for the specified RDF format.
	 * @throws RDFParseException            If an error was found while parsing the RDF data.
	 */
	public static Model parse(Reader reader, String baseURI, RDFFormat dataFormat, ParserConfig settings,
			ValueFactory valueFactory, ParseErrorListener errors, ModelFactory modelFactory, Resource... contexts)
			throws IOException, RDFParseException, UnsupportedRDFormatException {
		Model result = modelFactory.createEmptyModel();
		RDFParser parser = createParser(dataFormat, valueFactory);
		parser.setParserConfig(settings);
		parser.setParseErrorListener(errors);
		parser.setRDFHandler(new ContextStatementCollector(result, valueFactory, contexts));
		// Model and ContextStatementCollector should not throw
		// RDFHandlerException exceptions
		parser.parse(reader, baseURI);
		return result;
	}

	/**
	 * Writes the given statements to the given {@link OutputStream} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link OutputStream} to write the statements to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, OutputStream output, RDFFormat dataFormat)
			throws RDFHandlerException {
		write(model, output, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given statements to the given {@link OutputStream} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link OutputStream} to write the statements to.
	 * @param baseURI    The base URI to relativize IRIs against.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws URISyntaxException           If the baseURI is invalid
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, OutputStream output, String baseURI, RDFFormat dataFormat)
			throws RDFHandlerException, UnsupportedRDFormatException, URISyntaxException {
		write(model, output, baseURI, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given statements to the given {@link Writer} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link Writer} to write the statements to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, Writer output, RDFFormat dataFormat)
			throws RDFHandlerException {
		write(model, output, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given statements to the given {@link Writer} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link Writer} to write the statements to.
	 * @param baseURI    The base URI to relativize IRIs against.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws URISyntaxException           If the baseURI is invalid
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, Writer output, String baseURI, RDFFormat dataFormat)
			throws RDFHandlerException, UnsupportedRDFormatException, URISyntaxException {
		write(model, output, baseURI, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given statements to the given {@link OutputStream} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link OutputStream} to write the statements to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @param settings   The {@link WriterConfig} containing settings for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, OutputStream output, RDFFormat dataFormat,
			WriterConfig settings) throws RDFHandlerException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output);
		writer.setWriterConfig(settings);
		write(model, writer);
	}

	/**
	 * Writes the given statements to the given {@link OutputStream} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link OutputStream} to write the statements to.
	 * @param baseURI    The base URI to relativize IRIs against.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @param settings   The {@link WriterConfig} containing settings for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws URISyntaxException           If the baseURI is invalid
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, OutputStream output, String baseURI, RDFFormat dataFormat,
			WriterConfig settings) throws RDFHandlerException, UnsupportedRDFormatException, URISyntaxException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output, baseURI);
		writer.setWriterConfig(settings);
		write(model, writer);
	}

	/**
	 * Writes the given statements to the given {@link Writer} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link Writer} to write the statements to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @param settings   The {@link WriterConfig} containing settings for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, Writer output, RDFFormat dataFormat, WriterConfig settings)
			throws RDFHandlerException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output);
		writer.setWriterConfig(settings);
		write(model, writer);
	}

	/**
	 * Writes the given statements to the given {@link Writer} in the given format.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model      A collection of statements, such as a {@link Model}, to be written.
	 * @param output     The {@link Writer} to write the statements to.
	 * @param baseURI    The base URI to relativize IRIs against.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statements.
	 * @param settings   The {@link WriterConfig} containing settings for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statements.
	 * @throws URISyntaxException           If the baseURI is invalid
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Iterable<Statement> model, Writer output, String baseURI, RDFFormat dataFormat,
			WriterConfig settings) throws RDFHandlerException, UnsupportedRDFormatException, URISyntaxException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output, baseURI);
		writer.setWriterConfig(settings);
		write(model, writer);
	}

	/**
	 * Writes the given statements to the given {@link RDFHandler}.
	 * <p>
	 * If the collection is a {@link Model}, its namespaces will also be written.
	 *
	 * @param model  A collection of statements, such as a {@link Model}, to be written.
	 * @param writer
	 * @throws RDFHandlerException Thrown if there is an error writing the statements.
	 */
	public static void write(Iterable<Statement> model, RDFHandler writer) throws RDFHandlerException {
		writer.startRDF();

		if (model instanceof NamespaceAware) {
			for (Namespace nextNamespace : ((NamespaceAware) model).getNamespaces()) {
				writer.handleNamespace(nextNamespace.getPrefix(), nextNamespace.getName());
			}
		}

		for (final Statement st : model) {
			writer.handleStatement(st);
		}
		writer.endRDF();
	}

	/**
	 * Writes the given statement to the given {@link OutputStream} in the given format.
	 * <p>
	 *
	 * @param st         The statement to be written.
	 * @param output     The {@link OutputStream} to write the statement to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statement.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statement.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Statement st, OutputStream output, RDFFormat dataFormat) throws RDFHandlerException {
		write(st, output, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given single statement to the given {@link OutputStream} in the given format.
	 *
	 * @param st         The statement to be written.
	 * @param output     The {@link OutputStream} to write the statement to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statement.
	 * @param settings   The {@link WriterConfig} containing setting for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statement.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Statement st, OutputStream output, RDFFormat dataFormat, WriterConfig settings)
			throws RDFHandlerException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output);
		writer.setWriterConfig(settings);
		write(st, writer);
	}

	/**
	 * Writes the given single statement to the given {@link Writer} in the given format.
	 * <p>
	 *
	 * @param statement  A statement to be written.
	 * @param output     The {@link Writer} to write the statement to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statement.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statement.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Statement statement, Writer output, RDFFormat dataFormat) throws RDFHandlerException {
		write(statement, output, dataFormat, new WriterConfig());
	}

	/**
	 * Writes the given single statement to the given {@link Writer} in the given format.
	 * <p>
	 *
	 * @param statement  A statement to be written.
	 * @param output     The {@link Writer} to write the statement to.
	 * @param dataFormat The {@link RDFFormat} to use when writing the statement.
	 * @param settings   The {@link WriterConfig} containing settings for configuring the writer.
	 * @throws RDFHandlerException          Thrown if there is an error writing the statement.
	 * @throws UnsupportedRDFormatException If no {@link RDFWriter} is available for the specified RDF format.
	 */
	public static void write(Statement statement, Writer output, RDFFormat dataFormat, WriterConfig settings)
			throws RDFHandlerException {
		final RDFWriter writer = Rio.createWriter(dataFormat, output);
		writer.setWriterConfig(settings);
		write(statement, writer);
	}

	/**
	 * Writes the given single statement to the given {@link RDFHandler}.
	 * <p>
	 *
	 * @param statement A statement, to be written.
	 * @param writer
	 * @throws RDFHandlerException Thrown if there is an error writing the statement.
	 */
	public static void write(Statement statement, RDFHandler writer) throws RDFHandlerException {
		writer.startRDF();
		writer.handleStatement(statement);
		writer.endRDF();
	}

	public static void main(String[] args)
			throws IOException, RDFParseException, RDFHandlerException, UnsupportedRDFormatException {
		if (args.length < 2) {
			System.out.println("Usage: java org.eclipse.rdf4j.rio.Rio <inputFile> <outputFile>");
			System.exit(1);
			return;
		}

		String inputFile = args[0];
		String outputFile = args[1];

		try (FileOutputStream outStream = new FileOutputStream(outputFile);
				FileInputStream inStream = new FileInputStream(inputFile)) {
			createParser(getParserFormatForFileName(inputFile).orElse(RDFFormat.RDFXML))
					.setRDFHandler(
							createWriter(getWriterFormatForFileName(outputFile).orElse(RDFFormat.RDFXML), outStream))
					.parse(inStream, "file:" + inputFile);
		}
	}

	/**
	 * Helper method to use to create a lambda for {@link Optional#orElseThrow(Supplier)} to indicate a format is
	 * unsupported.
	 *
	 * @param unsupportedFormat The format that was not found.
	 * @return A lambda that can be used to generate an exception if the format is not found.
	 */
	public static Supplier<UnsupportedRDFormatException> unsupportedFormat(RDFFormat unsupportedFormat) {
		return () -> new UnsupportedRDFormatException("Did not recognise RDF format object " + unsupportedFormat);
	}

	/**
	 * Helper method to use to create a lambda for {@link Optional#orElseThrow(Supplier)} to indicate a format is
	 * unsupported.
	 *
	 * @param unsupportedFormat The format that was not found.
	 * @return A lambda that can be used to generate an exception if the format is not found.
	 */
	public static Supplier<UnsupportedRDFormatException> unsupportedFormat(String unsupportedFormat) {
		return () -> new UnsupportedRDFormatException("Did not recognise RDF format string " + unsupportedFormat);
	}
}
