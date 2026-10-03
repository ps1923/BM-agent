FROM quay.io/centos/centos:stream9

ARG WORKSPACE_UID=10001

RUN dnf install --assumeyes \
        java-17-openjdk-devel \
        maven \
        python3 \
        python3-pip \
        shadow-utils \
    && dnf clean all \
    && rm --recursive --force /var/cache/dnf \
    && useradd --create-home --uid "${WORKSPACE_UID}" --shell /usr/sbin/nologin bmhs \
    && ln --symbolic /usr/bin/python3 /usr/local/bin/python \
    && mkdir --parents /workspace \
    && chown bmhs:bmhs /workspace

WORKDIR /workspace
USER bmhs

CMD ["sleep", "infinity"]
