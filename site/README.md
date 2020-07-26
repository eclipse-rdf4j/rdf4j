# Eclipse RDF4J Hugo website 

This directory contains the source of the official RDF4J project website at
https://rdf4j.org/, including all technical documentation.

The RDF4J project website is generated using the [Hugo](https://gohugo.io/) static website generator, and uses the [Eclipse Foundation Solstice Theme](https://github.com/EclipseFdn/hugo-solstice-theme).

The solstice theme is included as a git submodule. To check out a local copy, run 

    git submodule update --init --recursive
  
You should only need to do this once after cloning the rdf4j repository.

Most website content can be found in the `content/` directory, as (Goldmark)
Markdown files. We allow inclusion of raw html as well, but please use this
sparingly: we prefer use of markdown to make future maintenance as easy as
possible.

To generate a site locally for easy editing/debugging, simply execute

    hugo server --disableFastRender

A local copy of the website will be accessible on http://localhost:1313/. You
can edit the content using your text editor of choice, the hugo server will
automatically re-render the pages you edit as you go.
