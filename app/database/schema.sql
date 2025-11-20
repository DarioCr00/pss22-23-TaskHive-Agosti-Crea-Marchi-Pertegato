CREATE TABLE notifications (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_task INT,
    message VARCHAR(255) NOT NULL,
    noti_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_user) REFERENCES users(id_user),
    FOREIGN KEY (id_task) REFERENCES tasks(id_task)
);