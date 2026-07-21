# syntax=docker/dockerfile:1.7

ARG RUNTIME_IMAGE=eclipse-temurin:26-jdk-noble

FROM ${RUNTIME_IMAGE} AS build

ARG JDK_GIT_REF=jdk-26+35
ARG HSDIS_BACKEND=capstone
ARG JOBS=4

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      autoconf \
      build-essential \
      ca-certificates \
      file \
      gawk \
      git \
      libasound2-dev \
      libcapstone-dev \
      libcups2-dev \
      libfontconfig1-dev \
      libfreetype-dev \
      libx11-dev \
      libxext-dev \
      libxrandr-dev \
      libxrender-dev \
      libxt-dev \
      libxtst-dev \
      make \
      pkg-config \
      unzip \
      zip && \
    rm -rf /var/lib/apt/lists/*

RUN if ! git clone --depth 1 --branch "${JDK_GIT_REF}" https://github.com/openjdk/jdk.git /src/jdk; then \
      rm -rf /src/jdk && \
      git clone --filter=blob:none https://github.com/openjdk/jdk.git /src/jdk && \
      git -C /src/jdk checkout "${JDK_GIT_REF}"; \
    fi

WORKDIR /src/jdk

RUN bash configure \
      --with-boot-jdk="${JAVA_HOME}" \
      --with-hsdis="${HSDIS_BACKEND}" \
      --enable-headless-only \
      --enable-hsdis-bundling \
      --disable-warnings-as-errors \
      --with-conf-name=hsdis

RUN make build-hsdis LOG=info JOBS="${JOBS}" || make hsdis LOG=info JOBS="${JOBS}"

RUN mkdir -p /opt/hsdis && \
    find build -type f \( -name 'hsdis-*.so' -o -name 'libhsdis-*.so' \) -exec cp -v {} /opt/hsdis/ \; && \
    test -n "$(find /opt/hsdis -type f -name '*hsdis*.so' -print -quit)"

FROM ${RUNTIME_IMAGE}

COPY --from=build /opt/hsdis/ /opt/hsdis/

ENV LD_LIBRARY_PATH=/opt/hsdis

CMD ["java", "-version"]
