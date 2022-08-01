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

package org.eclipse.rdf4j.spring.tx;

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
@ConditionalOnProperty(prefix = "rdf4j.spring.tx", name = "enabled")
@EnableConfigurationProperties(TxProperties.class)
public class TxConfig {

	@Bean
	RDF4JRepositoryTransactionManager getTxManager(
			@Autowired TransactionalRepositoryConnectionFactory txConnectionFactory) {
		return new RDF4JRepositoryTransactionManager(txConnectionFactory);
	}
}
