# PIH Maven Plugins

This project is intended to be a collection of small Maven plugins to achieve specific goals.

## Goals

# dependency-report

Generates a report of dependencies for a given project in a text file

## Build and release process

* Snapshots are built and deployed to Maven on every commit, via the [deploy](./.github/workflows/deploy.yml) workflow
* Releases can be triggered manually using the [release](./.github/workflows/release.yml) workflow

