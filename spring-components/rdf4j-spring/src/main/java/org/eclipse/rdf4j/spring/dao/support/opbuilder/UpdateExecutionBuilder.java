/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.support.opbuilder;

import static org.eclipse.rdf4j.spring.dao.support.operation.OperationUtils.setBindings;

import java.util.Map;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.spring.dao.support.UpdateCallback;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class UpdateExecutionBuilder extends OperationBuilder<Update, UpdateExecutionBuilder> {

	public UpdateExecutionBuilder(Update update, RDF4JTemplate template) {
		super(update, template);
	}

	public void execute() {
		Update update = getOperation();
		setBindings(update, getBindings());
		update.execute();
	}

	public void execute(UpdateCallback updateCallback) {
		Map<String, Value> bindings = getBindings();
		Update update = getOperation();
		setBindings(update, bindings);
		update.execute();
		updateCallback.accept(bindings);
	}
}
