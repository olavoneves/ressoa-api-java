package br.com.ressoa.dispositivo;

/**
 * Situação operacional do aparelho. {@code SEM_SINAL} e {@code BATERIA_BAIXA} são
 * derivados da telemetria (API .NET e gatilho do banco).
 */
public enum StatusDispositivo {
    ATIVO,
    SEM_SINAL,
    BATERIA_BAIXA,
    INATIVO
}
