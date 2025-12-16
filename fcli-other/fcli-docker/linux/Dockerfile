# This Dockerfile defines the following targets for building fcli Docker images:
# - fcli-alpine
# - fcli-ubi9
# - fcli-scratch
# You can select between these variants by passing the --target option to the 'docker build'
# command, for example 'docker build . --target fcli-alpine'. If no target is specified, all
# targets may be built but the final image will correspond to the fcli-scratch target.
#
# In general, the fcli-scratch image is preferred to avoid having to deal with any CVEs reported
# on the base images, and potentially having to provide refreshed images for older fcli versions
# to fix such CVEs.
#
# For now, we automatically publish both the fcli-scratch and fcli-ubi9 images. The scratch image
# has minimal attack surface, while the UBI9 image provides a shell-based environment for running
# multiple fcli commands in sequence. If there's demand, we can also publish alpine and/or other
# (like busybox) images that include a shell. Primary advantage is that this would allow for running
# a sequence of fcli commands in a single container invocation (potentially removing the need to
# mount the data directory to a persistent volume/host directory, as fcli state will be stored inside
# the container for as long the container is alive). Primary disadvantage is that users might expect
# to be able to run something like the following:
#   fcli tool sc-client install
#   scancentral package ...
#   submit scan request, ...
# However, these images don't include Java (required to run ScanCentral Client) nor any other
# build tools (which may be required by ScanCentral Client to properly package application source
# code). Users would need to build a custom image that includes fcli and any other tools required
# for building/packaging an application, or alternatively any of the fcli Docker images can be
# used to install ScanCentral Client on the host system (using proper volume mappings), from
# where it can be run with (presumably) all relevant build tools and Java being available.
# 
# Build Arguments:
# - FCLI_VERSION: fcli version to download (required, e.g., "v3.14.0")
# - ALPINE_BASE: Alpine base image (default: alpine:3.23.0)
# - UBI_BASE: Red Hat UBI base image (default: redhat/ubi9:9.7)
#
# All of the targets defined below have more or less the same structure:
# - Add fcli user to /etc/passwd
# - Add fcli executable to the image
# - Add /data (and /tmp for fcli-scratch) directories with 777 permissions to the image
# - Set WORKDIR to /data
# - Set FCLI_USER_HOME to /data
# - Set container user to fcli user
# - Define an appropriate entrypoint (fcli for fcli-scratch, shell for others)
#
# Regarding FCLI_USER_HOME, fcli uses user.home by default, but if the Docker container is being
# run as a different user through 'docker run -u <UID>:<GID>', user.home returns a directory 
# containing a '?' to represent unknown user name for the given UID, which causes issues. Fcli
# versions 2.5.4 and up allow for overriding user.home through FCLI_USER_HOME.

# ============================================================================
# Global build arguments (must be before any FROM to use in FROM statements)
# ============================================================================
ARG ALPINE_BASE=alpine:3.23.0
ARG UBI_BASE=redhat/ubi9:9.7

# ============================================================================
# Stage: fcli-downloader
# Downloads fcli binary and verifies signature
# ============================================================================
FROM alpine:3.23.0 AS fcli-downloader

# Install dependencies for download and signature verification
RUN apk add --no-cache curl openssl ca-certificates

# Build arguments
ARG FCLI_VERSION
ARG TARGETPLATFORM

# Validate FCLI_VERSION is provided
RUN test -n "$FCLI_VERSION" || (echo "ERROR: FCLI_VERSION build argument is required" && exit 1)

# Fortify public key for signature verification
RUN mkdir -p /tmp/keys && \
    echo "-----BEGIN PUBLIC KEY-----" > /tmp/keys/fortify.pub && \
    echo "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArij9U9yJVNc53oEMFWYp" >> /tmp/keys/fortify.pub && \
    echo "NrXUG1UoRZseDh/p34q1uywD70RGKKWZvXIcUAZZwbZtCu4i0UzsrKRJeUwqanbc" >> /tmp/keys/fortify.pub && \
    echo "woJvYanp6lc3DccXUN1w1Y0WOHOaBxiiK3B1TtEIH1cK/X+ZzazPG5nX7TSGh8Tp" >> /tmp/keys/fortify.pub && \
    echo "/uxQzUFli2mDVLqaP62/fB9uJ2joX9Gtw8sZfuPGNMRoc8IdhjagbFkhFT7WCZnk" >> /tmp/keys/fortify.pub && \
    echo "FH/4Co007lmXLAe12lQQqR/pOTeHJv1sfda1xaHtj4/Tcrq04Kx0ZmGAd5D9lA92" >> /tmp/keys/fortify.pub && \
    echo "8pdBbzoe/mI5/Sk+nIY3AHkLXB9YAaKJf//Wb1yiP1/hchtVkfXyIaGM+cVyn7AN" >> /tmp/keys/fortify.pub && \
    echo "VQIDAQAB" >> /tmp/keys/fortify.pub && \
    echo "-----END PUBLIC KEY-----" >> /tmp/keys/fortify.pub

# Determine platform-specific archive name
RUN case "$TARGETPLATFORM" in \
      "linux/amd64") ARCHIVE="fcli-linux.tgz" ;; \
      "linux/arm64") ARCHIVE="fcli-linux.tgz" ;; \
      *) echo "Unsupported platform: $TARGETPLATFORM" && exit 1 ;; \
    esac && \
    echo "$ARCHIVE" > /tmp/archive_name

# Download fcli and signature
RUN ARCHIVE=$(cat /tmp/archive_name) && \
    DOWNLOAD_URL="https://github.com/fortify/fcli/releases/download/${FCLI_VERSION}/${ARCHIVE}" && \
    SIGNATURE_URL="${DOWNLOAD_URL}.rsa_sha256" && \
    echo "Downloading fcli from: $DOWNLOAD_URL" && \
    curl -fsSL "$DOWNLOAD_URL" -o "/tmp/${ARCHIVE}" && \
    echo "Downloading signature from: $SIGNATURE_URL" && \
    curl -fsSL "$SIGNATURE_URL" -o "/tmp/${ARCHIVE}.rsa_sha256"

# Verify signature
RUN ARCHIVE=$(cat /tmp/archive_name) && \
    echo "Verifying signature..." && \
    openssl dgst -sha256 -verify /tmp/keys/fortify.pub \
      -signature "/tmp/${ARCHIVE}.rsa_sha256" "/tmp/${ARCHIVE}" && \
    echo "✓ Signature verification successful"

# Extract fcli binary
RUN ARCHIVE=$(cat /tmp/archive_name) && \
    mkdir -p /tmp/fcli-bin && \
    tar -xzf "/tmp/${ARCHIVE}" -C /tmp/fcli-bin && \
    chmod +x /tmp/fcli-bin/fcli && \
    /tmp/fcli-bin/fcli --version

# ============================================================================
# Stage: fcli-alpine
# Alpine-based image with shell access
# ============================================================================
FROM ${ALPINE_BASE} AS fcli-alpine

# Bring build args into this stage
ARG FCLI_VERSION
ARG ALPINE_BASE
LABEL org.opencontainers.image.source="https://github.com/fortify/fcli" \
      org.opencontainers.image.vendor="Fortify" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.documentation="https://github.com/fortify/fcli#readme" \
      org.opencontainers.image.title="Fortify CLI (Alpine)" \
      org.opencontainers.image.description="Fortify CLI on Alpine Linux base image" \
      org.opencontainers.image.version="${FCLI_VERSION}" \
      com.fortify.fcli.version="${FCLI_VERSION}" \
      com.fortify.base.image="${ALPINE_BASE}"

RUN echo "fcli:x:10001:10001:fcli:/data:/sbin/nologin" >> /etc/passwd
COPY --from=fcli-downloader /tmp/fcli-bin/fcli /bin/fcli
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
CMD ["/bin/sh"]

# ============================================================================
# Stage: fcli-ubi9
# Red Hat UBI9 standard image with bash and package manager
# Uses standard ubi9 (not minimal) to allow sub-images to install packages
# ============================================================================
FROM ${UBI_BASE} AS fcli-ubi9

# Bring build args into this stage
ARG FCLI_VERSION
ARG UBI_BASE
LABEL org.opencontainers.image.source="https://github.com/fortify/fcli" \
      org.opencontainers.image.vendor="Fortify" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.documentation="https://github.com/fortify/fcli#readme" \
      org.opencontainers.image.title="Fortify CLI (UBI9)" \
      org.opencontainers.image.description="Fortify CLI on Red Hat Universal Base Image 9" \
      org.opencontainers.image.version="${FCLI_VERSION}" \
      com.fortify.fcli.version="${FCLI_VERSION}" \
      com.fortify.base.image="${UBI_BASE}"

RUN echo "fcli:x:10001:10001:fcli:/data:/sbin/nologin" >> /etc/passwd
COPY --from=fcli-downloader /tmp/fcli-bin/fcli /bin/fcli
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
CMD ["/bin/bash"]

# ============================================================================
# Stage: fcli-scratch
# Minimal scratch-based image (default/preferred)
# ============================================================================
FROM scratch AS fcli-scratch

# OCI labels - common across all images
ARG FCLI_VERSION
LABEL org.opencontainers.image.source="https://github.com/fortify/fcli" \
      org.opencontainers.image.vendor="Fortify" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.documentation="https://github.com/fortify/fcli#readme" \
      org.opencontainers.image.title="Fortify CLI (Scratch)" \
      org.opencontainers.image.description="Fortify CLI on minimal scratch image" \
      org.opencontainers.image.version="${FCLI_VERSION}" \
      com.fortify.fcli.version="${FCLI_VERSION}" \
      com.fortify.base.image="scratch"

ADD minimal-passwd /etc/passwd
COPY --from=fcli-downloader /tmp/fcli-bin/fcli /bin/fcli
ADD tmp.tgz /
ADD data.tgz /
WORKDIR /data
ENV FCLI_USER_HOME=/data
USER fcli
ENTRYPOINT ["/bin/fcli"]
