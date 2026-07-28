# API 명세

---

## 회원가입

POST /api/auth/signup

### Request

```json
{
  "email":"test@test.com",
  "password":"1234",
  "nickname":"홍길동"
}