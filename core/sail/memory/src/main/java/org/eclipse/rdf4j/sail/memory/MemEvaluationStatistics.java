/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import net.agkn.hll.HLL;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;

/**
 * Uses the MemoryStore's statement sizes to give cost estimates based on the size of the expected results. This process
 * could be improved with repository statistics about size and distribution of statements.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
class MemEvaluationStatistics extends EvaluationStatistics {

    private final MemValueFactory valueFactory;
    private final MemStatementList memStatementList;

    MemEvaluationStatistics(MemValueFactory valueFactory, MemStatementList memStatementList) {
        this.valueFactory = valueFactory;
        this.memStatementList = memStatementList;
    }

    @Override
    protected CardinalityCalculator createCardinalityCalculator() {
        return new MemCardinalityCalculator();
    }

    protected class MemCardinalityCalculator extends CardinalityCalculator {

        @Override
        public double getCardinality(StatementPattern sp) {

            Value subj = getConstantValue(sp.getSubjectVar());
            if (!(subj != null && subj.isResource())) {
                // can happen when a previous optimizer has inlined a comparison
                // operator.
                // this can cause, for example, the subject variable to be
                // equated to a literal value.
                // See SES-970 / SES-998
                subj = null;
            }
            Value pred = getConstantValue(sp.getPredicateVar());
            if (!(pred != null && pred.isIRI())) {
                // can happen when a previous optimizer has inlined a comparison
                // operator. See SES-970 / SES-998
                pred = null;
            }
            Value obj = getConstantValue(sp.getObjectVar());
            Value context = getConstantValue(sp.getContextVar());
            if (!(context != null && context.isResource())) {
                // can happen when a previous optimizer has inlined a comparison
                // operator. See SES-970 / SES-998
                context = null;
            }

            if (subj == null && pred == null && obj == null && context == null) {
                return memStatementList.size();
            } else {
                return minStatementCount(subj, pred, obj, context);
            }

        }

        @Override
        public void meet(Join node) {

            try {
                if (node.getRightArg() instanceof StatementPattern) {
                    StatementPattern rightArg = (StatementPattern) node.getRightArg();
                    if (node.getLeftArg() instanceof StatementPattern) {
                        StatementPattern leftArg = (StatementPattern) node.getLeftArg();

                        Var r_subjectVar = rightArg.getSubjectVar();
                        if (!r_subjectVar.isConstant()) {
                            if (!leftArg.getSubjectVar().isConstant() && leftArg.getSubjectVar().getName().equals(r_subjectVar.getName())) {
//                                System.out.println("r_subjectVar == left:subjectVar");

                                if (rightArg.getPredicateVar().getValue() instanceof IRI && leftArg.getPredicateVar().getValue() instanceof IRI) {
                                    MemIRI rightPredicate = valueFactory.getMemURI(((IRI) rightArg.getPredicateVar().getValue()));
                                    MemIRI leftPredicate = valueFactory.getMemURI(((IRI) leftArg.getPredicateVar().getValue()));


                                    HLL rightHLL = rightPredicate.predicateStatements_subjects.clone();
                                    HLL leftHLL = leftPredicate.predicateStatements_subjects.clone();


                                    double rightRowsPerId = ((double) rightPredicate.getPredicateStatementCount()) / rightHLL.cardinality();
                                    double leftRowsPerId = ((double) leftPredicate.getPredicateStatementCount()) / leftHLL.cardinality();

									long unionCardinality = getUnionCardinality(rightHLL, leftHLL);
									long intersectionCardinality = rightHLL.cardinality() + leftHLL.cardinality() - unionCardinality;

									double joinCardinality = intersectionCardinality * rightRowsPerId * leftRowsPerId;

									this.cardinality = joinCardinality;

									return;


								/*

 								# Calculate estimated number of rows per unique ID for each table
    							num_rows_per_id_table1 = table1.size / table1.id.hll.cardinality()
    							num_rows_per_id_table2 = table2.size / table2.id.hll.cardinality()

    							# Merge the HLL sketches from the ID columns of both tables
    							unionHLL = table1.id.hll.merge(table2.id.hll)

    							# Calculate the intersection cardinality using the inclusion-exclusion principle
    							intersectionCardinality = table1.id.hll.cardinality() + table2.id.hll.cardinality() - unionHLL.cardinality()

    							# The number of rows resulting from the join operation is the intersection cardinality
    							# multiplied by the estimated number of rows per unique ID from both tables
    							joinCardinality = intersectionCardinality * num_rows_per_id_table1 * num_rows_per_id_table2

    							return joinCardinality

								 */

                                }


                            } else if (!leftArg.getPredicateVar().isConstant() && leftArg.getPredicateVar().getName().equals(r_subjectVar.getName())) {
//                                System.out.println("r_subjectVar == left:predicateVar");

                            } else if (!leftArg.getObjectVar().isConstant() && leftArg.getObjectVar().getName().equals(r_subjectVar.getName())) {
//                                System.out.println("r_subjectVar == left:objectVar");

                            }
                        }

                        Var r_predicateVar = rightArg.getPredicateVar();
                        if (!r_predicateVar.isConstant()) {
                            if (!leftArg.getSubjectVar().isConstant() && leftArg.getSubjectVar().getName().equals(r_predicateVar.getName())) {
//                                System.out.println("r_predicateVar == left:subjectVar");

                            } else if (!leftArg.getPredicateVar().isConstant() && leftArg.getPredicateVar().getName().equals(r_predicateVar.getName())) {
//                                System.out.println("r_predicateVar == left:predicateVar");

                            } else if (!leftArg.getObjectVar().isConstant() && leftArg.getObjectVar().getName().equals(r_predicateVar.getName())) {
//                                System.out.println("r_predicateVar == left:objectVar");

                            }
                        }

                        Var r_objectVar = rightArg.getObjectVar();
                        if (!r_objectVar.isConstant()) {
                            if (!leftArg.getSubjectVar().isConstant() && leftArg.getSubjectVar().getName().equals(r_objectVar.getName())) {
//                                System.out.println("r_objectVar == left:subjectVar");

                            } else if (!leftArg.getPredicateVar().isConstant() && leftArg.getPredicateVar().getName().equals(r_objectVar.getName())) {
//                                System.out.println("r_objectVar == left:predicateVar");

                            } else if (!leftArg.getObjectVar().isConstant() && leftArg.getObjectVar().getName().equals(r_objectVar.getName())) {
//                                System.out.println("r_objectVar == left:objectVar");

                            }
                        }


                    }
                }


                super.meet(node);
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

		private long getUnionCardinality(HLL rightHLL, HLL leftHLL) throws CloneNotSupportedException {
			HLL clone = rightHLL.clone();
			clone.union(leftHLL);
			return clone.cardinality();
		}

		private int minStatementCount(Value subj, Value pred, Value obj, Value context) {
            int minListSizes = Integer.MAX_VALUE;

            if (subj != null) {
                MemResource memSubj = valueFactory.getMemResource((Resource) subj);
                if (memSubj != null) {
                    minListSizes = memSubj.getSubjectStatementCount();
                    if (minListSizes == 0) {
                        return 0;
                    }
                } else {
                    // couldn't find the value in the value factory, which means that there are no statements with that
                    // value
                    return 0;
                }
            }

            if (pred != null) {
                MemIRI memPred = valueFactory.getMemURI((IRI) pred);
                if (memPred != null) {
                    minListSizes = Math.min(minListSizes, memPred.getPredicateStatementCount());
                    if (minListSizes == 0) {
                        return 0;
                    }
                } else {
                    // couldn't find the value in the value factory, which means that there are no statements with that
                    // value
                    return 0;
                }
            }

            if (obj != null) {
                MemValue memObj = valueFactory.getMemValue(obj);
                if (memObj != null) {
                    minListSizes = Math.min(minListSizes, memObj.getObjectStatementCount());
                    if (minListSizes == 0) {
                        return 0;
                    }
                } else {
                    // couldn't find the value in the value factory, which means that there are no statements with that
                    // value
                    return 0;
                }
            }

            if (context != null) {
                MemResource memContext = valueFactory.getMemResource((Resource) context);
                if (memContext != null) {
                    minListSizes = Math.min(minListSizes, memContext.getContextStatementCount());
                } else {
                    // couldn't find the value in the value factory, which means that there are no statements with that
                    // value
                    return 0;
                }
            }

            assert minListSizes != Integer.MAX_VALUE : "minListSizes should have been updated before this point";

            return minListSizes;
        }

        protected Value getConstantValue(Var var) {
            if (var != null) {
                return var.getValue();
            }

            return null;
        }
    }

}
