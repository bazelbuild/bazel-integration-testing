#!/bin/sh -e

# Get the path to the install base extracted during repository fetching.
MYSELF="$0"
while [ -h "${MYSELF}" ]; do
  MYSELF="$(readlink "${MYSELF}")"
done
BASEDIR="$(dirname "${MYSELF}")"

# Create a new install base symlinked to the one created during repository
# fetching. This way, Bazel can set the timestamp on this install base but
# we do not have to extact Bazel itself.
INSTALL_BASE="${TEST_TMPDIR:-${TMP:/tmp}}/bazel_install_base"
if [ ! -d "${INSTALL_BASE}" ]; then
  mkdir -p "${INSTALL_BASE}"
  for f in "${BASEDIR}/install_base"/*; do
    ln -s "$f" "${INSTALL_BASE}/$(basename "$f")"
  done
fi

# Finally run Bazel with the new intall base
"${BASEDIR}/bin/bazel-real" --install_base="${INSTALL_BASE}" "$@"
