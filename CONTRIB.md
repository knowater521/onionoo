# Contributor Guide

Please refer to our Contributor and Java coding style documents
available in The Tor Project's wiki:
https://trac.torproject.org/projects/tor/wiki/org/teams/MetricsTeam/Documentation#guides

## Documentation overview

Onionoo needs documentation for users, service operators, and potential
contributors.

Users in this case are not so much end users, but developers of Onionoo
clients;
end users of Onionoo clients should refer to those clients'
documentation.
Service operators are people who run a public Onionoo server.
Potential contributors are developers who consider writing code for the
Onionoo server itself.

This document outlines what documentation exists or should exist for
Onionoo.

### Documentation for users

Most Onionoo users who are interested in reading its documentation are
developers who write an Onionoo client.
In addition to that there are a smaller number of people who read user
documentation to query Onionoo directly rather than using one of the
available Onionoo clients.
But for the purpose of this document, most users are interested in reading
documentation of the RESTful API that Onionoo provides, but they don't
care much about its Java sources.

#### Overview

The starting point for every user and even every potential contributor is
the overview page.
It describes what Onionoo is, lists known Onionoo clients, and points to
available documentation for users and potential contributors.
The product overview must be easily accessible, which is why it is
available via `https://metrics.torproject.org/onionoo.html`.

The sources of the product overview are available in `web/index.html`.

#### Getting Started guide

New users of the RESTful API may find it overwhelming to read through the
protocol specification before making their first request.
A tutorial or Getting Started guide would get these users up to speed
within 5 or 10 minutes by suggesting some easy requests to make and refine
towards getting a desired result.

There is a small Getting Started guide available at the bottom of
`web/protocol.html`.  Maybe it's too well hidden, but it's a start.

#### Protocol specification

Once a user knows what Onionoo provides, they can read up the details in
its protocol specification.
This document tells them what requests they can make and how to interpret
responses.
Ideally, the protocol specification makes it unnecessary to read up
further documentation of Tor descriptors.
A positive side effect of making Onionoo's protocol specification as
precise as possible is that Onionoo client developers are in a better
position to make the documentation of Onionoo clients more useful for
their users.

The sources of the protocol specification can be found in
`web/protocol.html`.

### Documentation for service operators

A service operator is a person who wants to deploy the Onionoo server.
That person wouldn't have to understand the inner workings of the Onionoo
code, but would know enough about it to set it up and handle common
problems.

#### Deployment Guide

The Deployment Guide explains how to set up a production environment for
running the Onionoo server and how to deploy Onionoo into it.

There is a somewhat outdated Deployment Guide available in the `INSTALL`
file.
It should be updated, moved to a documentation subdirectory, and linked
from the README file.

### Documentation for potential contributors

A potential contributor is a person who knows what Onionoo provides and
who wants to help write code (or documentation) for the Onionoo server.

#### README

The README (or README.md) file is the starting point for understanding
what's in the source tree.
It should contain a short description of what Onionoo is, in case the
source tree is the first time the reader gets in touch with Onionoo,
rather than the overview page.
The README file should guide the reader by telling them which document to
read next for what aspect of the Onionoo sources.

#### FAQ

FAQs might be useful for things like:

 - Maven vs. Ant: Maven has its own package manager, Ant lets us use
Debian packages;
 - why are certain Java libraries not up-to-date: most recent version
shipped with Debian stable;
 - which Java libraries could be used for Onionoo development: whatever is
in Debian stable, but be sure to ask first;

#### Design document

The design document specifies the operation of the Onionoo server without
requiring any knowledge of its source code.
The existing design document in `DESIGN` was written as part of a (failed)
project to port Onionoo from Java to Python.
This file is now outdated, and maybe should rather be deleted than
updated.
The way how Onionoo operates can also be explained as part of the source
code documentation.

#### Source code overview

A potential contributor shouldn't be forced to read through source code
files to understand how these (compiled) files interact with each other.
There should be an overview of the source code that guides the developer
where to start reading.

There is currently no such documentation available, but it may be possible
to write it using JavaDoc's `package-info.java` and have it integrated
into the generated JavaDoc output (which doesn't exist yet).

#### Source code documentation

Documenting the source code itself makes it easier for a potential
contributor to understand what it's doing.
Source code can be documented using JavaDoc comments for inclusion in the
generated JavaDoc output, or using non-JavaDoc comments to be read
together with the source code.
There should be JavaDoc comments for all interfaces and public methods.
The soon-to-be-available client API could try to provide that from
scratch.

There is currently little source code documentation in Onionoo, simply
because the code is self-explanatory!
More seriously, this needs fixing.

#### Contributor Guide

The Contributor Guide would tell potential contributors how to write,
test, and submit patches.
This includes setting up a suitable development environment using Vagrant,
writing code that conforms to the code style guidelines, up to writing
useful commit messages and coming up with a good Git history.

There is currently no Contributor Guide for Onionoo, but it would be very
useful to have one.
Once it exists it should live in the documentation subdirectory (which
doesn't exist yet).

#### Coding Rules

A Coding Rules document would contain:

 - naming rules for variables and methods,
 - JavaDoc conventions,
 - big _NO_ to uses of `System.out` and `System.err` (as soon as logging
is in place),
 - rules about when to use external APIs,
 - etc.

This document might refer to some standard Java coding guides for
defaults.

#### Documentation overview

Last but not least, this document should be part of Onionoo's
documentation.
The purpose is that contributors know what documentation exists, or should
not exist, and why.

## Versions

Onionoo uses version strings to indicate changes to the protocol or to the
server implementation.
A version string consists of three version numbers `x.y.z`:

 - `x` is the major protocol version number and is raised when previously
required fields are dropped or turned into optional fields, when request
parameters or response documents are removed, or when there are structural
changes.
 - `y` is the minor protocol version number and is raised when new fields,
request parameters, or response documents are added or optional fields are
dropped.
 - `z` is the server version number and is raised when the server
implementation is changed without affecting the protocol specification.

Changes are added to the `ChangeLog` file in the root directory.

