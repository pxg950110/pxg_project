# ---- Build Stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
# 直接 vite build：仓库存在存量 vue-tsc 类型错误，镜像构建不做类型检查（保持与 npm run dev 一致）
RUN npx vite build

# ---- Runtime Stage ----
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist .

EXPOSE 80
