/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * An immutable empty BindingSet.
 *
 * @author Arjohn Kampman
 */
public class EmptyBindingSet implements BindingSet {

	private static final long serialVersionUID = -6010968140688315954L;

	private static final EmptyBindingSet singleton = new EmptyBindingSet();

	public static BindingSet getInstance() {
		return singleton;
	}

	private final EmptyBindingIterator iter = new EmptyBindingIterator();

	@Override
	public Iterator<Binding> iterator() {
		return iter;
	}

	@Override
	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public Binding getBinding(String bindingName) {
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		return false;
	}

	@Override
	public Value getValue(String bindingName) {
		return null;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BindingSet) {
			return ((BindingSet) o).size() == 0;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "[]";
	}

	/*----------------------------------*
	 * Inner class EmptyBindingIterator *
	 *----------------------------------*/

	private static class EmptyBindingIterator implements Iterator<Binding> {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Binding next() {
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new IllegalStateException();
		}
	}
}
