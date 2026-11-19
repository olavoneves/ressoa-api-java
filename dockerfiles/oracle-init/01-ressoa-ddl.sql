-- Executado UMA vez, no primeiro boot do container, pelo entrypoint da
-- gvenzl/oracle-xe (como SYS). Cria as tabelas no esquema da aplicacao.
-- RESSOADB e RESSOA precisam bater com ORACLE_DATABASE e APP_USER do
-- scripts/00_variables.sh (o usuario e criado pelo entrypoint antes daqui).
ALTER SESSION SET CONTAINER = RESSOADB;
ALTER SESSION SET CURRENT_SCHEMA = RESSOA;
@/opt/ressoa/script-bd.sql
