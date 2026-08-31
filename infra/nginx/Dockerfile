FROM node:24-bookworm-slim AS web-builder
WORKDIR /workspace
RUN corepack enable && corepack prepare pnpm@11.24.0 --activate
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY apps/web ./apps/web
COPY packages ./packages
RUN pnpm install --frozen-lockfile \
    && pnpm build:packages \
    && pnpm build:web

FROM nginx:1.28.0-alpine
COPY infra/nginx/nginx.conf /etc/nginx/nginx.conf
COPY --from=web-builder /workspace/apps/web/dist /usr/share/nginx/html
EXPOSE 80

