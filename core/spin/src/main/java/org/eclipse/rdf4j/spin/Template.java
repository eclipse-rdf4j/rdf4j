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
package org.eclipse.rdf4j.spin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;

import com.google.common.base.Joiner;

/**
 * Class to represent a SPIN template.
 */
public class Template {

	private final IRI IRI;

	private ParsedOperation parsedOp;

	private final List<Argument> arguments = new ArrayList<>(4);

	public Template(IRI IRI) {
		this.IRI = IRI;
	}

	public IRI getUri() {
		return IRI;
	}

	public void setParsedOperation(ParsedOperation op) {
		this.parsedOp = op;
	}

	public ParsedOperation getParsedOperation() {
		return parsedOp;
	}

	public void addArgument(Argument arg) {
		arguments.add(arg);
	}

	public List<Argument> getArguments() {
		return arguments;
	}

	public ParsedOperation call(Map<IRI, Value> argValues) throws MalformedSpinException {
		MapBindingSet args = new MapBindingSet();
		for (Argument arg : arguments) {
			IRI argPred = arg.getPredicate();
			Value argValue = argValues.get(argPred);
			if (argValue == null && !arg.isOptional()) {
				throw new MalformedSpinException("Missing value for template argument: " + argPred);
			}
			if (argValue == null) {
				argValue = arg.getDefaultValue();
			}
			if (argValue != null) {
				args.addBinding(argPred.getLocalName(), argValue);
			}
		}

		ParsedOperation callOp;
		if (parsedOp instanceof ParsedBooleanQuery) {
			callOp = new ParsedBooleanTemplate(this, args);
		} else if (parsedOp instanceof ParsedTupleQuery) {
			callOp = new ParsedTupleTemplate(this, args);
		} else if (parsedOp instanceof ParsedGraphQuery) {
			callOp = new ParsedGraphTemplate(this, args);
		} else if (parsedOp instanceof ParsedUpdate) {
			callOp = new ParsedUpdateTemplate(this, args);
		} else {
			throw new AssertionError("Unrecognised ParsedOperation: " + parsedOp.getClass());
		}
		return callOp;
	}

	@Override
	public String toString() {
		return IRI + "(" + Joiner.on(", ").join(arguments) + ")";
	}
}
