/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.plan;

import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public class Tuple {

	public List<Value> line = new ArrayList<>();

	public Tuple(List<Value> list) {
		line = list;
	}

	public Tuple() {
	}

	public List<Value> getlist() {
		return line;
	}

	@Override
	public String toString() {
		return "Tuple{" + "line=" + Arrays.toString(line.toArray()) + "}";
	}
}
