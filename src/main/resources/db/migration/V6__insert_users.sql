INSERT INTO users (username, password, email, country, gender, phone_number)
VALUES ('juanperez', '$2a$10$ceKFgJmjila.BdGarFUFB.XUZKFj5P0mSd9rW9yZCVehxrhYig7ta', 'juanperez@gmail.com', 'Mexico', 'Male', '1234567890');

INSERT INTO role (role_name)
VALUES ('USER'), ('ADMIN');

INSERT INTO user_role (user_id, role_id)
VALUES (1, 1), -- ROLE_USER
       (1, 2); -- ROLE_ADMIN
