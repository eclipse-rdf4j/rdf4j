package com.fluidops.fedx.util;

import java.util.HashSet;

import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fluidops.fedx.algebra.ConjunctiveFilterExpr;
import com.fluidops.fedx.algebra.FilterExpr;

public class FilterUtilTest {

	
	@Test
	public void testConjunctiveFilterExpr() throws Exception {
		
		FilterExpr left = createFilterExpr("age", 15, CompareOp.GT);
		FilterExpr right = createFilterExpr("age", 25, CompareOp.LT);
		ConjunctiveFilterExpr expr = new ConjunctiveFilterExpr(left, right);
		
		Assertions.assertEquals(
				"( ( ?age > '15'^^<http://www.w3.org/2001/XMLSchema#int> ) && ( ?age < '25'^^<http://www.w3.org/2001/XMLSchema#int> ) )",
				FilterUtils.toSparqlString(expr));
	}
	
	private FilterExpr createFilterExpr( String leftVarName, int rightConstant, CompareOp operator) {
		Compare compare = new Compare(new Var(leftVarName), valueConstant(rightConstant), operator);
		return new FilterExpr(compare, new HashSet<String>());
	
	}
	
	
	private ValueExpr valueConstant(int constant) {
		return new ValueConstant(FedXUtil.valueFactory().createLiteral(constant));
	}
}
