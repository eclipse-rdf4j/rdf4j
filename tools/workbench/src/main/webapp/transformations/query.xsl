<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$query.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<xsl:variable name="queryLn"
			select="sparql:results/sparql:result/sparql:binding[@name='queryLn']" />
		<xsl:variable name="query"
			select="sparql:results/sparql:result/sparql:binding[@name='query']" />
		<xsl:variable name="explanation"
			select="sparql:results/sparql:result/sparql:binding[@name='explanation']/sparql:literal" />
		<xsl:variable name="explanationFormat"
			select="sparql:results/sparql:result/sparql:binding[@name='explanation-format']/sparql:literal" />
		<form action="query" method="post" onsubmit="return workbench.query.doSubmit()">
			<input type="hidden" name="action" id="action" />
			<input type="hidden" name="explain" id="explain" />
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$query-language.label" />
						</th>
						<td>
							<select id="queryLn" name="queryLn" onchange="workbench.query.onQlChange()">
								<xsl:for-each select="$info//sparql:binding[@name='query-format']">
									<option value="{substring-before(sparql:literal, ' ')}">
										<xsl:choose>
											<xsl:when
												test="$info//sparql:binding[@name='default-queryLn']/sparql:literal = substring-before(sparql:literal, ' ')">
												<xsl:attribute name="selected">true</xsl:attribute>
											</xsl:when>
											<xsl:when test="$queryLn = substring-before(sparql:literal, ' ')">
												<xsl:attribute name="selected">true</xsl:attribute>
											</xsl:when>
										</xsl:choose>
										<xsl:value-of select="substring-after(sparql:literal, ' ')" />
									</option>
								</xsl:for-each>
							</select>
						</td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$query-string.label" />
						</th>
						<td>
							<textarea id="query" name="query" rows="16" cols="80"
								wrap="soft">
								<xsl:value-of select="$query" />
							</textarea>
						</td>
						<td>
							<input type="hidden" name="ref" value="text" />
						</td>
					</tr>
					<tr>
						<td></td>
						<td>
							<span id="queryString.errors" class="error">
								<xsl:value-of select="//sparql:binding[@name='error-message']" />
							</span>
						</td>
					</tr>
					<tr id="query-explanation-row">
						<xsl:if test="string-length(normalize-space($explanation)) = 0">
							<xsl:attribute name="style">display:none;</xsl:attribute>
						</xsl:if>
						<th>
							<xsl:value-of select="$query-explanation.label" />
						</th>
						<td>
							<pre id="query-explanation"
								data-format="{normalize-space($explanationFormat)}"
								style="max-height:none; overflow:auto; white-space:pre-wrap; word-break:break-word; background:#ffffff; color:#000000; border:1px solid #d0d0d0; padding:0.75em;">
								<xsl:value-of select="$explanation" />
							</pre>
							<div id="query-explanation-dot-view"
								style="display:none; margin-top:0.75em; background:#ffffff; border:1px solid #d0d0d0; padding:0.75em; font-size:1em; width:100%; box-sizing:border-box; overflow:auto;"></div>
						</td>
						<td>
						</td>
					</tr>
					<tr id="query-explanation-controls-row">
						<xsl:if test="string-length(normalize-space($explanation)) = 0">
							<xsl:attribute name="style">display:none;</xsl:attribute>
						</xsl:if>
						<th></th>
						<td>
							<select id="explain-format" name="explain-format">
								<option value="text">
									<xsl:if test="normalize-space($explanationFormat) = '' or normalize-space($explanationFormat) = 'text'">
										<xsl:attribute name="selected">selected</xsl:attribute>
									</xsl:if>
									Text
								</option>
								<option value="dot">
									<xsl:if test="normalize-space($explanationFormat) = 'dot'">
										<xsl:attribute name="selected">selected</xsl:attribute>
									</xsl:if>
									DOT
								</option>
								<option value="json">
									<xsl:if test="normalize-space($explanationFormat) = 'json'">
										<xsl:attribute name="selected">selected</xsl:attribute>
									</xsl:if>
									JSON
								</option>
							</select>
							<select id="explain-level">
								<option value="Unoptimized">Unoptimized</option>
								<option value="Optimized" selected="selected">Optimized</option>
								<option value="Executed">Executed</option>
								<option value="Telemetry">Telemetry</option>
								<option value="Timed">Timed</option>
							</select>
							<input id="rerun-explanation" type="button"
								value="{$explain-query.label}" onclick="workbench.query.runExplain()" />
							<input id="download-explanation" type="button"
								value="{$download-explanation.label}">
								<xsl:if test="string-length(normalize-space($explanation)) = 0">
									<xsl:attribute name="disabled">disabled</xsl:attribute>
								</xsl:if>
							</input>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$result-limit.label" />
						</th>
						<td>
							<xsl:call-template name="limit-select">
                                <xsl:with-param name="limit_id">limit_query</xsl:with-param>
                            </xsl:call-template>
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$query-options.label" />
						</th>
						<td>
							<input id="infer" name="infer" type="checkbox" value="true">
								<xsl:if
									test="$info//sparql:binding[@name='default-infer']/sparql:literal = 'true'">
									<xsl:attribute name="checked">true</xsl:attribute>
								</xsl:if>
							</input>
							<xsl:value-of select="$include-inferred.label" />
							<input id="save-private" name="save-private" type="checkbox"
								value="true" />
							<xsl:value-of select="$save-private.label" />
						</td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$query-actions.label" />
						</th>
						<td>
							<input type="button" onclick="workbench.query.resetNamespaces()" value="Clear" />
							<input id="exec" type="submit" value="{$execute.label}" />
							<input id="explain-trigger" type="button"
								value="{$explain-query.label}" onclick="workbench.query.runExplain()" />
							<input id="save" type="submit" value="{$save.label}"
								disabled="disabled" />
							<input id="query-name" name="query-name" type="text" size="32"
								maxlength="32" value="" />
							<span id="save-feedback"></span>
						</td>
					</tr>
				</tbody>
			</table>
		</form>
		<script type="text/javascript">
        var sparqlNamespaces = {
          <xsl:for-each
                select="document(//sparql:link[@href='namespaces']/@href)//sparql:results/sparql:result">
                <xsl:value-of
                    select="concat('&quot;', sparql:binding[@name='prefix']/sparql:literal, ':&quot;:&quot;', sparql:binding[@name='namespace']/sparql:literal, '&quot;,')" />
                <xsl:text>
                </xsl:text>
            </xsl:for-each>
        };
        </script>
		<script src="../../scripts/codemirror.4.5.0.min.js" type="text/javascript"></script>
		<script src="../../scripts/yasqe.min.js" type="text/javascript"></script>
		<script src="../../scripts/yasqeHelper.js" type="text/javascript"></script>
		<script src="../../scripts/viz/viz.js" type="text/javascript"></script>
		<script src="../../scripts/viz/full.render.js" type="text/javascript"></script>
		<script src="https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.2/dist/svg-pan-zoom.min.js" type="text/javascript"></script>
		<script src="../../scripts/query.js" type="text/javascript"></script>

	</xsl:template>
</xsl:stylesheet>
