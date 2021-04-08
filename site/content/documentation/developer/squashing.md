---
title: "Squashing Commits"
toc: true
autonumbering: true
---

When submitting a pull request to RDF4J, we sometimes ask that you squash your commits, to clean up the commit history a bit. Here we explain a simple way to do that.

<!--more-->

## Squashing in five steps

On the command line, a relatively simple way to squash commits is as follows:

1. Make sure your local `main` and `develop` branches are up to date with the upstream.
2. Check out your pull request branch.
3. Run `git rebase -i main` (or `git rebase -i develop` if your branch started from the _develop_ branch).
    - You should see a list of commits, each commit starting with the word `pick`.
   - Make sure the first commit says "pick" and change the rest from "pick" to "squash".
4. Save and close the editor.
   It will give you the opportunity to change the commit message. This is where you make sure the message is meaningful and starts with the issue number.
5. Save and close the editor again.
   Then you have to force-push the final, squashed commit: `git push --force-with-lease origin`.

## Example

Let's say you have been working on an improvement, and you have a list of five
commits on your branch, which you are now ready to merge into the
`develop` branch. The commit log of your branch looks as follows:

```
* 43d2565 (HEAD -> GH-1234-my-feature-branch) fixed typo
* ce064f9 adjusted related class FooBar to be more efficient
* b135e03 oops forgot one
* 73e58a1 GH-1234 added feature: tests now succeed
* a0178bd GH-1234 added tests for my feature
```

You have some commits that have descriptions like "fixed typo" or "oops". Some messages miss a reference to the issue number. These are the commits that you will want to adjust by squashing, and editing the commit message.

Before you start squashing, first make sure that your local `main` or `develop` branch (the branch that you want to merge your changes into) is up-to-date with the upstream:

```bash
git checkout develop
git pull
```

{{< warning >}}
If you are working on a forked copy of the eclipse/rdf4j Github repository, you will need to make sure that your fork's main/develop branch is up to date with the original as well. See <a href="https://stackoverflow.com/questions/7244321/how-do-i-update-a-github-forked-repository">this Stackoverflow article</a> for tips on how to handle this.
{{< / warning >}}

You can now switch back to your feature branch:

```bash
git checkout GH-1234-my-feature-branch
```

The second step is starting an "interactive rebase", using the following command:

```bash
git rebase -i develop
```

In this command, `develop` identifies the branch against which we
want to rebase our current branch, the `-i` flag indicates that we want to
rebase interactively (that is, being asked what commits to keep, which ones to
squash, etc), and finally the `--signoff` flag is there to make sure that the
new "squashed" commits are correctly signed off.

When you execute this, git will open an editor with the following contents:

```
pick 43d2565 fixed typo in FooBar
pick ce064f9 adjusted related class FooBar to be more efficient
pick b135e03 oops forgot one
pick 73e58a1 GH-1234 added feature: tests now succeed
pick a0178bd GH-1234 added tests for my feature

# Rebase 43d2565..a0178bd onto c0fa78d (5 command(s))
#
# Commands:
# p, pick = use commit
# r, reword = use commit, but edit the commit message
# e, edit = use commit, but stop for amending
# s, squash = use commit, but meld into previous commit
# f, fixup = like "squash", but discard this commit's log message
# x, exec = run command (the rest of the line) using shell
# d, drop = remove commit
#
# These lines can be re-ordered; they are executed from top to bottom.
#
# If you remove a line here THAT COMMIT WILL BE LOST.
#
# However, if you remove everything, the rebase will be aborted.
#
# Note that empty commits are commented out
```

Notice the first five lines: these identify your commits, and the word 'pick' in front of each indicates that you want to keep that commit as-is. To define your squash operation, you edit these lines, changing the word 'pick' into something else (usually 'squash', or 'fixup'), and then save and close the editor.

### Squashing everything down to one commit

The simplest way to squash is just to stick everything into one commit. You do this by changing the word "pick" in every line except the first one to "squash":


```
pick 43d2565 fixed typo in FooBar
squash ce064f9 adjusted related class FooBar to be more efficient
squash b135e03 oops forgot one
squash 73e58a1 GH-1234 added feature: tests now succeed
squash a0178bd GH-1234 added tests for my feature
```

After you save this change and close the editor, git will immediately open a new editor where you can make adjustments to the commit message for the new "squashed" commit. It will look like something like this:

```
# This is a combination of 5 commits.
# This is the 1st commit message:

fixed typo in FooBar

# This is the commit message #2:

adjusted related class FooBar to be more efficient

# This is the commit message #3:

oops forgot one

# This is the commit message #4:

GH-1234 added feature: tests now succeed

# This is the commit message #5:

GH-1234 added tests for my feature

# Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# Date:      Tue Oct 13 10:01:24 2020 +1100
#
# interactive rebase in progress; onto dc0fa78d
```

As you can see, it has preserved all the original commit messages and put them in this single, new commit message, each on its own line. You can now edit this commit message if you wish.

One thing you will definitely need to do is edit the first line of the commit message. This will become the commit that encapsulates the entire change you made. It should therefore contain the issue number, and also have a description of the entire fix, not just the fact that it fixes a typo. You can edit that line, getting something like this:

```
# This is a combination of 5 commits.
# This is the 1st commit message:

GH-1234 added new feature ABC and adjusted FooBar

# This is the commit message #2:

adjusted related class FooBar to be more efficient

# This is the commit message #3:

oops forgot one

# This is the commit message #4:

GH-1234 added feature: tests now succeed

# This is the commit message #5:

GH-1234 added tests for my feature

# Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# Date:      Tue Oct 13 10:01:24 2020 +1100
#
# interactive rebase in progress; onto dc0fa78
```

You can choose to keep the other commit messages in place or just remove them if
you think they add no value. In this case, they're a bit messy, so you can clean it up
a little further, removing the lines about typos and small mistakes, and making
it a bit easier to read, ending up with something like this:

```
# This is a combination of 5 commits.
# This is the 1st commit message:

GH-1234 added new feature ABC and adjusted FooBar

- added feature and tests
- adjusted related class FooBar to be more efficient

# Please enter the commit message for your changes. Lines starting
# with '#' will be ignored, and an empty message aborts the commit.
#
# Date:      Tue Oct 13 10:01:24 2020 +1100
#
# interactive rebase in progress; onto dc0fa78
```

After you save and close, the cleaned up git log of our feature branch will look like this:

```
* d01376a (HEAD -> GH-1234-my-feature-branch) GH-1234 addded new feature ABC and adjusted FooBar
```

So your local commit history now looks clean, but you still need to push these
changes to your upstream branch. Because doing a rebase like this changes the
history (you have modified existing commits, after all), you will need to
"force-push" your changes:

```bash
git push --force-with-lease origin
```

### Preserving more than one commit

Sometimes, you want to preserve more than one commit separately on your
feature branch. Let's say that instead of sticking everything in one commit, you
want to keep the changes to the FooBar class and the actual new feature + test
code separate. You can do this by leaving multiple "pick" options in place.
For example:


```
pick 43d2565 fixed typo in FooBar
squash ce064f9 adjusted related class FooBar to be more efficient
pick b135e03 oops forgot one
squash 73e58a1 GH-1234 added feature: tests now succeed
squash a0178bd GH-1234 added tests for my feature
```

The first two commits (relating to the class `FooBar`) will be squashed into one new commit, and then the further three commits will all be squashed into one as well. Note that if you do this, git will open a new commit editor _twice_, one for each new squashed commit you're adding, and you can adjust each new commit's message accordingly. You can end up with something like this:

```
* d01376a (HEAD -> GH-1234-my-feature-branch) GH-1234 adjusted FooBar for better performance
* 30467d2  GH-1234 addded new feature ABC
```

## Keeping your feature branch up to date: rebase vs merge

While you are working on a feature in your own branch, in parallel other changes
can be made on the main/develop branch, by other developers. It is sometimes
necessary that you bring your feature branch up-to-date with those changes, so
that you can reuse their work in your feature, or for example when you've both
been working on the same part of the code and there are conflicts to be
resolved.

Although in RDF4J we merge all feature branches _into_ the main branches using
merge-commits, we recommend that you use `git rebase` instead of `git merge` to
bring your feature branch up-to-date. There are two large advantages to this:

1. it makes your feature branch "shorter" (the starting point of your branch
moves up), which makes the git history easier to read once your feature branch
has been merged;
2. it makes doing squashing a lot easier. When merging in changes through
   merge-commits, _especially_ when those changes involve resolving conflicts,
   squashing later on becomes really difficult, as git will repeatedly ask you
   to resolve the same merge conflict multiple times (this is caused by the way
   rebase works - it "replays" commits one by one, and stops at each step if it
   detects a conflict, even if you have in fact already resolved that conflict
   in a later commit).

A potential disadvantage of rebasing is that, if you are working together with
another developer on the same branch, you need to inform them whenever you do a
rebase and a force-push, so that they can update their local copy. A rebase
"rewrites history", which can cause problems if your co-developer is still
working with the "unrevised history" and adds their own commits on top of that,
instead of on top of your rewritten history. The trick is to communicate with
each other.

## Phew, all of this is a lot of work, can't you make this easier?

Unfortunately, at the end of the day, we really can't: RDF4J is a large project and to be able to keep track of what changed when, and for what reason, we require a git history that is descriptive, and links commits back to the issue they tried to fix.

However, you can make things easier for yourself, with a few of these tips:

1. make sure that most of the commits you do are already descriptive in the first place, and perhaps squashing more than once if you are working on a large branch, to keep things "tidy as you go".
2. Do not immediately push every commit that you do, so that you can **amend your latest commit** if you discover a typo or other small change, rather than adding a new separate commit.
3. If you need to sync your feature branch with the main or develop branch, consider using 'git rebase' instead of 'git merge', so that squashing later on does not become too difficult.

{{< info >}}
<strong>Practicing clean git commits is like brushing your teeth</strong>: yes, it's a bit of a chore, and you could use the time to do something more fun, but it's a lot nicer for the people around you (think coffee breath), and also better for you (think having to figure out why something was changed a year from now). And once you're used to doing it, it quickly becomes a habit.
{{</ info >}}
