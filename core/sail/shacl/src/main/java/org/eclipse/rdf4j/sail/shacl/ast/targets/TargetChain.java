/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class TargetChain {

	private final ArrayDeque<Targetable> chain = new ArrayDeque<>();

	private boolean optimizable = true;

	public TargetChain() {
	}

	public TargetChain(TargetChain targetChain) {
		optimizable = targetChain.optimizable;
	}

	public TargetChain add(Targetable o) {

		TargetChain targetChain = new TargetChain(this);
		targetChain.chain.addAll(this.chain);
		targetChain.chain.addLast(o);

		return targetChain;
	}

	public TargetChain setOptimizable(boolean optimizable) {
		TargetChain targetChain = new TargetChain(this);
		targetChain.chain.addAll(this.chain);

		if (targetChain.optimizable) {
			targetChain.optimizable = optimizable;
		}

		return targetChain;
	}

	public Collection<Targetable> getChain() {
		return Collections.unmodifiableCollection(chain);
	}

	public boolean isOptimizable() {
		return optimizable;
	}

	public Optional<Path> getPath() {
		Targetable last = chain.getLast();

		if (last instanceof Path) {
			return Optional.of((Path) last);
		}

		return Optional.empty();
	}

	public EffectiveTarget getEffectiveTarget(ConstraintComponent.Scope scope,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		ArrayDeque<Targetable> newChain = new ArrayDeque<>(chain);

		Targetable targetable = null;

		if (scope == ConstraintComponent.Scope.propertyShape) {
			targetable = newChain.removeLast();
		}

		return new EffectiveTarget(newChain, targetable, rdfsSubClassOfReasoner, stableRandomVariableProvider);
	}

}
