-- Migration: Add saldo column to duenios_cuenta
-- Run this in Supabase Dashboard > SQL Editor if the database already exists

ALTER TABLE duenios_cuenta
    ADD COLUMN IF NOT EXISTS saldo NUMERIC(15,2) DEFAULT 0.00;
