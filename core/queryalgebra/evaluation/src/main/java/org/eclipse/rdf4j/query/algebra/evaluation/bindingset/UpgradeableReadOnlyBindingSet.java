/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.bindingset;

import java.util.Collection;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

abstract class UpgradeableReadOnlyBindingSet implements ModifiableUpgradableBindingSet {

	private boolean requiresUpgrade;

	@Override
	public final void addAll(BindingSet bindingSet) {
		requiresUpgrade = true;
	}

	@Override
	public final void addBinding(Binding binding) {
		requiresUpgrade = true;
	}

	@Override
	public final void addBinding(String name, Value value) {
		requiresUpgrade = true;
	}

	@Override
	public final void setBinding(Binding binding) {
		requiresUpgrade = true;
	}

	@Override
	public final void setBinding(String name, Value value) {
		requiresUpgrade = true;
	}

	@Override
	public final void removeBinding(String name) {
		requiresUpgrade = true;
	}

	@Override
	public final void removeAll(Collection<String> bindingNames) {
		requiresUpgrade = true;
	}

	@Override
	public final void retainAll(Collection<String> bindingNames) {
		requiresUpgrade = true;
	}

	@Override
	public final boolean operationFailedAndRequiresUpgrade() {
		return requiresUpgrade;
	}
}
