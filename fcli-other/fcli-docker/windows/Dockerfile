# This Dockerfile allows for building a Windows-based fcli image. Although functional, for now 
# this is considered just a prototype and not automatically published; we can fine-tune and 
# publish if there's any demand for a Windows-based image. Note that may suffer from some of 
# the same issues regarding base OS CVEs and ability run run ScanCentral Client as detailed in 
# the Linux Dockerfile.
#
# Build Arguments:
# - FCLI_VERSION: fcli version to download (required, e.g., "v3.14.0")
# - SERVERCORE_BASE: Windows Server Core base image (default: mcr.microsoft.com/windows/servercore:ltsc2022)
# - NANOSERVER_BASE: Windows Nano Server base image (default: mcr.microsoft.com/windows/nanoserver:ltsc2022)
# 
# The Dockerfile uses a multi-stage build:
# 1. Download stage (servercore): Downloads fcli, signature, and VC++ redistributables
# 2. Install stage (servercore): Installs VC++ redistributables
# 3. Final stage (nanoserver): Minimal image with just fcli and required DLLs

# ============================================================================
# Global build arguments (must be before any FROM to use in FROM statements)
# ============================================================================
ARG SERVERCORE_BASE=mcr.microsoft.com/windows/servercore:ltsc2022
ARG NANOSERVER_BASE=mcr.microsoft.com/windows/nanoserver:ltsc2022

# ============================================================================
# Stage: fcli-downloader
# Downloads fcli, signature file, and VC++ redistributables
# ============================================================================
FROM ${SERVERCORE_BASE} as fcli-downloader

SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]

# Build arguments
ARG FCLI_VERSION

# Validate FCLI_VERSION is provided
RUN if (-not $env:FCLI_VERSION) { throw 'ERROR: FCLI_VERSION build argument is required' }

# Download fcli and signature
RUN Write-Host "Downloading fcli ${env:FCLI_VERSION}..." ; \
    $url = "https://github.com/fortify/fcli/releases/download/${env:FCLI_VERSION}/fcli-windows.zip" ; \
    $sigUrl = "${url}.rsa_sha256" ; \
    Write-Host "Download URL: $url" ; \
    Invoke-WebRequest -Uri $url -OutFile C:\fcli-windows.zip ; \
    Write-Host "Downloading signature..." ; \
    Invoke-WebRequest -Uri $sigUrl -OutFile C:\fcli-windows.zip.rsa_sha256

# Verify signature using .NET crypto
RUN Write-Host 'Verifying signature...' ; \
    $publicKeyPem = @' \
-----BEGIN PUBLIC KEY----- \
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArij9U9yJVNc53oEMFWYp \
NrXUG1UoRZseDh/p34q1uywD70RGKKWZvXIcUAZZwbZtCu4i0UzsrKRJeUwqanbc \
woJvYanp6lc3DccXUN1w1Y0WOHOaBxiiK3B1TtEIH1cK/X+ZzazPG5nX7TSGh8Tp \
/uxQzUFli2mDVLqaP62/fB9uJ2joX9Gtw8sZfuPGNMRoc8IdhjagbFkhFT7WCZnk \
FH/4Co007lmXLAe12lQQqR/pOTeHJv1sfda1xaHtj4/Tcrq04Kx0ZmGAd5D9lA92 \
8pdBbzoe/mI5/Sk+nIY3AHkLXB9YAaKJf//Wb1yiP1/hchtVkfXyIaGM+cVyn7AN \
VQIDAQAB \
-----END PUBLIC KEY----- \
'@ ; \
    $publicKeyBase64 = ($publicKeyPem -replace '-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|\s', '') ; \
    $publicKeyBytes = [Convert]::FromBase64String($publicKeyBase64) ; \
    $rsa = [System.Security.Cryptography.RSA]::Create() ; \
    $rsa.ImportSubjectPublicKeyInfo($publicKeyBytes, [ref]$null) ; \
    $signature = [System.IO.File]::ReadAllBytes('C:\fcli-windows.zip.rsa_sha256') ; \
    $fileBytes = [System.IO.File]::ReadAllBytes('C:\fcli-windows.zip') ; \
    $hash = [System.Security.Cryptography.SHA256]::Create().ComputeHash($fileBytes) ; \
    $verified = $rsa.VerifyHash($hash, $signature, [System.Security.Cryptography.HashAlgorithmName]::SHA256, [System.Security.Cryptography.RSASignaturePadding]::Pkcs1) ; \
    if (-not $verified) { throw 'Signature verification failed' } ; \
    Write-Host '✓ Signature verification successful'

# Extract fcli
RUN Write-Host 'Extracting fcli...' ; \
    Expand-Archive -Path C:\fcli-windows.zip -DestinationPath C:\fcli-bin ; \
    if (-not (Test-Path C:\fcli-bin\fcli.exe)) { throw 'fcli.exe not found after extraction' } ; \
    C:\fcli-bin\fcli.exe --version

# Download VC++ redistributable
RUN Write-Host 'Downloading Visual C++ Redistributable...' ; \
    Invoke-WebRequest -Uri 'https://aka.ms/vs/17/release/vc_redist.x64.exe' -OutFile C:\vc_redist.x64.exe

# ============================================================================
# Stage: vcredist-installer
# Installs Visual C++ redistributables (requires servercore)
# ============================================================================
FROM ${SERVERCORE_BASE} as vcredist-installer

# Bring build arg into this stage
ARG SERVERCORE_BASE

COPY --from=fcli-downloader C:\vc_redist.x64.exe C:\vc_redist.x64.exe

# Install VC++ redistributables silently
RUN C:\vc_redist.x64.exe /install /quiet /norestart ; \
    Start-Sleep -Seconds 10

# ============================================================================
# Stage: fcli-ltsc2022
# Final nanoserver-based image with fcli and required DLLs
# ============================================================================
FROM ${NANOSERVER_BASE} AS fcli-ltsc2022

# Bring build args into this stage
ARG FCLI_VERSION
ARG NANOSERVER_BASE
LABEL org.opencontainers.image.source="https://github.com/fortify/fcli" \
      org.opencontainers.image.vendor="Fortify" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.documentation="https://github.com/fortify/fcli#readme" \
      org.opencontainers.image.version="${FCLI_VERSION}" \
      com.fortify.fcli.version="${FCLI_VERSION}" \
      org.opencontainers.image.title="Fortify CLI (Windows)" \
      org.opencontainers.image.description="Fortify CLI on Windows Nano Server" \
      com.fortify.base.image="${NANOSERVER_BASE}"

# Copy VC++ runtime DLLs required by fcli
# These DLLs are needed for the native Windows fcli executable to run
COPY --from=vcredist-installer C:/Windows/System32/vcruntime140.dll C:/Windows/System32/vcruntime140.dll
COPY --from=vcredist-installer C:/Windows/System32/vcruntime140_1.dll C:/Windows/System32/vcruntime140_1.dll
COPY --from=vcredist-installer C:/Windows/System32/msvcp140.dll C:/Windows/System32/msvcp140.dll

# Copy fcli executable
COPY --from=fcli-downloader C:/fcli-bin/fcli.exe C:/bin/fcli.exe

# Add data directory (same structure as Linux)
ADD data.tgz C:/

WORKDIR C:/data
ENV FCLI_USER_HOME=C:/data
USER ContainerUser
ENV PATH=$PATH:C:\bin

# Test that fcli works with copied DLLs
RUN C:\bin\fcli.exe --version

CMD ["cmd.exe"]
