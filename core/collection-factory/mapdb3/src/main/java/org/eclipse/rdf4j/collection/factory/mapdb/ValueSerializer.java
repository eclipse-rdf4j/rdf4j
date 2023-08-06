/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerIntegerPacked;
import org.mapdb.serializer.SerializerString;

/**
 * A minimally optimized serializer for values.
 *
 * @author Jerven Bolleman
 */
class ValueSerializer implements Serializer<Value> {
	private static final int IS_NEW_COREDATATYPE = 5;
	private static final int IS_RDF_DATATYPE = 4;
	private static final int IS_GEO_DATATYPE = 3;
	private static final int IS_XSD_DATATYPE = 2;
	private static final int NOT_COREDATYPE = 1;
	private static final int IS_LANGUAGE = 0;
	private static final int IS_BNODE = 0;
	private static final int IS_IRI = 1;
	private static final int IS_LITERAL = 2;
	private static final int IS_TRIPLE = 3;
	private static final int IS_NULL = 4;
	private final SerializerIntegerPacked si = new SerializerIntegerPacked();
	private final SerializerString ss = new SerializerString();
	private final ValueFactory vf;

	public ValueSerializer() {
		this(SimpleValueFactory.getInstance());
	}

	public ValueSerializer(ValueFactory vf) {
		super();
		this.vf = vf;
	}

	@Override
	public void serialize(DataOutput2 out, Value value) throws IOException {
		if (value instanceof BNode) {
			si.serialize(out, IS_BNODE);
			serializeBNode(out, (BNode) value);
		} else if (value instanceof IRI) {
			si.serialize(out, IS_IRI);
			serializeIRI(out, (IRI) value);
		} else if (value instanceof Literal) {
			si.serialize(out, IS_LITERAL);
			serializeLiteral(out, (Literal) value);
		} else if (value instanceof Triple) {
			si.serialize(out, IS_TRIPLE);
			serializeTriple(out, (Triple) value);
		} else {
			si.serialize(out, IS_NULL);
		}
	}

	private void serializeBNode(DataOutput2 out, BNode bnode) throws IOException {
		ss.serialize(out, bnode.getID());
	}

	private void serializeLiteral(DataOutput2 out, Literal value) throws IOException {
		final Optional<String> language = value.getLanguage();
		if (language.isPresent()) {
			si.serialize(out, IS_LANGUAGE);
			ss.serialize(out, value.stringValue());
			ss.serialize(out, language.get());
		} else {
			final CoreDatatype cd = value.getCoreDatatype();
			if (cd == null) {
				si.serialize(out, NOT_COREDATYPE);
				serializeIRI(out, value.getDatatype());
			} else if (cd.isXSDDatatype()) {
				si.serialize(out, IS_XSD_DATATYPE);
				si.serialize(out, ((CoreDatatype.XSD) cd).ordinal());
				// TODO optimize the storage of valid pure int etc.
				// without needing to parse strings
			} else if (cd.isGEODatatype()) {
				si.serialize(out, IS_GEO_DATATYPE);
				si.serialize(out, ((CoreDatatype.GEO) cd).ordinal());
			} else if (cd.isRDFDatatype()) {
				si.serialize(out, IS_RDF_DATATYPE);
				si.serialize(out, ((CoreDatatype.RDF) cd).ordinal());
			} else {
				si.serialize(out, IS_NEW_COREDATATYPE);
				serializeIRI(out, value.getDatatype());
			}
			ss.serialize(out, value.stringValue());
		}
	}

	private void serializeIRI(DataOutput2 out, IRI value) throws IOException {
		ss.serialize(out, value.stringValue());
	}

	private void serializeTriple(DataOutput2 out, Triple value) throws IOException {
		serialize(out, value.getSubject());
		serialize(out, value.getPredicate());
		serialize(out, value.getObject());
	}

	@Override
	public Value deserialize(DataInput2 input, int available) throws IOException {
		int t = si.deserialize(input, available);
		switch (t) {
		case IS_BNODE:
			return deserializeBnode(input, available);
		case IS_IRI:
			return deserializeIRI(input, available);
		case IS_LITERAL:
			return deserializeLiteral(input, available);
		case IS_TRIPLE:
			return deserializeTriple(input, available);
		case IS_NULL:
		default:
			return null;
		}
	}

	private Value deserializeTriple(DataInput2 input, int available) throws IOException {
		final Resource subj = (Resource) deserialize(input, available);
		final IRI pred = (IRI) deserialize(input, available);
		final Value obj = deserialize(input, available);
		return vf.createTriple(subj, pred, obj);
	}

	private Value deserializeLiteral(DataInput2 input, int available) throws IOException {
		int t = si.deserialize(input, available);
		switch (t) {
		case IS_LANGUAGE: {
			String language = ss.deserialize(input, available);
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, language);
		}
		case NOT_COREDATYPE: {
			IRI datatype = deserializeIRI(input, available);
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, datatype);
		}
		case IS_XSD_DATATYPE: {
			CoreDatatype.XSD datatype = CoreDatatype.XSD.values()[si.deserialize(input, available)];
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, datatype);
		}
		case IS_GEO_DATATYPE: {
			CoreDatatype.GEO datatype = CoreDatatype.GEO.values()[si.deserialize(input, available)];
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, datatype);
		}
		case IS_RDF_DATATYPE: {
			CoreDatatype.RDF datatype = CoreDatatype.RDF.values()[si.deserialize(input, available)];
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, datatype);
		}
		case IS_NEW_COREDATATYPE: {
			IRI datatype = deserializeIRI(input, available);
			String value = ss.deserialize(input, available);
			return vf.createLiteral(value, datatype);
		}
		}
		return null;
	}

	private IRI deserializeIRI(DataInput2 input, int available) throws IOException {
		return vf.createIRI(ss.deserialize(input, available));
	}

	private BNode deserializeBnode(DataInput2 input, int available) throws IOException {
		return vf.createBNode(ss.deserialize(input, available));
	}

}
