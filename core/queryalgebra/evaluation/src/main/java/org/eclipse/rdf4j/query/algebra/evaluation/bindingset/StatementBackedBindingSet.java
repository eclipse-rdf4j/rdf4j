/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.bindingset;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

class StatementBackedBindingSet extends UpgradeableReadOnlyBindingSet {

	private final String subjectName;
	private final String predicateName;
	private final String objectName;
	private final String contextName;
	private final Set<String> names;
	private final Statement statement;

	public StatementBackedBindingSet(String subjectName, String predicateName, String objectName, String contextName,
									 Statement statement, Set<String> names, Set<String> namesWithoutContext) {
		this.subjectName = subjectName;
		this.predicateName = predicateName;
		this.objectName = objectName;
		this.contextName = contextName;
		this.names = statement.getContext() != null ? names : namesWithoutContext;
		this.statement = statement;
	}

	@Override
	public Iterator<Binding> iterator() {
		return new Iterator<>() {

			int index = 0;
			Binding next;

			private void calculateNext() {

				while (next == null && index < 4) {
					switch (index) {
						case 0:
							next = getSimpleBinding(subjectName, statement.getSubject());
							break;
						case 1:
							next = getSimpleBinding(predicateName, statement.getPredicate());
							break;
						case 2:
							next = getSimpleBinding(objectName, statement.getObject());
							break;
						case 3:
							next = getSimpleBinding(contextName, statement.getContext());
							break;
					}
					index++;
				}

			}

			@Override
			public boolean hasNext() {
				calculateNext();

				return next != null;
			}

			@Override
			public Binding next() {
				calculateNext();

				Binding temp = next;
				next = null;

				return temp;
			}

		};
	}

	@Override
	public Set<String> getBindingNames() {
		return names;
	}

	@Override
	public Binding getBinding(String bindingName) {
		if (bindingName == null) {
			return null;
		}

		if (subjectName == bindingName) {
			return getSimpleBinding(subjectName, statement.getSubject());
		} else if (predicateName == bindingName) {
			return getSimpleBinding(predicateName, statement.getPredicate());
		} else if (objectName == bindingName) {
			return getSimpleBinding(objectName, statement.getObject());
		} else if (contextName == bindingName) {
			return getSimpleBinding(contextName, statement.getContext());
		}

		if (names.contains(bindingName)) {
			if (fastEquals(bindingName, subjectName)) {
				return getSimpleBinding(subjectName, statement.getSubject());
			} else if (fastEquals(bindingName, predicateName)) {
				return getSimpleBinding(predicateName, statement.getPredicate());
			} else if (fastEquals(bindingName, objectName)) {
				return getSimpleBinding(objectName, statement.getObject());
			} else if (fastEquals(bindingName, contextName)) {
				return getSimpleBinding(contextName, statement.getContext());
			}
		}

		return null;
	}

	private SimpleBinding getSimpleBinding(String name, Value value) {
		if (name != null && value != null) {
			return new SimpleBinding(name, value);
		}
		return null;
	}

	@Override
	public boolean hasBinding(String bindingName) {
		if (bindingName == null) {
			return false;
		}
		if (subjectName == bindingName) {
			return true;
		} else if (predicateName == bindingName) {
			return true;
		} else if (objectName == bindingName) {
			return true;
		} else if (contextName == bindingName) {
			return statement.getContext() != null;
		}

		return names.contains(bindingName);
	}

	private static boolean fastEquals(String name1NonNull, String name2Nullable) {
		return name2Nullable != null && name1NonNull.hashCode() == name2Nullable.hashCode()
			&& name1NonNull.equals(name2Nullable);
	}

	@Override
	public Value getValue(String bindingName) {
		if (bindingName == null) {
			return null;
		}
		if (subjectName == bindingName) {
			return statement.getSubject();
		} else if (predicateName == bindingName) {
			return statement.getPredicate();
		} else if (objectName == bindingName) {
			return statement.getObject();
		} else if (contextName == bindingName) {
			return statement.getContext();
		}

		if (names.contains(bindingName)) {
			if (fastEquals(bindingName, subjectName)) {
				return statement.getSubject();
			} else if (fastEquals(bindingName, predicateName)) {
				return statement.getPredicate();
			} else if (fastEquals(bindingName, objectName)) {
				return statement.getObject();
			} else if (fastEquals(bindingName, contextName)) {
				return statement.getContext();
			}
		}

		return null;
	}

	@Override
	public int size() {
		return names.size();
	}

	void fillListBasedBindingSet(List<String> names, List<Value> values) {
		if (subjectName != null) {
			names.add(subjectName);
			values.add(statement.getSubject());
		}

		if (predicateName != null) {
			names.add(predicateName);
			values.add(statement.getPredicate());
		}

		if (objectName != null) {
			names.add(objectName);
			values.add(statement.getObject());
		}

		if (contextName != null && statement.getContext() != null) {
			names.add(contextName);
			values.add(statement.getContext());
		}
	}
}
