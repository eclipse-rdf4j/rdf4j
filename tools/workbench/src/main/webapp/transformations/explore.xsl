<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
   <!ENTITY rdfs  "http://www.w3.org/2000/01/rdf-schema#" >
 ]>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$explore.title" />
	</xsl:variable>

	<xsl:variable name="nextX.label">
		<xsl:value-of select="$next.label" />
		<xsl:text> </xsl:text>
		<xsl:value-of select="count(//sparql:result)" />
	</xsl:variable>

	<xsl:variable name="previousX.label">
		<xsl:value-of select="$previous.label" />
		<xsl:text> </xsl:text>
		<xsl:value-of select="count(//sparql:result)" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:include href="table.xsl" />

	<xsl:template name="sort-list">
		<xsl:param name="title" />
		<xsl:param name="list" />
		<div>
			<h3>
				<xsl:value-of select="$title" />
			</h3>
			<ul>
				<xsl:for-each select="$list">
					<xsl:sort select="." />
					<li>
						<xsl:apply-templates select="." />
					</li>
				</xsl:for-each>
			</ul>
		</div>
	</xsl:template>

	<xsl:template match="sparql:sparql">
		<xsl:if test="$info//sparql:binding[@name='default-limit']/sparql:literal = count(//sparql:result)">
		<p id="result-limited">
			<xsl:value-of select="$result-limited.desc" />
		</p>
		</xsl:if>
		<xsl:if
			test="1 = count(//sparql:result/sparql:binding[@name='predicate']/sparql:uri[text() = '&rdfs;label'])">
			<xsl:for-each
				select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;label']">
				<h2>
					<xsl:value-of
						select="sparql:binding[@name='object']/sparql:literal" />
				</h2>
			</xsl:for-each>
		</xsl:if>
		<xsl:if
			test="1 = count(//sparql:result/sparql:binding[@name='predicate']/sparql:uri[text() = '&rdfs;comment'])">
			<xsl:for-each
				select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;comment']">
				<p>
					<xsl:value-of
						select="sparql:binding[@name='object']/sparql:literal" />
				</p>
			</xsl:for-each>
		</xsl:if>
		<p id="explore-resource-summary" class="workbench-page-meta" hidden="hidden">
			<span id="explore-resource-value"></span>
			<span id="explore-result-count"></span>
		</p>
		<form id="explore-form" class="workbench-island" action="explore">
			<div id="explore-controls">
			<div id="explore-resource-field" class="workbench-field">
				<label for="resource"><xsl:value-of select="$resource.label" /></label>
				<input id="resource" name="resource" size="48" type="text" />
			</div>
			<xsl:if test="count(//sparql:binding[@name='error-message']) &gt; 0">
				<span class="error" role="alert">
					<xsl:value-of select="//sparql:binding[@name='error-message']" />
				</span>
			</xsl:if>
			<details id="explore-result-options" class="workbench-options">
				<summary><xsl:value-of select="$result-options.label" /></summary>
				<div class="workbench-options__body">
					<div class="workbench-field">
						<label for="limit_explore"><xsl:value-of select="$result-limit.label" /></label>
						<xsl:call-template name="limit-select">
							<xsl:with-param name="onchange">workbench.paging.addLimit('explore');</xsl:with-param>
							<xsl:with-param name="limit_id">limit_explore</xsl:with-param>
						</xsl:call-template>
					</div>
					<label class="workbench-check" for="explore-show-datatypes">
						<input id="explore-show-datatypes" type="checkbox" name="show-datatypes"
							value="show-dataypes" checked="checked" />
						<span><xsl:value-of select="$show-datatypes.label" /></span>
					</label>
				</div>
			</details>
			</div>
		</form>
		<section id="explore-results" class="workbench-island workbench-responsive-records">
		<xsl:if test="not(sparql:results/sparql:result)">
			<p class="workbench-empty" role="status"><xsl:value-of select="$no-results.label" /></p>
		</xsl:if>
		<table class="simple">
			<tr>
				<td>
					<xsl:if
						test="//sparql:result/sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subClassOf'">
						<xsl:call-template name="sort-list">
							<xsl:with-param name="title"
								select="$super-classes.title" />
							<xsl:with-param name="list"
								select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subClassOf']/sparql:binding[@name='object']" />
						</xsl:call-template>
						<xsl:call-template name="sort-list">
							<xsl:with-param name="title"
								select="$sub-classes.title" />
							<xsl:with-param name="list"
								select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subClassOf']/sparql:binding[@name='subject']" />
						</xsl:call-template>
					</xsl:if>
				</td>
				<td>
					<xsl:if
						test="//sparql:result/sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;domain'">
						<xsl:call-template name="sort-list">
							<xsl:with-param name="title"
								select="$properties.title" />
							<xsl:with-param name="list"
								select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;domain']/sparql:binding[@name='subject']" />
						</xsl:call-template>
						<xsl:if
							test="//sparql:result/sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subPropertyOf'">
							<xsl:call-template name="sort-list">
								<xsl:with-param name="title"
									select="$super-properties.title" />
								<xsl:with-param name="list"
									select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subPropertyOf']/sparql:binding[@name='object']" />
							</xsl:call-template>
							<xsl:call-template name="sort-list">
								<xsl:with-param name="title"
									select="$sub-properties.title" />
								<xsl:with-param name="list"
									select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;subPropertyOf']/sparql:binding[@name='subject']" />
							</xsl:call-template>
						</xsl:if>
						<xsl:call-template name="sort-list">
							<xsl:with-param name="title"
								select="$property-domain.title" />
							<xsl:with-param name="list"
								select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;domain']/sparql:binding[@name='object']" />
						</xsl:call-template>
					</xsl:if>
					<xsl:if
						test="//sparql:result/sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;range'">
						<xsl:call-template name="sort-list">
							<xsl:with-param name="title"
								select="$property-range.title" />
							<xsl:with-param name="list"
								select="//sparql:result[sparql:binding[@name='predicate']/sparql:uri/text() = '&rdfs;range']/sparql:binding[@name='object']" />
						</xsl:call-template>
					</xsl:if>
				</td>
			</tr>
		</table>
		<xsl:if
			test="sparql:head/sparql:variable/@name != 'error-message' and sparql:results">
			<table class="data">
				<xsl:apply-templates select="*" />
			</table>
		</xsl:if>
		<div id="explore-pagination" class="workbench-form-actions">
			<span class="explore-pagination__label"><xsl:value-of select="$result-offset.label" /></span>
			<span class="workbench-action workbench-action--secondary">
				<label class="workbench-action-hit-area">
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">previous</xsl:with-param>
					</xsl:call-template>
					<span class="workbench-action-label"><input id="previousX" type="button"
						value="{$previousX.label}" onclick="workbench.paging.previousOffset('explore');" /></span>
				</label>
			</span>
			<span class="workbench-action workbench-action--secondary">
				<label class="workbench-action-hit-area">
					<xsl:call-template name="workbench-action-icon">
						<xsl:with-param name="name">next</xsl:with-param>
					</xsl:call-template>
					<span class="workbench-action-label"><input id="nextX" type="button"
						value="{$nextX.label}" onclick="workbench.paging.nextOffset('explore');" /></span>
				</label>
			</span>
		</div>
		</section>
		<script src="../../scripts/paging.js" type="text/javascript">  </script>
		<script src="../../scripts/explore.js" type="text/javascript">  </script>
	</xsl:template>

</xsl:stylesheet>
