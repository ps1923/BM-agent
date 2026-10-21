FROM node:22-bookworm-slim@sha256:3ed98443660ff48413b5af19ecefce3b9d50845a901a43420d47cc63a9fddbe9

ARG DSH_VERSION=0.2.1-rc.2
ARG PNPM_VERSION=10.34.7

RUN npm install --global "pnpm@${PNPM_VERSION}" "@deepseek-ai/dsh@${DSH_VERSION}" \
    && npm cache clean --force

RUN groupadd --gid 10001 dsh \
    && useradd --uid 10001 --gid dsh --create-home --shell /usr/sbin/nologin dsh \
    && mkdir -p /workspace \
    && chown dsh:dsh /workspace

WORKDIR /workspace
COPY --chown=dsh:dsh . /opt/bm-hs-dsh-plugin/
RUN mkdir -p /run/bmhs-dsh && chown dsh:dsh /run/bmhs-dsh

USER dsh
RUN dsh --version \
    && pnpm --version \
    && dsh plugin --profile web add /opt/bm-hs-dsh-plugin

ENTRYPOINT ["dsh"]
