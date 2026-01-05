# Albumix Backend (Spring Boot REST API)

Albumix Backend is a **production-ready REST API** built using **Spring Boot** that powers the Albumix photo gallery application.  
It provides **secure authentication**, **album & photo management**, and **Cloudinary-based image storage**, designed with scalability and real-world deployment in mind.

---

## 🚀 Tech Stack

### Core Technologies
- **Java 21**
- **Spring Boot 3.x**
- **Spring Security (JWT-based Authentication)**
- **Spring Data JPA (Hibernate)**
- **MySQL (Production Database)**

### Cloud & Storage
- **Cloudinary** – Cloud-based image storage, upload & deletion

### API & Tooling
- **RESTful APIs**
- **Swagger / OpenAPI 3** – API documentation
- **Maven** – Dependency management

---

## 🔐 Authentication & Security

- Stateless **JWT Authentication**
- Secure token generation using **RSA keys**
- Role-based access control (`USER`, `ADMIN`)
- Protected endpoints using **OAuth2 Resource Server**
- Password encryption with **BCrypt**
- Session-less backend (no server-side sessions)

---

## 📦 Features Implemented

### 👤 User & Account Management
- User registration
- Login & JWT token generation
- View profile (email + roles)
- Change password
- Delete account securely

### 📁 Album Management
- Create albums
- Update album details
- Delete albums
- Fetch all albums of logged-in user

### 🖼️ Photo Management
- Upload multiple photos
- Generate thumbnails
- View photos by album
- Delete photos
- Secure photo download

### ☁️ Cloudinary Integration
- Upload images directly to Cloudinary
- Delete images from Cloudinary
- Token-protected Cloudinary APIs
- Production-ready cloud storage (no local dependency)

### 🧪 API Documentation
- Swagger UI available for testing APIs
- Clear request/response schemas

---

## 📂 Project Structure
```
src/main/java/com/shank/AlbumsAPI
│
├── controller        # REST Controllers
├── service           # Business logic
├── repository        # JPA repositories
├── model             # Entity classes
├── payload           # DTOs
├── security          # JWT & security config
├── config            # Cloudinary, Swagger, Seed Data
└── util              # Utility helpers
```
---

## ▶️ How to Run Locally

### 1️⃣ Clone the repository
```
git clone https://github.com/shankagr7805/albumix-backend.git
cd albumix-backend
```

## 2️⃣ Configure environment variables
```
export DB_URL=jdbc:mysql://localhost:3306/albumix
export DB_USERNAME=albumix_user
export DB_PASSWORD=strongpassword

export CLOUDINARY_CLOUD_NAME=xxx
export CLOUDINARY_API_KEY=xxx
export CLOUDINARY_API_SECRET=xxx
```

## 3️⃣ Run the application
```
./mvnw spring-boot:run
```

---

## 🧪 Authentication Flow

### Login
```POST /api/v2/auth/token```

### Receive JWT token

### Pass token in header:
```Authorization: Bearer <TOKEN>```

### Access secured APIs
