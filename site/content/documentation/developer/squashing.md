---
title: "Squashing Commits"
layout: "doc"
hide_page_title: "false"
---

When submitting a pull request to RDF4J, we sometimes ask that you squash your commits before we merge.

On the command line, the process is as follows:

1. Make sure your local _master_ and _develop_ branches are up to date with the upstream.
2. Check out your pull request branch. 
3. Run `git rebase -i master` (or `git rebase -i develop` if your PR is against the _develop_ branch).  
   You should see a list of commits, each commit starting with the word `pick`.
   Make sure the first commit says "pick" and change the rest from "pick" to "squash". 
4. Save and close the editor.
   It will give you the opportunity to change the commit message.
5. Save and close the editor again.
   Then you have to force push the final, squashed commit: `git push --force-with-lease origin`.

