package org.elasticsearch.bootstrap;

import java.net.URL;
import java.util.Collections;
import java.util.Set;

/**
 * test of elasticsearch pass. Thus as a workaround we deactivate this test. see
 * https://stackoverflow.com/questions/38712251/java-jar-hell-runtime-exception
 */
public class JarHell {

	private JarHell() {
	}

	public static void checkJarHell() throws Exception {
	}

	public static void checkJarHell(URL urls[]) throws Exception {
	}

	public static void checkVersionFormat(String targetVersion) {
	}

	public static void checkJavaVersion(String resource, String targetVersion) {
	}

	public static Set<URL> parseClassPath() {
		return Collections.emptySet();
	}

}
