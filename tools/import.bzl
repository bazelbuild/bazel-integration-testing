# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

def _get_basename(url):
  return url[url.rindex("/") + 1:]

def _bazel_external_dependency_archive_impl(repository_ctx):
  build_lines = [
      "filegroup(",
      "    name = \"%s\"," % repository_ctx.name,
      "    srcs = [",
  ]

  for sha256 in repository_ctx.attr.srcs:
    urls = repository_ctx.attr.srcs[sha256]
    basename = sha256
    repository_ctx.download(
        url = urls,
        sha256 = sha256,
        output = basename,
    )
    build_lines.append("        \"%s\",  # %s" % (basename, urls[0]))

  build_lines += [
      "    ],",
      "    visibility = [\"//visibility:public\"],",
      ")",
  ]

  repository_ctx.file("BUILD", "\n".join(build_lines))

bazel_external_dependency_archive = repository_rule(
    attrs = {
        "srcs": attr.string_list_dict(allow_empty = False),
    },
    implementation = _bazel_external_dependency_archive_impl,
)
"""
Import external dependencies by their download URLs and sha256 checksums,
for Bazel 0.12.0 and above (below, it is a no-op).

```python
load(
    "@build_bazel_integration_testing//:bazel_integration_test.bzl", 
    "bazel_external_dependency_archive",
)
bazel_external_dependency_archive(
    name = "test_archive",
    srcs = {
        "90a8e1603eeca48e7e879f3afbc9560715322985f39a274f6f6070b43f9d06fe": [
            "http://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar",
        ],
    },
)
```

By specifying the filegroup `@test_archive` to, e.g., as an `external_deps` of
`bazel_java_integration_test`, the file above (`junit-4.11.jar`) is made available without network
access and without changing the import logic.  For instance, the scratch WORKSPACE could contain
the following, without downloading actually taking place:

```
load("@bazel_tools//tools/build_defs/repo:java.bzl", "java_import_external")
java_import_external(
    name = "org_junit",
    licenses = ["restricted"],  # Eclipse Public License 1.0",
    jar_urls = [
        "http://repo1.maven.org/maven2/junit/junit/4.11/junit-4.11.jar",
    ],
    jar_sha256 = "90a8e1603eeca48e7e879f3afbc9560715322985f39a274f6f6070b43f9d06fe",
)
```
"""
