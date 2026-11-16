-- =====================================================================
-- Ressoa — Transição Oracle → MongoDB (executar no ORACLE)
-- Gera, com SQL/JSON, os documentos no mesmo formato de 01-carga.js:
-- prova que o modelo documental sai diretamente do relacional.
--   BAIRRO 1─N AVALIACAO_RISCO → array "avaliacoes" incorporado
--   BAIRRO 1─N DISPOSITIVO     → array "dispositivos" incorporado
--   USUARIO (dono)             → só o nome, dentro do aparelho (LGPD)
-- =====================================================================
SET LONG 1000000
SET LONGCHUNKSIZE 1000000
SET PAGESIZE 0
SET LINESIZE 32767

-- Collection "bairros"
SELECT JSON_SERIALIZE(
         JSON_OBJECT(
           '_id'              VALUE B.ID,
           'nome'             VALUE B.NOME,
           'municipio'        VALUE B.MUNICIPIO,
           'uf'               VALUE B.UF,
           'codigoIbge'       VALUE B.CODIGO_IBGE,
           'aguaIntermitente' VALUE CASE B.AGUA_INTERMITENTE WHEN 'S' THEN 'true' ELSE 'false' END FORMAT JSON,
           'nivelRisco'       VALUE B.NIVEL_RISCO,
           'avaliacoes'       VALUE (
               SELECT JSON_ARRAYAGG(
                        JSON_OBJECT('semanaEpidemiologica' VALUE A.SEMANA_EPIDEMIOLOGICA,
                                    'nivel'                VALUE A.NIVEL,
                                    'tempMinMedia'         VALUE A.TEMP_MIN_MEDIA,
                                    'casos100k'            VALUE A.CASOS_100K,
                                    'rt'                   VALUE A.RT,
                                    'criadaEm'             VALUE TO_CHAR(A.CRIADA_EM, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'))
                        ORDER BY A.CRIADA_EM RETURNING CLOB)
                 FROM AVALIACAO_RISCO A WHERE A.BAIRRO_ID = B.ID),
           'dispositivos'     VALUE (
               SELECT JSON_ARRAYAGG(
                        JSON_OBJECT('numeroSerie'    VALUE D.NUMERO_SERIE,
                                    'tipoRecipiente' VALUE D.TIPO_RECIPIENTE,
                                    'volumeLitros'   VALUE D.VOLUME_LITROS,
                                    'pulsosPorDia'   VALUE D.PULSOS_POR_DIA,
                                    'status'         VALUE D.STATUS,
                                    'dono'           VALUE U.NOME)
                        ORDER BY D.NUMERO_SERIE RETURNING CLOB)
                 FROM DISPOSITIVO D JOIN USUARIO U ON U.ID = D.USUARIO_ID
                WHERE D.BAIRRO_ID = B.ID)
           RETURNING CLOB)
       PRETTY) AS DOCUMENTO
  FROM BAIRRO B
 ORDER BY B.ID;

-- Collection "leituras" (série temporal; referência estendida ao aparelho)
SELECT JSON_OBJECT(
         'registradaEm'      VALUE TO_CHAR(L.REGISTRADA_EM, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
         'dispositivo'       VALUE JSON_OBJECT('numeroSerie' VALUE D.NUMERO_SERIE, 'bairroId' VALUE D.BAIRRO_ID),
         'chaveIdempotencia' VALUE L.CHAVE_IDEMPOTENCIA,
         'bateriaPct'        VALUE L.BATERIA_PCT,
         'temperaturaAgua'   VALUE L.TEMPERATURA_AGUA,
         'pulsoOk'           VALUE CASE L.PULSO_OK WHEN 'S' THEN 'true' ELSE 'false' END FORMAT JSON) AS DOCUMENTO
  FROM LEITURA L
  JOIN DISPOSITIVO D ON D.ID = L.DISPOSITIVO_ID
 ORDER BY L.REGISTRADA_EM, D.NUMERO_SERIE;
