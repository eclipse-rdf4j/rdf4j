<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$query-result.title" />
		<xsl:text> (</xsl:text>
		<xsl:value-of select="count(//sparql:result)" />
		<xsl:text>)</xsl:text>
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template match="sparql:sparql">
		<xsl:if test="/sparql:sparql/workbench:metadata/workbench:query-text">
			<textarea id="wb-query-text" style="display:none;"><xsl:value-of
				select="/sparql:sparql/workbench:metadata/workbench:query-text" /></textarea>
		</xsl:if>
		<form>
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$download-format.label" />
						</th>
						<td>
							<select id="Accept" name="Accept">
								<xsl:for-each
									select="$info//sparql:binding[@name='graph-download-format']">
									<option value="{substring-before(sparql:literal, ' ')}">
										<xsl:if
											test="$info//sparql:binding[@name='default-Accept']/sparql:literal = substring-before(sparql:literal, ' ')">
											<xsl:attribute name="selected">true</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="substring-after(sparql:literal, ' ')" />
									</option>
								</xsl:for-each>
							</select>
						</td>
						<td>
							<input type="submit" onclick="workbench.paging.addGraphParam('Accept');return false"
								value="{$download.label}" />
						</td>
					</tr>
				</tbody>
			</table>
		</form>
		<form>
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$result-limit.label" />
						</th>
						<td>
							<xsl:call-template name="limit-select">
								<xsl:with-param name="onchange">
									workbench.paging.addGraphParam('limit_query');
								</xsl:with-param>
                                <xsl:with-param name="limit_id">limit_query</xsl:with-param>
							</xsl:call-template>
						</td>
						<td id="result-limited">
							<xsl:if
								test="$info//sparql:binding[@name='default-limit']/sparql:literal = count(//sparql:result)">
								<xsl:value-of select="$result-limited.desc" />
							</xsl:if>
						</td>
					</tr>
				</tbody>
			</table>
		</form>
		<table class="data">
			<xsl:apply-templates select="*" />
		</table>
		<script src="../../scripts/paging.js" type="text/javascript">
		</script>
	</xsl:template>

	<xsl:template match="sparql:head">
		<thead>
			<tr>
				<xsl:apply-templates select="sparql:variable" />
			</tr>
		</thead>
	</xsl:template>

	<xsl:template match="sparql:results">
		<tbody>
			<xsl:apply-templates select="sparql:result" />
		</tbody>
	</xsl:template>

	<xsl:template match="sparql:result">
		<xsl:variable name="result" select="." />
		<tr>
			<xsl:for-each select="../../sparql:head/sparql:variable">
				<xsl:variable name="name" select="@name" />
				<td>
					<xsl:apply-templates select="$result/sparql:binding[@name=$name]" />
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>

</xsl:stylesheet>
