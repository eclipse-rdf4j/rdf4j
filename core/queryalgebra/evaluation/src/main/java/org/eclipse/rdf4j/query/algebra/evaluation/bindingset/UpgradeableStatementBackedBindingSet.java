/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.bindingset;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.ModifiableBindingSet;
import org.eclipse.rdf4j.query.algebra.Var;

public class UpgradeableStatementBackedBindingSet
		extends UpgradeableBindingSet<StatementBackedBindingSet, ModifiableUpgradableBindingSet> {

	final StatementBackedBindingSet statementBackedBindingSet;

	private UpgradeableStatementBackedBindingSet(StatementBackedBindingSet statementBackedBindingSet) {
		this.statementBackedBindingSet = statementBackedBindingSet;
	}

	@Override
	ModifiableUpgradableBindingSet upgrade(StatementBackedBindingSet initial) {
		return ModifiableUpgradableBindingSetWrapper.wrap(new DynamicQueryBindingSet(initial));
	}

	@Override
	StatementBackedBindingSet getInitial() {
		return statementBackedBindingSet;
	}

	void fillListBasedBindingSet(List<String> names, List<Value> values) {
		statementBackedBindingSet.fillListBasedBindingSet(names, values);
	}

	public static class Factory {

		private final boolean namesAreUnique;
		private final String subjectName;
		private final String objectName;
		private final String predicateName;
		private final String contextName;
		private final Set<String> names;
		private final Set<String> namesWithoutContext;

		public Factory(Var subjVar, Var predVar, Var objVar, Var conVar) {
			names = new HashSet<>(10);

			subjectName = getName(subjVar, names);
			predicateName = getName(predVar, names);
			objectName = getName(objVar, names);

			if (conVar != null) {
				contextName = conVar.getName();
				namesWithoutContext = new HashSet<>(names);
				names.add(contextName);
			} else {
				namesWithoutContext = names;
				contextName = null;
			}

			namesAreUnique = namesAreUnique(subjectName, objectName, predicateName, contextName, names);
		}

		private static String getName(Var var, Set<String> names) {
			if (var != null && !(var.isAnonymous() && var.isConstant())) {

				if(var.isConstant()){
					System.out.println();
				}
				String name = var.getName();
				names.add(name);
				return name;
			} else {

				return null;
			}
		}

		private static boolean namesAreUnique(String subjectName, String objectName, String predicateName,
				String contextName, Set<String> names) {
			int namesCount = 0;
			if (subjectName != null) {
				namesCount++;
			}
			if (objectName != null) {
				namesCount++;
			}
			if (predicateName != null) {
				namesCount++;
			}
			if (contextName != null) {
				namesCount++;
			}
			return names.size() == namesCount;
		}

		public ModifiableBindingSet getBindingSet(Statement statement) {
			if (namesAreUnique) {
				return new UpgradeableStatementBackedBindingSet(new StatementBackedBindingSet(subjectName,
						predicateName, objectName, contextName, statement, names, namesWithoutContext));
			} else {
				DynamicQueryBindingSet bindings = new DynamicQueryBindingSet();
				if (subjectName != null) {
					bindings.addBinding(subjectName, statement.getSubject());
				}
				if (predicateName != null) {
					bindings.addBinding(predicateName, statement.getPredicate());
				}
				if (objectName != null) {
					bindings.addBinding(objectName, statement.getObject());
				}
				if (contextName != null && statement.getContext() != null) {
					bindings.addBinding(contextName, statement.getContext());
				}
				return bindings;
			}
		}
	}

}
