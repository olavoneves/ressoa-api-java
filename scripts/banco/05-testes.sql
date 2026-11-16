-- =====================================================================
-- Ressoa — 05 · Testes dos pacotes e gatilhos
-- Disciplina: Mastering Relational and Non-Relational Database
--
-- Cada teste imprime [PASSOU] ou [FALHOU]. Os dados criados aqui ficam
-- num "bairro de teste" próprio e TUDO é desfeito no final (ROLLBACK),
-- então o script pode rodar no banco em uso pelas APIs.
-- =====================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    v_passou  PLS_INTEGER := 0;
    v_falhou  PLS_INTEGER := 0;

    v_bairro      BAIRRO.ID%TYPE;
    v_usuario     USUARIO.ID%TYPE;
    v_disp        DISPOSITIVO.ID%TYPE;
    v_aval        AVALIACAO_RISCO.ID%TYPE;
    v_nivel       AVALIACAO_RISCO.NIVEL%TYPE;
    v_subiu       VARCHAR2(1);
    v_leitura     LEITURA.ID%TYPE;
    v_leitura2    LEITURA.ID%TYPE;
    v_repetida    VARCHAR2(1);
    v_status      DISPOSITIVO.STATUS%TYPE;
    v_qtd         PLS_INTEGER;
    v_antigo      CLOB;
    v_codigo      PLS_INTEGER;

    PROCEDURE afirmar(p_condicao IN BOOLEAN, p_descricao IN VARCHAR2) IS
    BEGIN
        IF p_condicao THEN
            v_passou := v_passou + 1;
            DBMS_OUTPUT.PUT_LINE('[PASSOU] ' || p_descricao);
        ELSE
            v_falhou := v_falhou + 1;
            DBMS_OUTPUT.PUT_LINE('[FALHOU] ' || p_descricao);
        END IF;
    END afirmar;
BEGIN
    SAVEPOINT inicio_testes;

    -- ===== PKG_RISCO.fn_nivel_risco =====================================
    afirmar(PKG_RISCO.fn_nivel_risco(130, 0.8, 20) = 'VERMELHO', 'fn_nivel_risco: casos > 100 → VERMELHO');
    afirmar(PKG_RISCO.fn_nivel_risco(50, 1.2, 20)  = 'LARANJA',  'fn_nivel_risco: Rt > 1 → LARANJA');
    afirmar(PKG_RISCO.fn_nivel_risco(10, 0.9, 23)  = 'AMARELO',  'fn_nivel_risco: temperatura > 22 → AMARELO');
    afirmar(PKG_RISCO.fn_nivel_risco(10, 0.9, 20)  = 'VERDE',    'fn_nivel_risco: sem gatilho → VERDE');
    afirmar(PKG_RISCO.fn_nivel_risco(150, 1.5, 25) = 'VERMELHO', 'fn_nivel_risco: vários gatilhos → o mais grave');
    afirmar(PKG_RISCO.fn_nivel_risco(100, 1, 22)   = 'VERDE',    'fn_nivel_risco: limites exatos não disparam');
    afirmar(PKG_RISCO.fn_nivel_risco(NULL, NULL, NULL) = 'VERDE', 'fn_nivel_risco: indicadores nulos → VERDE');

    -- Massa de teste isolada
    INSERT INTO BAIRRO (NOME, MUNICIPIO, UF, CODIGO_IBGE, AGUA_INTERMITENTE)
    VALUES ('Bairro de Teste PL/SQL', 'São Paulo', 'SP', '3550308', 'S')
    RETURNING ID INTO v_bairro;
    INSERT INTO USUARIO (NOME, EMAIL, SENHA_HASH, PAPEL, BAIRRO_ID)
    VALUES ('Morador Teste', 'teste.plsql@ressoa.app', 'x', 'CIDADAO', v_bairro)
    RETURNING ID INTO v_usuario;

    -- ===== PKG_RISCO.pr_atualizar_risco_bairro ==========================
    PKG_RISCO.pr_atualizar_risco_bairro(v_bairro, 202646, 120.5, 1.3, 24.0, v_aval, v_nivel, v_subiu);
    SELECT NIVEL_RISCO INTO v_status FROM BAIRRO WHERE ID = v_bairro;
    afirmar(v_aval IS NOT NULL AND v_nivel = 'VERMELHO', 'pr_atualizar_risco_bairro: grava avaliação VERMELHO');
    afirmar(v_status = 'VERMELHO', 'pr_atualizar_risco_bairro: atualiza BAIRRO.NIVEL_RISCO');
    afirmar(v_subiu = 'S', 'pr_atualizar_risco_bairro: informa que o risco subiu (VERDE → VERMELHO)');

    PKG_RISCO.pr_atualizar_risco_bairro(v_bairro, 202647, 40, 0.9, 21, v_aval, v_nivel, v_subiu);
    afirmar(v_nivel = 'VERDE' AND v_subiu = 'N', 'pr_atualizar_risco_bairro: queda de risco não marca subida');

    BEGIN
        PKG_RISCO.pr_atualizar_risco_bairro(-1, 202646, 10, 0.5, 20, v_aval, v_nivel, v_subiu);
        afirmar(FALSE, 'pr_atualizar_risco_bairro: bairro inexistente deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20001, 'pr_atualizar_risco_bairro: bairro inexistente → ORA-20001');
    END;

    BEGIN
        PKG_RISCO.pr_atualizar_risco_bairro(v_bairro, 202699, 10, 0.5, 20, v_aval, v_nivel, v_subiu);
        afirmar(FALSE, 'pr_atualizar_risco_bairro: semana 99 deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20002, 'pr_atualizar_risco_bairro: semana inválida → ORA-20002');
    END;

    -- ===== PKG_DISPOSITIVO.fn_pct_dispositivos_ativos ===================
    afirmar(PKG_DISPOSITIVO.fn_pct_dispositivos_ativos(v_bairro) = 0, 'fn_pct_dispositivos_ativos: bairro sem aparelhos → 0');

    INSERT INTO DISPOSITIVO (NUMERO_SERIE, USUARIO_ID, BAIRRO_ID, TIPO_RECIPIENTE, VOLUME_LITROS, STATUS)
    VALUES ('TST-0001', v_usuario, v_bairro, 'TAMBOR', 200, 'ATIVO') RETURNING ID INTO v_disp;
    INSERT INTO DISPOSITIVO (NUMERO_SERIE, USUARIO_ID, BAIRRO_ID, TIPO_RECIPIENTE, VOLUME_LITROS, STATUS)
    VALUES ('TST-0002', v_usuario, v_bairro, 'BALDE', 20, 'ATIVO');
    INSERT INTO DISPOSITIVO (NUMERO_SERIE, USUARIO_ID, BAIRRO_ID, TIPO_RECIPIENTE, VOLUME_LITROS, STATUS)
    VALUES ('TST-0003', v_usuario, v_bairro, 'BALDE', 20, 'SEM_SINAL');
    INSERT INTO DISPOSITIVO (NUMERO_SERIE, USUARIO_ID, BAIRRO_ID, TIPO_RECIPIENTE, VOLUME_LITROS, STATUS)
    VALUES ('TST-0004', v_usuario, v_bairro, 'BALDE', 20, 'INATIVO');
    afirmar(PKG_DISPOSITIVO.fn_pct_dispositivos_ativos(v_bairro) = 66.7,
            'fn_pct_dispositivos_ativos: 2 ativos de 3 (INATIVO fora da conta) → 66,7%');

    -- ===== Gatilho de auditoria =========================================
    SELECT COUNT(*) INTO v_qtd FROM AUDITORIA
     WHERE TABELA = 'DISPOSITIVO' AND OPERACAO = 'INSERT'
       AND JSON_VALUE(VALOR_NOVO, '$.numeroSerie') LIKE 'TST-%';
    afirmar(v_qtd = 4, 'TRG_AUDITORIA_DISPOSITIVO: registra os 4 INSERTs');

    UPDATE DISPOSITIVO SET VOLUME_LITROS = 250 WHERE ID = v_disp;
    SELECT VALOR_ANTIGO INTO v_antigo FROM AUDITORIA
     WHERE TABELA = 'DISPOSITIVO' AND OPERACAO = 'UPDATE'
       AND JSON_VALUE(VALOR_NOVO, '$.numeroSerie') = 'TST-0001'
       AND JSON_VALUE(VALOR_NOVO, '$.volumeLitros') = '250';
    afirmar(JSON_VALUE(v_antigo, '$.volumeLitros') = '200',
            'TRG_AUDITORIA_DISPOSITIVO: UPDATE guarda valor antigo (200) e novo (250)');

    -- ===== PKG_DISPOSITIVO.pr_registrar_leitura + TRG_BATERIA_BAIXA =====
    PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-1', 80, 24.5, 'S', v_leitura, v_repetida);
    afirmar(v_leitura IS NOT NULL AND v_repetida = 'N', 'pr_registrar_leitura: grava leitura nova');

    PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-1', 80, 24.5, 'S', v_leitura2, v_repetida);
    SELECT COUNT(*) INTO v_qtd FROM LEITURA WHERE CHAVE_IDEMPOTENCIA = 'teste-chave-1';
    afirmar(v_leitura2 = v_leitura AND v_repetida = 'S' AND v_qtd = 1,
            'pr_registrar_leitura: mesma chave não duplica (idempotência)');

    BEGIN
        PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-1', 50, 24.5, 'S', v_leitura2, v_repetida);
        afirmar(FALSE, 'pr_registrar_leitura: chave reutilizada deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20014, 'pr_registrar_leitura: chave com conteúdo diferente → ORA-20014');
    END;

    PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-2', 9, 26.0, 'N', v_leitura, v_repetida);
    SELECT STATUS INTO v_status FROM DISPOSITIVO WHERE ID = v_disp;
    afirmar(v_status = 'BATERIA_BAIXA', 'TRG_BATERIA_BAIXA: bateria 9% → BATERIA_BAIXA');

    SELECT COUNT(*) INTO v_qtd FROM AUDITORIA
     WHERE TABELA = 'DISPOSITIVO' AND OPERACAO = 'UPDATE'
       AND JSON_VALUE(VALOR_NOVO, '$.numeroSerie') = 'TST-0001'
       AND JSON_VALUE(VALOR_ANTIGO, '$.status') = 'ATIVO'
       AND JSON_VALUE(VALOR_NOVO, '$.status') = 'BATERIA_BAIXA';
    afirmar(v_qtd = 1, 'Gatilhos em cadeia: mudança de status feita pelo gatilho também é auditada');

    PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-3', 95, 24.0, 'S', v_leitura, v_repetida);
    SELECT STATUS INTO v_status FROM DISPOSITIVO WHERE ID = v_disp;
    afirmar(v_status = 'ATIVO', 'pr_registrar_leitura: bateria recarregada reativa o aparelho');

    INSERT INTO LEITURA (DISPOSITIVO_ID, CHAVE_IDEMPOTENCIA, REGISTRADA_EM, BATERIA_PCT, TEMPERATURA_AGUA, PULSO_OK)
    VALUES (v_disp, 'teste-chave-antiga', SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '2' DAY, 5, 24.0, 'S');
    SELECT STATUS INTO v_status FROM DISPOSITIVO WHERE ID = v_disp;
    afirmar(v_status = 'ATIVO', 'TRG_BATERIA_BAIXA: leitura antiga (acumulada offline) não muda o status');

    afirmar(PKG_DISPOSITIVO.fn_taxa_sucesso_pulso(v_disp, 7) = 75,
            'fn_taxa_sucesso_pulso: 3 pulsos OK de 4 leituras → 75%');

    BEGIN
        PKG_DISPOSITIVO.pr_registrar_leitura('TST-0004', 'teste-chave-4', 80, 24.0, 'S', v_leitura, v_repetida);
        afirmar(FALSE, 'pr_registrar_leitura: aparelho inativo deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20012, 'pr_registrar_leitura: aparelho INATIVO → ORA-20012');
    END;

    BEGIN
        PKG_DISPOSITIVO.pr_registrar_leitura('NAO-EXISTE', 'teste-chave-5', 80, 24.0, 'S', v_leitura, v_repetida);
        afirmar(FALSE, 'pr_registrar_leitura: aparelho inexistente deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20011, 'pr_registrar_leitura: aparelho inexistente → ORA-20011');
    END;

    BEGIN
        PKG_DISPOSITIVO.pr_registrar_leitura('TST-0001', 'teste-chave-6', 150, 24.0, 'S', v_leitura, v_repetida);
        afirmar(FALSE, 'pr_registrar_leitura: bateria 150% deveria falhar');
    EXCEPTION
        WHEN OTHERS THEN
            afirmar(SQLCODE = -20013, 'pr_registrar_leitura: leitura fora da faixa → ORA-20013');
    END;

    -- ===== Resultado ====================================================
    ROLLBACK TO inicio_testes;  -- desfaz toda a massa de teste
    DBMS_OUTPUT.PUT_LINE(RPAD('=', 60, '='));
    DBMS_OUTPUT.PUT_LINE('Testes: ' || (v_passou + v_falhou) || ' · passaram: ' || v_passou
                         || ' · falharam: ' || v_falhou);
    IF v_falhou > 0 THEN
        RAISE_APPLICATION_ERROR(-20999, v_falhou || ' teste(s) falharam');
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        v_codigo := SQLCODE;
        ROLLBACK TO inicio_testes;
        IF v_codigo <> -20999 THEN
            DBMS_OUTPUT.PUT_LINE('[ERRO INESPERADO] ' || SQLERRM);
        END IF;
        RAISE;
END;
/
