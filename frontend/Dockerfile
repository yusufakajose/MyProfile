# Frontend Dockerfile (multi-stage: build → nginx)

FROM node:20-alpine3.20 AS build
WORKDIR /app
RUN apk --no-cache upgrade
COPY package*.json ./
ENV NODE_OPTIONS=--max_old_space_size=2048
# Cache npm files across builds (requires BuildKit)
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL}
# Speed up builds and shrink image by disabling source maps
ENV GENERATE_SOURCEMAP=false
# Use cached npm data for the build step too
RUN --mount=type=cache,target=/root/.npm npm run build

FROM nginx:1.27-alpine3.20
RUN apk --no-cache upgrade
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
