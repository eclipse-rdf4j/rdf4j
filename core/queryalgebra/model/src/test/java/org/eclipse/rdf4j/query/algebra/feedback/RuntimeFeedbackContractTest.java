/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.rdf4j.query.algebra.feedback.RuntimeFeedbackContract.Access;
import org.eclipse.rdf4j.query.algebra.feedback.RuntimeFeedbackContract.Algorithm;
import org.eclipse.rdf4j.query.algebra.feedback.RuntimeFeedbackContract.DependentPredictionVector;
import org.eclipse.rdf4j.query.algebra.feedback.RuntimeFeedbackContract.PredictionVector;
import org.eclipse.rdf4j.query.algebra.feedback.RuntimeFeedbackContract.SemanticKind;
import org.junit.jupiter.api.Test;

class RuntimeFeedbackContractTest {

	private static final RuntimeFeedbackDescriptor DESCRIPTOR = new RuntimeFeedbackDescriptor() {
	};

	private static PredictionVector vector(double rows) {
		return new PredictionVector(rows, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}

	private static DependentPredictionVector dependent(double startupOnceWork) {
		return new DependentPredictionVector(startupOnceWork, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
				Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}

	private static RuntimeFeedbackContract contract(double lower, double point, double upper,
			double regressionLimit) {
		return new RuntimeFeedbackContract(DESCRIPTOR, vector(10), vector(20), lower, point, upper, regressionLimit,
				SemanticKind.ORDINARY, Algorithm.SCAN, Access.FULL_SCAN, 1, 0L, 0L, 0L,
				RuntimeFeedbackContract.ADMIT_LOGICAL);
	}

	// REINFORCE: prediction vectors accept NaN for unknown dimensions but reject negative and infinite values
	@Test
	void predictionVectorsAcceptNaNButRejectNegativeAndInfinite() {
		assertThat(PredictionVector.UNKNOWN.rows()).isNaN();
		assertThat(PredictionVector.UNKNOWN.peakMemoryRows()).isNaN();
		assertThat(DependentPredictionVector.UNKNOWN.reopenWork()).isNaN();
		assertThat(vector(0).rows()).isZero();

		assertThatThrownBy(() -> vector(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("rows");
		assertThatThrownBy(() -> vector(Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> dependent(-0.5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("startup-once work");
		assertThatThrownBy(() -> dependent(Double.NEGATIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
	}

	// REINFORCE: the objective envelope must be finite, non-negative and ordered lower <= point <= upper
	@Test
	void objectiveEnvelopeMustBeOrderedFiniteAndNonNegative() {
		assertThat(contract(1, 2, 3, 0).objectiveLower()).isEqualTo(1);
		assertThat(contract(1, 2, 3, 0).objectivePoint()).isEqualTo(2);
		assertThat(contract(2, 2, 2, 0).objectiveUpper()).isEqualTo(2);

		assertThatThrownBy(() -> contract(3, 2, 3, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(1, 4, 3, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(-1, 2, 3, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(1, 2, Double.POSITIVE_INFINITY, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(1, Double.NaN, 3, 0)).isInstanceOf(IllegalArgumentException.class);
	}

	// REINFORCE: the regression limit may be unbounded but never negative, NaN, or negative infinity
	@Test
	void regressionLimitAllowsPositiveInfinityOnly() {
		assertThat(contract(1, 2, 3, Double.POSITIVE_INFINITY).regressionLimit()).isInfinite();
		assertThat(contract(1, 2, 3, 0).regressionLimit()).isZero();

		assertThatThrownBy(() -> contract(1, 2, 3, -0.5)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(1, 2, 3, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> contract(1, 2, 3, Double.NEGATIVE_INFINITY))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// REINFORCE: epochs are non-negative and required references are null-checked
	@Test
	void epochsMustBeNonNegativeAndReferencesNonNull() {
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, Algorithm.SCAN, Access.NONE, 0, -1L, 0L, 0L, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, Algorithm.SCAN, Access.NONE, 0, 0L, -1L, 0L, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, Algorithm.SCAN, Access.NONE, 0, 0L, 0L, -1L, 0))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> new RuntimeFeedbackContract(null, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, Algorithm.SCAN, Access.NONE, 0, 0L, 0L, 0L, 0))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				null, Algorithm.SCAN, Access.NONE, 0, 0L, 0L, 0L, 0))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, null, Access.NONE, 0, 0L, 0L, 0L, 0))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1, 1,
				SemanticKind.ORDINARY, Algorithm.SCAN, null, 0, 0L, 0L, 0L, 0))
				.isInstanceOf(NullPointerException.class);
	}

	// REINFORCE: the short constructors derive the logical cardinality from the prediction rows and default the
	// dependent vectors to UNKNOWN; the explicit constructor validates the logical cardinalities separately
	@Test
	void shortConstructorsDeriveLogicalCardinalityFromPredictionRows() {
		RuntimeFeedbackContract derived = contract(1, 2, 3, 0);
		assertThat(derived.rawLogicalCardinality()).isEqualTo(10);
		assertThat(derived.appliedLogicalCardinality()).isEqualTo(20);
		assertThat(derived.rawDependentPrediction()).isSameAs(DependentPredictionVector.UNKNOWN);
		assertThat(derived.appliedDependentPrediction()).isSameAs(DependentPredictionVector.UNKNOWN);

		RuntimeFeedbackContract explicit = new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 40, 50,
				dependent(3), dependent(4), 1, 2, 3, 0, SemanticKind.EXISTS, Algorithm.HASH_JOIN,
				Access.EXACT_LOOKUP, 7, 1L, 2L, 3L, 0);
		assertThat(explicit.rawLogicalCardinality()).isEqualTo(40);
		assertThat(explicit.appliedLogicalCardinality()).isEqualTo(50);
		assertThat(explicit.rawPrediction().rows()).isEqualTo(1);
		assertThat(explicit.rawDependentPrediction().startupOnceWork()).isEqualTo(3);
		assertThat(explicit.appliedDependentPrediction().startupOnceWork()).isEqualTo(4);

		RuntimeFeedbackContract unknownCardinality = new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1),
				Double.NaN, Double.NaN, DependentPredictionVector.UNKNOWN, DependentPredictionVector.UNKNOWN, 1, 2, 3,
				0, SemanticKind.EXISTS, Algorithm.HASH_JOIN, Access.EXACT_LOOKUP, 7, 1L, 2L, 3L, 0);
		assertThat(unknownCardinality.rawLogicalCardinality()).isNaN();
		assertThat(unknownCardinality.appliedLogicalCardinality()).isNaN();

		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), -1, 50,
				DependentPredictionVector.UNKNOWN, DependentPredictionVector.UNKNOWN, 1, 2, 3, 0,
				SemanticKind.EXISTS, Algorithm.HASH_JOIN, Access.EXACT_LOOKUP, 7, 1L, 2L, 3L, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("raw logical cardinality");
		assertThatThrownBy(() -> new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 40,
				Double.POSITIVE_INFINITY, DependentPredictionVector.UNKNOWN, DependentPredictionVector.UNKNOWN, 1, 2,
				3, 0, SemanticKind.EXISTS, Algorithm.HASH_JOIN, Access.EXACT_LOOKUP, 7, 1L, 2L, 3L, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("applied logical cardinality");
	}

	// REINFORCE: withDescriptor is identity-preserving for the same descriptor and copies every other field
	@Test
	void withDescriptorPreservesEveryOtherField() {
		int flags = RuntimeFeedbackContract.ADMIT_FILTER | RuntimeFeedbackContract.DATABASE_EXACT;
		RuntimeFeedbackContract original = new RuntimeFeedbackContract(DESCRIPTOR, vector(10), vector(20), 30, 40,
				dependent(1), dependent(2), 1, 2, 3, 4, SemanticKind.NOT_EXISTS, Algorithm.MEMOIZED_CORRELATED,
				Access.RANGE_LOOKUP, 9, 5L, 6L, 7L, flags);

		assertThat(original.withDescriptor(DESCRIPTOR)).isSameAs(original);

		RuntimeFeedbackDescriptor replacement = new RuntimeFeedbackDescriptor() {
		};
		RuntimeFeedbackContract moved = original.withDescriptor(replacement);

		assertThat(moved).isNotSameAs(original);
		assertThat(moved.descriptor()).isSameAs(replacement);
		assertThat(original.descriptor()).isSameAs(DESCRIPTOR);
		assertThat(moved.rawPrediction()).isEqualTo(original.rawPrediction());
		assertThat(moved.appliedPrediction()).isEqualTo(original.appliedPrediction());
		assertThat(moved.rawLogicalCardinality()).isEqualTo(30);
		assertThat(moved.appliedLogicalCardinality()).isEqualTo(40);
		assertThat(moved.rawDependentPrediction()).isEqualTo(dependent(1));
		assertThat(moved.appliedDependentPrediction()).isEqualTo(dependent(2));
		assertThat(moved.objectiveLower()).isEqualTo(1);
		assertThat(moved.objectivePoint()).isEqualTo(2);
		assertThat(moved.objectiveUpper()).isEqualTo(3);
		assertThat(moved.regressionLimit()).isEqualTo(4);
		assertThat(moved.semanticKind()).isEqualTo(SemanticKind.NOT_EXISTS);
		assertThat(moved.algorithm()).isEqualTo(Algorithm.MEMOIZED_CORRELATED);
		assertThat(moved.access()).isEqualTo(Access.RANGE_LOOKUP);
		assertThat(moved.physicalImplementationId()).isEqualTo(9);
		assertThat(moved.dataEpoch()).isEqualTo(5L);
		assertThat(moved.catalogEpoch()).isEqualTo(6L);
		assertThat(moved.modelEpoch()).isEqualTo(7L);
		assertThat(moved.admissionFlags()).isEqualTo(flags);

		assertThatThrownBy(() -> original.withDescriptor(null)).isInstanceOf(NullPointerException.class);
	}

	// REINFORCE: admits() tests individual admission bits
	@Test
	void admitsChecksIndividualFlagBits() {
		int flags = RuntimeFeedbackContract.ADMIT_FILTER | RuntimeFeedbackContract.ORIGIN_CERTIFIED;
		RuntimeFeedbackContract contract = new RuntimeFeedbackContract(DESCRIPTOR, vector(1), vector(1), 1, 1, 1,
				1, SemanticKind.FILTER, Algorithm.STREAMING_CORRELATED, Access.PREFIX_SCAN, 0, 0L, 0L, 0L, flags);

		assertThat(contract.admits(RuntimeFeedbackContract.ADMIT_FILTER)).isTrue();
		assertThat(contract.admits(RuntimeFeedbackContract.ORIGIN_CERTIFIED)).isTrue();
		assertThat(contract.admits(RuntimeFeedbackContract.ADMIT_LOGICAL)).isFalse();
		assertThat(contract.admits(RuntimeFeedbackContract.ADMIT_PHYSICAL)).isFalse();
		assertThat(contract.admits(RuntimeFeedbackContract.ADMIT_SEMI_ANTI)).isFalse();
		assertThat(contract.admits(RuntimeFeedbackContract.ADMIT_LIFECYCLE)).isFalse();
		assertThat(contract.admits(RuntimeFeedbackContract.DATABASE_EXACT)).isFalse();
		assertThat(contract.admits(RuntimeFeedbackContract.EXACT_FACT_ELIGIBLE)).isFalse();
		assertThat(contract.admits(0)).isFalse();
		assertThat(contract.admissionFlags()).isEqualTo(flags);
	}
}
