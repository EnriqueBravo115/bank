CREATE TABLE users
(
    id                 INT NOT NULL,
    username           VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    country            VARCHAR(255),
    gender             VARCHAR(15),
    phone_number       VARCHAR(20),
    registration_date  TIMESTAMP DEFAULT current_timestamp,
    PRIMARY KEY(id)
)

CREATE TABLE roles
(
    id        INT NOT NULL,
    rolename  VARCHAR(255) NOT NULL
)

CREATE TABLE user_role
(
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
)
