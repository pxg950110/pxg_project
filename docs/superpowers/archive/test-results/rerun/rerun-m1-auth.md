# Module 1: Auth (Authentication) Rerun Results

**Test Date:** 2026-04-12
**Environment:** http://localhost:3000

| Test ID | Description | Status | Details |
|---------|-------------|--------|---------|
| T1.1 | Login page loads at /login | PASS | Page loads correctly with title "登录 - MAIDC". Displays welcome text "欢迎回来", username/password fields, remember-me checkbox, login button, and audit warning notices. |
| T1.2 | Login with admin/Admin@123 | PASS | POST /api/v1/auth/login returns 200 with JWT accessToken and refreshToken. GET /api/v1/users/me returns user info {id:1, username:"admin", realName:"系统管理员", roles:["admin"]}. Redirects to /dashboard/overview after successful login. |
| T1.3 | Login with wrong password | PARTIAL | API correctly returns error code 4102 "用户名或密码错误". Page stays on /login (no redirect). However, no visible error message/notification is displayed to the user on the frontend -- this is a UI bug. |
| T1.4 | /users/me API returns user info | PASS | GET /api/v1/users/me with Bearer token returns: {id:1, username:"admin", realName:"系统管理员", roles:["admin"], orgId:0, permissions:[]}. All fields populated correctly. |
| T1.5 | Logout functionality | PASS | User dropdown (hover on avatar) shows "退出登录" option. Clicking it opens a confirmation modal "确定要退出登录吗？". Confirming logs out and redirects to /login page. Token is cleared from localStorage. |

## Summary
- PASS: 4
- PARTIAL: 1 (T1.3 - API returns error correctly but no visible error message on UI)
- FAIL: 0

## API Details

### Login API
- **Endpoint:** POST /api/v1/auth/login
- **Request Body:** `{"username":"admin","password":"Admin@123"}`
- **Success Response (200):** `{"code":200,"message":"success","data":{"accessToken":"eyJ...","refreshToken":"eyJ...","tokenType":"Bearer","expiresIn":7200,"user":{"id":1,"username":"admin","realName":"系统管理员","roles":["admin"],"orgId":0}}}`
- **Error Response (200):** `{"code":4102,"message":"用户名或密码错误","data":null}`

### Users Me API
- **Endpoint:** GET /api/v1/users/me
- **Headers:** Authorization: Bearer {token}
- **Response (200):** `{"code":200,"message":"success","data":{"id":1,"username":"admin","realName":"系统管理员","roles":["admin"],"orgId":0,"permissions":[]}}`

## Issues Found
1. **Login error message not displayed (T1.3):** When login fails with wrong password, the API returns error code 4102 but the frontend does not show any visible error notification/message to the user. The page simply stays on /login without feedback.
