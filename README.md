# Bazel-integration-testing

**Because your build infra also needs build (and tests)**

Bazel CI  | Travis CI
:---:     | :---:
[![Build status](https://badge.buildkite.com/b0041826d71f5484c22145f44b3eac12357f51feb6ba6abb57.svg?branch=master)](https://buildkite.com/bazel/bazel-integration-testing-postsubmit) | [![Build Status](https://travis-ci.org/bazelbuild/bazel-integration-testing.svg?branch=master)](https://travis-ci.org/bazelbuild/bazel-integration-testing)


## Problem statement

Bazel-integration-testing gives confidence when developing code that is strongly dependent on Bazel.

It does so by allowing you to have **isolated, reproducible tests which run
Bazel inside of them**.  Happy path testing is often possible inside of Bazel
by adding examples of your usage, but failing cases are much harder or
impossible.  Examples for such code are Starlark plugins (both open and closed
source), tools that augment Bazel/Bazel query and Bazel itself.  This project
was inspired by Bazel's own integration tests which were in bash at the time
and were coupled inside.

## Example usage

 *  Wix uses these rules to develop and test several internal rules, macros and
    also tools that need to run `bazel query` and `bazel`.
 *  [bazel-watcher](https://github.com/bazelbuild/bazel-watcher) runs Bazel in
    its E2Es to verify its assumptions on `bazel`'s command line interface.


## Target audience

 *   Bazel Starlark rule developers (both open source and closed source).
 *   Bazel ecosystem tool developers (both open source and closed source).
 *   Bazel developers.

## Alternatives

For integration testing as of Jan'19 there is no alternative which is usable
outside of Bazel.  For some use-cases and tests one can use bazel-skylib's
[unittest](https://github.com/bazelbuild/bazel-skylib/blob/master/lib/unittest.bzl)
framework.  This is faster and might be easier to setup in the test (smaller
scope) but has the built-in limitations of unit testing where assumptions are
made about the environment.  To be clear it's usually a good idea to have a mix
of unit tests and integration tests for your feedback loop and confidence.

## Architecture

The project is built from repository rules, build rules and test drivers.  The
build rules are per language (currently JVM, go, python) and are essentially a
wrapper over the `$language_test` rule. For every Bazel version configured a
wrapper is generated along with a `test_suite` which invokes the test for each
requested version of Bazel. Each language has a specific test driver which
eases creation of scratch WORKSAPCE files, BUILD files, source files, running
of Bazel and more. Additionally, the JVM build rules also provide the
`bazel_external_dependency_archive` repository rule to support external
dependencies in the test workspace.

## Getting started

In order to use `bazel_java_integration_test`, `bazel_go_integration_test`, or
`bazel_py_integration_test`, you must add the following to your WORKSPACE file:

```python

# First pick the version you want to download.
bazel_integation_testing_version="FILLMEIN"

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "io_bazel_bazel_integation_testing",
    url = "https://github.com/bazelbuild/bazel-integation-testing/archive/%s.zip" % bazel_integation_testing_version,
    type = "zip",
    strip_prefix= "bazel-integation-testing-%s" % bazel_integation_testing_version,
    sha256 = "FILLMEIN",
)

load("@build_bazel_integration_testing//tools:repositories.bzl", "bazel_binaries")
#depend on the Bazel binaries, also accepts an array of versions
bazel_binaries()
```

For JVM tests one you need to load the extra java dependencies needed for `java_test`:

```python
load("@build_bazel_integration_testing//java:java.bzl", "bazel_java_integration_test_deps")
bazel_java_integration_test_deps()
```

## Usage

 *  [Jvm](java/README.md)  
 *  [Go](go/README.md)
 *  Python - TODO

## More info

### Which Bazel versions are supported

TODO

### How to have external repositories in your scratch workspace

TODO

### Remote execution support

We need to add more info (and also port a small configuration utility) but I'll
just add that Wix uses this library on RBE successfully for a few good months.

## State of the project

### Active development?

The project is under active development.

Because the project solves many use cases right now and unfortunately because
of capacity issues we're more in a reactive mode but we strongly encourage
feedback in the form of issues and PRs.  Note that it is easier for us to
review and assist when PRs are Small and especially when they are prefaced with
an issue describing the need and possibly iterating there on the design.  

### What is missing

 *   More documentation (for `go` and `python` as well as much better
     documentation for the workspace drivers).
 *   Adding external dependencies is possible in some cases (`http_archive`) but
     is not easy. We'd like to see if we can expand this support and make it
     easier.
 *   More adoption by rule-sets (`rules_docker`, `rules_scala`, etc)
 *   Up-to-date linting.

## Adopters

Here's a (non-exhaustive) list of companies and projects that use
`bazel-integration-testing`. Don't see yours? [You can add it in a
PR](https://github.com/bazelbuild/bazel-integration-testing/edit/master/README.md)!

* [bazel-watcher](https://github.com/bazelbuild/bazel-watcher)
* [Wix](https://www.wix.com/)
