#!/usr/bin/env sh
# Compile all Java sources into ./out (run from project root)
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p out
LIST="$(mktemp)"
find src -name "*.java" > "$LIST"
javac -d out -encoding UTF-8 @"$LIST"
rm -f "$LIST"
echo "Compiled to $ROOT/out — run: java -cp out com.hotel.oop.HotelServerApp"
