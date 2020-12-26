[![Gitter chat](http://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg)](https://gitter.im/openzipkin/zipkin)
[![Build Status](https://github.com/openzipkin/brave-karaf/workflows/test/badge.svg)](https://github.com/openzipkin/brave-karaf/actions?query=workflow%3Atest)
[![Maven Central](https://img.shields.io/maven-central/v/io.zipkin.brave.karaf/brave-exporter.svg)](https://search.maven.org/search?q=g:io.brave.karaf%20AND%20a:brave-exporter)

Brave for Apache Karaf sets up tracing components such that tools built with Karaf needn't configure
these explicitly.

This repository includes OSGi services for tracing and reporting components.

## Artifacts
All artifacts publish to the group ID "io.zipkin.brave.karaf". We use a common
release version for all components.

### Library Releases
Snapshots are uploaded to [Sonatype](https://oss.sonatype.org/content/repositories/releases/io/zipkin/brave/karaf) which
synchronizes with [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.zipkin.brave.karaf%22)

### Library Snapshots
Snapshots are uploaded to [Sonatype](https://oss.sonatype.org/content/repositories/snapshots) after
commits to master.
