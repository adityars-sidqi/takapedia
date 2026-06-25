-- Membuat database terpisah per service (prinsip Database per Service)
-- Dijalankan otomatis saat container Postgres pertama kali dibuat

CREATE DATABASE auth_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;

-- Catatan: database lain bisa ditambah seiring fase berjalan
