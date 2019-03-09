/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;

/**
 * @author Håvard Ottestad
 */
public class TupleHelper {
	public static Tuple join(Tuple left, Tuple right) {

		ArrayList<Value> newLine = new ArrayList<>(left.line.size() + right.line.size() - 1);


		newLine.addAll(left.line);

		for (int i = 1; i < right.line.size(); i++) {
			newLine.add(right.line.get(i));
		}

		Tuple tuple = new Tuple(newLine);
		tuple.addHistory(left);
		tuple.addHistory(right);
		tuple.addAllCausedByPropertyShape(left.getCausedByPropertyShapes());
		tuple.addAllCausedByPropertyShape(right.getCausedByPropertyShapes());
		return tuple;

	}
}
