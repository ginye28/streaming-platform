# ── 1단계: 빌드 ──
FROM node:22-alpine AS build

WORKDIR /app

# 잠금 파일만 먼저 넣어 두면, 소스만 바뀌었을 때 npm ci 를 다시 하지 않는다.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite 는 빌드할 때 값을 코드에 박아 넣는다. 실행할 때 바꿀 수 없으니 여기서 정한다.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ── 2단계: 실행 ──
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
