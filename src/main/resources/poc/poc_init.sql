-- ===========================================
-- SCRIPT DI POPOLAMENTO POC
-- ===========================================

-- Pulizia tabelle (opzionale per test ripetuti)
--TRUNCATE TABLE tb_poc_transazione RESTART IDENTITY CASCADE;
--TRUNCATE TABLE tb_poc_cliente RESTART IDENTITY CASCADE;

-- ===========================================
-- 1️⃣ Tabella anagrafica clienti
-- ===========================================
-- Sequence per tb_poc_cliente
CREATE SEQUENCE seq_poc_cliente
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE tb_poc_cliente
(
    id_cliente BIGINT PRIMARY KEY DEFAULT nextval('seq_poc_cliente'),
    nome       VARCHAR(100),
    cognome    VARCHAR(100),
    email      VARCHAR(150)
);

ALTER TABLE tb_poc_cliente
    ADD CONSTRAINT uq_tb_poc_cliente_email UNIQUE (email);



-- ===========================================
-- 2️⃣ Tabella transazioni / attività
-- ===========================================
CREATE TABLE tb_poc_transazione
(
    id_transazione BIGINT PRIMARY KEY DEFAULT nextval('seq_poc_transazione'),
    id_cliente     BIGINT REFERENCES tb_poc_cliente (id_cliente),
    importo        NUMERIC(10, 2),
    stato          VARCHAR(20),
    data_creazione DATE
);


-- Sequence per tb_poc_transazione
CREATE SEQUENCE seq_poc_transazione
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ===========================================
-- CLIENTI
-- ===========================================
INSERT INTO tb_poc_cliente (nome, cognome, email)
VALUES ('Marco', 'Rossi', 'marco.rossi@email.com'),
       ('Sara', 'Verdi', 'sara.verdi@email.com'),
       ('Giulia', 'Neri', 'giulia.neri@email.com'),
       ('Alessandro', 'Fontana', 'alessandro.fontana@email.com'),
       ('Chiara', 'Gallo', 'chiara.gallo@email.com'),
       ('Francesco', 'De Luca', 'francesco.deluca@email.com'),
       ('Elena', 'Moretti', 'elena.moretti@email.com'),
       ('Marta', 'Giordano', 'marta.giordano@email.com'),
       ('Paolo', 'Ricci', 'paolo.ricci@email.com'),
       ('Valentina', 'Barbieri', 'valentina.barbieri@email.com');

-- ===========================================
-- TRANSAZIONI
-- ===========================================
INSERT INTO tb_poc_transazione (id_cliente, importo, stato, data_creazione)
VALUES (1, 125.50, 'ATTIVO', '2025-10-01'),
       (2, 340.00, 'SOSPESO', '2025-09-25'),
       (3, 15.75, 'ATTIVO', '2025-09-28'),
       (4, 560.10, 'DISATTIVO', '2025-09-30'),
       (5, 44.00, 'ATTIVO', '2025-09-27'),
       (6, 120.30, 'ATTIVO', '2025-09-24'),
       (7, 75.90, 'SOSPESO', '2025-09-22'),
       (8, 99.99, 'ATTIVO', '2025-09-26'),
       (9, 270.00, 'DISATTIVO', '2025-09-20'),
       (10, 130.60, 'ATTIVO', '2025-09-28'),
       (1, 89.90, 'ATTIVO', '2025-10-02'),
       (1, 250.00, 'ATTIVO', '2025-09-30'),
       (2, 19.99, 'SOSPESO', '2025-09-22'),
       (2, 110.00, 'ATTIVO', '2025-09-23'),
       (2, 340.50, 'ATTIVO', '2025-09-27'),
       (3, 78.00, 'ATTIVO', '2025-09-29'),
       (3, 15.50, 'DISATTIVO', '2025-09-15'),
       (3, 42.10, 'ATTIVO', '2025-09-28'),
       (4, 800.00, 'SOSPESO', '2025-10-01'),
       (4, 33.00, 'ATTIVO', '2025-09-28'),
       (5, 52.75, 'ATTIVO', '2025-09-29'),
       (5, 600.00, 'DISATTIVO', '2025-09-26'),
       (5, 78.90, 'ATTIVO', '2025-09-30'),
       (6, 99.90, 'ATTIVO', '2025-09-22'),
       (6, 15.00, 'SOSPESO', '2025-09-23'),
       (6, 500.00, 'DISATTIVO', '2025-09-21'),
       (7, 10.00, 'ATTIVO', '2025-09-18'),
       (7, 33.50, 'ATTIVO', '2025-09-28'),
       (7, 91.25, 'SOSPESO', '2025-09-27'),
       (7, 48.00, 'ATTIVO', '2025-09-19'),
       (8, 200.10, 'ATTIVO', '2025-09-28'),
       (8, 19.99, 'ATTIVO', '2025-09-29'),
       (8, 550.00, 'DISATTIVO', '2025-09-30'),
       (8, 12.00, 'SOSPESO', '2025-09-20'),
       (9, 27.99, 'ATTIVO', '2025-09-25'),
       (9, 78.80, 'ATTIVO', '2025-09-28'),
       (9, 190.50, 'ATTIVO', '2025-09-26'),
       (9, 15.00, 'DISATTIVO', '2025-09-27'),
       (10, 49.99, 'ATTIVO', '2025-09-29'),
       (10, 70.00, 'SOSPESO', '2025-09-22'),
       (10, 199.99, 'ATTIVO', '2025-09-30'),
       (10, 30.10, 'ATTIVO', '2025-09-21'),
       (10, 440.00, 'DISATTIVO', '2025-09-19'),
       (1, 10.10, 'ATTIVO', '2025-09-28'),
       (2, 500.00, 'ATTIVO', '2025-09-25'),
       (3, 340.10, 'SOSPESO', '2025-09-26'),
       (4, 29.00, 'ATTIVO', '2025-09-24'),
       (5, 310.00, 'ATTIVO', '2025-09-23'),
       (6, 59.99, 'SOSPESO', '2025-09-27'),
       (7, 400.00, 'ATTIVO', '2025-09-25'),
       (8, 75.00, 'ATTIVO', '2025-09-26'),
       (9, 12.50, 'SOSPESO', '2025-09-29'),
       (10, 99.00, 'ATTIVO', '2025-09-30');


-- 1️⃣ Create sequence
CREATE SEQUENCE seq_tb_poc_tracing_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- 2️⃣ Create tracing table
CREATE TABLE tb_poc_tracing
(
    id    BIGINT       NOT NULL DEFAULT nextval('seq_tb_poc_tracing_id'),
    mail  VARCHAR(255) NOT NULL UNIQUE,
    count INTEGER               DEFAULT 0,
    PRIMARY KEY (id)
);

