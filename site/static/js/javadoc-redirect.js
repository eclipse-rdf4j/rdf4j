function parseSemver(version) {
  if (typeof version !== 'string') {
    return null;
  }

  const parts = version.match(/\d+/g);
  if (!parts || parts.length === 0) {
    return null;
  }

  return parts.map((part) => Number.parseInt(part, 10));
}

function compareParts(aParts, bParts) {
  const maxLength = Math.max(aParts.length, bParts.length);
  for (let i = 0; i < maxLength; i += 1) {
    const a = aParts[i] ?? 0;
    const b = bParts[i] ?? 0;
    if (a !== b) {
      return a - b;
    }
  }
  return 0;
}

function compareSemver(a, b) {
  const parsedA = parseSemver(a);
  const parsedB = parseSemver(b);

  if (!parsedA || !parsedB) {
    return 0;
  }

  return compareParts(parsedA, parsedB);
}

function findClosestVersion(requestedVersion, availableVersions) {
  const requestedParts = parseSemver(requestedVersion);
  if (!requestedParts) {
    return null;
  }

  const parsedVersions = availableVersions
    .map((version) => ({ version, parts: parseSemver(version) }))
    .filter((entry) => entry.parts)
    .sort((a, b) => compareParts(a.parts, b.parts));

  let lower = null;
  for (const entry of parsedVersions) {
    const comparison = compareParts(entry.parts, requestedParts);
    if (comparison === 0) {
      return entry.version;
    }
    if (comparison > 0) {
      return entry.version;
    }
    lower = entry.version;
  }

  return lower;
}

function isJavadocPath(pathname) {
  return typeof pathname === 'string' && pathname.startsWith('/javadoc/');
}

export function findRedirectPath(pathname, availableVersions) {
  if (!isJavadocPath(pathname)) {
    return null;
  }

  const segments = pathname.split('/');
  // ['', 'javadoc', '<version>', ...]
  const requestedVersion = segments[2];
  if (!requestedVersion) {
    return null;
  }

  const targetVersion = findClosestVersion(requestedVersion, availableVersions ?? []);
  if (!targetVersion || targetVersion === requestedVersion) {
    return null;
  }

  const nextSegments = segments.slice();
  nextSegments[2] = targetVersion;

  return nextSegments.join('/') || '/';
}

export function extractVersions(manifestEntries) {
  if (!Array.isArray(manifestEntries)) {
    return [];
  }

  return manifestEntries
    .map((entry) => (typeof entry === 'string' ? entry : entry?.name))
    .filter((value) => typeof value === 'string')
    .filter((value) => parseSemver(value));
}

export async function attemptJavadocRedirect(manifestUrl = '/javadoc/manifest.json') {
  if (typeof window === 'undefined' || typeof window.location === 'undefined') {
    return null;
  }

  const { pathname, search, hash } = window.location;
  if (!isJavadocPath(pathname)) {
    return null;
  }

  let versions = [];
  try {
    const response = await fetch(manifestUrl, { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`Unexpected response ${response.status}`);
    }
    const manifest = await response.json();
    versions = extractVersions(manifest);
  } catch (error) {
    console.warn('Unable to load javadoc manifest for redirect:', error);
    return null;
  }

  const targetPath = findRedirectPath(pathname, versions);
  if (!targetPath) {
    return null;
  }

  const nextUrl = new URL(window.location.href);
  nextUrl.pathname = targetPath;
  nextUrl.search = search;
  nextUrl.hash = hash;
  window.location.replace(nextUrl.toString());
  return nextUrl.toString();
}

if (typeof window !== 'undefined') {
  window.JavadocRedirect = {
    attemptJavadocRedirect,
    extractVersions,
    findRedirectPath,
  };
}

export default {
  attemptJavadocRedirect,
  extractVersions,
  findRedirectPath,
};
