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
								<option value="lmdb">
									LMDB Store
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
								value="lmdb" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-title.label" />
						</th>
						<td>
							<input type="text" id="title" name="Repository title" size="48"
								value="LMDB store" />
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
						<th>Query Iteration Cache sync threshold</th>
						<td>
							<input type="text" id="iterationCacheSyncThreshold"
								name="Query Iteration Cache sync threshold" size="16" value="10000" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Triple DB size</th>
						<td>
							<input type="text" id="tripleDBSize" name="Triple DB size" size="16"
								value="10485760" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Value DB size</th>
						<td>
							<input type="text" id="valueDBSize" name="Value DB size" size="16"
								value="10485760" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Force sync</th>
						<td>
							<select id="forceSync" name="Force sync">
								<option value="false" selected="selected">
									<xsl:value-of select="$false.label" />
								</option>
								<option value="true">
									<xsl:value-of select="$true.label" />
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>No readahead</th>
						<td>
							<select id="noReadahead" name="No readahead">
								<option value="false" selected="selected">
									<xsl:value-of select="$false.label" />
								</option>
								<option value="true">
									<xsl:value-of select="$true.label" />
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Value cache size</th>
						<td>
							<input type="text" id="valueCacheSize" name="Value cache size" size="16"
								value="512" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Value ID cache size</th>
						<td>
							<input type="text" id="valueIDCacheSize" name="Value ID cache size" size="16"
								value="128" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Namespace cache size</th>
						<td>
							<input type="text" id="namespaceCacheSize" name="Namespace cache size" size="16"
								value="64" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Namespace ID cache size</th>
						<td>
							<input type="text" id="namespaceIDCacheSize" name="Namespace ID cache size" size="16"
								value="32" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Auto grow</th>
						<td>
							<select id="autoGrow" name="Auto grow">
								<option value="true" selected="selected">
									<xsl:value-of select="$true.label" />
								</option>
								<option value="false">
									<xsl:value-of select="$false.label" />
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Page cardinality estimator</th>
						<td>
							<select id="pageCardinalityEstimator" name="Page cardinality estimator">
								<option value="true" selected="selected">
									<xsl:value-of select="$true.label" />
								</option>
								<option value="false">
									<xsl:value-of select="$false.label" />
								</option>
							</select>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>Value eviction interval</th>
						<td>
							<input type="text" id="valueEvictionInterval" name="Value eviction interval" size="16"
								value="60000" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-evaluation-mode.label" />
						</th>
						<td>
							<select id="queryEvalMode" name="Query Evaluation Mode">
								<option selected="selected" value="STRICT">
									strict
								</option>
								<option value="STANDARD">
									standard
								</option>
							</select>
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
		<script src="../../scripts/create.js" type="text/javascript">
		</script>
	</xsl:template>

</xsl:stylesheet>
