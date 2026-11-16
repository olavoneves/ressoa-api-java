-- =====================================================================
-- Ressoa — executa toda a parte Oracle da disciplina de Banco, na ordem.
-- SQL*Plus / SQLcl, a partir desta pasta:   @00-executar-tudo.sql
-- SQL Developer: abrir cada arquivo e executar com F5 (Run Script).
--
-- Pré-requisito: esquema criado por ../script-bd.sql
-- =====================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET ECHO OFF
SET FEEDBACK ON
WHENEVER SQLERROR CONTINUE

PROMPT ===== 01 · Dados de teste =====
@@01-dados-teste.sql
PROMPT ===== 02 · Pacotes (funções e procedimentos) =====
@@02-pacotes.sql
PROMPT ===== 03 · Gatilhos =====
@@03-gatilhos.sql
PROMPT ===== 04 · Blocos anônimos =====
@@04-blocos-anonimos.sql
PROMPT ===== 05 · Testes =====
@@05-testes.sql
PROMPT ===== 06 · Relatórios =====
@@06-relatorios-oracle.sql
