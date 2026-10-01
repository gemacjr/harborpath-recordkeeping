FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY angular.json tsconfig.json ./
COPY apps ./apps
COPY libs ./libs
RUN npx ng build shell

FROM nginx:1.27-alpine
COPY apps/shell/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/shell/browser /usr/share/nginx/html
COPY apps/shell/public/federation.manifest.docker.json /usr/share/nginx/html/federation.manifest.json
EXPOSE 80
