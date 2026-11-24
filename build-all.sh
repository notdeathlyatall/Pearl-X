#!/usr/bin/env bash
# Builds Pearl X for every supported Minecraft version.
# Requires JDK 25 (Fabric Loom 1.19.4 needs it); 1.21.x jars are still compiled with --release 21.
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
    echo "Java not found. Install JDK 25 and set JAVA_HOME." >&2
    exit 1
fi

java_major="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [ -z "${java_major:-}" ] || [ "$java_major" -lt 25 ]; then
    echo "JDK 25 or newer is required (found: ${java_major:-unknown})." >&2
    echo "Set JAVA_HOME to a JDK 25 installation and try again." >&2
    exit 1
fi

./gradlew buildAll "$@"
echo
echo "Jars written to build/dist:"
ls -1 build/dist 2>/dev/null || true
