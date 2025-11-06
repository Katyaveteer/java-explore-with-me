-- Схема по умолчанию
CREATE SCHEMA IF NOT EXISTS public;

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE
);

-- Таблица категорий
CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Таблица событий
CREATE TABLE IF NOT EXISTS events (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                VARCHAR(120)  NOT NULL,
    annotation           VARCHAR(2000) NOT NULL,
    description          TEXT          NOT NULL,
    event_date           TIMESTAMP     NOT NULL,
    published_on         TIMESTAMP,
    created_on           TIMESTAMP     NOT NULL DEFAULT NOW(),
    paid                 BOOLEAN       NOT NULL DEFAULT FALSE,
    participant_limit    INTEGER       NOT NULL DEFAULT 0,
    request_moderation   BOOLEAN       NOT NULL DEFAULT TRUE,
    state                VARCHAR(20)   NOT NULL DEFAULT 'PENDING',

    initiator_id         BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id          BIGINT        NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,

    lat                  FLOAT         NOT NULL,
    lon                  FLOAT         NOT NULL
);

-- Таблица заявок на участие
CREATE TABLE IF NOT EXISTS requests (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created      TIMESTAMP    NOT NULL DEFAULT NOW(),
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    event_id     BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    requester_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT uq_request UNIQUE (event_id, requester_id)
);

-- Таблица подборок (compilations)
CREATE TABLE IF NOT EXISTS compilations (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title  VARCHAR(50) NOT NULL,
    pinned BOOLEAN     NOT NULL DEFAULT FALSE
);

-- Связь многие-ко-многим: подборка ↔ события
CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT NOT NULL REFERENCES compilations (id) ON DELETE CASCADE,
    event_id       BIGINT NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    PRIMARY KEY (compilation_id, event_id)
);

-- Индексы для ускорения фильтрации и поиска
CREATE INDEX IF NOT EXISTS idx_events_state ON events (state);
CREATE INDEX IF NOT EXISTS idx_events_category ON events (category_id);
CREATE INDEX IF NOT EXISTS idx_events_initiator ON events (initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events (event_date);
CREATE INDEX IF NOT EXISTS idx_requests_event ON requests (event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester ON requests (requester_id);
CREATE INDEX IF NOT EXISTS idx_compilations_pinned ON compilations (pinned);