FROM node:22-alpine AS build

WORKDIR /app/frontend-v2

ARG VITE_BASE_PATH=/

COPY frontend-v2/package.json frontend-v2/package-lock.json ./
RUN npm ci

COPY frontend-v2/ ./
RUN npm run build -- --base "${VITE_BASE_PATH}"

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/frontend-v2/dist/ /usr/share/nginx/html/

EXPOSE 8080
