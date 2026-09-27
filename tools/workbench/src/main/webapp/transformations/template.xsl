<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns:workbench="https://rdf4j.org/schema/workbench#" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:output method="html" doctype-system="about:legacy-compat" />

	<xsl:variable name="info"
		select="document(sparql:sparql/sparql:head/sparql:link[@href='info']/@href)" />
	<xsl:variable name="default-workbench-theme"
		select="$info//sparql:binding[@name='default-workbench-theme']/sparql:literal/text()" />

	<xsl:template name="workbench-feature-enabled">
		<xsl:param name="feature-id" />
		<xsl:variable name="feature"
			select="$info//sparql:result[sparql:binding[@name='query-feature-id']/sparql:literal = $feature-id]" />
		<xsl:choose>
			<xsl:when test="not($feature) or $feature/sparql:binding[@name='query-feature-enabled']/sparql:literal = 'true'">
				<xsl:text>true</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>false</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="workbench-feature-hidden">
		<xsl:param name="feature-id" />
		<xsl:variable name="enabled">
			<xsl:call-template name="workbench-feature-enabled">
				<xsl:with-param name="feature-id" select="$feature-id" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="normalize-space($enabled) = 'false'">
			<xsl:attribute name="hidden">hidden</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="/sparql:sparql/workbench:metadata/workbench:embedded = 'true'">
				<html xml:lang="en" lang="en">
					<head>
						<meta name="rdf4j-workbench-theme-default" content="{$default-workbench-theme}" />
						<script src="../../scripts/workbench-theme.js" type="text/javascript"></script>
						<title>
							<xsl:value-of select="$workbench.title" />
							-
							<xsl:value-of select="$title" />
						</title>
						<link title="Default" rel="stylesheet" type="text/css"
							href="../../styles/default/print.css" media="print" />
						<link title="Default" rel="stylesheet" type="text/css"
							href="../../styles/default/screen.css" media="screen" />
						<link rel="stylesheet" type="text/css"
							href="../../styles/workbench-refresh.css" media="screen" />
						<link title="Basic" rel="alternate stylesheet" type="text/css"
							href="../../styles/basic/all.css" media="all" />
						<link rel="stylesheet" type="text/css"
							href="../../styles/query.css" media="screen" />
					</head>
					<body class="query-result-embedded-body">
						<div id="query-result-embedded" class="query-result-embedded">
							<div id="rdf4j-query-result" hidden="hidden"
								data-query-request-id="{/sparql:sparql/workbench:metadata/workbench:query-request-id}"
								data-query-result-status="{/sparql:sparql/workbench:metadata/workbench:query-result-status}"
								data-query-language="{/sparql:sparql/workbench:metadata/workbench:query-language}"
								data-query-infer="{/sparql:sparql/workbench:metadata/workbench:infer}"
								data-query-timeout="{/sparql:sparql/workbench:metadata/workbench:query-timeout}"
								data-query-embedded="true"></div>
							<script src="../../scripts/template.js" type="text/javascript"></script>
							<script src="../../scripts/jquery-1.11.0.min.js" type="text/javascript"></script>
							<xsl:if test="not(/sparql:sparql/sparql:head)">
								<div id="query-result-embedded-header" class="query-result-embedded-header">
									<h2><xsl:value-of select="$title" /></h2>
									<button id="query-result-fullscreen" class="query-results__fullscreen" type="button"
										aria-label="{$full-screen.label}" title="{$full-screen.label}" aria-pressed="false"
										onclick="window.rdf4jQueryResultToggleFullscreen()">
										<svg class="query-results__fullscreen-icon" viewBox="0 0 24 24" focusable="false"
											aria-hidden="true">
											<path d="M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5"></path>
										</svg>
										<span class="query-results__fullscreen-label"><xsl:value-of select="$full-screen.label" /></span>
									</button>
								</div>
							</xsl:if>
							<xsl:apply-templates />
							<xsl:if test="/sparql:sparql/workbench:metadata/workbench:query-request-id">
								<script src="../../scripts/queryResult.js" type="text/javascript"></script>
							</xsl:if>
						</div>
					</body>
				</html>
			</xsl:when>
			<xsl:otherwise>
		<html xml:lang="en" lang="en">
		<head>
				<meta name="rdf4j-workbench-theme-default" content="{$default-workbench-theme}" />
				<script src="../../scripts/workbench-theme.js" type="text/javascript"></script>
				<title>
					<xsl:value-of select="$workbench.title" />
					-
					<xsl:value-of select="$title" />
				</title>
				<meta name="DC.title" content="{$title}" />
				<xsl:if test="/sparql:sparql/workbench:metadata/workbench:query-request-id">
					<meta id="rdf4j-query-result"
						data-query-request-id="{/sparql:sparql/workbench:metadata/workbench:query-request-id}"
						data-query-result-status="{/sparql:sparql/workbench:metadata/workbench:query-result-status}" />
				</xsl:if>
				<link title="Default" rel="stylesheet" type="text/css"
					href="../../styles/default/print.css" media="print" />
						<link title="Default" rel="stylesheet" type="text/css"
							href="../../styles/default/screen.css" media="screen" />
						<link rel="stylesheet" type="text/css"
							href="../../styles/workbench-refresh.css" media="screen" />
				<link title="Basic" rel="alternate stylesheet" type="text/css"
					href="../../styles/basic/all.css" media="all" />
				<link rel="stylesheet" type="text/css"
                    href="../../styles/yasqe.min.css" />
				<link rel="shortcut icon" href="../../favicon.ico" type="image/ico" />
				<link rel="icon" href="../../favicon.png" type="image/png" />
			</head>
			<body class="workbench-body">
				<div id="header" class="workbench-header">
					<div id="contentheader" class="workbench-context">
					<table>
						<tr>
							<th>
								<xsl:value-of select="$server.label" />
							</th>
							<td>
								<xsl:choose>
									<xsl:when test="$info">
										<xsl:value-of
											select="$info//sparql:binding[@name='server']/sparql:uri" />
									</xsl:when>
									<xsl:otherwise>
										<span class="disabled">
											<xsl:value-of select="$none.label" />
										</span>
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<td class="change">
								<a href="../NONE/server">
									<xsl:value-of select="$change.label" />
								</a>
							</td>
						</tr>
						<tr>
							<th>
								<xsl:value-of select="$repository.label" />
							</th>
							<td>
								<xsl:choose>
									<xsl:when test="$info//sparql:binding[@name='id']">
										<xsl:value-of
											select="$info//sparql:binding[@name='description']/sparql:literal" />
										(
										<xsl:value-of
											select="$info//sparql:binding[@name='id']/sparql:literal" />
										)
									</xsl:when>
									<xsl:otherwise>
										<span class="disabled">
											<xsl:value-of select="$none.label" />
										</span>
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<td class="change">
								<a href="../NONE/repositories">
									<xsl:value-of select="$change.label" />
								</a>
							</td>
						</tr>
						<tr>
							<th>
								<xsl:value-of select="$server-user.label" />
							</th>
							<td id="selected-user"></td>
							<td class="change">
								<a href="../NONE/server">
									<xsl:value-of select="$change.label" />
								</a>
							</td>
						</tr>
					</table>
				</div>
					<div id="logo" class="workbench-brand">
						<img src="../../images/logo.png" alt="rdf4j" />
						<img class="product" src="../../images/product.png" alt="workbench" />
					</div>
					<div class="workbench-theme-control">
						<label for="workbench-theme"><xsl:value-of select="$workbench-theme.label" /></label>
						<select id="workbench-theme" name="workbench-theme">
							<option value="system"><xsl:value-of select="$theme-system.label" /></option>
							<option value="light"><xsl:value-of select="$theme-light.label" /></option>
							<option value="dark"><xsl:value-of select="$theme-dark.label" /></option>
						</select>
					</div>
				</div>
				<details id="workbench-navigation-disclosure" class="workbench-navigation-disclosure" open="open">
					<summary id="workbench-navigation-summary">
						<svg class="workbench-menu-icon" viewBox="0 0 24 24" width="18" height="18"
							focusable="false" aria-hidden="true">
							<path d="M4 6h16M4 12h16M4 18h16"></path>
						</svg>
						<span><xsl:value-of select="$menu.label" /></span>
						<svg class="workbench-menu-chevron workbench-disclosure-chevron" viewBox="0 0 24 24" width="18" height="18"
							focusable="false" aria-hidden="true">
							<path d="m6 9 6 6 6-6"></path>
						</svg>
					</summary>
					<xsl:call-template name="navigation-container" />
				</details>
				<div id="content" class="workbench-main">
					<h1 id="title_heading">
						<xsl:value-of select="$title" />
					</h1>
					<p id="noscript-message" class="ERROR">Scripting is not enabled. The
						RDF4J Workbench
						application requires scripting to be
						enabled in order to work
						properly.
					</p>
					<!-- These scripts need to be loaded before other templates are applied. -->
					<script src="../../scripts/template.js" type="text/javascript"></script>
					<script src="../../scripts/jquery-1.11.0.min.js" type="text/javascript"></script>
					<div id="workbench-page-surface" class="workbench-page-surface">
						<xsl:apply-templates />
					</div>
					<xsl:if test="/sparql:sparql/workbench:metadata/workbench:query-request-id">
						<script src="../../scripts/queryResult.js" type="text/javascript"></script>
					</xsl:if>
				</div>
				<div id="footer" class="workbench-footer">
						<div>
							<xsl:value-of select="$copyright.label" />
						</div>
				</div>
			</body>
		</html>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="navigation-container">
		<div id="navigation" class="workbench-nav">
			<ul class="maingroup">
				<xsl:call-template name="navigation" />
			</ul>
		</div>
	</xsl:template>

	<xsl:template name="navigation">
		<xsl:variable name="menu-items"
			select="$info//sparql:result[sparql:binding[@name='menu-item-id']]" />
		<xsl:for-each
			select="$menu-items[not(sparql:binding[@name='menu-group-id']/sparql:literal = preceding-sibling::sparql:result/sparql:binding[@name='menu-group-id']/sparql:literal)]">
			<xsl:variable name="group-id" select="sparql:binding[@name='menu-group-id']/sparql:literal" />
			<xsl:variable name="group-label" select="sparql:binding[@name='menu-group-label']/sparql:literal" />
			<xsl:variable name="group-icon" select="sparql:binding[@name='menu-group-icon']/sparql:literal" />
			<xsl:variable name="group-items"
				select="$menu-items[sparql:binding[@name='menu-group-id']/sparql:literal = $group-id]" />
			<xsl:choose>
				<xsl:when test="count($group-items) = 1">
					<xsl:variable name="item-id"
						select="$group-items[1]/sparql:binding[@name='menu-item-id']/sparql:literal" />
					<xsl:variable name="item-label"
						select="$group-items[1]/sparql:binding[@name='menu-item-label']/sparql:literal" />
					<xsl:variable name="item-icon"
						select="$group-items[1]/sparql:binding[@name='menu-item-icon']/sparql:literal" />
					<xsl:variable name="item-href"
						select="$group-items[1]/sparql:binding[@name='menu-item-href']/sparql:literal" />
					<xsl:variable name="read-only-page"
						select="$item-id = 'summary' or $item-id = 'namespaces' or $item-id = 'contexts' or $item-id = 'types' or $item-id = 'explore' or $item-id = 'query' or $item-id = 'saved-queries' or $item-id = 'export'" />
					<xsl:variable name="write-page"
						select="$item-id = 'update' or $item-id = 'add' or $item-id = 'remove' or $item-id = 'clear'" />
					<xsl:variable name="disabled"
						select="($read-only-page and not($info//sparql:binding[@name='readable']/sparql:literal/text() = 'true')) or ($write-page and not($info//sparql:binding[@name='writeable']/sparql:literal/text() = 'true'))" />
					<li class="workbench-nav-group workbench-nav-group--single"
						data-workbench-menu-group="{$group-id}" data-workbench-menu-label="{$group-label}">
						<xsl:call-template name="navigation-link">
							<xsl:with-param name="label" select="$item-label" />
							<xsl:with-param name="href" select="$item-href" />
							<xsl:with-param name="icon" select="$item-icon" />
							<xsl:with-param name="disabled" select="$disabled" />
							<xsl:with-param name="group-label" select="$group-label" />
						</xsl:call-template>
					</li>
				</xsl:when>
				<xsl:otherwise>
					<li class="workbench-nav-group" data-workbench-menu-group="{$group-id}"
						data-workbench-menu-label="{$group-label}">
						<details class="workbench-nav-group__disclosure">
							<summary id="workbench-nav-summary-{$group-id}"
								aria-controls="workbench-nav-items-{$group-id}"
								class="query-nav-group-label workbench-nav-group__summary">
								<xsl:call-template name="query-nav-icon">
									<xsl:with-param name="name" select="$group-icon" />
								</xsl:call-template>
								<span class="workbench-nav-group__label"><xsl:value-of select="$group-label" /></span>
								<svg class="workbench-nav-group__chevron workbench-disclosure-chevron"
									viewBox="0 0 24 24" width="18" height="18" focusable="false" aria-hidden="true">
									<path d="m6 9 6 6 6-6"></path>
								</svg>
							</summary>
							<ul id="workbench-nav-items-{$group-id}" class="group" aria-label="{$group-label}">
								<xsl:for-each select="$group-items">
									<xsl:variable name="item-id"
										select="sparql:binding[@name='menu-item-id']/sparql:literal" />
									<xsl:variable name="item-label"
										select="sparql:binding[@name='menu-item-label']/sparql:literal" />
									<xsl:variable name="item-icon"
										select="sparql:binding[@name='menu-item-icon']/sparql:literal" />
									<xsl:variable name="item-href"
										select="sparql:binding[@name='menu-item-href']/sparql:literal" />
									<xsl:variable name="read-only-page"
										select="$item-id = 'summary' or $item-id = 'namespaces' or $item-id = 'contexts' or $item-id = 'types' or $item-id = 'explore' or $item-id = 'query' or $item-id = 'saved-queries' or $item-id = 'export'" />
									<xsl:variable name="write-page"
										select="$item-id = 'update' or $item-id = 'add' or $item-id = 'remove' or $item-id = 'clear'" />
									<xsl:variable name="disabled"
										select="($read-only-page and not($info//sparql:binding[@name='readable']/sparql:literal/text() = 'true')) or ($write-page and not($info//sparql:binding[@name='writeable']/sparql:literal/text() = 'true'))" />
									<xsl:call-template name="navigation-entry">
										<xsl:with-param name="label" select="$item-label" />
										<xsl:with-param name="href" select="$item-href" />
										<xsl:with-param name="icon" select="$item-icon" />
										<xsl:with-param name="disabled" select="$disabled" />
									</xsl:call-template>
								</xsl:for-each>
							</ul>
						</details>
					</li>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="navigation-entry">
		<xsl:param name="label" />
		<xsl:param name="href" />
		<xsl:param name="disabled" />
		<xsl:param name="icon" />
		<li>
			<xsl:call-template name="navigation-link">
				<xsl:with-param name="label" select="$label" />
				<xsl:with-param name="href" select="$href" />
				<xsl:with-param name="disabled" select="$disabled" />
				<xsl:with-param name="icon" select="$icon" />
			</xsl:call-template>
		</li>
	</xsl:template>

	<xsl:template name="navigation-link">
		<xsl:param name="label" />
		<xsl:param name="href" />
		<xsl:param name="disabled" />
		<xsl:param name="icon" />
		<xsl:param name="group-label" />
			<xsl:choose>
				<xsl:when test="$disabled">
					<span class="disabled" title="{$group-label}">
						<xsl:call-template name="query-nav-icon">
							<xsl:with-param name="name" select="$icon" />
						</xsl:call-template>
						<xsl:value-of select="$label" />
					</span>
				</xsl:when>
				<xsl:otherwise>
					<a href="{$href}" data-workbench-nav-href="{$href}" title="{$group-label}">
						<xsl:if test="normalize-space($group-label) and normalize-space($group-label) != normalize-space($label)">
							<xsl:attribute name="aria-label">
								<xsl:value-of select="concat(normalize-space($group-label), ': ', normalize-space($label))" />
							</xsl:attribute>
						</xsl:if>
						<xsl:call-template name="query-nav-icon">
							<xsl:with-param name="name" select="$icon" />
						</xsl:call-template>
						<xsl:value-of select="$label" />
					</a>
				</xsl:otherwise>
			</xsl:choose>
	</xsl:template>

	<xsl:template name="query-nav-icon">
		<xsl:param name="name" />
		<svg class="query-nav-icon query-nav-icon--{$name}" viewBox="0 0 24 24" width="18" height="18"
			focusable="false" aria-hidden="true">
			<xsl:choose>
				<xsl:when test="$name = 'server'">
					<rect x="4" y="4" width="16" height="6" rx="1"></rect>
					<rect x="4" y="14" width="16" height="6" rx="1"></rect>
					<path d="M7 7h.01M7 17h.01"></path>
				</xsl:when>
				<xsl:when test="$name = 'repository'">
					<ellipse cx="12" cy="6" rx="7" ry="3"></ellipse>
					<path d="M5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6"></path>
				</xsl:when>
				<xsl:when test="$name = 'create'">
					<path d="M12 5v14M5 12h14"></path>
				</xsl:when>
				<xsl:when test="$name = 'delete'">
					<path d="M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5"></path>
				</xsl:when>
				<xsl:when test="$name = 'summary'">
					<path d="M5 19V9M12 19V5M19 19v-7"></path>
				</xsl:when>
				<xsl:when test="$name = 'namespaces'">
					<path d="M8 4H6a2 2 0 0 0-2 2v3a3 3 0 0 1-3 3 3 3 0 0 1 3 3v3a2 2 0 0 0 2 2h2M16 4h2a2 2 0 0 1 2 2v3a3 3 0 0 0 3 3 3 3 0 0 0-3 3v3a2 2 0 0 1-2 2h-2"></path>
				</xsl:when>
				<xsl:when test="$name = 'contexts'">
					<rect x="5" y="5" width="14" height="14" rx="2"></rect>
					<rect x="2" y="2" width="14" height="14" rx="2"></rect>
				</xsl:when>
				<xsl:when test="$name = 'types'">
					<path d="m20 13-7 7-9-9V4h7l9 9Z"></path>
					<circle cx="8" cy="8" r="1"></circle>
				</xsl:when>
				<xsl:when test="$name = 'explore'">
					<circle cx="10.5" cy="10.5" r="6.5"></circle>
					<path d="m16 16 5 5"></path>
				</xsl:when>
				<xsl:when test="$name = 'query'">
					<path d="m5 7 5 5-5 5M13 17h6"></path>
				</xsl:when>
				<xsl:when test="$name = 'saved'">
					<path d="M6 4h12v16l-6-3-6 3V4Z"></path>
				</xsl:when>
				<xsl:when test="$name = 'export'">
					<path d="M12 4v12M7 11l5 5 5-5M5 20h14"></path>
				</xsl:when>
				<xsl:when test="$name = 'update'">
					<path d="m4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20ZM14 7l3 3"></path>
				</xsl:when>
				<xsl:when test="$name = 'add'">
					<path d="M12 5v14M5 12h14"></path>
				</xsl:when>
				<xsl:when test="$name = 'remove'">
					<path d="M5 12h14"></path>
				</xsl:when>
				<xsl:when test="$name = 'clear'">
					<circle cx="12" cy="12" r="8"></circle>
					<path d="m9 9 6 6m0-6-6 6"></path>
				</xsl:when>
				<xsl:when test="$name = 'information'">
					<circle cx="12" cy="12" r="8"></circle>
					<path d="M12 11v5M12 8h.01"></path>
				</xsl:when>
				<xsl:when test="$name = 'explore-group'">
					<circle cx="5" cy="12" r="2"></circle>
					<circle cx="18" cy="6" r="2"></circle>
					<circle cx="18" cy="18" r="2"></circle>
					<path d="m7 11 9-4M7 13l9 4"></path>
				</xsl:when>
				<xsl:when test="$name = 'modify-group'">
					<path d="m4 17 4-4 3 3 8-8"></path>
					<path d="M4 20h16"></path>
				</xsl:when>
				<xsl:when test="$name = 'system-group'">
					<circle cx="12" cy="12" r="3"></circle>
					<path d="M19 12a7 7 0 0 0-.2-1.6l2-1.2-2-3.4-2.2 1a7 7 0 0 0-2.7-1.6L14 3h-4l-.3 2.2A7 7 0 0 0 7 6.8l-2.2-1-2 3.4 2 1.2A7 7 0 0 0 4.6 12c0 .6.1 1.1.2 1.6l-2 1.2 2 3.4 2.2-1a7 7 0 0 0 2.7 1.6L10 21h4l.3-2.2a7 7 0 0 0 2.7-1.6l2.2 1 2-3.4-2-1.2c.1-.5.2-1 .2-1.6Z"></path>
				</xsl:when>
			</xsl:choose>
		</svg>
	</xsl:template>

	<xsl:template name="workbench-status-icon">
		<xsl:param name="status" />
		<xsl:param name="label" />
		<svg class="workbench-status-icon workbench-status-icon--{$status}" viewBox="0 0 24 24"
			width="18" height="18" role="img" aria-label="{$label}" focusable="false">
			<title><xsl:value-of select="$label" /></title>
			<xsl:choose>
				<xsl:when test="$status = 'readable'">
					<path d="M3 12s3.5-5 9-5 9 5 9 5-3.5 5-9 5-9-5-9-5Z"></path>
					<circle cx="12" cy="12" r="2.5"></circle>
				</xsl:when>
				<xsl:when test="$status = 'writeable'">
					<path d="m4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z"></path>
					<path d="m14 7 3 3"></path>
				</xsl:when>
				<xsl:when test="$status = 'positive'">
					<path d="m5 12 4 4L19 6"></path>
				</xsl:when>
				<xsl:otherwise>
					<path d="m7 7 10 10M17 7 7 17"></path>
				</xsl:otherwise>
			</xsl:choose>
		</svg>
	</xsl:template>

	<xsl:template name="workbench-action-icon">
		<xsl:param name="name" />
		<xsl:param name="additional-class" select="''" />
		<xsl:variable name="icon-class">
			<xsl:text>workbench-action-icon workbench-action-icon--</xsl:text>
			<xsl:value-of select="$name" />
			<xsl:if test="normalize-space($additional-class)">
				<xsl:text> </xsl:text>
				<xsl:value-of select="$additional-class" />
			</xsl:if>
		</xsl:variable>
		<svg class="{$icon-class}" viewBox="0 0 24 24"
			width="16" height="16" focusable="false" aria-hidden="true">
			<xsl:choose>
				<xsl:when test="$name = 'add' or $name = 'create'">
					<path d="M12 5v14M5 12h14" />
				</xsl:when>
				<xsl:when test="$name = 'upload'">
					<path d="M12 16V4m0 0L7 9m5-5 5 5M5 20h14" />
				</xsl:when>
				<xsl:when test="$name = 'download'">
					<path d="M12 4v12m0 0 5-5m-5 5-5-5M5 20h14" />
				</xsl:when>
				<xsl:when test="$name = 'execute'">
					<path d="M8 5 19 12 8 19V5Z" />
				</xsl:when>
				<xsl:when test="$name = 'explain'">
					<circle cx="6" cy="12" r="2.5" />
					<circle cx="18" cy="6" r="2.5" />
					<circle cx="18" cy="18" r="2.5" />
					<path d="m8.2 11 7.3-4m-7.3 6 7.3 4" />
				</xsl:when>
				<xsl:when test="$name = 'eye'">
					<path d="M3 12s3.5-5 9-5 9 5 9 5-3.5 5-9 5-9-5-9-5Z" />
					<circle cx="12" cy="12" r="2.5" />
				</xsl:when>
				<xsl:when test="$name = 'edit'">
					<path d="m4 20 4.5-1 10-10a2.1 2.1 0 0 0-3-3l-10 10L4 20Z" />
					<path d="m14 7 3 3" />
				</xsl:when>
				<xsl:when test="$name = 'update'">
					<path d="m5 12 5 5L19 7" />
				</xsl:when>
				<xsl:when test="$name = 'chevron'">
					<path d="m6 9 6 6 6-6" />
				</xsl:when>
				<xsl:when test="$name = 'cancel'">
					<path d="M19 12H5m6-6-6 6 6 6" />
				</xsl:when>
				<xsl:when test="$name = 'close'">
					<path d="m6 6 12 12M18 6 6 18" />
				</xsl:when>
				<xsl:when test="$name = 'previous'">
					<path d="m14 5-7 7 7 7" />
				</xsl:when>
				<xsl:when test="$name = 'next'">
					<path d="m10 5 7 7-7 7" />
				</xsl:when>
				<xsl:when test="$name = 'source-file'">
					<path d="M5 4h9l5 5v11H5Z" />
					<path d="M14 4v5h5M8 13h8M8 17h6" />
				</xsl:when>
				<xsl:when test="$name = 'source-url'">
					<path d="M10 14 8.5 15.5a3.5 3.5 0 0 1-5-5L6 8a3.5 3.5 0 0 1 5 0" />
					<path d="m14 10 1.5-1.5a3.5 3.5 0 0 1 5 5L18 16a3.5 3.5 0 0 1-5 0" />
					<path d="m8 16 8-8" />
				</xsl:when>
				<xsl:when test="$name = 'source-text'">
					<path d="M5 4h14v16H5Z" />
					<path d="M8 8h8M8 12h8M8 16h5" />
				</xsl:when>
				<xsl:when test="$name = 'settings'">
					<circle cx="12" cy="12" r="3" />
					<path d="M19 12a7 7 0 0 0-.2-1.6l2-1.2-2-3.4-2.2 1a7 7 0 0 0-2.7-1.6L14 3h-4l-.3 2.2A7 7 0 0 0 7 6.8l-2.2-1-2 3.4 2 1.2A7 7 0 0 0 4.6 12c0 .6.1 1.1.2 1.6l-2 1.2 2 3.4 2.2-1a7 7 0 0 0 2.7 1.6L10 21h4l.3-2.2a7 7 0 0 0 2.7-1.6l2.2 1 2-3.4-2-1.2c.1-.5.2-1 .2-1.6Z" />
				</xsl:when>
				<xsl:when test="$name = 'compare'">
					<path d="M5 7h6v6H5zM13 11h6v6h-6z" />
					<path d="m11 10 2 2" />
				</xsl:when>
				<xsl:when test="$name = 'copy'">
					<rect x="8" y="8" width="11" height="12" rx="1.5" />
					<path d="M6 16H5a1.5 1.5 0 0 1-1.5-1.5V5A1.5 1.5 0 0 1 5 3.5h8.5A1.5 1.5 0 0 1 15 5v1" />
				</xsl:when>
				<xsl:when test="$name = 'link'">
					<path d="M9.5 14.5 8 16a3.5 3.5 0 0 1-5-5l2.5-2.5a3.5 3.5 0 0 1 5 0" />
					<path d="m14.5 9.5 1.5-1.5a3.5 3.5 0 0 1 5 5L18.5 15a3.5 3.5 0 0 1-5 0" />
					<path d="m8 16 8-8" />
				</xsl:when>
				<xsl:when test="$name = 'swap'">
					<path d="M4 7h14m-4-4 4 4-4 4M20 17H6m4 4-4-4 4-4" />
				</xsl:when>
				<xsl:when test="$name = 'delete' or $name = 'remove' or $name = 'clear'">
					<path d="M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" />
				</xsl:when>
				<xsl:otherwise>
					<circle cx="12" cy="12" r="8" />
					<path d="M12 8v8M8 12h8" />
				</xsl:otherwise>
			</xsl:choose>
		</svg>
	</xsl:template>

	<xsl:template name="workbench-create-actions">
		<xsl:param name="create-input-type">button</xsl:param>
		<xsl:param name="create-onclick">checkOverwrite()</xsl:param>
		<span class="workbench-action workbench-action--secondary" data-workbench-action="cancel">
			<label class="workbench-action-hit-area">
				<xsl:call-template name="workbench-action-icon">
					<xsl:with-param name="name">cancel</xsl:with-param>
				</xsl:call-template>
				<span class="workbench-action-label">
					<input type="button" value="{$cancel.label}" data-href="repositories"
						onclick="document.location.href=this.getAttribute('data-href')" />
				</span>
			</label>
		</span>
		<span class="workbench-action workbench-action--primary" data-workbench-action="create">
			<label class="workbench-action-hit-area">
				<xsl:call-template name="workbench-action-icon">
					<xsl:with-param name="name">create</xsl:with-param>
				</xsl:call-template>
				<span class="workbench-action-label">
					<xsl:choose>
						<xsl:when test="$create-input-type = 'submit'">
							<input id="create" type="submit" value="{$create.label}" />
						</xsl:when>
						<xsl:otherwise>
							<input id="create" type="button" value="{$create.label}">
								<xsl:if test="string-length(normalize-space($create-onclick)) &gt; 0">
									<xsl:attribute name="onclick"><xsl:value-of select="$create-onclick" /></xsl:attribute>
								</xsl:if>
							</input>
						</xsl:otherwise>
					</xsl:choose>
				</span>
			</label>
		</span>
	</xsl:template>

	<xsl:template name="limit-select">
		<xsl:param name="onchange" />
        <xsl:param name="limit_id" />
		<xsl:param name="feature-id" select="''" />
		<xsl:param name="limit_default"
			select="$info//sparql:binding[@name='default-limit']/sparql:literal/text()" />
		<select>
            <xsl:attribute name="id">
                <xsl:value-of select="$limit_id" />
            </xsl:attribute>
			<xsl:attribute name="name">
                <xsl:value-of select="$limit_id" />
            </xsl:attribute>
			<xsl:if test="string-length(normalize-space($feature-id)) &gt; 0">
				<xsl:call-template name="workbench-feature-hidden">
					<xsl:with-param name="feature-id" select="$feature-id" />
				</xsl:call-template>
			</xsl:if>
			<xsl:if test="$onchange">
				<xsl:attribute name="onchange">
					<xsl:value-of select="$onchange" />
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="$limit_default != '' and not($limit_default = '0' or $limit_default = '10' or $limit_default = '50' or $limit_default = '100' or $limit_default = '200')">
				<option value="{$limit_default}" selected="selected"><xsl:value-of select="$limit_default" /></option>
			</xsl:if>
			<option value="0">
				<xsl:if test="$limit_default = '0'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$all.label" />
			</option>
			<option value="10">
				<xsl:if test="$limit_default = '10'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$limit10.label" />
			</option>
			<option value="50">
				<xsl:if test="$limit_default = '50'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$limit50.label" />
			</option>
			<option value="100">
				<xsl:if test="$limit_default = '100'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$limit100.label" />
			</option>
			<option value="200">
				<xsl:if test="$limit_default = '200'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:value-of select="$limit200.label" />
			</option>
		</select>
	</xsl:template>

</xsl:stylesheet>
