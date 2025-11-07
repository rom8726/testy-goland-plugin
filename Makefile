# Testy Viewer — Makefile
# Usage examples (run from this directory):
#   make build            # Build plugin distribution ZIP under build/distributions/
#   make run              # Run GoLand IDE with the plugin for manual testing
#   make test             # Run unit tests (if any)
#   make verify           # Verify plugin (plugin verifier)
#   make publish TOKEN=xxxxxxxx CHANNEL=default  # Publish to JetBrains Marketplace
#
# Notes:
# - This Makefile expects a Gradle wrapper at project root (../../gradlew).
#   If it is not present, it will fall back to system 'gradle' on PATH.
# - Publishing requires a JetBrains Marketplace token.

SHELL := /bin/bash

# Resolve Gradle executable: prefer root wrapper, otherwise use system gradle
ROOT_GRADLEW := ./gradlew
GRADLE_BIN   := $(if $(wildcard $(ROOT_GRADLEW)),$(ROOT_GRADLEW),gradle)
# Always execute Gradle in this module directory
GRADLE := $(GRADLE_BIN) -p .

# Default channel to publish to. You can override: make publish CHANNEL=stable
CHANNEL ?= default

# Colors
Y := \033[33m
G := \033[32m
R := \033[31m
Z := \033[0m

.PHONY: help build run test verify clean dist publish paths

help:
	@echo "Testy Viewer — Gradle commands"
	@echo "  make build      — Build plugin ZIP (build/distributions)"
	@echo "  make run        — Run GoLand with the plugin"
	@echo "  make test       — Run tests"
	@echo "  make verify     — Run plugin verification"
	@echo "  make dist       — Alias to build"
	@echo "  make clean      — Clean build outputs"
	@echo "  make publish    — Publish to Marketplace (requires TOKEN)"
	@echo "                     Usage: make publish TOKEN=xxxx CHANNEL=$(CHANNEL)"
	@echo "  make paths      — Show resolved Gradle executable and output dir"

paths:
	@echo -e "$(Y)Gradle:$(Z) $(GRADLE_BIN)"
	@echo -e "$(Y)Module:$(Z) $$(pwd)"
	@echo -e "$(Y)Distributions:$(Z) build/distributions"

build:
	@echo -e "$(G)[build]$(Z) Building plugin ZIP..."
	@$(GRADLE) --no-daemon buildPlugin
	@echo -e "$(G)[build]$(Z) Done."

# Alias

dist: build

run:
	@echo -e "$(G)[run]$(Z) Launching IDE with the plugin..."
	@$(GRADLE) --no-daemon runIde

verify:
	@echo -e "$(G)[verify]$(Z) Verifying plugin..."
	@$(GRADLE) --no-daemon verifyPlugin

test:
	./gradlew -p . --no-daemon unitTest

clean:
	@echo -e "$(G)[clean]$(Z) Cleaning..."
	@$(GRADLE) --no-daemon clean

# Publish to JetBrains Marketplace.
# Requires environment or make variable TOKEN to be set.
# Channel can be overridden: make publish TOKEN=xxx CHANNEL=stable
publish:
	@if [[ -z "$$TOKEN" ]]; then \
	  echo -e "$(R)[publish] Missing TOKEN. Set as: make publish TOKEN=xxxxxxxx CHANNEL=$(CHANNEL)$(Z)"; \
	  exit 1; \
	echo; fi
	@echo -e "$(G)[publish]$(Z) Publishing to channel: $(CHANNEL)"
	@$(GRADLE) --no-daemon \
	  publishPlugin \
	  -Pintellij.publish.token=$$TOKEN \
	  -Pintellij.publish.channels=$(CHANNEL)
	@echo -e "$(G)[publish]$(Z) Done."
