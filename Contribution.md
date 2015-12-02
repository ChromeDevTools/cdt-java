# Contributing to the project #

We welcome any kind of contribution to our project. This could be a simple bug report, a thought-out new feature design, or even a code patch.

## How to send a patch ##

We use the same code review site as the Chromium project: http://codereview.chromium.org/

You have several options on how to upload your patch for review:
  * the [web interface](http://codereview.chromium.org/new) (this seems the least convenient option, as you have to prepare a patch manually)
  * the upload.py Python script (if your working files reside in an SVN checkout)
  * the [git-cl](http://neugierig.org/software/git/?r=git-cl) utility (if your working files reside in a GIT-SVN repository). The Chromium [Git Code Reviews](http://code.google.com/p/chromium/wiki/UsingGit#Code_Reviews) section gives you a hint or two on how to use git-cl for code reviews.

### Using `upload.py` for your patch work ###
The script assumes that you have your _chromedevtools_ SVN checkout made with the command-line svn (the Eclipse SVN plugin may not work here). See the [Source Checkout](http://code.google.com/p/chromedevtools/source/checkout) hint for this project.

1. Make sure you have an account at http://codereview.chromium.org/.

2. Download the `upload.py` script [here](http://codereview.chromium.org/static/upload.py).

3. Prepare all your changes as if you were going to run `svn commit` (e.g. do all the `svn add` work).

4. Run the `upload.py` script (make sure it is in your PATH):
```
upload.py -s codereview.chromium.org
```
The script will ask you for the name of the patch, your codereview.chromium.org email and password (the latter two get cached). If the command succeeds, you will get a URL of your newly created issue (like http://codereview.chromium.org/1707006).

**NB!** This _issue_ (== patch under review) is not the same as what is called _Issues_ (== problem reports) on code.google.com.

5. To update your patch (rather than create a new one), run
```
./upload.py -s codereview.chromium.org -i <issue_number>
```