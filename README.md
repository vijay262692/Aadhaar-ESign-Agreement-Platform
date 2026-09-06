# PV Talent Partners – Agreement eSign Portal

A Spring Boot + PostgreSQL backend with a plain HTML/CSS/JavaScript UI for the Candidate Placement Services Agreement.

## What is included

- Admin dashboard
- Agreement creation form
- Candidate signing page
- Consultant signing page
- Agreement status dashboard
- PDF upload/download endpoint
- Secure random signing tokens
- eSign integration interface and callback placeholder
- PostgreSQL persistence
- CORS configuration
- Responsive HTML/CSS UI

## Important

This project intentionally does NOT implement Aadhaar OTP/eSign itself. Production Aadhaar-based eSign must be connected through an authorised eSign provider. The `ESignService` interface and `/api/esign/callback` endpoint are the integration points.

The demo UI can simulate signing for local testing only. Disable the demo signing endpoint in production.

## Run

Requirements:
- Java 11+
- Maven 3.9+
- PostgreSQL

Create database:

    CREATE DATABASE pv_talent;

Configure `src/main/resources/application.properties`.

Then:

    mvn spring-boot:run

Open:

    http://localhost:8080/

API:
- POST /api/agreements
- GET /api/agreements
- GET /api/agreements/{id}
- POST /api/agreements/{id}/esign/start
- POST /api/esign/callback
- POST /api/agreements/{id}/demo-sign  (local demo only)
- POST /api/agreements/{id}/pdf
- GET /api/agreements/{id}/pdf

For production, add authentication/authorization for the admin APIs, object storage for PDFs, HTTPS, CSRF protection where applicable, rate limiting, audit logging, and a real authorised eSign provider.
