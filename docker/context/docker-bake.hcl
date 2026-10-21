# SPDX-FileCopyrightText: 2024-2026 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: EPL-2.0

variable "REFINERY_VERSION" {
  default = ""
}

variable "NODE_VERSION" {
  default = ""
}

variable "ALPINE_VERSION" {
  default = ""
}

variable "REFINERY_PUSH" {
  default = "false"
}

variable "REFINERY_IMAGE_REPOSITORY" {
  default = "ghcr.io/graphs4value"
}

variable "REFINERY_SOURCE_URL" {
  default = "https://github.com/graphs4value/refinery"
}

group "default" {
  targets = ["cli", "web", "chat"]
}

target "base" {
  dockerfile = "Dockerfile.base"
  platforms = ["linux/amd64", "linux/arm64"]
  output = ["type=cacheonly"]
}

target "cli" {
  dockerfile = "Dockerfile.cli"
  platforms = ["linux/amd64", "linux/arm64"]
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=${REFINERY_IMAGE_REPOSITORY}/refinery-cli:${REFINERY_VERSION},${REFINERY_IMAGE_REPOSITORY}/refinery-cli:latest\",annotation-index.org.opencontainers.image.source=${REFINERY_SOURCE_URL},\"annotation-index.org.opencontainers.image.description=Command line interface for Refinery, an efficient graph solver for generating well-formed models\",annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
  contexts = {
    base = "target:base"
  }
}

target "web" {
  dockerfile = "Dockerfile.web"
  platforms = ["linux/amd64", "linux/arm64"]
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=${REFINERY_IMAGE_REPOSITORY}/refinery:${REFINERY_VERSION},${REFINERY_IMAGE_REPOSITORY}/refinery:latest\",annotation-index.org.opencontainers.image.source=${REFINERY_SOURCE_URL},annotation-index.org.opencontainers.image.description=Refinery: an efficient graph solver for generating well-formed models,annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
  contexts = {
    base = "target:base"
  }
}

target "chat" {
  dockerfile = "Dockerfile.chat"
  platforms = ["linux/amd64", "linux/arm64"]
  args = {
    NODE_VERSION = "${NODE_VERSION}"
    ALPINE_VERSION = "${ALPINE_VERSION}"
  }
  output = [
    "type=image,push=${REFINERY_PUSH},\"name=${REFINERY_IMAGE_REPOSITORY}/refinery-chat:${REFINERY_VERSION},${REFINERY_IMAGE_REPOSITORY}/refinery-chat:latest\",annotation-index.org.opencontainers.image.source=${REFINERY_SOURCE_URL},\"annotation-index.org.opencontainers.image.description=Chat integration for Refinery, an efficient graph solver for generating well-formed models\",annotation-index.org.opencontainers.image.licenses=EPL-2.0"
  ]
}
