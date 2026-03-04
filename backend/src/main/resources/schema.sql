CREATE TABLE IF NOT EXISTS users
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT    NOT NULL UNIQUE,
    password   TEXT    NOT NULL,
    email      TEXT,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS clusters
(
    id                 TEXT    PRIMARY KEY,
    user_id            INTEGER NOT NULL,
    name               TEXT    NOT NULL,
    auth_type          TEXT    NOT NULL,
    api_server_url     TEXT,
    skip_tls_verify    INTEGER NOT NULL DEFAULT 0,
    token              TEXT,
    ca_cert_data       TEXT,
    client_cert_data   TEXT,
    client_key_data    TEXT,
    kubeconfig_content TEXT,
    default_cluster    INTEGER NOT NULL DEFAULT 0,
    status             TEXT,
    status_message     TEXT,
    created_at         INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);
