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
			<input type="hidden" name="ref" value="text" />
			<style type="text/css">
				.query-form {
					--query-form-label-width:10rem;
					width:100%;
					display:flex;
					flex-direction:column;
					gap:0.75em;
				}

				.query-form__row {
					display:grid;
					grid-template-columns:max-content minmax(0, 1fr);
					grid-template-columns:var(--query-form-label-width) minmax(0, 1fr);
					align-items:start;
					column-gap:1em;
					row-gap:0.5em;
				}

				.query-form__row--stacked {
					grid-template-columns:minmax(0, 1fr);
				}

				.query-form__label {
					font-family: Roboto, Libre Franklin, Helvetica Neue, Helvetica, Arial, sans-serif;
					font-weight:bold;
					white-space:nowrap;
					margin-top: auto;
					margin-bottom: auto;
				}

				.query-form__label:after {
					content: ':';
				}

				.query-form__label--blank:after {
					content: '';
				}

				.query-form__row--stacked .query-form__label {
					padding-top:0;
				}

				.query-form__row--stacked .query-form__label:after {
					content:'';
				}

				.query-form__field {
					min-width:0;
				}

				.query-form__field input[type='text'],
				.query-form__field textarea,
				.query-form__field select {
					box-sizing:border-box;
					max-width:100%;
				}

				.query-form__field--controls,
				.query-form__field--options,
				.query-form__field--actions {
					display:flex;
					flex-wrap:wrap;
					align-items:center;
					gap:0.1em;
				}

				.query-form__field--actions #query-name {
					min-width:14em;
				}

				#query {
					width:100%;
					max-width:100%;
				}

				#query-explanation {
					max-height:none;
					width:100%;
					max-width:100%;
					overflow:auto;
					white-space:pre;
					word-break:normal;
					overflow-wrap:normal;
					background:#ffffff;
					color:#000000;
					border:1px solid #d0d0d0;
					padding:0.75em;
					box-sizing:border-box;
				}

				#query-explanation-dot-view {
					display:none;
					background:#ffffff;
					border:1px solid #d0d0d0;
					padding:0;
					font-size:1em;
					width:100%;
					box-sizing:border-box;
					overflow:auto;
					height:75vh;
				}

				@media (max-width: 900px) {
					.query-form__row {
						grid-template-columns:minmax(0, 1fr);
					}

					.query-form__label {
						padding-top:0;
					}

					.query-form__label:after {
						content:'';
					}
				}
			</style>
			<div class="query-form">
				<div class="query-form__row">
					<label class="query-form__label" for="queryLn">
						<xsl:value-of select="$query-language.label" />
					</label>
					<div class="query-form__field">
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
					</div>
				</div>
				<div class="query-form__row query-form__row--stacked">
					<label class="query-form__label" for="query">
						<xsl:value-of select="$query-string.label" />
					</label>
					<div class="query-form__field">
						<textarea id="query" name="query" rows="16" cols="80" wrap="soft">
							<xsl:value-of select="$query" />
						</textarea>
					</div>
				</div>
				<div class="query-form__row">
					<span class="query-form__label query-form__label--blank"></span>
					<div class="query-form__field">
						<span id="queryString.errors" class="error">
							<xsl:value-of select="//sparql:binding[@name='error-message']" />
						</span>
					</div>
				</div>
				<div id="query-explanation-row" class="query-form__row query-form__row--stacked">
					<xsl:if test="string-length(normalize-space($explanation)) = 0">
						<xsl:attribute name="style">display:none;</xsl:attribute>
					</xsl:if>
					<span class="query-form__label">
						<xsl:value-of select="$query-explanation.label" />
					</span>
					<div class="query-form__field">
						<pre id="query-explanation" style="margin-top:0; margin-bottom: 0; max-height:75vh; overflow:scroll;"
							data-format="{normalize-space($explanationFormat)}">
							<xsl:value-of select="$explanation" />
						</pre>
						<div id="query-explanation-dot-view"></div>
					</div>
				</div>
				<div id="query-explanation-controls-row" class="">
					<xsl:if test="string-length(normalize-space($explanation)) = 0">
						<xsl:attribute name="style">display:none;</xsl:attribute>
					</xsl:if>
<!--					<span class="query-form__label query-form__label&#45;&#45;blank"></span>-->
					<div class="query-form__field query-form__field--controls">
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
					</div>
				</div>
				<div class="query-form__row">
					<span class="query-form__label">
						<xsl:value-of select="$result-limit.label" />
					</span>
					<div class="query-form__field">
						<xsl:call-template name="limit-select">
                                <xsl:with-param name="limit_id">limit_query</xsl:with-param>
                            </xsl:call-template>
					</div>
				</div>
				<div class="query-form__row">
					<span class="query-form__label">
						<xsl:value-of select="$query-options.label" />
					</span>
					<div class="query-form__field query-form__field--options">
						<input id="infer" name="infer" type="checkbox" value="true">
							<xsl:if
								test="$info//sparql:binding[@name='default-infer']/sparql:literal = 'true'">
								<xsl:attribute name="checked">true</xsl:attribute>
							</xsl:if>
						</input>
						<label for="infer">
							<xsl:value-of select="$include-inferred.label" />
						</label>
						<input id="save-private" name="save-private" type="checkbox" value="true" />
						<label for="save-private">
							<xsl:value-of select="$save-private.label" />
						</label>
					</div>
				</div>
				<div class="query-form__row">
					<span class="query-form__label">
						<xsl:value-of select="$query-actions.label" />
					</span>
					<div class="query-form__field query-form__field--actions">
						<input type="button" onclick="workbench.query.resetNamespaces()" value="Clear" />
						<input id="exec" type="submit" value="{$execute.label}" />
						<input id="explain-trigger" type="button"
							value="{$explain-query.label}" onclick="workbench.query.runExplain()" />
						<input id="save" type="submit" value="{$save.label}" disabled="disabled" />
						<input id="query-name" name="query-name" type="text" size="32"
							maxlength="32" value="" />
						<span id="save-feedback"></span>
					</div>
				</div>
			</div>
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
