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

package org.eclipse.rdf4j.model.base;

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Map;

/**
 * Component-based temporal amount value.
 *
 * <p>
 * Represents temporal amount values as a collection of {@link ChronoUnit} components.
 * </p>
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 *
 * @apiNote {@link java.time} doesn't include concrete {@link TemporalAmount} classes able to represent XML Schema
 *          duration values including both date and time components.
 */
class ComponentTemporalAmount implements TemporalAmount {

	private final Map<? extends TemporalUnit, Long> components;
	private final List<TemporalUnit> units;

	ComponentTemporalAmount(Map<? extends TemporalUnit, Long> components) {

		this.components = components;

		this.units = unmodifiableList(components.keySet()
				.stream()
				.sorted(comparing(TemporalUnit::getDuration).reversed()) // as per getUnits() specs
				.collect(toList())
		);

	}

	@Override
	public long get(TemporalUnit unit) {
		return components.getOrDefault(unit, 0L);
	}

	@Override
	public List<TemporalUnit> getUnits() {
		return units;
	}

	@Override
	public Temporal addTo(Temporal temporal) {

		Temporal value = temporal;

		for (final Map.Entry<? extends TemporalUnit, Long> component : components.entrySet()) {
			value = value.plus(component.getValue(), component.getKey());
		}

		return value;
	}

	@Override
	public Temporal subtractFrom(Temporal temporal) {

		Temporal value = temporal;

		for (final Map.Entry<? extends TemporalUnit, Long> component : components.entrySet()) {
			value = value.minus(component.getValue(), component.getKey());
		}

		return value;
	}

}
