#!/bin/bash

set -e

MODULE=$1
VERSION=$2

if [[ -z "$MODULE" || -z "$VERSION" ]]; then
  echo "❌ Usage: ./tag-release.sh <module-name> <version>"
  echo "   e.g.: ./tag-release.sh config-server 1.0.1"
  exit 1
fi

TAG="${MODULE}-v${VERSION}"

# Pastikan berada di root repo Git
if [[ ! -d .git ]]; then
  echo "❌ Script must be run from the root of a Git repository."
  exit 1
fi

# Tambahkan perubahan terakhir jika ada
git add $MODULE
git commit -m "🔖 Release ${MODULE} version ${VERSION}" || echo "ℹ️ No changes to commit"

# Buat tag
git tag "$TAG"

# Push commit dan tag
git push origin HEAD
git push origin "$TAG"

echo "✅ Tagged ${MODULE} as ${TAG}"
