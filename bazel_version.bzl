# Copied from https://github.com/tensorflow/tensorflow/blob/dfa7d2d2953c3d33d2e8deb898240be008de5be6/tensorflow/workspace.bzl

# Parse the bazel version string from `native.bazel_version`.
# e.g.
# "0.10.0rc1 abc123d" => (0, 10, 0)
# "0.3.0" => (0, 3, 0)
def _parse_bazel_version(bazel_version):
  for i in range(len(bazel_version)):
      c = bazel_version[i]
      if not (c.isdigit() or c == "."):
        bazel_version = bazel_version[:i]
        break
  return tuple([int(n) for n in bazel_version.split(".")])

def check_bazel_version(minimum_bazel_version, maximum_bazel_version=None):
  """Check that a specific bazel version is being used.

  Args:
     minimum_bazel_version: minimum version of Bazel expected
     maximum_bazel_version: maximum version of Bazel expected
  """
  if "bazel_version" not in dir(native):
    fail("\nCurrent Bazel version is lower than 0.2.1, expected at least %s\n" % minimum_bazel_version)
  elif not native.bazel_version:
    print("\nCurrent Bazel is not a release version, cannot check for compatibility.")
    print("Make sure that you are running at least Bazel %s.\n" % minimum_bazel_version)
  else:
    current_bazel_version = _parse_bazel_version(native.bazel_version)
    min_bazel_version = _parse_bazel_version(minimum_bazel_version)
    if min_bazel_version > current_bazel_version:
      fail("\nCurrent Bazel version is {}, expected at least {}\n".format(
          native.bazel_version, minimum_bazel_version))
    if maximum_bazel_version:
      max_bazel_version = _parse_bazel_version(maximum_bazel_version)
      if max_bazel_version < current_bazel_version:
        fail("\nCurrent Bazel version is {}, expected at most {}\n".format(
            native.bazel_version, maximum_bazel_version))

  pass
