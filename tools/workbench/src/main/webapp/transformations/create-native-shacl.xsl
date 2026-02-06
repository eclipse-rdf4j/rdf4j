<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">
	<xsl:include href="../locale/messages.xsl" />
	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>
	<xsl:include href="template.xsl" />
	<xsl:template match="sparql:sparql">
		<form action="create" method="post">
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-type.label" />
						</th>
						<td>
							<select id="type" name="type">
								<option value="native-shacl">
									Native Java Store SHACL
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-id.label" />
						</th>
						<td>
							<input type="text" id="id" name="Repository ID" size="16"
								value="native-shacl" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-title.label" />
						</th>
						<td>
							<input type="text" id="title" name="Repository title" size="48"
								value="Native store with SHACL" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-indexes.label" />
						</th>
						<td>
							<input type="text" id="indexes" name="Triple indexes" size="16"
								value="spoc,posc" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-evaluation-mode.label" />
						</th>
						<td>
							<select id="queryEvalMode" name="Query Evaluation Mode">
								<option selected="selected"
									value="STRICT">
									strict
								</option>
								<option
									value="STANDARD">
									standard
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Parallel validation</th>
						<td>
							<input type="radio" name="Parallel validation" value="true"
								checked="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Parallel validation" value="false" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Log validation plans</th>
						<td>
							<input type="radio" name="Log validation plans" value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Log validation plans" value="false"
								checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Log validation violations</th>
						<td>
							<input type="radio" name="Log validation violations" value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Log validation violations" value="false"
								checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Validation enabled</th>
						<td>
							<input type="radio" name="Validation enabled" value="true"
								checked="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Validation enabled" value="false" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Cache select nodes</th>
						<td>
							<input type="radio" name="Cache select nodes" value="true"
								checked="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Cache select nodes" value="false" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Global log validation execution</th>
						<td>
							<input type="radio" name="Global log validation execution"
								value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Global log validation execution"
								value="false" checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>RDFS subclass reasoning</th>
						<td>
							<input type="radio" name="RDFS subclass reasoning" value="true"
								checked="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="RDFS subclass reasoning" value="false" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Performance logging</th>
						<td>
							<input type="radio" name="Performance logging" value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Performance logging" value="false"
								checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Serializable validation</th>
						<td>
							<input type="radio" name="Serializable validation" value="true"
								checked="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Serializable validation" value="false" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Eclipse RDF4J SHACL extensions</th>
						<td>
							<input type="radio" name="Eclipse RDF4J SHACL extensions"
								value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="Eclipse RDF4J SHACL extensions"
								value="false" checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>DASH data shapes</th>
						<td>
							<input type="radio" name="DASH data shapes" value="true" />
							<xsl:value-of select="$true.label" />
							<input type="radio" name="DASH data shapes" value="false"
								checked="true" />
							<xsl:value-of select="$false.label" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Validation results limit total</th>
						<td>
							<input type="text" id="validationResultsLimitTotal"
								name="Validation results limit total" size="10"
								value="1000000" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Validation results limit per constraint</th>
						<td>
							<input type="text" id="validationResultsLimitPerConstraint"
								name="Validation results limit per constraint" size="10"
								value="1000" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Transactional validation limit</th>
						<td>
							<input type="text" id="transactionalValidationLimit"
								name="Transactional validation limit" size="10" value="500000" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Shapes graphs</th>
						<td>
							<input type="text" id="shapesGraphs" name="Shapes graphs" size="64"
								value="&lt;http://rdf4j.org/schema/rdf4j#SHACLShapeGraph&gt;" />
						</td>
						<td></td>
					</tr>
					<tr>
						<td></td>
						<td>
							<input type="button" value="{$cancel.label}" style="float:right"
								data-href="repositories"
								onclick="document.location.href=this.getAttribute('data-href')" />
							<input id="create" type="button" value="{$create.label}"
								onclick="checkOverwrite()" />
						</td>
					</tr>
				</tbody>
			</table>
		</form>
		<script src="../../scripts/create.js" type="text/javascript"></script>
	</xsl:template>
</xsl:stylesheet>
