CREATE TABLE users
(
    id                 SERIAL PRIMARY KEY,
    username           VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    country            VARCHAR(255) NOT NULL,
    gender             VARCHAR(15)  NOT NULL,
    phone_number       VARCHAR(20) NOT NULL,
    registration_date  TIMESTAMP DEFAULT current_timestamp
);

CREATE TABLE role
(
    id        SERIAL PRIMARY KEY,
    role_name  VARCHAR(255) NOT NULL
);

CREATE TABLE user_role
(
    user_id INT REFERENCES users (id) ON DELETE CASCADE,
    role_id INT REFERENCES role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);
