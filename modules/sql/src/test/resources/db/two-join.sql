CREATE TABLE students
(
    id         TEXT PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name  TEXT NOT NULL
);

CREATE TABLE classes
(
    id   TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE enrollments
(
    student_id TEXT NOT NULL,
    class_id TEXT NOT NULL,
    start_date TIMESTAMP,
    PRIMARY KEY (student_id, class_id)
);

INSERT INTO students(id, first_name, last_name) VALUES ('1', 'Student', 'One');

INSERT INTO classes(id, name) VALUES ('1', 'Class1');

INSERT INTO enrollments(student_id, class_id, start_date) VALUES ('1', '1', '2021-10-01');
