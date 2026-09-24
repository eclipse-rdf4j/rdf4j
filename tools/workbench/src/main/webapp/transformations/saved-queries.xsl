<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="url-encode.xsl" />

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title" select="$saved-queries.title" />

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql/sparql:results">
		<div id="saved-queries" class="workbench-page-layout">
		<xsl:for-each select="sparql:result">
			<xsl:variable name="queryLn"
				select="normalize-space(sparql:binding[@name='queryLn'])" />
			<xsl:variable name="queryText" select="sparql:binding[@name='queryText']" />
			<xsl:variable name="query-url-encoded">
				<xsl:call-template name="url-encode">
					<xsl:with-param name="str" select="normalize-space($queryText)" />
				</xsl:call-template>
			</xsl:variable>
			<xsl:variable name="infer"
				select="normalize-space(sparql:binding[@name='infer'])" />
			<xsl:variable name="rowsPerPage"
				select="normalize-space(sparql:binding[@name='rowsPerPage'])" />
			<xsl:variable name="queryTimeout">
				<xsl:choose>
					<xsl:when test="string-length(normalize-space(sparql:binding[@name='queryTimeout'])) &gt; 0">
						<xsl:value-of select="normalize-space(sparql:binding[@name='queryTimeout'])" />
					</xsl:when>
					<xsl:otherwise>0</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="query"
				select="normalize-space(sparql:binding[@name='query'])" />
			<xsl:variable name="queryHREF"
				select="concat('query?action=exec&amp;queryLn=', $queryLn, '&amp;query=', $query-url-encoded, '&amp;infer=', $infer, '&amp;limit_query=', $rowsPerPage, '&amp;query-timeout=', $queryTimeout)" />
			<xsl:variable name="user"
				select="normalize-space(sparql:binding[@name='user'])" />
			<xsl:variable name="previousUser"
				select="normalize-space(preceding::sparql:result[1]/sparql:binding[@name='user'])" />
			<xsl:variable name="queryName"
				select="normalize-space(sparql:binding[@name='queryName'])" />
			<xsl:if test="$user != $previousUser">
				<h2>
					<xsl:value-of select="$user" />
				</h2>
			</xsl:if>
			<article id="{$query}-div" class="saved-query-row workbench-island">
				<table>
					<tr>
						<th style="vertical-align:middle;width:24em">
							<xsl:value-of select="$queryName" />
						</th>
						<td style="vertical-align:middle">
							<form method="post" name="exec-query" action="query">
								<input type="hidden" name="action" value="exec" />
								<input type="hidden" name="queryLn" value="{$queryLn}" />
								<input type="hidden" name="query" value="{$queryName}" />
								<input type="hidden" name="ref" value="id" />
								<input type="hidden" name="owner" value="{$user}" />
								<input type="hidden" name="infer" value="{$infer}" />
								<input type="hidden" name="limit_query" value="{$rowsPerPage}" />
								<input type="hidden" name="query-timeout" value="{$queryTimeout}" />
								<span class="workbench-action workbench-action--primary">
									<label class="workbench-action-hit-area">
										<xsl:call-template name="workbench-action-icon">
											<xsl:with-param name="name">execute</xsl:with-param>
										</xsl:call-template>
										<span class="workbench-action-label"><input type="submit" value="Execute" /></span>
									</label>
								</span>
							</form>
						</td>
						<td style="vertical-align:middle">
							<!-- the path may only be up to 2048 characters long in Internet Explorer -->
							<xsl:choose>
								<xsl:when test="string-length($queryHREF) &lt; 2049">
									<span class="workbench-action workbench-action--secondary" data-workbench-action="bookmark">
										<a class="workbench-action-hit-area" href="{$queryHREF}">
											<xsl:call-template name="workbench-action-icon">
												<xsl:with-param name="name">link</xsl:with-param>
											</xsl:call-template>
											<span class="workbench-action-label"><xsl:value-of select="$bookmark.label" /></span>
										</a>
									</span>
								</xsl:when>
								<xsl:otherwise>
									<span class="workbench-action workbench-action--secondary workbench-action--disabled"
										data-workbench-action="bookmark" aria-disabled="true">
										<span class="workbench-action-hit-area">
											<xsl:call-template name="workbench-action-icon">
												<xsl:with-param name="name">link</xsl:with-param>
											</xsl:call-template>
											<span class="workbench-action-label">Link unavailable</span>
										</span>
									</span>
								</xsl:otherwise>
							</xsl:choose>
						</td>
						<td style="vertical-align:middle">
							<span class="workbench-action workbench-action--secondary">
								<label class="workbench-action-hit-area">
									<xsl:call-template name="workbench-action-icon">
										<xsl:with-param name="name">summary</xsl:with-param>
									</xsl:call-template>
									<span class="workbench-action-label"><input type="button" id="{$query}-toggle" value="Show"
										class="saved-query-toggle" data-query-urn="{$query}" /></span>
								</label>
							</span>
						</td>
						<td style="vertical-align:middle">
							<form method="post" name="edit-query" action="query">
								<input type="hidden" name="action" value="edit" />
								<input type="hidden" name="queryLn" value="{$queryLn}" />
								<input type="hidden" name="query" value="{$queryName}" />
								<input type="hidden" name="ref" value="id" />
								<input type="hidden" name="owner" value="{$user}" />
								<input type="hidden" name="infer" value="{$infer}" />
								<input type="hidden" name="limit_query" value="{$rowsPerPage}" />
								<input type="hidden" name="query-timeout" value="{$queryTimeout}" />
								<span class="workbench-action workbench-action--secondary">
									<label class="workbench-action-hit-area">
										<xsl:call-template name="workbench-action-icon">
											<xsl:with-param name="name">update</xsl:with-param>
										</xsl:call-template>
										<span class="workbench-action-label"><input type="submit" value="Edit" /></span>
									</label>
								</span>
							</form>
						</td>
						<td style="vertical-align:middle">
							<form method="post" id="{$query}" action="saved-queries?delete={$query}">
								<span class="workbench-action workbench-action--danger">
									<label class="workbench-action-hit-area">
										<xsl:call-template name="workbench-action-icon">
											<xsl:with-param name="name">delete</xsl:with-param>
										</xsl:call-template>
										<span class="workbench-action-label"><input type="button" value="Delete..."
											class="saved-query-delete" data-query-owner="{$user}"
											data-query-name="{$queryName}" data-query-urn="{$query}" /></span>
									</label>
								</span>
							</form>
						</td>
					</tr>
				</table>
				<table class="data" id="{$query}-metadata" style="display: none;">
					<tr>
						<th>Query Language</th>
						<td>
							<xsl:value-of select="$queryLn" />
						</td>
						<th>Include Inferred Statements</th>
						<td>
							<xsl:value-of select="$infer" />
						</td>
						<th>Rows Per Page</th>
						<td>
							<xsl:value-of select="$rowsPerPage" />
						</td>
						<th>Shared</th>
						<td>
							<xsl:value-of select="sparql:binding[@name='shared']" />
						</td>
					</tr>
				</table>
				<textarea id="{$query}-text" style="display: none;"><xsl:value-of select="sparql:binding[@name='queryText']" /></textarea>
			</article>
		</xsl:for-each>
		</div>
		<script src="../../scripts/codemirror.4.5.0.min.js" type="text/javascript"></script>
        <script src="../../scripts/yasqe.min.js" type="text/javascript"></script>
		<script src="../../scripts/saved-queries.js" type="text/javascript"></script>
	</xsl:template>
</xsl:stylesheet>
