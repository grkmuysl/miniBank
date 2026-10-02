CREATE USER account_app WITH PASSWORD 'local_dev_only';
CREATE DATABASE accountdb OWNER account_app;
REVOKE CONNECT ON DATABASE accountdb FROM PUBLIC;