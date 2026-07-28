# Trouble Shooting

---

## JWT 인증 실패

### 문제

토큰이 정상인데 401 발생

### 원인

Security Filter 순서 문제

### 해결

JwtAuthenticationFilter 위치 변경

---

## CORS 오류

### 문제

프론트에서 요청 시 CORS 발생

### 해결

CorsConfiguration 추가

---

## Docker Build 실패

...
