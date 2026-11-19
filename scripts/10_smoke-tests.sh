#!/usr/bin/env bash
#
# 10 - Smoke test contra a API Java publicada pela pipeline.
#
#   1. espera o /actuator/health responder (ate 10 min: o Spring Boot levou
#      ~4,5 min no B1 da Sprint 4). O health inclui banco e RabbitMQ.
#   2. login do gestor@ressoa.app, criado pela carga inicial da API
#      (DadosIniciais) com a senha SEED_SENHA_PADRAO: prova a cadeia
#      Key Vault -> variable group -> pipeline -> app settings -> app -> Oracle
#   3. CRUD completo nas duas tabelas do video: BAIRRO e DISPOSITIVO
#
# Tudo pela URL publica (https) - entrega em localhost zera a nota. No video,
# a persistencia de cada operacao e mostrada com SELECT no banco, nao com GET.
#
# Os registros criados aqui sao apagados no fim. Rode de novo a cada deploy
# ou a cada 98_retomar.sh (o banco volta zerado).
#
# Validacao local (sem Azure): WEBAPP_API_URL=http://localhost:8080 e
# SEED_SENHA_PADRAO na sessao; ai a senha nao e lida do Key Vault.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando curl

titulo "10 - SMOKE TEST"
API="${WEBAPP_API_URL}/api"
echo "Endpoint base: ${API}"

# Ao lado do script, e nao em /tmp: no Git Bash o curl.exe nao enxerga o
# /tmp emulado. `pwd -W` (C:/...): com MSYS_NO_PATHCONV o curl.exe nao
# entende /c/..., e o script pode ser chamado por caminho absoluto.
CORPO="$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })/.smoke-resposta.tmp"
trap 'rm -f "${CORPO}"' EXIT

FALHAS=0

# chamar <esperado> <rotulo> <args do curl...>
chamar() {
    local esperado="$1" rotulo="$2" obtido
    shift 2
    obtido=$(curl -s -o "${CORPO}" -w "%{http_code}" --max-time 60 "$@" || true)
    if [[ "${obtido}" == "${esperado}" ]]; then
        printf "  [ OK ] %-44s %s\n" "${rotulo}" "${obtido}"
    else
        printf "  [FALHA] %-43s esperado %s, obtido %s\n" "${rotulo}" "${esperado}" "${obtido}"
        echo "         corpo: $(head -c 300 "${CORPO}" 2>/dev/null)"
        FALHAS=$((FALHAS + 1))
    fi
}

primeiro_id() {
    grep -o '"id":[0-9]*' "${CORPO}" | head -1 | cut -d: -f2
}

titulo "PASSO 1 - SAUDE DA APLICACAO"
esperar_api
chamar 200 "GET    /actuator/health" "${WEBAPP_API_URL}/actuator/health"

titulo "PASSO 2 - LOGIN DO GESTOR"
# A senha vem do Key Vault: nenhum literal no repositorio vira credencial.
SENHA="${SEED_SENHA_PADRAO:-$(ler_segredo "${SECRET_SEED}")}"
chamar 200 "POST   /api/auth/login" \
    -X POST "${API}/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${SEED_GESTOR_EMAIL}\",\"senha\":\"${SENHA}\"}"
unset SENHA

TOKEN=$(grep -o '"token":"[^"]*' "${CORPO}" | cut -d'"' -f4 || true)
if [[ -z "${TOKEN}" ]]; then
    echo "ERRO: sem token. A carga inicial rodou (SEED_HABILITADO) com o SEED_SENHA_PADRAO do Key Vault?" >&2
    exit 1
fi
AUTH="Authorization: Bearer ${TOKEN}"
echo "         token obtido (${#TOKEN} chars)"

# Valores unicos por execucao (NOME+CODIGO_IBGE e NUMERO_SERIE sao UNIQUE):
# script reexecutavel.
CARIMBO="$(date +%s)"

titulo "PASSO 3 - TABELA BAIRRO"
# O nivel de risco nao entra no payload: muda pelas avaliacoes de risco.
chamar 201 "POST   /api/bairros" \
    -X POST "${API}/bairros" -H "${AUTH}" -H "Content-Type: application/json" \
    -d "{\"nome\":\"Smoke ${CARIMBO}\",\"municipio\":\"Recife\",\"uf\":\"PE\",\"codigoIbge\":\"2611606\",\"aguaIntermitente\":true}"
ID_BAIRRO=$(primeiro_id)
echo "         id_bairro criado: ${ID_BAIRRO}"

chamar 200 "GET    /api/bairros (paginado)"   "${API}/bairros?page=0&size=10"
chamar 200 "GET    /api/bairros/${ID_BAIRRO}" "${API}/bairros/${ID_BAIRRO}"
chamar 200 "PUT    /api/bairros/${ID_BAIRRO}" \
    -X PUT "${API}/bairros/${ID_BAIRRO}" -H "${AUTH}" -H "Content-Type: application/json" \
    -d "{\"nome\":\"Smoke ${CARIMBO} ALTERADO\",\"municipio\":\"Recife\",\"uf\":\"PE\",\"codigoIbge\":\"2611606\",\"aguaIntermitente\":false}"

titulo "PASSO 4 - TABELA DISPOSITIVO"
# Sem usuarioId: o GESTOR logado vira o dono do aparelho.
chamar 201 "POST   /api/dispositivos" \
    -X POST "${API}/dispositivos" -H "${AUTH}" -H "Content-Type: application/json" \
    -d "{\"numeroSerie\":\"SMK-${CARIMBO}\",\"tipoRecipiente\":\"TAMBOR\",\"volumeLitros\":200,\"pulsosPorDia\":1,\"bairroId\":${ID_BAIRRO}}"
ID_DISP=$(primeiro_id)
echo "         id_dispositivo criado: ${ID_DISP}"

chamar 200 "GET    /api/dispositivos (paginado)"  -H "${AUTH}" "${API}/dispositivos?page=0&size=10"
chamar 200 "GET    /api/dispositivos/${ID_DISP}"  -H "${AUTH}" "${API}/dispositivos/${ID_DISP}"
chamar 200 "PUT    /api/dispositivos/${ID_DISP}" \
    -X PUT "${API}/dispositivos/${ID_DISP}" -H "${AUTH}" -H "Content-Type: application/json" \
    -d "{\"tipoRecipiente\":\"CAIXA_DAGUA\",\"volumeLitros\":500,\"pulsosPorDia\":2,\"bairroId\":${ID_BAIRRO},\"status\":\"INATIVO\"}"

titulo "PASSO 5 - EXCLUSAO (filho antes do pai)"
# Bairro com aparelho vinculado nao pode ser excluido (409).
chamar 204 "DELETE /api/dispositivos/${ID_DISP}"  -X DELETE -H "${AUTH}" "${API}/dispositivos/${ID_DISP}"
chamar 404 "GET    /api/dispositivos/${ID_DISP}"  -H "${AUTH}" "${API}/dispositivos/${ID_DISP}"
chamar 204 "DELETE /api/bairros/${ID_BAIRRO}"     -X DELETE -H "${AUTH}" "${API}/bairros/${ID_BAIRRO}"
chamar 404 "GET    /api/bairros/${ID_BAIRRO}"     "${API}/bairros/${ID_BAIRRO}"

unset TOKEN AUTH

titulo "RESUMO"
if [[ "${FALHAS}" -eq 0 ]]; then
    echo "Todos os testes passaram: CRUD completo em BAIRRO e DISPOSITIVO."
else
    echo "${FALHAS} teste(s) falharam." >&2
    exit 1
fi
