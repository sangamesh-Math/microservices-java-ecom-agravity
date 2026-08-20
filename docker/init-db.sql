-- Initialize separate databases for microservices
CREATE DATABASE auth_user_db;
CREATE DATABASE order_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE auth_user_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
