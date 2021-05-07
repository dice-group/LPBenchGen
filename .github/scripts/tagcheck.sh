#!/bin/sh

if git rev-parse "$1" >/dev/null 2>&1; then
  echo "exists"
  exit 1
else
  echo "$1 does not exists"
  exit 0
fi

