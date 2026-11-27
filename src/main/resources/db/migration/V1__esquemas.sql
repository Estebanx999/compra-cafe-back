-- V1: crea los esquemas de los módulos del sistema. Sin tablas todavía.
-- Las tablas de cada módulo se crearán en migraciones posteriores,
-- dentro de su esquema correspondiente.

CREATE SCHEMA IF NOT EXISTS inventario;
CREATE SCHEMA IF NOT EXISTS compras;
CREATE SCHEMA IF NOT EXISTS ventas;
CREATE SCHEMA IF NOT EXISTS secadero;
CREATE SCHEMA IF NOT EXISTS clientes;
CREATE SCHEMA IF NOT EXISTS finanzas;
CREATE SCHEMA IF NOT EXISTS empleados;
