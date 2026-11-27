#!/usr/bin/env sh
set -e
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle is not installed on this runner." >&2
exit 1
