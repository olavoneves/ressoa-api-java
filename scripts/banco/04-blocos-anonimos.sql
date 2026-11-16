-- =====================================================================
-- Ressoa — 04 · Blocos PL/SQL anônimos
-- Disciplina: Mastering Relational and Non-Relational Database
--
-- Bloco 1 (cursor + repetição + decisão): painel de prioridade por bairro
-- Bloco 2 (cursor + repetição + decisão): conferência da classificação
--
-- Somente leitura: podem ser executados a qualquer momento, inclusive no
-- banco em uso pelas APIs.
-- =====================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

-- ---------------------------------------------------------------------
-- Bloco 1 — Painel de prioridade para o gestor
-- Para cada bairro: nível atual, cobertura de aparelhos ativos e a ação
-- recomendada. Decide a prioridade combinando risco e cobertura:
--   risco LARANJA/VERMELHO com cobertura < 80%  → PRIORIDADE MÁXIMA
--   risco LARANJA/VERMELHO com cobertura >= 80% → MONITORAR DE PERTO
--   água intermitente sem aparelho              → INSTALAR RESSOA
--   demais                                      → ROTINA
-- ---------------------------------------------------------------------
DECLARE
    CURSOR c_bairros IS
        SELECT B.ID, B.NOME, B.MUNICIPIO, B.UF, B.NIVEL_RISCO, B.AGUA_INTERMITENTE,
               (SELECT COUNT(*) FROM DISPOSITIVO D
                 WHERE D.BAIRRO_ID = B.ID AND D.STATUS <> 'INATIVO') AS APARELHOS
          FROM BAIRRO B
         ORDER BY PKG_RISCO.fn_peso_nivel(B.NIVEL_RISCO) DESC, B.NOME;

    v_cobertura   NUMBER;
    v_prioridade  VARCHAR2(30);
    v_qtd_maxima  PLS_INTEGER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('PAINEL DE PRIORIDADE — RESSOA');
    DBMS_OUTPUT.PUT_LINE(RPAD('Bairro', 28) || RPAD('Nível', 10) || RPAD('Aparelhos', 11)
                         || RPAD('Ativos', 9) || 'Prioridade');
    DBMS_OUTPUT.PUT_LINE(RPAD('-', 80, '-'));

    FOR r IN c_bairros LOOP
        v_cobertura := PKG_DISPOSITIVO.fn_pct_dispositivos_ativos(r.ID);

        IF r.NIVEL_RISCO IN ('LARANJA', 'VERMELHO') AND v_cobertura < 80 THEN
            v_prioridade := 'PRIORIDADE MÁXIMA';
            v_qtd_maxima := v_qtd_maxima + 1;
        ELSIF r.NIVEL_RISCO IN ('LARANJA', 'VERMELHO') THEN
            v_prioridade := 'MONITORAR DE PERTO';
        ELSIF r.AGUA_INTERMITENTE = 'S' AND r.APARELHOS = 0 THEN
            v_prioridade := 'INSTALAR RESSOA';
        ELSE
            v_prioridade := 'ROTINA';
        END IF;

        DBMS_OUTPUT.PUT_LINE(RPAD(r.NOME || '/' || r.UF, 28) || RPAD(r.NIVEL_RISCO, 10)
                             || RPAD(r.APARELHOS, 11) || RPAD(v_cobertura || '%', 9) || v_prioridade);
    END LOOP;

    DBMS_OUTPUT.PUT_LINE(RPAD('-', 80, '-'));
    DBMS_OUTPUT.PUT_LINE('Bairros em prioridade máxima: ' || v_qtd_maxima);
END;
/

-- ---------------------------------------------------------------------
-- Bloco 2 — Conferência da classificação de risco
-- Percorre a avaliação mais recente de cada bairro, recalcula o nível com
-- PKG_RISCO.fn_nivel_risco a partir dos indicadores e compara com o nível
-- gravado (pode ter sido informado manualmente pelo gestor). Mostra a
-- tendência em relação à semana anterior.
-- ---------------------------------------------------------------------
DECLARE
    CURSOR c_ultimas IS
        SELECT A.BAIRRO_ID, B.NOME, A.SEMANA_EPIDEMIOLOGICA, A.NIVEL,
               A.CASOS_100K, A.RT, A.TEMP_MIN_MEDIA,
               LAG(A.NIVEL) OVER (PARTITION BY A.BAIRRO_ID
                                  ORDER BY A.CRIADA_EM, A.ID) AS NIVEL_ANTERIOR,
               ROW_NUMBER() OVER (PARTITION BY A.BAIRRO_ID
                                  ORDER BY A.CRIADA_EM DESC, A.ID DESC) AS POSICAO
          FROM AVALIACAO_RISCO A
          JOIN BAIRRO B ON B.ID = A.BAIRRO_ID;

    v_calculado   VARCHAR2(10);
    v_tendencia   VARCHAR2(12);
    v_conferidos  PLS_INTEGER := 0;
    v_divergentes PLS_INTEGER := 0;
BEGIN
    DBMS_OUTPUT.PUT_LINE('CONFERÊNCIA DA CLASSIFICAÇÃO (avaliação mais recente por bairro)');

    FOR r IN c_ultimas LOOP
        IF r.POSICAO = 1 THEN   -- só a avaliação mais recente de cada bairro
            v_conferidos := v_conferidos + 1;
            v_calculado  := PKG_RISCO.fn_nivel_risco(r.CASOS_100K, r.RT, r.TEMP_MIN_MEDIA);

            v_tendencia := CASE
                WHEN r.NIVEL_ANTERIOR IS NULL THEN 'primeira'
                WHEN PKG_RISCO.fn_peso_nivel(r.NIVEL) > PKG_RISCO.fn_peso_nivel(r.NIVEL_ANTERIOR) THEN 'SUBIU'
                WHEN PKG_RISCO.fn_peso_nivel(r.NIVEL) < PKG_RISCO.fn_peso_nivel(r.NIVEL_ANTERIOR) THEN 'caiu'
                ELSE 'estável'
            END;

            IF v_calculado <> r.NIVEL THEN
                v_divergentes := v_divergentes + 1;
                DBMS_OUTPUT.PUT_LINE('  [DIVERGE] ' || r.NOME || ' SE ' || r.SEMANA_EPIDEMIOLOGICA
                                     || ': gravado ' || r.NIVEL || ', regra indica ' || v_calculado);
            ELSE
                DBMS_OUTPUT.PUT_LINE('  [OK] ' || RPAD(r.NOME, 22) || ' SE ' || r.SEMANA_EPIDEMIOLOGICA
                                     || ' ' || RPAD(r.NIVEL, 9) || ' tendência: ' || v_tendencia);
            END IF;
        END IF;
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('Conferidos: ' || v_conferidos || ' · divergentes: ' || v_divergentes);
END;
/
