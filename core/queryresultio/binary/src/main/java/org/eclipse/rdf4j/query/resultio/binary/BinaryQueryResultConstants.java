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
package org.eclipse.rdf4j.query.resultio.binary;

import java.io.DataInput;
import java.io.DataOutput;

/**
 * Interface defining constants for the binary table result format. Files in this format consist of a header followed by
 * zero or more records. Data fields are encoded as specified in the interfaces {@link DataInput} and
 * {@link DataOutput}, except for the encoding of string values. String values are encoded in UTF-8 and are preceeded by
 * a 32-bit integer specifying the length in bytes of this UTF-8 encoded string.
 * <p>
 * The file header is 13 bytes long:
 * <ul>
 * <li>Bytes 1-4 contain the ASCII codes for the string "BRTR", which stands for Binary RDF Table Result.
 * <li>Bytes 5-8 specify the format version (an integer).
 * <li>Byte 9 specifies some flags, specifically 'distinct' and 'ordered'.
 * <li>Bytes 10-13 specify the number of columns of the query result that will follow (an integer).
 * </ul>
 * Following this are the column headers, which are encoded as UTF-8 strings. There are as many column headers as the
 * number of columns that has been specified in the header.
 * <p>
 * Zero or more records follow after the column headers. This can be a mixture of records describing a result and
 * supporting records. The results table is described by the result records which are written from left to right, from
 * top to bottom. Each record starts with a record type marker (a single byte). The following records are defined in the
 * current format:
 * <ul>
 * <li><var>NULL</var> (byte value: 0):<br>
 * This indicates a NULL value in the table and consists of nothing more than the record type marker.
 * <li><var>REPEAT</var> (byte value: 1):<br>
 * This indicates that the next value is identical to the value in the same column in the previous row. The REPEAT
 * record consists of nothing more than the record type marker.
 * <li><var>NAMESPACE</var> (byte value: 2):<br>
 * This is a supporting record that assigns an ID (non-negative integer) to a namespace. This ID can later be used in in
 * a QNAME record to combine it with a local name to form a full URI. The record type marker is followed by a
 * non-negative integer for the ID and an UTF-8 encoded string for the namespace.
 * <li><var>QNAME </var>(byte value: 3):<br>
 * This indicates a URI value, the value of which is encoded as a namespace ID and a local name. The namespace ID is
 * required to be mapped to a namespace in a previous NAMESPACE record. The record type marker is followed by a
 * non-negative integer (the namespace ID) and an UTF-8 encoded string for the local name.
 * <li><var>URI</var> (byte value: 4):<br>
 * This also indicates a URI value, but one that does not use a namespace ID. This record type marker is simply followed
 * by an UTF-8 encoded string for the full URI.
 * <li><var>BNODE</var> (byte value: 5):<br>
 * This indicates a blank node. The record type marker is followed by an UTF-8 encoded string for the bnode ID.
 * <li><var>PLAIN_LITERAL</var> (byte value: 6):<br>
 * This indicates a plain literal value. The record type marker is followed by an UTF-8 encoded string for the literal's
 * label.
 * <li><var>LANG_LITERAL</var> (byte value: 7):<br>
 * This indicates a literal value with a language attribute. The record type marker is followed by an UTF-8 encoded
 * string for the literal's label, followed by an UTF-8 encoded string for the language attribute.
 * <li><var>DATATYPE_LITERAL</var> (byte value: 8):<br>
 * This indicates a datatyped literal. The record type marker is followed by an UTF-8 encoded string for the literal's
 * label. Following this label is either a QNAME or URI record for the literal's datatype.
 * <li><var>EMPTY_ROW</var> (byte value: 9):<br>
 * This indicates a row with zero values.
 * <li><var>TRIPLE</var> (byte value: 10):<br>
 * This indicates an RDF-star triple value. It is followed by the subject, predicate and object values of the triple.
 * <li><var>ERROR</var> (byte value: 126):<br>
 * This record indicates a error. The type of error is indicates by the byte directly following the record type marker:
 * <var>1</var> for a malformed query error, <var>2</var> for a query evaluation error. The error type byte is followed
 * by an UTF-8 string for the error message.
 * <li><var>TABLE_END</var> (byte value: 127):<br>
 * This is a special record that indicates the end of the results table and consists of nothing more than the record
 * type marker. Any data following this record should be ignored.
 * </ul>
 *
 * @author Arjohn Kampman
 */
public interface BinaryQueryResultConstants {

	/**
	 * Magic number for Binary RDF Table Result files.
	 */
	byte[] MAGIC_NUMBER = new byte[] { 'B', 'R', 'T', 'R' };

	/**
	 * The version number of the current format.
	 */
	int FORMAT_VERSION = 4;

	/* RECORD TYPES */
	int NULL_RECORD_MARKER = 0;

	int REPEAT_RECORD_MARKER = 1;

	int NAMESPACE_RECORD_MARKER = 2;

	int QNAME_RECORD_MARKER = 3;

	int URI_RECORD_MARKER = 4;

	int BNODE_RECORD_MARKER = 5;

	int PLAIN_LITERAL_RECORD_MARKER = 6;

	int LANG_LITERAL_RECORD_MARKER = 7;

	int DATATYPE_LITERAL_RECORD_MARKER = 8;

	int EMPTY_ROW_RECORD_MARKER = 9;

	int TRIPLE_RECORD_MARKER = 10;

	int ERROR_RECORD_MARKER = 126;

	int TABLE_END_RECORD_MARKER = 127;

	/* ERROR TYPES */
	int MALFORMED_QUERY_ERROR = 1;

	int QUERY_EVALUATION_ERROR = 2;
}
