CREATE TABLE branch
(
    id              SERIAL PRIMARY KEY,
    branch_code     VARCHAR(255),
    branch_name     VARCHAR(255),
    address         VARCHAR(255),
    phone_number    VARCHAR(255),
    email           VARCHAR(255)
);

CREATE TABLE employee
(
    id            SERIAL PRIMARY KEY,
    salary        INT NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    position      VARCHAR(255),
    email         VARCHAR(255),
    phone_number  VARCHAR(255),
    hire_date     TIMESTAMP,
    branch        VARCHAR(255),
    branch_id     INT,
    FOREIGN KEY (branch_id) REFERENCES branch (id) ON DELETE SET NULL
);
