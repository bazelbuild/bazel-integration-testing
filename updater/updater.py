# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Generate a Skylark file containing a map of hash of bazel installers."""
from __future__ import print_function

from urllib.request import urlopen
from urllib.error import HTTPError
import sys

from absl import app
from absl import flags


_URL_FORMAT = "http://releases.bazel.build/{version}/release/bazel-{version}-{installer}{platform_name}.{extension}.sha256"
_URL_EXISTS = "http://releases.bazel.build/{version}/release/index.html"

flags.DEFINE_string("output", "bazel_hash_dict.bzl", "The output file")

flags.DEFINE_string("map_name", "BAZEL_HASH_DICT",
                    "The name of the generated map in the output file")


flags.DEFINE_string("minimum_version", "0.26.0",
                    "The lowest version of Bazel supported")

FLAGS = flags.FLAGS


# Constants
PLATFORM_NAME = "platform_name"
EXTENSION = "extension"
INSTALLER = "installer"

PLATFORMS = [{
  PLATFORM_NAME: "darwin-x86_64",
  EXTENSION: "sh",
  INSTALLER: "installer-",
}, {
  PLATFORM_NAME: "linux-x86_64",
  EXTENSION: "sh",
  INSTALLER: "installer-",
}, {
  PLATFORM_NAME: "windows-x86_64",
  EXTENSION: "zip",
  INSTALLER: "",
}]

#versions bazel team decided to skip and not cut
skipped_versions = [
  [0, 17, 0],
]

def version_exists(v):
  try:
    # Force 404 before we actually add the information
    urlopen(_URL_EXISTS.format(version = v)).read()
    return True
  except HTTPError as e:
    if e.code == 404:
        return False
    raise e

def get_all_versions():
  splitted_version = FLAGS.minimum_version.split(".")
  if len(splitted_version) != 3:
    sys.stderr.write(("Invalid version '%s', "
                      "expected a dot-separated version with 3 components "
                      "(e.g. 3.1.2)") % FLAGS.minimum_version)
    sys.exit(-1)

  version = [
      int(splitted_version[0]),
      int(splitted_version[1]),
      int(splitted_version[2])
  ]
  versions = []
  while True:
    successfully_fetched_at_leat_once = False
    v = "%s.%s.%s" % (version[0], version[1], version[2])
    print("Getting SHA-256 for version " + v)
    # Force 404 before we actually add the information
    if version_exists(v):
      versions.append(v)
      version[2] += 1
    else:
      if skipped_version(version):
        version[2] += 1
      elif version[1] == 0 and version[2] == 0:
          # 404 with the minor and the patch version both == 0 means that there is not
          # a version X.0.0 available which means we are terminated.
          # This handles the case where 1.0.0 is not released.
          return versions
      elif version[2] == 0:
        version[1] = 0
        version[0] += 1
      else:
        version[2] = 0
        version[1] += 1

def print_platforms(f, v):
  f.write("    \"%s\": {\n" % v)
  for platform in PLATFORMS:
    platform_url = _URL_FORMAT.format(version = v, **platform)
    r = urlopen( platform_url)

    content = r.read().decode("utf-8")
    components = content.split(" ", 1)

    f.write("        \"%s\": \"%s\",\n" % (platform[PLATFORM_NAME], components[0]))
    successfully_fetched_at_leat_once = True
  f.write("    },\n")

def get_hash_map(f):
  """Construct the hash map reading the release website, writing it to f."""
  for v in get_all_versions():
    print_platforms(f, v)


def skipped_version(version):
  return version in skipped_versions


def print_command_line(f):
  """Print the current command line."""
  f.write("bazel run //updater --")
  for i in range(1, len(sys.argv)):
    arg = sys.argv[i].replace("'", "'\\''")
    if arg.endswith("tools/bazel_hash_dict.bzl"):
      arg = "$(pwd)/tools/bazel_hash_dict.bzl"
    f.write(" '%s'" % (arg))

def print_hash_dict(f):
  f.write("# To update this file, please run\n# ")
  print_command_line(f)
  f.write("\n\n%s = {\n" % FLAGS.map_name)
  get_hash_map(f)
  f.write("}\n")

def main(unused_argv):
  print("Creating hash dict at ", FLAGS.output)

  if FLAGS.output == "-":
    return print_hash_dict(sys.stdout)
  else:
    with open(FLAGS.output, "w") as f:
      return print_hash_dict(f)


if __name__ == "__main__":
  app.run(main)
