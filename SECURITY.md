# Security Policy

Eclipse RDF4J adheres to the [Eclipse Foundation Vulnerability Reporting Policy](https://www.eclipse.org/security/policy/).

## How To Report a Vulnerability

If you think you have found a vulnerability in this repository, please report it to us through coordinated disclosure.

**Please do not report security vulnerabilities through public issues, discussions, or change requests.**

Create a [confidential issue](https://gitlab.eclipse.org/security/vulnerability-reports/-/work_items/new?issue[confidential]=true) in the Eclipse Foundation Vulnerability Reporting Tracker.

You can find more information about reporting and disclosure at the [Eclipse Foundation Security page](https://www.eclipse.org/security/).

Please include as much of the information listed below as you can to help us better understand and resolve the issue:

* The type of issue (e.g., buffer overflow, SQL injection, or cross-site scripting)
* Affected version(s)
* Impact of the issue, including how an attacker might exploit the issue
* Step-by-step instructions to reproduce the issue
* The location of the affected source code (tag/branch/commit or direct URL)
* Full paths of source file(s) related to the manifestation of the issue
* Configuration required to reproduce the issue
* Log files that are related to this issue (if possible)
* Proof-of-concept or exploit code (if possible)

This information will help us triage your report more quickly.

## Supported Versions

Eclipse RDF4J supports security updates for the following releases:


| Version | Supported          |
| ------- | ------------------ |
| current release   | ✅ |
| latest minor release before the current   | ✅(on request only) |
| latest major release before the current   | ✅(on request only) |
| anything older   | :x:                |

For example if the current release is 4.1, we support security patches for 4.1.x (the current release) and 4.0.x (latest minor before current), as well as for 3.7.x (latest major before current), but not for 3.6.x or older. Security patches for the current release are provided proactively by the team, while patches for older supported releases are provided on request only.
