/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.app.net;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.common.app.config.Configuration;
import org.eclipse.rdf4j.common.app.util.ConfigurationUtil;

/**
 * Utility class for handling proxy connection settings.
 */
public class ProxySettings implements Configuration {

	private final static String PROPNAME_PROXIES_ENABLED = "proxies.enabled";

	private final static String PROPNAME_PROXIES_NONPROXYHOSTS_STARTING = "proxies.nonProxyHosts.starting";

	private final static String PROPNAME_HTTP_PROXYHOST = "http.proxyHost";

	private final static String PROPNAME_HTTP_PROXYPORT = "http.proxyPort";

	private final static String PROPNAME_HTTPS_PROXYHOST = "https.proxyHost";

	private final static String PROPNAME_HTTPS_PROXYPORT = "https.proxyPort";

	private final static String PROPNAME_FTP_PROXYHOST = "ftp.proxyHost";

	private final static String PROPNAME_FTP_PROXYPORT = "ftp.proxyPort";

	private final static String PROPNAME_SOCKS_PROXYHOST = "socksProxyHost";

	private final static String PROPNAME_SOCKS_PROXYPORT = "socksProxyPort";

	private Properties props;

	public static final String PROXY_SETTINGS_FILENAME = "proxy.properties";

	private final File confDir;

	private File propsFile;

	public ProxySettings(File applicationDataDir) throws IOException {
		confDir = new File(applicationDataDir, DIR);
	}

	public void setProperty(String key, String val) {
		if (val == null) {
			props.remove(key);
		} else {
			props.setProperty(key, val);
		}
	}

	private void setSystemProperty(String key, String val) {
		if (val == null) {
			System.getProperties().remove(key);
		} else {
			System.setProperty(key, val);
		}
	}

	public boolean getProxiesEnabled() {
		String val = props.getProperty(PROPNAME_PROXIES_ENABLED);
		if (val != null) {
			val = val.trim();
		}
		return String.valueOf(true).equalsIgnoreCase(val);
	}

	/**
	 *
	 * @param proxiesEnabled
	 */
	public void setProxiesEnabled(boolean proxiesEnabled) {
		props.setProperty(PROPNAME_PROXIES_ENABLED, String.valueOf(proxiesEnabled));
	}

	/**
	 *
	 * @return proxy
	 */
	public String getHttpProxyHost() {
		return props.getProperty(PROPNAME_HTTP_PROXYHOST);
	}

	/**
	 *
	 * @param httpProxyHost
	 */
	public void setHttpProxyHost(String httpProxyHost) {
		setProperty(PROPNAME_HTTP_PROXYHOST, httpProxyHost);
		setProxySystemProperty(PROPNAME_HTTP_PROXYHOST, httpProxyHost);
	}

	/**
	 * Get HTTP proxy port as string
	 *
	 * @return proxy port
	 */
	public String getHttpProxyPort() {
		return props.getProperty(PROPNAME_HTTP_PROXYPORT);
	}

	/**
	 * Set HTTP proxy port
	 *
	 * @param httpProxyPort proxy port
	 */
	public void setHttpProxyPort(String httpProxyPort) {
		setProperty(PROPNAME_HTTP_PROXYPORT, httpProxyPort);
		setProxySystemProperty(PROPNAME_HTTP_PROXYPORT, httpProxyPort);
	}

	/**
	 * Get HTTPS proxy host
	 *
	 * @return proxy host as string
	 */
	public String getHttpsProxyHost() {
		return props.getProperty(PROPNAME_HTTPS_PROXYHOST);
	}

	/**
	 * Get HTTPS proxy host
	 *
	 * @param httpsProxyHost
	 */
	public void setHttpsProxyHost(String httpsProxyHost) {
		setProperty(PROPNAME_HTTPS_PROXYHOST, httpsProxyHost);
		setProxySystemProperty(PROPNAME_HTTPS_PROXYHOST, httpsProxyHost);
	}

	public String getHttpsProxyPort() {
		return props.getProperty(PROPNAME_HTTPS_PROXYPORT);
	}

	public void setHttpsProxyPort(String httpsProxyPort) {
		setProperty(PROPNAME_HTTPS_PROXYPORT, httpsProxyPort);
		setProxySystemProperty(PROPNAME_HTTPS_PROXYPORT, httpsProxyPort);
	}

	public String getFtpProxyHost() {
		return props.getProperty(PROPNAME_FTP_PROXYHOST);
	}

	public void setFtpProxyHost(String ftpProxyHost) {
		setProperty(PROPNAME_FTP_PROXYHOST, ftpProxyHost);
		setProxySystemProperty(PROPNAME_FTP_PROXYHOST, ftpProxyHost);
	}

	public String getFtpProxyPort() {
		return props.getProperty(PROPNAME_FTP_PROXYPORT);
	}

	public void setFtpProxyPort(String ftpProxyPort) {
		setProperty(PROPNAME_FTP_PROXYPORT, ftpProxyPort);
		setProxySystemProperty(PROPNAME_FTP_PROXYPORT, ftpProxyPort);
	}

	public String getSocksProxyHost() {
		return props.getProperty(PROPNAME_SOCKS_PROXYHOST);
	}

	public void setSocksProxyHost(String socksProxyHost) {
		setProperty(PROPNAME_SOCKS_PROXYHOST, socksProxyHost);
		setProxySystemProperty(PROPNAME_SOCKS_PROXYHOST, socksProxyHost);
	}

	public String getSocksProxyPort() {
		return props.getProperty(PROPNAME_SOCKS_PROXYPORT);
	}

	public void setSocksProxyPort(String socksProxyPort) {
		setProperty(PROPNAME_SOCKS_PROXYPORT, socksProxyPort);
		setProxySystemProperty(PROPNAME_SOCKS_PROXYPORT, socksProxyPort);
	}

	private void setProxySystemProperty(String key, String val) {
		if (getProxiesEnabled()) {
			setSystemProperty(key, val);
		}
		// See SES-1100: Sesame should leave proxy settings alone if not enabled
		// else {
		// setSystemProperty(key, null);
		// }
	}

	/**
	 * Get the semicolon-separated list of hostnames starting with given strings, that do not use the proxy settings.
	 */
	public String getNonProxyHostsStarting() {
		return props.getProperty(PROPNAME_PROXIES_NONPROXYHOSTS_STARTING);
	}

	/**
	 * Set the semicolon separated list of hostnames starting with given strings, that do not use the proxy settings.
	 */
	public void setNonProxyHostsStarting(String nonProxyHostsStarting) {
		setProperty(PROPNAME_PROXIES_NONPROXYHOSTS_STARTING, nonProxyHostsStarting);

		// parse nonproxy hosts
		StringBuilder sysPropBuffer = new StringBuilder();
		if (nonProxyHostsStarting != null) {
			StringTokenizer st = new StringTokenizer(nonProxyHostsStarting, ";");
			while (st.hasMoreTokens()) {
				sysPropBuffer.append(st.nextToken().trim());
				sysPropBuffer.append('*');
				if (st.hasMoreTokens()) {
					sysPropBuffer.append('|');
				}
			}
		}
		String sysPropValue = null;
		if (sysPropBuffer.length() > 0) {
			sysPropValue = sysPropBuffer.toString();
		}

		// set system properties accordingly
		setProxySystemProperty("http.nonProxyHosts", sysPropValue);
		setProxySystemProperty("ftp.nonProxyHosts", sysPropValue);
	}

	/**
	 * (Re-)loads the proxy system properties.
	 */
	@Override
	public void load() throws IOException {
		Properties proxyConfig = ConfigurationUtil.loadConfigurationProperties(PROXY_SETTINGS_FILENAME, null);

		propsFile = new File(confDir, PROXY_SETTINGS_FILENAME);

		props = ConfigurationUtil.loadConfigurationProperties(propsFile, proxyConfig);
	}

	/**
	 * Saves the currently known settings.
	 */
	@Override
	public void save() throws IOException {
		if (!props.isEmpty()) {
			ConfigurationUtil.saveConfigurationProperties(props, propsFile, false);
		}
		ConfigurationUtil.saveConfigurationProperties(props,
				new File(propsFile.getParentFile(), propsFile.getName() + ".default"), true);

	}

	@Override
	public void destroy() throws IOException {
		// no-op
	}

	@Override
	public void init() throws IOException {
		load();

		// make sure some system properties are set properly
		setHttpProxyHost(getHttpProxyHost());
		setHttpProxyPort(getHttpProxyPort());
		setHttpsProxyHost(getHttpsProxyHost());
		setHttpsProxyPort(getHttpsProxyPort());
		setFtpProxyHost(getFtpProxyHost());
		setFtpProxyPort(getFtpProxyPort());
		setSocksProxyHost(getSocksProxyHost());
		setSocksProxyPort(getSocksProxyPort());
		setNonProxyHostsStarting(getNonProxyHostsStarting());

		save();
	}
}
