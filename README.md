Bazel-integration-testing
=================================
**Because your build infra also needs build (and tests)**

Bazel CI
:---:
[![Build status](https://badge.buildkite.com/b0041826d71f5484c22145f44b3eac12357f51feb6ba6abb57.svg?branch=master)](https://buildkite.com/bazel/bazel-integration-testing-postsubmit)


## Problem statement  
Bazel-integration-testing aims to give confidence when developing code that is strongly dependent on Bazel.  
It does so by allowing you to have **isolated reproducible tests which run Bazel inside of them**.  
Happy path testing are often possible inside of bazel by adding examples of your usage but failing cases are much harder.      
Few examples for such code can be Starlark plugins (both open and closed source), tools that augment Bazel/Bazel query and actually also Bazel itself.    
This project was inspired by Bazel's own integration tests which were in bash at the time and were coupled inside.    
Internally at Wix we use this to develop and test several internal rules, macros and also tools that need to run `bazel query` and `bazel` while bazel-watcher runs Bazel in its E2Es to verify its assumptions on `bazel run` amongst other things.
All of these set up scratch workspaces isolated to the specific tests.  
## Target audience  
* Bazel Starlark rule developers (both open source and closed source)  
* Bazel ecosystem tool developers (both open source and closed source)   
* Bazel developers  
## Alternatives
For integration testing as of Jan'19 there is no alternative which is usable outside of Bazel.  
For some use-cases and tests one can use bazel-skylib's [unittest](https://github.com/bazelbuild/bazel-skylib/blob/master/lib/unittest.bzl) framework.  
This is faster and might be easier to setup in the test (smaller scope) but has the built-in limitations of unit testing where you make assumptions about the environment.  
To be clear it's usually a good idea to have a mix of unit tests and integration tests for your feedback loop and confidence.
## Architecture  
The project is built from repository rules, build rules and test drivers.  
The build rules are per stack (currently JVM, go, python) and are essentially a wrapper over the `$stack_test` rule. For every bazel version configured such a wrapper is generated along with a `test_suite` aggregate.  
The JVM build rules are a bit more advanced in that they integrate with `bazel_external_dependency_archive` repository rule to support external dependencies in the test workspace.  
Lastly each stack has a specific test driver which eases creation of scratch WORKSAPCE files, BUILD files, source files, running of Bazel and more.                                                                                                                                                                                                 
## Getting started

In order to use `bazel_java_integration_test`, `bazel_go_integration_test`, or `bazel_py_integration_test`,
you must add the following to your WORKSPACE file:

```python

bazel_integation_testing_version="404010b3763262526d3a0e09073d8a8f22ed3d4b"

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
             name = "io_bazel_bazel_integation_testing",
             url = "https://github.com/bazelbuild/bazel-integation-testing/archive/%s.zip"%bazel_integation_testing_version,
             type = "zip",
             strip_prefix= "bazel-integation-testing-%s" % bazel_integation_testing_version,
             sha256 = "9561123fbef215f949e086067069f9dc6fa2cff31a7896c8cf16757cddd78b1f",
             )


load("@build_bazel_integration_testing//tools:repositories.bzl", "bazel_binaries")
#depend on the Bazel binaries, also accepts an array of versions
bazel_binaries()
```
For JVM tests one would like to call the below instead of the above call to `bazel_binaries` since it does that plus more dependencies needed for `java_test`:
```python
load("@build_bazel_integration_testing//tools:bazel_java_integration_test.bzl", "bazel_java_integration_test_deps")
bazel_java_integration_test_deps()
```

## Usage
### [Jvm](java/README.md)  
### Go
TODO
### Python
TODO

## More info
### Which bazel versions are supported
TODO
### How to have external repositories in your scratch workspace
TODO
### Remote execution support
We need to add more info (and also port a small configuration utility) but I'll just add that Wix uses this library on RBE successfully for a few good months.
## State of the project
### Active development?
The project is under active development.  
Because the project solves many use cases right now and unfortunately because of capacity issues we're more in a reactive mode but we strongly encourage feedback in the form of issues and PRs.  
Note that it is easier for us to review and assist when PRs are Small and especially when they are prefaced with an issue describing the need and possibly iterating there on the design.  
We hope to have more capacity in the next few months to also ramp up the missing pieces below.
### What is missing  
* More documentation (for `go` and `python` as well as much better documentation for the workspace drivers).    
* Adding external dependencies is possible in some cases (http_archive) but is not easy. We'd like to see if we can expand this support and make it easier.
* More adoption by rule-sets (rules_docker, rules_scala, etc)
* Up-to-date linting
## Adopters

Here's a (non-exhaustive) list of companies and projects that use `bazel-integration-testing`. Don't see yours? [You can add it in a PR](https://github.com/bazelbuild/bazel-integration-testing/edit/master/README.md)!

* [bazel-watcher](https://github.com/bazelbuild/bazel-watcher)
* [Wix](https://www.wix.com/)
