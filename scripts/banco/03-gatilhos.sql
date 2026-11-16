-- =====================================================================
-- Ressoa — 03 · Gatilhos
-- Disciplina: Mastering Relational and Non-Relational Database
--
--   TRG_AUDITORIA_DISPOSITIVO  trilha de auditoria (data, operação,
--                              valor antigo e novo) em AUDITORIA
--   TRG_BATERIA_BAIXA          leitura com bateria < 15% marca o aparelho
--                              como BATERIA_BAIXA
--
-- Os gatilhos valem para qualquer origem (API Java, API .NET, PL/SQL ou
-- SQL manual), por isso ficam no banco.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Auditoria de DISPOSITIVO: quem (usuário do banco), quando, o quê e
-- os valores antes/depois em JSON.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AUDITORIA_DISPOSITIVO
AFTER INSERT OR UPDATE OR DELETE ON DISPOSITIVO
FOR EACH ROW
DECLARE
    v_operacao AUDITORIA.OPERACAO%TYPE;
    v_antigo   CLOB;
    v_novo     CLOB;
BEGIN
    IF INSERTING THEN
        v_operacao := 'INSERT';
    ELSIF UPDATING THEN
        v_operacao := 'UPDATE';
    ELSE
        v_operacao := 'DELETE';
    END IF;

    IF UPDATING OR DELETING THEN
        SELECT JSON_OBJECT(
            'id'             VALUE :OLD.ID,
            'numeroSerie'    VALUE :OLD.NUMERO_SERIE,
            'usuarioId'      VALUE :OLD.USUARIO_ID,
            'bairroId'       VALUE :OLD.BAIRRO_ID,
            'tipoRecipiente' VALUE :OLD.TIPO_RECIPIENTE,
            'volumeLitros'   VALUE :OLD.VOLUME_LITROS,
            'pulsosPorDia'   VALUE :OLD.PULSOS_POR_DIA,
            'status'         VALUE :OLD.STATUS
            RETURNING CLOB)
          INTO v_antigo
          FROM DUAL;
    END IF;

    IF INSERTING OR UPDATING THEN
        SELECT JSON_OBJECT(
            'id'             VALUE :NEW.ID,
            'numeroSerie'    VALUE :NEW.NUMERO_SERIE,
            'usuarioId'      VALUE :NEW.USUARIO_ID,
            'bairroId'       VALUE :NEW.BAIRRO_ID,
            'tipoRecipiente' VALUE :NEW.TIPO_RECIPIENTE,
            'volumeLitros'   VALUE :NEW.VOLUME_LITROS,
            'pulsosPorDia'   VALUE :NEW.PULSOS_POR_DIA,
            'status'         VALUE :NEW.STATUS
            RETURNING CLOB)
          INTO v_novo
          FROM DUAL;
    END IF;

    INSERT INTO AUDITORIA (TABELA, OPERACAO, VALOR_ANTIGO, VALOR_NOVO, USUARIO_BD, DATA_HORA)
    VALUES ('DISPOSITIVO', v_operacao, v_antigo, v_novo, USER, SYSTIMESTAMP);
END;
/

-- ---------------------------------------------------------------------
-- Bateria baixa: uma leitura em tempo real (registrada há menos de 1 h)
-- com bateria abaixo de 15% marca o aparelho como BATERIA_BAIXA.
-- Leituras antigas, acumuladas enquanto o aparelho estava sem sinal,
-- não mudam o status (mesma regra da API .NET), e aparelho INATIVO
-- (desligado pelo dono) não é alterado.
-- ---------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_BATERIA_BAIXA
AFTER INSERT ON LEITURA
FOR EACH ROW
WHEN (NEW.BATERIA_PCT < 15)
BEGIN
    -- As APIs gravam REGISTRADA_EM em UTC; compara no mesmo fuso.
    IF :NEW.REGISTRADA_EM >= SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' HOUR THEN
        UPDATE DISPOSITIVO
           SET STATUS = 'BATERIA_BAIXA'
         WHERE ID = :NEW.DISPOSITIVO_ID
           AND STATUS IN ('ATIVO', 'SEM_SINAL');
    END IF;
END;
/

-- Conferência: gatilhos habilitados
SELECT TRIGGER_NAME, TABLE_NAME, TRIGGERING_EVENT, STATUS
  FROM USER_TRIGGERS
 WHERE TRIGGER_NAME IN ('TRG_AUDITORIA_DISPOSITIVO', 'TRG_BATERIA_BAIXA')
 ORDER BY TRIGGER_NAME;
