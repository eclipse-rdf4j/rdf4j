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

package org.eclipse.rdf4j.benchmark.rio.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFHandler;

/**
 * Synthetic dataset generator.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class DataSetGenerator {
	public static final String NAMESPACE = "http://example.com/gererator/";
	private static final String WORD_DIR = "generated-data/words.txt";
	private static final int TOTAL_PREDS = 500;
	private static final int AVG_GRAPH_SIZE = 500;
	private final ValueFactory factory;
	private final StringBuilder valueBuilder;
	private final AtomicInteger contextCount = new AtomicInteger(0);
	private final AtomicInteger maxCurrentContextCount = new AtomicInteger(0);
	private ArrayList<String> dictionary;
	private ArrayList<IRI> predicateIRIs;
	private ArrayList<IRI> contextIRIs;
	private Random randomResource;
	private Random randomProbability;
	private BNode prevBNode;
	private IRI currentContext;
	private int totalWords;
	private int avgWordSize;

	public DataSetGenerator() {
		this.factory = SimpleValueFactory.getInstance();
		this.valueBuilder = new StringBuilder();
		this.randomResource = new Random(64);
		this.randomProbability = new Random(64);
		this.avgWordSize = 0;
		fetchWords();
		generatePredicateList();
	}

	public void generateStatements(RDFHandler handler, int percentBNodes, int percentLiterals, int textMinLength,
			int textMaxLength, int totalStatements, boolean isTextOnly, boolean withContext) {
		if (!(percentBNodes + percentLiterals <= 100 && totalStatements > 0 && textMaxLength > 0
				&& textMinLength < textMaxLength)) {
			throw new IllegalArgumentException("Invalid data set variables");
		}

		if (withContext) {
			generateContextList(totalStatements);
			writeBinaryStatementsWithContext(handler, percentBNodes, percentLiterals, textMinLength, textMaxLength,
					totalStatements, isTextOnly);
			maxCurrentContextCount.set(0);
			contextCount.set(0);
			currentContext = null;
		} else {
			writeBinaryStatementsWithoutContext(handler, percentBNodes, percentLiterals, textMinLength,
					textMaxLength,
					totalStatements, isTextOnly);
		}

		prevBNode = null;
		randomResource = new Random(64);
		randomProbability = new Random(64);
	}

	private void writeBinaryStatementsWithContext(RDFHandler handler, int percentBNodes, int percentLiterals,
			int textMinLength, int textMaxLength, int totalStatements, boolean isTextOnly) {
		handler.startRDF();
		for (int i = 0; i < totalStatements; i++) {
			handler.handleStatement(
					fetchStatementWithContext(isTextOnly, textMinLength, textMaxLength, percentBNodes,
							percentLiterals));

		}
		handler.endRDF();
	}

	private void writeBinaryStatementsWithoutContext(RDFHandler handler, int percentBNodes, int percentLiterals,
			int textMinLength, int textMaxLength, int totalStatements, boolean isTextOnly) {
		handler.startRDF();
		for (int i = 0; i < totalStatements; i++) {
			handler.handleStatement(
					fetchStatementWithoutContext(isTextOnly, textMinLength, textMaxLength, percentBNodes,
							percentLiterals));

		}
		handler.endRDF();
	}

	private Statement fetchStatementWithContext(boolean isTextOnly, int textMinLength, int textMaxLength,
			int percentBNodes, int percentLiterals) {
		if (prevBNode != null) {
			// should not have nested statement to bnode with different context
			Statement statement = generateStatement(prevBNode, currentContext, isTextOnly, textMinLength, textMaxLength,
					percentLiterals, true);
			if (getPercentProbability() >= percentBNodes) {
				prevBNode = null;
			}
			return statement;
		}
		if (getPercentProbability() < percentBNodes) {
			updateContext();
			prevBNode = createBNode();
			return generateStatement(prevBNode, currentContext, isTextOnly, textMinLength, textMaxLength,
					percentLiterals, false);
		} else {
			updateContext();
			return generateStatement(null, currentContext, isTextOnly, textMinLength, textMaxLength,
					percentLiterals, false);
		}
	}

	private Statement fetchStatementWithoutContext(boolean isTextOnly, int textMinLength, int textMaxLength,
			int percentBNodes, int percentLiterals) {
		if (prevBNode != null) {
			// add nested statement to blank node
			Statement statement = generateStatement(prevBNode, null, isTextOnly, textMinLength, textMaxLength,
					percentLiterals, true);
			if (getPercentProbability() >= percentBNodes) {
				prevBNode = null;
			}
			return statement;
		}
		if (getPercentProbability() < percentBNodes) {
			prevBNode = createBNode();
			return generateStatement(prevBNode, null, isTextOnly, textMinLength, textMaxLength, percentLiterals,
					false);
		} else {
			return generateStatement(null, null, isTextOnly, textMinLength, textMaxLength, percentLiterals, false);
		}
	}

	private void updateContext() {
		if (contextCount.get() < maxCurrentContextCount.get()) {
			contextCount.incrementAndGet();
		} else {
			resetContext();
		}
	}

	private void resetContext() {
		maxCurrentContextCount.set(randomResource.nextInt(1800) + 200);
		contextCount.set(0);
		currentContext = fetchRandomContext();
	}

	private Statement generateStatement(BNode bNode, IRI context, boolean isTextOnly, int textMinLength,
			int textMaxLength, int percentLiterals, boolean isBNodeSubj) {
		if (context != null) {
			return createStatementWithContext(bNode, isTextOnly, textMinLength, textMaxLength, percentLiterals,
					isBNodeSubj, context);
		} else {
			return createStatementWithoutContext(bNode, isTextOnly, textMinLength, textMaxLength, percentLiterals,
					isBNodeSubj);
		}
	}

	private Statement createStatementWithContext(BNode bNode, boolean isTextOnly, int textMinLength,
			int textMaxLength,
			int percentLiterals, boolean isBNodeSubj, IRI context) {
		boolean shouldCreateLiteral = getPercentProbability() < percentLiterals;
		if (shouldCreateLiteral && bNode == null) {
			return factory.createStatement(generateIri(), fetchRandomPredicate(),
					generateLiteral(isTextOnly, textMinLength, textMaxLength), context);

		} else if (shouldCreateLiteral && isBNodeSubj) {
			return factory.createStatement(bNode, fetchRandomPredicate(),
					generateLiteral(isTextOnly, textMinLength, textMaxLength), context);
		} else {
			if (bNode == null) {
				return factory.createStatement(generateIri(), fetchRandomPredicate(),
						generateIri(), context);
			} else {
				return factory.createStatement(generateIri(), fetchRandomPredicate(),
						bNode, context);
			}
		}
	}

	private Statement createStatementWithoutContext(BNode bNode, boolean isTextOnly, int textMinLength,
			int textMaxLength,
			int percentLiterals, boolean isBNodeSubj) {
		boolean shouldCreateLiteral = getPercentProbability() < percentLiterals;
		if (shouldCreateLiteral && bNode == null) {
			return factory.createStatement(generateIri(), fetchRandomPredicate(),
					generateLiteral(isTextOnly, textMinLength, textMaxLength));
		} else if (shouldCreateLiteral && isBNodeSubj) {
			return factory.createStatement(bNode, fetchRandomPredicate(),
					generateLiteral(isTextOnly, textMinLength, textMaxLength));
		} else {
			if (bNode == null) {
				return factory.createStatement(generateIri(), fetchRandomPredicate(),
						generateIri());
			} else {
				return factory.createStatement(generateIri(), fetchRandomPredicate(),
						bNode);
			}
		}
	}

	private IRI fetchRandomPredicate() {
		return predicateIRIs.get(getPredicateIndex());
	}

	private IRI fetchRandomContext() {
		return contextIRIs.get(getContextIndex());
	}

	private Literal generateLiteral(boolean isTextOnly, int minLength, int maxLength) {
		if (isTextOnly) {
			return createStringLiteral(minLength, maxLength);
		} else {
			// 50% chance to generate Num or String Literal
			if (getPercentProbability() < 50) {
				return createNumberLiteral();
			} else {
				return createStringLiteral(minLength, maxLength);
			}
		}
	}

	private Literal createNumberLiteral() {
		int i = randomResource.nextInt(4);
		switch (i) {
		case 0:
			return factory.createLiteral(randomResource.nextDouble());
		case 1:
			return factory.createLiteral(randomResource.nextFloat());
		case 2:
			return factory.createLiteral(randomResource.nextInt());
		default:
			return factory.createLiteral(randomResource.nextLong());
		}
	}

	private Literal createStringLiteral(int minLength, int maxLength) {
		valueBuilder.setLength(0);
		valueBuilder.append(dictionary.get(getDictionaryIndex()));
		String value;
		if (valueBuilder.length() > maxLength) {
			value = valueBuilder.substring(0, maxLength);
		} else if (valueBuilder.length() > minLength) {
			value = valueBuilder.toString();
		} else {
			// need to append more chars
			int diffMinMax = maxLength - minLength;
			int addMaxWords = diffMinMax / avgWordSize;
			// randomize the length of literal so not too large or too small
			int wordsToAdd = randomResource.nextInt(addMaxWords) + 1;
			while (valueBuilder.length() < minLength) {
				for (int i = 0; i < wordsToAdd; i++) {
					valueBuilder.append(dictionary.get(getDictionaryIndex())).append(" ");
				}
			}
			if (valueBuilder.length() > maxLength) {
				value = valueBuilder.substring(0, maxLength);
			} else {
				value = valueBuilder.toString();
			}
		}
		return factory.createLiteral(value);
	}

	private int getDictionaryIndex() {
		return randomResource.nextInt(totalWords);
	}

	private BNode createBNode() {
		return factory.createBNode();
	}

	private void fetchWords() {
		dictionary = new ArrayList<>(1024);
		try (BufferedReader reader = new BufferedReader(
				new FileReader(
						Objects.requireNonNull(this.getClass().getClassLoader().getResource(WORD_DIR)).getFile()))) {
			reader.lines().forEach(line -> {
				avgWordSize += line.length();
				dictionary.add(line);
			});
			totalWords = dictionary.size();
			avgWordSize /= totalWords;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getPredicateIndex() {
		return randomResource.nextInt(TOTAL_PREDS);
	}

	private void generatePredicateList() {
		predicateIRIs = new ArrayList<>(1024);
		for (int i = 0; i < TOTAL_PREDS; i++) {
			predicateIRIs.add(generateIri());
		}
	}

	private IRI generateIri() {
		return factory.createIRI(NAMESPACE, "daf" + Math.abs(randomResource.nextLong()));
	}

	private int getContextIndex() {
		return randomResource.nextInt(contextIRIs.size());
	}

	private void generateContextList(int totalStatements) {
		contextIRIs = new ArrayList<>(1024);
		int totalContexts = totalStatements / AVG_GRAPH_SIZE;
		for (int i = 0; i < totalContexts; i++) {
			contextIRIs.add(generateIri());
		}
	}

	private int getPercentProbability() {
		return randomProbability.nextInt(101);
	}
}
