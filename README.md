# Wedding Management System

Hệ thống quản lý và thống kê đám cưới: khách mời, tiền mừng, chi phí, ngân sách, sơ đồ bàn, check-in QR, nhà cung cấp, công việc chuẩn bị và báo cáo.

Frontend: **Angular 21** (standalone, Material, Chart.js).  
Backend: **Java 21 + Spring Boot 4.1** (JPA, Security, JWT, Flyway, OpenAPI).  
Database: **PostgreSQL 17**. Tiền tệ **VND** (số nguyên / `NUMERIC`, không dùng float).

## Chạy nhanh trên máy local

### Yêu cầu

- JDK 21
- Maven 3.9+
- Node.js 22 + npm
- PostgreSQL 17 (hoặc dùng Docker Compose bên dưới)

Tạo database:

```sql
CREATE USER wedding WITH PASSWORD 'wedding';
CREATE DATABASE wedding OWNER wedding;
```

### Backend

Trong thư mục `backend/`:

```bash
# Windows PowerShell
$env:APP_ADMIN_PASSWORD="Admin@123456"
$env:APP_JWT_SECRET="local-dev-secret-change-me-32chars-min"
$env:SPRING_PROFILES_ACTIVE="dev"

mvn spring-boot:run
```

- API: http://localhost:8080/api  
- Swagger: http://localhost:8080/swagger-ui.html  
- Health: http://localhost:8080/actuator/health  

Profile `dev` **seed dữ liệu mẫu một lần** (40 khách, sự kiện, tiền mừng, chi phí, bàn tiệc, công việc). Không seed lại nếu đã có khách. Production (`SPRING_PROFILES_ACTIVE=prod`) bắt đầu với dữ liệu sạch.

Tài khoản mặc định (chỉ tạo khi bảng `users` trống):

| Trường | Giá trị |
|---|---|
| Username | `admin` (hoặc `APP_ADMIN_USERNAME`) |
| Password | giá trị `APP_ADMIN_PASSWORD` — **không hardcode trong source** |

### Frontend

Trong thư mục `frontend/`:

```bash
npm install --legacy-peer-deps
npx ng serve
```

Mở http://localhost:4200 — `proxy.conf.json` chuyển `/api` sang cổng 8080.

### Lệnh kiểm tra

```bash
# Frontend
cd frontend
npx ng build --configuration production
npx ng test --watch=false

# Backend
cd backend
mvn -B verify
```

## Docker Compose (VPS / máy có Docker)

```bash
cp .env.example .env
# Sửa APP_JWT_SECRET và APP_ADMIN_PASSWORD trong .env

docker compose up -d --build
```

- Website: http://localhost:8081  
- API trực tiếp: http://localhost:8080/api  
- PostgreSQL: cổng 5432 (đổi `POSTGRES_PORT` nếu trùng)

Dữ liệu PostgreSQL và file upload (hóa đơn, chứng từ) nằm trong Docker volume, không commit lên GitHub.

Tắt:

```bash
docker compose down
```

Xóa luôn dữ liệu DB (cẩn thận):

```bash
docker compose down -v
```

## Cấu trúc monorepo

```
wedding-management/
├── frontend/          Angular 21
├── backend/           Spring Boot
├── database/migration Flyway (V1 schema, V2 danh mục chi phí)
├── .github/workflows  CI build FE + BE
├── docker-compose.yml
├── .env.example
└── README.md
```

## Chức năng chính

- **Tổng quan**: KPI, 6 biểu đồ, khoản chi lớn, khách chưa xác nhận, việc đến hạn — số liệu từ backend.
- **Khách mời**: CRUD, nhóm/hộ, lời mời theo sự kiện, nhập/xuất Excel, cảnh báo trùng SĐT, lưu trữ mềm (không mất lịch sử tiền mừng).
- **Tiền mừng**: tiền mặt / CK / vàng / hiện vật (giá trị tùy chọn, không tự đoán); một khách nhiều khoản.
- **Chi phí**: danh mục, nhiều lần thanh toán, hóa đơn lưu trên đĩa (không Base64).
- **Ngân sách**: thu = tiền mừng + nguồn khác; chi độc lập; cảnh báo vượt hạng mục.
- **Sự kiện**: ăn hỏi, tiệc nhà trai/gái… thống kê riêng, không cộng trùng.
- **Sơ đồ bàn**: kéo-thả, sức chứa, xác nhận khi vượt chỗ.
- **Check-in**: tìm tên hoặc quét QR; vẫn check-in tay nếu không có camera.
- **Nhà cung cấp, Kanban công việc, báo cáo Excel / in PDF**.
- **Nhật ký, công nợ, tìm kiếm tổng thể, nhắc việc, sao lưu/khôi phục JSON**.

Website **không cho đăng ký công khai**. Chỉ quản trị viên tạo thêm tài khoản (STAFF) trong Cài đặt.

## Biến môi trường quan trọng

| Biến | Ý nghĩa |
|---|---|
| `APP_JWT_SECRET` | Secret ký JWT (≥ 32 ký tự). Bắt buộc ở production. |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Tài khoản admin lần đầu |
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | PostgreSQL |
| `APP_UPLOAD_DIR` | Thư mục lưu hóa đơn / chứng từ |
| `APP_SEED_DEMO_DATA` | `true` chỉ khi muốn dữ liệu demo |
| `SPRING_PROFILES_ACTIVE` | `dev` hoặc `prod` |
| `APP_CORS_ALLOWED_ORIGINS` | Origin frontend |

Không commit file `.env`, mật khẩu hay thư mục `uploads/`.

## Deploy VPS

1. Cài Docker + Docker Compose.  
2. Clone repo, tạo `.env` từ `.env.example`.  
3. `docker compose up -d --build`.  
4. Trỏ domain / nginx-proxy tới cổng `FRONTEND_PORT` (mặc định 8081).  
5. Đổi mật khẩu admin ngay sau lần đăng nhập đầu.

GitHub chỉ lưu source và chạy CI. Ứng dụng chạy trên VPS (hoặc bất kỳ nền tảng nào hỗ trợ Docker), không chạy PostgreSQL trên GitHub.

## Múi giờ và định dạng

- Múi giờ: `Asia/Ho_Chi_Minh`
- Ngày: `DD/MM/YYYY`
- Tiền: `1.000.000 ₫`
