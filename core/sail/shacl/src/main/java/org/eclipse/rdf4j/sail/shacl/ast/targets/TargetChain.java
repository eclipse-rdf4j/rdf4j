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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Namespace;
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

	public Set<Namespace> getNamespaces() {
		return chain.stream().flatMap(targetable -> targetable.getNamespaces().stream()).collect(Collectors.toSet());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TargetChain that = (TargetChain) o;

		if (chain.size() != that.chain.size()) {
			return false;
		}

		for (Targetable targetable : chain) {
			if (!that.chain.contains(targetable)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (Targetable targetable : chain) {
			hashCode += targetable.hashCode();
		}
		return hashCode;
	}
}
