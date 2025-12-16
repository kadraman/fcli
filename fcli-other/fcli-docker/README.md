# fcli Docker Images

This directory contains Dockerfiles and build configurations for creating fcli Docker images.

## Available Images

### Published Images (Docker Hub: `fortifydocker/fcli`)

#### 1. **fcli-scratch** (Primary/Recommended)
- **Base:** `scratch` (minimal, no OS layer)
- **Size:** ~15-20 MB
- **Use case:** Single command execution, CI/CD pipelines
- **Security:** Minimal attack surface, no CVEs from base OS
- **Shell:** None (direct fcli entrypoint)

```bash
# Run single command
docker run --rm fortifydocker/fcli:latest --version

# With volume mount for data persistence
docker run --rm -v $(pwd)/data:/data fortifydocker/fcli:latest tool sc-client install
```

#### 2. **fcli-ubi9** (Shell-based)
- **Base:** Red Hat Universal Base Image 9 (standard, not minimal)
- **Size:** ~200-250 MB
- **Use case:** Interactive usage, shell scripts, base for custom images requiring additional packages
- **Security:** Red Hat maintained base, regular security updates
- **Package Manager:** yum/dnf available for installing additional tools
- **Shell:** `/bin/bash`

```bash
# Interactive shell
docker run -it --rm fortifydocker/fcli:latest-ubi9 /bin/bash

# Run multiple commands
docker run --rm fortifydocker/fcli:latest-ubi9 bash -c "fcli --version && fcli tool list"
```

### Test-Only Images (Not Published)

#### 3. **fcli-alpine**
- Built and tested in CI/CD but not published
- **Base:** Alpine Linux
- **Shell:** `/bin/sh`
- Can be built locally if needed

#### 4. **fcli-windows**
- Built and tested on Windows runners
- **Base:** Windows Server Core ltsc2022
- **Shell:** PowerShell
- Not published; prototype only
- Provides full PowerShell and package management capability

## Image Tags

| Tag Pattern | Description | Example |
|-------------|-------------|---------|
| `latest` | Latest stable release (scratch) | `fortifydocker/fcli:latest` |
| `{version}` | Specific version (scratch) | `fortifydocker/fcli:3.14.0` |
| `{version}-ubi9` | Specific version (UBI9) | `fortifydocker/fcli:3.14.0-ubi9` |
| `{version}-{date}` | Republished with updated base images | `fortifydocker/fcli:3.14.0-20251216` |
| `{version}-ubi9-{date}` | UBI9 with updated base images | `fortifydocker/fcli:3.14.0-ubi9-20251216` |

## Building Locally

### Prerequisites
- Docker 20.10+ or Docker Desktop
- Docker Buildx (for multi-platform builds)

### Linux Images

```bash
cd fcli-other/fcli-docker/linux

# Build scratch image (default)
docker build . \
  --build-arg FCLI_VERSION=v3.14.0 \
  --target fcli-scratch \
  -t fcli:scratch

# Build UBI9 image
docker build . \
  --build-arg FCLI_VERSION=v3.14.0 \
  --target fcli-ubi9 \
  -t fcli:ubi9

# Build Alpine image
docker build . \
  --build-arg FCLI_VERSION=v3.14.0 \
  --target fcli-alpine \
  -t fcli:alpine

# Build with custom base images
docker build . \
  --build-arg FCLI_VERSION=v3.14.0 \
  --build-arg ALPINE_BASE=alpine:3.23.0 \
  --build-arg UBI_BASE=redhat/ubi9:9.7 \
  --target fcli-scratch \
  -t fcli:scratch
```

### Windows Images

```powershell
cd fcli-other/fcli-docker/windows

docker build . `
  --build-arg FCLI_VERSION=v3.14.0 `
  --target fcli-ltsc2022 `
  -t fcli:windows
```

## Usage Examples

### Basic Command Execution

```bash
# Check version
docker run --rm fortifydocker/fcli:latest --version

# List available tools
docker run --rm fortifydocker/fcli:latest tool list

# Get help
docker run --rm fortifydocker/fcli:latest --help
```

### Data Persistence

```bash
# Create data directory
mkdir -p ./fcli-data

# Install tools with persistent storage
docker run --rm \
  -v $(pwd)/fcli-data:/data \
  fortifydocker/fcli:latest \
  tool sc-client install

# Tools are now available in ./fcli-data/fortify/tools/
ls -la ./fcli-data/fortify/tools/
```

### CI/CD Integration

#### GitHub Actions

```yaml
jobs:
  scan:
    runs-on: ubuntu-latest
    container:
      image: fortifydocker/fcli:latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Run fcli commands
        run: |
          fcli --version
          fcli tool sc-client install
```

#### GitLab CI

```yaml
scan:
  image: fortifydocker/fcli:latest-ubi9
  script:
    - fcli --version
    - fcli tool sc-client install
```

#### Jenkins Pipeline

```groovy
pipeline {
    agent {
        docker {
            image 'fortifydocker/fcli:latest'
            args '-v $HOME/.fortify:/data'
        }
    }
    stages {
        stage('Setup') {
            steps {
                sh 'fcli --version'
                sh 'fcli tool sc-client install'
            }
        }
    }
}
```

### Running as Different User

```bash
# Run as specific UID/GID
docker run --rm \
  -u $(id -u):$(id -g) \
  -v $(pwd)/data:/data \
  fortifydocker/fcli:latest \
  tool list

# The FCLI_USER_HOME environment variable handles user home directory resolution
```

### Interactive Shell (UBI9 only)

```bash
# Start interactive bash session
docker run -it --rm \
  -v $(pwd)/data:/data \
  fortifydocker/fcli:latest-ubi9 \
  /bin/bash

# Inside container:
fcli --version
fcli tool sc-client install
fcli tool list

# UBI9 has package manager - install additional tools if needed
yum install -y jq
exit
```

## Architecture & Security

### Signature Verification

All Docker builds:
1. Download fcli binary from GitHub releases
2. Download corresponding `.rsa_sha256` signature file
3. Verify signature using Fortify's public RSA key
4. Build fails if signature verification fails

Public key is embedded in Dockerfiles and matches the key used by `fortify-setup-js`.

### Multi-Stage Build Process

```
┌─────────────────────────┐
│  fcli-downloader        │  Alpine-based stage
│  - Downloads fcli       │  - Installs curl, openssl
│  - Verifies signature   │  - Validates signature
│  - Extracts binary      │  - Outputs /tmp/fcli-bin/fcli
└─────────┬───────────────┘
          │
          ├────────────────────────────────┐
          │                                │
┌─────────▼─────────┐   ┌────────────▼────────────┐
│  fcli-scratch     │   │  fcli-ubi9              │
│  Copies from      │   │  Standard UBI9 (not     │
│  downloader       │   │  minimal) for package   │
└───────────────────┘   │  installation support   │
                        └─────────────────────────┘
```

### Security Features

- ✅ **Non-root user:** All images run as UID 10001 (user `fcli`)
- ✅ **Signature verification:** RSA SHA256 verification before build
- ✅ **Minimal attack surface:** Scratch image has no OS layer
- ✅ **Pinned base images:** Default base images are pinned to specific versions
- ✅ **SBOM & Provenance:** GitHub Actions build includes SBOM and attestation
- ✅ **No secrets:** No credentials or tokens embedded in images

### OCI Labels

All images include standard OCI labels:
- `org.opencontainers.image.source`
- `org.opencontainers.image.version`
- `org.opencontainers.image.vendor`
- `org.opencontainers.image.licenses`
- `org.opencontainers.image.documentation`
- `com.fortify.fcli.version` - fcli version included
- `com.fortify.base.image` - Base image used for build

```bash
# Inspect labels
docker inspect fortifydocker/fcli:latest | jq '.[0].Config.Labels'
```

## Build Arguments

### Linux Dockerfile

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `FCLI_VERSION` | Yes | - | fcli release tag (e.g., `v3.14.0`) |
| `ALPINE_BASE` | No | `alpine:3.23.0` | Alpine base image for downloader and alpine target |
| `UBI_BASE` | No | `redhat/ubi9:9.7` | Red Hat UBI9 standard base image (not minimal) |

### Windows Dockerfile

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `FCLI_VERSION` | Yes | - | fcli release tag (e.g., `v3.14.0`) |
| `SERVERCORE_BASE` | No | `mcr.microsoft.com/windows/servercore:ltsc2022` | Windows Server Core for VC++ install |
| `NANOSERVER_BASE` | No | `mcr.microsoft.com/windows/nanoserver:ltsc2022` | Windows Nano Server for final image |

## CI/CD Workflow

The `.github/workflows/docker.yml` workflow provides:

- **Automated builds:** Triggered via workflow_dispatch
- **Multi-image support:** Builds scratch, UBI9, Alpine (test), and Windows (test)
- **Signature verification:** Built into Dockerfile
- **Automated testing:** Tests tool installation in each image
- **SBOM generation:** Provenance and SBOM attestation
- **Base image updates:** Support for republishing with updated base images
- **Selective publishing:** Publishes only scratch and UBI9 to Docker Hub

### Triggering Builds

```bash
# Via GitHub CLI
gh workflow run docker.yml \
  -f releaseTag=v3.14.0 \
  -f doPublish=true \
  -f alpineBase=alpine:3.23.0 \
  -f ubiBase=redhat/ubi9:9.7

# Update base images for existing release
gh workflow run docker.yml \
  -f releaseTag=v3.14.0 \
  -f doPublish=true \
  -f updateBaseImages=true \
  -f ubiBase=redhat/ubi9:9.7
```

## Maintenance

### Updating Base Images

Base images should be updated periodically for security patches:

1. **Check for updates:**
   - Alpine: https://hub.docker.com/_/alpine
   - Red Hat UBI9: https://catalog.redhat.com/software/containers/ubi9/ubi/615bcf606feffc5384e8452e

2. **Test locally:**
   ```bash
   docker build . \
     --build-arg FCLI_VERSION=v3.14.0 \
     --build-arg ALPINE_BASE=alpine:3.23.0 \
     --build-arg UBI_BASE=redhat/ubi9:9.7 \
     --target fcli-scratch
   ```

3. **Update defaults in Dockerfile** or **trigger workflow with custom bases**

4. **Republish existing fcli version** if needed:
   ```bash
   gh workflow run docker.yml \
     -f releaseTag=v3.14.0 \
     -f doPublish=true \
     -f updateBaseImages=true \
     -f ubiBase=redhat/ubi9:9.7
   ```
   This creates tags like `3.14.0-20251216` and `3.14.0-ubi9-20251216`

### Testing Checklist

Before publishing:
- [ ] Build completes successfully
- [ ] Signature verification passes
- [ ] `fcli --version` works in container
- [ ] `fcli tool sc-client install` succeeds with volume mount
- [ ] Installed tools are accessible in mounted volume
- [ ] Container runs as non-root user
- [ ] Image size is reasonable

## Troubleshooting

### Signature Verification Fails

```
ERROR: Signature verification failed
```

**Cause:** Downloaded fcli binary doesn't match signature.

**Solution:**
- Verify `FCLI_VERSION` matches an existing GitHub release
- Check network connectivity
- Verify Fortify public key in Dockerfile is up-to-date

### Permission Denied on Volume Mount

```
ERROR: Permission denied writing to /data
```

**Cause:** Host directory permissions don't allow container user (UID 10001).

**Solution:**
```bash
# Run as your user
docker run --rm -u $(id -u):$(id -g) -v $(pwd)/data:/data fortifydocker/fcli:latest tool list

# Or fix host directory permissions
chmod 777 data
```

### Windows Image: Missing DLLs

```
ERROR: vcruntime140.dll not found
```

**Cause:** Required VC++ runtime DLLs not copied correctly.

**Solution:**
- Rebuild image (DLLs are copied from vcredist-installer stage)
- Add missing DLLs to COPY list in Dockerfile if fcli requires additional ones
- Check that VC++ redistributable installed successfully

### fcli Tool Installation Fails

```
ERROR: Cannot write to /data/fortify/tools
```

**Cause:** Volume mount permissions or path issues.

**Solution:**
```bash
# Ensure volume mount is correct
docker run --rm -v $(pwd)/data:/data fortifydocker/fcli:latest tool sc-client install

# Check that /data exists and is writable
docker run --rm -v $(pwd)/data:/data fortifydocker/fcli:latest-ubi9 ls -la /data
```

## Contributing

When modifying Dockerfiles:
1. Maintain security best practices (non-root user, signature verification)
2. Keep images minimal
3. Test all targets before committing
4. Update this README with any new features or changes
5. Pin base images to specific versions
6. Add appropriate OCI labels

## Resources

- [fcli Documentation](https://github.com/fortify/fcli#readme)
- [Docker Hub: fortifydocker/fcli](https://hub.docker.com/r/fortifydocker/fcli)
- [Alpine Docker Images](https://hub.docker.com/_/alpine)
- [Red Hat UBI Images](https://catalog.redhat.com/software/containers/search?q=ubi9)
- [Docker Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [OCI Image Spec](https://github.com/opencontainers/image-spec/blob/main/annotations.md)
