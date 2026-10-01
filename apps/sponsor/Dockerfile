FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY angular.json tsconfig.json ./
COPY apps ./apps
COPY libs ./libs
RUN npx ng build sponsor

FROM nginx:1.27-alpine
COPY apps/remote.nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/sponsor/browser /usr/share/nginx/html
EXPOSE 80
