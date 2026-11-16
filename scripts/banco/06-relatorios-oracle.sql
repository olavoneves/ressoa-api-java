-- =====================================================================
-- Ressoa — 06 · Relatórios no Oracle (base da comparação com o MongoDB)
-- Disciplina: Mastering Relational and Non-Relational Database
--
-- Cada consulta tem a equivalente em mongodb/02-relatorios.js.
-- Com a carga de referência (01-dados-teste.sql) os resultados são
-- idênticos aos do MongoDB (ver README, seção "Relatórios").
-- =====================================================================
SET LINESIZE 200
SET PAGESIZE 100
COLUMN BAIRRO FORMAT A20
COLUMN MUNICIPIO FORMAT A16
COLUMN NUMERO_SERIE FORMAT A14

-- ---------------------------------------------------------------------
-- R1 (find) — Alertas da semana 08–14/11: leituras com falha no pulso
-- ou bateria abaixo de 20%. Quem precisa de visita técnica?
-- ---------------------------------------------------------------------
SELECT D.NUMERO_SERIE,
       L.REGISTRADA_EM,
       L.BATERIA_PCT,
       L.PULSO_OK
  FROM LEITURA L
  JOIN DISPOSITIVO D ON D.ID = L.DISPOSITIVO_ID
 WHERE L.REGISTRADA_EM >= TIMESTAMP '2026-11-08 00:00:00'
   AND L.REGISTRADA_EM <  TIMESTAMP '2026-11-15 00:00:00'
   AND (L.PULSO_OK = 'N' OR L.BATERIA_PCT < 20)
 ORDER BY L.REGISTRADA_EM, D.NUMERO_SERIE;

-- ---------------------------------------------------------------------
-- R2 (find) — Bairros com água intermitente em risco LARANJA ou
-- VERMELHO, com os indicadores da avaliação mais recente.
-- ---------------------------------------------------------------------
SELECT B.NOME AS BAIRRO,
       B.MUNICIPIO,
       B.NIVEL_RISCO,
       A.SEMANA_EPIDEMIOLOGICA,
       A.CASOS_100K,
       A.RT
  FROM BAIRRO B
  JOIN AVALIACAO_RISCO A
    ON A.BAIRRO_ID = B.ID
   AND A.ID = (SELECT MAX(A2.ID) KEEP (DENSE_RANK LAST ORDER BY A2.CRIADA_EM)
                 FROM AVALIACAO_RISCO A2
                WHERE A2.BAIRRO_ID = B.ID)
 WHERE B.AGUA_INTERMITENTE = 'S'
   AND B.NIVEL_RISCO IN ('LARANJA', 'VERMELHO')
 ORDER BY B.NOME;

-- ---------------------------------------------------------------------
-- R3 (agregação) — Cobertura de aparelhos por bairro: total em operação
-- (sem INATIVOS), ativos e % ativos. Confere com a função do pacote.
-- ---------------------------------------------------------------------
SELECT B.NOME AS BAIRRO,
       COUNT(*) AS APARELHOS,
       COUNT(CASE WHEN D.STATUS = 'ATIVO' THEN 1 END) AS ATIVOS,
       ROUND(COUNT(CASE WHEN D.STATUS = 'ATIVO' THEN 1 END) * 100 / COUNT(*), 1) AS PCT_ATIVOS,
       PKG_DISPOSITIVO.fn_pct_dispositivos_ativos(B.ID) AS PCT_PELA_FUNCAO
  FROM BAIRRO B
  JOIN DISPOSITIVO D ON D.BAIRRO_ID = B.ID
 WHERE D.STATUS <> 'INATIVO'
 GROUP BY B.ID, B.NOME
 ORDER BY PCT_ATIVOS, B.NOME;

-- ---------------------------------------------------------------------
-- R4 (agregação, extra) — Saúde da proteção por bairro na semana:
-- leituras, taxa de sucesso do pulso e bateria média.
-- ---------------------------------------------------------------------
SELECT B.NOME AS BAIRRO,
       COUNT(*) AS LEITURAS,
       ROUND(COUNT(CASE WHEN L.PULSO_OK = 'S' THEN 1 END) * 100 / COUNT(*), 1) AS PCT_PULSO_OK,
       ROUND(AVG(L.BATERIA_PCT), 1) AS BATERIA_MEDIA
  FROM LEITURA L
  JOIN DISPOSITIVO D ON D.ID = L.DISPOSITIVO_ID
  JOIN BAIRRO B ON B.ID = D.BAIRRO_ID
 WHERE L.REGISTRADA_EM >= TIMESTAMP '2026-11-08 00:00:00'
   AND L.REGISTRADA_EM <  TIMESTAMP '2026-11-15 00:00:00'
 GROUP BY B.NOME
 ORDER BY PCT_PULSO_OK, B.NOME;
