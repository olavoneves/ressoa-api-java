-- =====================================================================
-- Ressoa — 02 · Pacotes PL/SQL (funções e procedimentos)
-- Disciplina: Mastering Relational and Non-Relational Database
--
--   PKG_RISCO        fn_nivel_risco            (função)
--                    pr_atualizar_risco_bairro (procedimento)
--   PKG_DISPOSITIVO  fn_pct_dispositivos_ativos (função)
--                    fn_taxa_sucesso_pulso      (função extra)
--                    pr_registrar_leitura       (procedimento)
--
-- Datas de negócio em UTC (mesma convenção das APIs Java e .NET).
-- As regras são as mesmas das APIs (Java: ClassificadorRisco;
-- .NET: RegrasTelemetria), para o banco dar a mesma resposta que o app.
--
-- Erros de negócio (RAISE_APPLICATION_ERROR):
--   -20001 bairro inexistente          -20002 semana epidemiológica inválida
--   -20003 indicador fora da faixa     -20011 aparelho inexistente
--   -20012 aparelho inativo            -20013 leitura fora da faixa
--   -20014 chave de idempotência reutilizada com conteúdo diferente
-- =====================================================================

-- ---------------------------------------------------------------------
-- PKG_RISCO: classificação do risco de dengue por bairro
-- ---------------------------------------------------------------------
CREATE OR REPLACE PACKAGE PKG_RISCO AS
    -- Limiares da tabela de gatilhos do Ressoa
    C_CASOS_VERMELHO CONSTANT NUMBER := 100;  -- casos por 100 mil habitantes
    C_RT_LARANJA     CONSTANT NUMBER := 1;    -- número de reprodução
    C_TEMP_AMARELO   CONSTANT NUMBER := 22;   -- temperatura mínima média (°C)

    -- Aplica a tabela de gatilhos (o mais grave vence):
    -- casos > 100 → VERMELHO; Rt > 1 → LARANJA; temp. mínima > 22 → AMARELO; senão VERDE.
    FUNCTION fn_nivel_risco(
        p_casos_100k IN NUMBER,
        p_rt         IN NUMBER,
        p_temp_min   IN NUMBER
    ) RETURN VARCHAR2 DETERMINISTIC;

    -- Registra a avaliação semanal do bairro, calcula o nível e atualiza
    -- BAIRRO.NIVEL_RISCO. Devolve o ID da avaliação e se o risco subiu
    -- (base para alertar os moradores).
    PROCEDURE pr_atualizar_risco_bairro(
        p_bairro_id    IN  BAIRRO.ID%TYPE,
        p_semana       IN  AVALIACAO_RISCO.SEMANA_EPIDEMIOLOGICA%TYPE,
        p_casos_100k   IN  AVALIACAO_RISCO.CASOS_100K%TYPE,
        p_rt           IN  AVALIACAO_RISCO.RT%TYPE,
        p_temp_min     IN  AVALIACAO_RISCO.TEMP_MIN_MEDIA%TYPE,
        o_avaliacao_id OUT AVALIACAO_RISCO.ID%TYPE,
        o_nivel        OUT AVALIACAO_RISCO.NIVEL%TYPE,
        o_risco_subiu  OUT VARCHAR2
    );

    -- Peso do nível (VERDE=1 ... VERMELHO=4), útil para comparar níveis.
    FUNCTION fn_peso_nivel(p_nivel IN VARCHAR2) RETURN PLS_INTEGER DETERMINISTIC;
END PKG_RISCO;
/

CREATE OR REPLACE PACKAGE BODY PKG_RISCO AS

    FUNCTION fn_nivel_risco(
        p_casos_100k IN NUMBER,
        p_rt         IN NUMBER,
        p_temp_min   IN NUMBER
    ) RETURN VARCHAR2 DETERMINISTIC IS
    BEGIN
        IF p_casos_100k > C_CASOS_VERMELHO THEN
            RETURN 'VERMELHO';
        ELSIF p_rt > C_RT_LARANJA THEN
            RETURN 'LARANJA';
        ELSIF p_temp_min > C_TEMP_AMARELO THEN
            RETURN 'AMARELO';
        END IF;
        RETURN 'VERDE';  -- sem gatilho (indicadores nulos também caem aqui)
    END fn_nivel_risco;

    FUNCTION fn_peso_nivel(p_nivel IN VARCHAR2) RETURN PLS_INTEGER DETERMINISTIC IS
    BEGIN
        RETURN CASE p_nivel
                   WHEN 'VERDE'    THEN 1
                   WHEN 'AMARELO'  THEN 2
                   WHEN 'LARANJA'  THEN 3
                   WHEN 'VERMELHO' THEN 4
                   ELSE 0
               END;
    END fn_peso_nivel;

    PROCEDURE pr_atualizar_risco_bairro(
        p_bairro_id    IN  BAIRRO.ID%TYPE,
        p_semana       IN  AVALIACAO_RISCO.SEMANA_EPIDEMIOLOGICA%TYPE,
        p_casos_100k   IN  AVALIACAO_RISCO.CASOS_100K%TYPE,
        p_rt           IN  AVALIACAO_RISCO.RT%TYPE,
        p_temp_min     IN  AVALIACAO_RISCO.TEMP_MIN_MEDIA%TYPE,
        o_avaliacao_id OUT AVALIACAO_RISCO.ID%TYPE,
        o_nivel        OUT AVALIACAO_RISCO.NIVEL%TYPE,
        o_risco_subiu  OUT VARCHAR2
    ) IS
        v_nivel_anterior BAIRRO.NIVEL_RISCO%TYPE;
        v_semana_num     PLS_INTEGER := MOD(p_semana, 100);
    BEGIN
        -- Validações de negócio
        IF p_semana IS NULL OR v_semana_num NOT BETWEEN 1 AND 53 OR p_semana < 200001 THEN
            RAISE_APPLICATION_ERROR(-20002, 'Semana epidemiológica inválida (formato AAAASS): ' || p_semana);
        END IF;
        IF p_casos_100k < 0 OR p_rt < 0 OR p_temp_min NOT BETWEEN -10 AND 45 THEN
            RAISE_APPLICATION_ERROR(-20003, 'Indicador fora da faixa permitida');
        END IF;

        BEGIN
            SELECT NIVEL_RISCO INTO v_nivel_anterior
              FROM BAIRRO
             WHERE ID = p_bairro_id
               FOR UPDATE;  -- evita duas avaliações simultâneas no mesmo bairro
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20001, 'Bairro inexistente: ' || p_bairro_id);
        END;

        o_nivel := fn_nivel_risco(p_casos_100k, p_rt, p_temp_min);

        INSERT INTO AVALIACAO_RISCO (BAIRRO_ID, SEMANA_EPIDEMIOLOGICA, NIVEL, TEMP_MIN_MEDIA, CASOS_100K, RT, CRIADA_EM)
        VALUES (p_bairro_id, p_semana, o_nivel, p_temp_min, p_casos_100k, p_rt, SYS_EXTRACT_UTC(SYSTIMESTAMP))  -- UTC, como as APIs
        RETURNING ID INTO o_avaliacao_id;

        UPDATE BAIRRO SET NIVEL_RISCO = o_nivel WHERE ID = p_bairro_id;

        o_risco_subiu := CASE WHEN fn_peso_nivel(o_nivel) > fn_peso_nivel(v_nivel_anterior) THEN 'S' ELSE 'N' END;
        -- Sem COMMIT: quem chama controla a transação.
    END pr_atualizar_risco_bairro;

END PKG_RISCO;
/

-- ---------------------------------------------------------------------
-- PKG_DISPOSITIVO: telemetria e cobertura dos aparelhos Ressoa
-- ---------------------------------------------------------------------
CREATE OR REPLACE PACKAGE PKG_DISPOSITIVO AS
    C_BATERIA_MINIMA CONSTANT NUMBER := 15;  -- abaixo disso: BATERIA_BAIXA

    -- % de aparelhos ATIVOS do bairro (desconsidera INATIVOS, que o dono
    -- desligou). Bairro sem aparelhos → 0.
    FUNCTION fn_pct_dispositivos_ativos(p_bairro_id IN BAIRRO.ID%TYPE) RETURN NUMBER;

    -- % de pulsos bem-sucedidos do aparelho nos últimos p_dias até p_referencia (UTC)
    -- (NULL se não houver leitura no período).
    FUNCTION fn_taxa_sucesso_pulso(
        p_dispositivo_id IN DISPOSITIVO.ID%TYPE,
        p_dias           IN PLS_INTEGER DEFAULT 7,
        p_referencia     IN TIMESTAMP DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP)
    ) RETURN NUMBER;

    -- Registra a leitura diária do aparelho de forma idempotente:
    -- a mesma chave com o mesmo conteúdo devolve a leitura já gravada
    -- (o_repetida = 'S'); com conteúdo diferente → erro -20014.
    -- Bateria baixa é tratada pelo gatilho TRG_BATERIA_BAIXA; bateria boa
    -- reativa o aparelho que estava BATERIA_BAIXA ou SEM_SINAL.
    PROCEDURE pr_registrar_leitura(
        p_numero_serie IN  DISPOSITIVO.NUMERO_SERIE%TYPE,
        p_chave        IN  LEITURA.CHAVE_IDEMPOTENCIA%TYPE,
        p_bateria_pct  IN  LEITURA.BATERIA_PCT%TYPE,
        p_temperatura  IN  LEITURA.TEMPERATURA_AGUA%TYPE,
        p_pulso_ok     IN  LEITURA.PULSO_OK%TYPE,
        o_leitura_id   OUT LEITURA.ID%TYPE,
        o_repetida     OUT VARCHAR2
    );
END PKG_DISPOSITIVO;
/

CREATE OR REPLACE PACKAGE BODY PKG_DISPOSITIVO AS

    FUNCTION fn_pct_dispositivos_ativos(p_bairro_id IN BAIRRO.ID%TYPE) RETURN NUMBER IS
        v_total  PLS_INTEGER;
        v_ativos PLS_INTEGER;
    BEGIN
        SELECT COUNT(*),
               COUNT(CASE WHEN STATUS = 'ATIVO' THEN 1 END)
          INTO v_total, v_ativos
          FROM DISPOSITIVO
         WHERE BAIRRO_ID = p_bairro_id
           AND STATUS <> 'INATIVO';

        IF v_total = 0 THEN
            RETURN 0;
        END IF;
        RETURN ROUND(v_ativos * 100 / v_total, 1);
    END fn_pct_dispositivos_ativos;

    FUNCTION fn_taxa_sucesso_pulso(
        p_dispositivo_id IN DISPOSITIVO.ID%TYPE,
        p_dias           IN PLS_INTEGER DEFAULT 7,
        p_referencia     IN TIMESTAMP DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP)
    ) RETURN NUMBER IS
        v_total PLS_INTEGER;
        v_ok    PLS_INTEGER;
    BEGIN
        SELECT COUNT(*), COUNT(CASE WHEN PULSO_OK = 'S' THEN 1 END)
          INTO v_total, v_ok
          FROM LEITURA
         WHERE DISPOSITIVO_ID = p_dispositivo_id
           AND REGISTRADA_EM >  p_referencia - NUMTODSINTERVAL(p_dias, 'DAY')
           AND REGISTRADA_EM <= p_referencia;

        IF v_total = 0 THEN
            RETURN NULL;
        END IF;
        RETURN ROUND(v_ok * 100 / v_total, 1);
    END fn_taxa_sucesso_pulso;

    PROCEDURE pr_registrar_leitura(
        p_numero_serie IN  DISPOSITIVO.NUMERO_SERIE%TYPE,
        p_chave        IN  LEITURA.CHAVE_IDEMPOTENCIA%TYPE,
        p_bateria_pct  IN  LEITURA.BATERIA_PCT%TYPE,
        p_temperatura  IN  LEITURA.TEMPERATURA_AGUA%TYPE,
        p_pulso_ok     IN  LEITURA.PULSO_OK%TYPE,
        o_leitura_id   OUT LEITURA.ID%TYPE,
        o_repetida     OUT VARCHAR2
    ) IS
        v_disp_id  DISPOSITIVO.ID%TYPE;
        v_status   DISPOSITIVO.STATUS%TYPE;
        r_existente LEITURA%ROWTYPE;
    BEGIN
        IF p_bateria_pct NOT BETWEEN 0 AND 100 OR p_temperatura NOT BETWEEN -10 AND 60
           OR p_pulso_ok NOT IN ('S', 'N') OR p_chave IS NULL THEN
            RAISE_APPLICATION_ERROR(-20013, 'Leitura fora da faixa ou incompleta');
        END IF;

        BEGIN
            SELECT ID, STATUS INTO v_disp_id, v_status
              FROM DISPOSITIVO
             WHERE NUMERO_SERIE = p_numero_serie;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(-20011, 'Aparelho inexistente: ' || p_numero_serie);
        END;

        -- Idempotência: a chave já foi usada?
        BEGIN
            SELECT * INTO r_existente FROM LEITURA WHERE CHAVE_IDEMPOTENCIA = p_chave;
            IF r_existente.DISPOSITIVO_ID = v_disp_id
               AND r_existente.BATERIA_PCT = p_bateria_pct
               AND r_existente.TEMPERATURA_AGUA = p_temperatura
               AND r_existente.PULSO_OK = p_pulso_ok THEN
                o_leitura_id := r_existente.ID;
                o_repetida   := 'S';
                RETURN;
            END IF;
            RAISE_APPLICATION_ERROR(-20014, 'Chave de idempotência já usada com outro conteúdo: ' || p_chave);
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                NULL;  -- chave nova: segue para gravar
        END;

        IF v_status = 'INATIVO' THEN
            RAISE_APPLICATION_ERROR(-20012, 'Aparelho inativo não recebe leituras: ' || p_numero_serie);
        END IF;

        INSERT INTO LEITURA (DISPOSITIVO_ID, CHAVE_IDEMPOTENCIA, REGISTRADA_EM, BATERIA_PCT, TEMPERATURA_AGUA, PULSO_OK)
        VALUES (v_disp_id, p_chave, SYS_EXTRACT_UTC(SYSTIMESTAMP), p_bateria_pct, p_temperatura, p_pulso_ok)  -- UTC, como as APIs
        RETURNING ID INTO o_leitura_id;
        o_repetida := 'N';

        -- Bateria recuperada reativa o aparelho (a queda é tratada pelo gatilho)
        IF p_bateria_pct >= C_BATERIA_MINIMA AND v_status IN ('BATERIA_BAIXA', 'SEM_SINAL') THEN
            UPDATE DISPOSITIVO SET STATUS = 'ATIVO' WHERE ID = v_disp_id;
        END IF;
    END pr_registrar_leitura;

END PKG_DISPOSITIVO;
/

-- Conferência: objetos válidos
SELECT OBJECT_NAME, OBJECT_TYPE, STATUS
  FROM USER_OBJECTS
 WHERE OBJECT_NAME IN ('PKG_RISCO', 'PKG_DISPOSITIVO')
 ORDER BY OBJECT_NAME, OBJECT_TYPE;
