# How to contribute

Spotted a typo? Want to improve the reference docs or even add new tutorials? That's great, we welcome all contributions to our documentation! 
Before you dive in, here are some things you need to know.

**Table of Contents**  

- [Legal stuff](#legal-stuff)
- [Creating your contribution](#creating-your-contribution)
	
## Legal stuff

RDF4J is a project governed by the [Eclipse Foundation](http://www.eclipse.org/), which has strict [policies and guidelines](https://wiki.eclipse.org/Development_Resources#Policies_and_Guidelines) regarding contributions.

In order for any contributions to the RDF4J documentation to be accepted, you MUST do the following things:

1. Digitally sign the [Eclipse Foundation Contributor License Agreement](http://www.eclipse.org/legal/CLA.php).
To sign the Eclipse CLA you need to:

  * Obtain an Eclipse Foundation userid. If you already use Eclipse Bugzilla or Gerrit you already have one of those. If you don’t, you need to
[register](https://dev.eclipse.org/site_login/createaccount.php).

  * Login into the [projects portal](https://projects.eclipse.org/), select “My Account”, and then the “Contributor License Agreement” tab. Read through, then sign.

2. Add your github username in your Eclipse Foundation account settings. Log in it to Eclipse and go to account settings.

3. "Sign-off" your commits

Every commit you make in your patch or pull request MUST be "signed off".
You do this by adding the `-s` flag when you make the commit(s).

## Creating your contribution

Once the legalities are out of the way you can dig in. Here's how:

1. Fork the `rdf4j-doc` repository on GitHub.
2. Create a new branch for your changes starting from the `master` branch.
3. Make your changes. Please make sure you use valid [AsciiDoc syntax](http://asciidoctor.org/docs/asciidoc-syntax-quick-reference/).
4. Commit your changes into the branch. Use meaningful commit messages. **Sign off** every commit you do (as explained in the [legal stuff](#legal-stuff).
5. Optionally squash your commits (not necessary, but if you want to clean your commit history a bit, _this_ is the point to do it).
6. Push your changes to your branch in your forked repository.
7. If your contribution is complete, use GitHub to submit a pull request (PR)
	for your contribution back to `master` in the central rdf4j-doc repository. In the PR description, please outline
  what you've improved/changed and why. 

Once you've put up a PR, we will review your contribution, possibly make some
suggestions for improvements, and once everything is complete it will be merged
into the `master` branch, to be included in the update of our online documentation.
