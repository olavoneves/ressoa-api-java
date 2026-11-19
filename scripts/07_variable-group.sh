#!/usr/bin/env bash
#
# 07 - Variable group "ressoa-secrets": as variaveis de ambiente que o stage
#      de Release do azure-pipeline.yml grava no Web App da API Java.
#
# Os nomes sao os das variaveis de ambiente que a API le
# (src/main/resources/application.properties e .env.example):
#   visiveis : DB_URL, DB_USERNAME (nao sao segredo)
#   secretas : DB_PASSWORD, JWT_SECRET, SEED_SENHA_PADRAO, GEMINI_API_KEY,
#              RABBITMQ_URL
# As demais (SEED_HABILITADO, CORS_ORIGENS, AI_CHAT_PROVIDER, GEMINI_MODEL,
# HEALTH_RABBIT...) ficam no padrao do application.properties.
#
# Secretas aparecem com cadeado no Library, mascaradas (***) nos logs e nao
# podem ser lidas de volta. Os valores saem do Key Vault direto para o Azure
# DevOps pela variavel de ambiente AZURE_DEVOPS_EXT_PIPELINE_VAR_<NOME>, que
# o az le sem o valor aparecer na linha de comando nem em disco.
#
# O grupo NAO fica liberado para todas as pipelines do projeto: o
# 09_pipeline.sh autoriza so a ressoa-api (senao o primeiro run para
# pedindo permissao e o disparo deixa de ser automatico).
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "07 - VARIABLE GROUP '${VARIABLE_GROUP}'"
az devops configure --defaults organization="${ADO_ORG_URL}" project="${ADO_PROJECT}"

# Segredos do Key Vault -> nome da variavel no grupo.
declare -A SEGREDOS=(
    [DB_PASSWORD]="${SECRET_APP_PASSWORD}"
    [JWT_SECRET]="${SECRET_JWT}"
    [SEED_SENHA_PADRAO]="${SECRET_SEED}"
    [GEMINI_API_KEY]="${SECRET_GEMINI}"
    [RABBITMQ_URL]="${SECRET_RABBITMQ}"
)

titulo "CONFERENCIA DO KEY VAULT"
FALTANDO=0
for variavel in "${!SEGREDOS[@]}"; do
    if ! az keyvault secret show --vault-name "${KEYVAULT_NAME}" \
            --name "${SEGREDOS[${variavel}]}" >/dev/null 2>&1; then
        echo "  FALTA ${SEGREDOS[${variavel}]} (para ${variavel})" >&2
        FALTANDO=1
    fi
done
if [[ "${FALTANDO}" -eq 1 ]]; then
    echo "ERRO: rode 02_key-vault.sh e informe os valores que faltam." >&2
    exit 1
fi
echo "  todos os segredos presentes"

titulo "GRUPO"
ID_GRUPO=$(az pipelines variable-group list \
               --query "[?name=='${VARIABLE_GROUP}'].id | [0]" \
               --output tsv | tr -d '\r')

if [[ -z "${ID_GRUPO}" ]]; then
    ID_GRUPO=$(az pipelines variable-group create \
                   --name "${VARIABLE_GROUP}" \
                   --description "Variaveis da API Java, copiadas do Key Vault ${KEYVAULT_NAME}" \
                   --variables DB_URL="${JDBC_URL}" DB_USERNAME="${APP_USER}" \
                   --query id --output tsv | tr -d '\r')
    echo "  grupo ........................ criado (id ${ID_GRUPO})"
else
    for par in "DB_URL=${JDBC_URL}" "DB_USERNAME=${APP_USER}"; do
        az pipelines variable-group variable update \
            --group-id "${ID_GRUPO}" --name "${par%%=*}" --value "${par#*=}" \
            --output none
    done
    echo "  grupo ........................ ja existe (id ${ID_GRUPO})"
fi

# Grava (ou regrava) um segredo, sem o valor passar pela linha de comando.
gravar_segredo() {
    local nome="$1" valor="$2"
    az pipelines variable-group variable delete \
        --group-id "${ID_GRUPO}" --name "${nome}" --yes \
        --output none 2>/dev/null || true
    # export num subshell: o valor nao aparece na lista de processos.
    (
        export "AZURE_DEVOPS_EXT_PIPELINE_VAR_${nome}=${valor}"
        az pipelines variable-group variable create \
            --group-id "${ID_GRUPO}" \
            --name "${nome}" \
            --secret true \
            --output none
    )
    echo "  ${nome} .......... gravado como segredo"
}

titulo "SEGREDOS"
for variavel in "${!SEGREDOS[@]}"; do
    gravar_segredo "${variavel}" "$(ler_segredo "${SEGREDOS[${variavel}]}")"
done

titulo "RESULTADO"
az pipelines variable-group variable list --group-id "${ID_GRUPO}" --output table
