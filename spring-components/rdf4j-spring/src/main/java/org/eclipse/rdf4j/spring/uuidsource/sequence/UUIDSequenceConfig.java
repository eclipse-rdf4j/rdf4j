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

package org.eclipse.rdf4j.spring.uuidsource.sequence;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@Configuration
@EnableConfigurationProperties(UUIDSequenceProperties.class)
@ConditionalOnProperty(prefix = "rdf4j.spring.uuidsource.sequence", name = "enabled")
public class UUIDSequenceConfig {
	@Bean
	public UUIDSequence getUUIDSource(@Valid @Autowired UUIDSequenceProperties properties) {
		return new UUIDSequence(properties);
	}
}
