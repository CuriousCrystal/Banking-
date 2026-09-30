# Web UI image (build context is the repository root):
#   docker build -f deploy/frontend.Dockerfile -t bfsi-frontend .

# ---- build the React app ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.js ./
COPY src ./src
# same origin: nginx proxies /api to the backend, so the browser needs no CORS and no backend address
ARG VITE_API_URL=/api
ARG VITE_APP_NAME=
ENV VITE_API_URL=$VITE_API_URL \
    VITE_APP_NAME=$VITE_APP_NAME
RUN npm run build

# ---- serve it ----
FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
