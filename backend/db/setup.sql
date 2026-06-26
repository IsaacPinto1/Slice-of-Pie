CREATE ROLE dev_user WITH LOGIN PASSWORD 'password';
ALTER ROLE dev_user CREATEDB;

CREATE DATABASE sliceofpie OWNER dev_user;