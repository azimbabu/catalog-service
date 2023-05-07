CREATE TABLE book
(
    id                 BIGSERIAL PRIMARY KEY NOT NULL,
    isbn               varchar(255) UNIQUE   NOT NULL,
    author             varchar(255)          NOT NULL,
    title              varchar(255)          NOT NULL,
    price              float8                NOT NULL,
    created_date       timestamp             NOT NULL,
    last_modified_date timestamp             NOT NULL,
    version            integer               NOT NULL
);