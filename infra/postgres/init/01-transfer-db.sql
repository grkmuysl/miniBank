CREATE USER transfer_app WITH PASSWORD 'local_dev_only';
CREATE DATABASE transferdb OWNER transfer_app;
REVOKE CONNECT ON DATABASE transferdb FROM PUBLIC;