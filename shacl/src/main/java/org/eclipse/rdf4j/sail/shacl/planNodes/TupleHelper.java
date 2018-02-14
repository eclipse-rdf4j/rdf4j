/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
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
	public static Tuple join(Tuple leftPeek, Tuple rightPeek) {

		ArrayList<Value> newLine = new ArrayList<>(leftPeek.line.size() + rightPeek.line.size() - 1);


		newLine.addAll(leftPeek.line);

		for (int i = 1; i < rightPeek.line.size(); i++) {
			newLine.add(rightPeek.line.get(i));
		}

		return new Tuple(newLine);

	}
}
