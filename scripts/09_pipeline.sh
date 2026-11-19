#!/usr/bin/env bash
#
# 09 - Registra a pipeline multi-stage (azure-pipeline.yml) e autoriza SO
#      ela no environment, no variable group e na service connection.
#
#   Stage Build   : dispara a cada commit na main (merge de PR, pois a main
#                   e protegida), roda Maven + testes JUnit, publica o
#                   resultado dos testes e o artefato (JAR).
#   Stage Release : roda sozinho depois do Build, quando ha artefato novo, e
#                   faz o deploy no Web App da API Java.
#
# PRE-REQUISITOS: codigo importado no Azure Repos (ADO_REPO), com o
# azure-pipeline.yml na raiz da main; 07 e 08 ja executados.
#
# --skip-first-run: o primeiro run fica para o merge de verdade, que e o
# que o video precisa mostrar.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "09 - PIPELINE ${PIPELINE_NAME}"
az devops configure --defaults organization="${ADO_ORG_URL}" project="${ADO_PROJECT}"

titulo "PRE-REQUISITOS"
if ! az repos show --repository "${ADO_REPO}" >/dev/null 2>&1; then
    echo "ERRO: repositorio '${ADO_REPO}' nao encontrado no projeto." >&2
    exit 1
fi
if ! az repos ref list --repository "${ADO_REPO}" --filter "heads/${ADO_BRANCH}" \
        --query "[0].name" --output tsv | grep -q .; then
    echo "ERRO: a branch '${ADO_BRANCH}' nao existe em '${ADO_REPO}'. Importe o codigo antes." >&2
    exit 1
fi
ID_GRUPO=$(az pipelines variable-group list --query "[?name=='${VARIABLE_GROUP}'].id | [0]" \
               --output tsv | tr -d '\r')
if [[ -z "${ID_GRUPO}" ]]; then
    echo "ERRO: variable group '${VARIABLE_GROUP}' nao encontrado. Rode 07 antes." >&2
    exit 1
fi
ID_SC=$(az devops service-endpoint list --query "[?name=='${SC_AZURE}'].id | [0]" \
            --output tsv | tr -d '\r')
if [[ -z "${ID_SC}" ]]; then
    echo "ERRO: service connection '${SC_AZURE}' nao encontrada. Rode 08 antes." >&2
    exit 1
fi
echo "  repositorio, branch, variable group e service connection ok"

titulo "REGISTRO DA PIPELINE"
if az pipelines show --name "${PIPELINE_NAME}" >/dev/null 2>&1; then
    echo "  ${PIPELINE_NAME} .......... ja existe (mantida)"
else
    az pipelines create \
        --name "${PIPELINE_NAME}" \
        --description "Build (Maven + JUnit + artefato) e Release (deploy no Web App ${WEBAPP_API})" \
        --repository "${ADO_REPO}" \
        --repository-type tfsgit \
        --branch "${ADO_BRANCH}" \
        --yml-path "${PIPELINE_YAML}" \
        --skip-first-run true \
        --output none
    echo "  ${PIPELINE_NAME} .......... criada"
fi

titulo "ENVIRONMENT ${ADO_ENVIRONMENT}"
# Licao da Sprint 4: criado pela propria pipeline, o environment nasce sem
# autorizar ninguem e o stage de deploy fica preso em
# "Checkpoint.Authorization" esperando um clique em "Permit". Criado e
# autorizado aqui, o Release roda sozinho desde o primeiro merge.
API_PROJETO="${ADO_ORG_URL}/$(printf '%s' "${ADO_PROJECT}" | sed 's/ /%20/g')/_apis"
RECURSO_ADO="499b84ac-1321-427f-aa17-267ca6975798"

ID_ENV=$(az rest --method get --resource "${RECURSO_ADO}" \
             --url "${API_PROJETO}/distributedtask/environments?name=${ADO_ENVIRONMENT}&api-version=7.1" \
             --query "value[0].id" --output tsv | tr -d '\r')
if [[ -z "${ID_ENV}" ]]; then
    ID_ENV=$(az rest --method post --resource "${RECURSO_ADO}" \
                 --headers "Content-Type=application/json" \
                 --url "${API_PROJETO}/distributedtask/environments?api-version=7.1" \
                 --body "{\"name\":\"${ADO_ENVIRONMENT}\",\"description\":\"Deploy da API Java no Web App ${WEBAPP_API}\"}" \
                 --query id --output tsv | tr -d '\r')
    echo "  environment .................. criado (id ${ID_ENV})"
else
    echo "  environment .................. ja existe (id ${ID_ENV})"
fi

titulo "AUTORIZACAO DA PIPELINE ${PIPELINE_NAME}"
# So esta pipeline usa o environment, o variable group (segredos) e a
# service connection (deploy): nenhum dos tres fica liberado para todas as
# pipelines do projeto.
ID_PIPELINE=$(az pipelines show --name "${PIPELINE_NAME}" --query id --output tsv | tr -d '\r')

# autorizar <tipo do recurso> <id> <rotulo>
autorizar() {
    az rest --method patch --resource "${RECURSO_ADO}" \
        --headers "Content-Type=application/json" \
        --url "${API_PROJETO}/pipelines/pipelinePermissions/$1/$2?api-version=7.1-preview.1" \
        --body "{\"pipelines\":[{\"id\":${ID_PIPELINE},\"authorized\":true}]}" \
        --output none
    echo "  $3 .......... autorizada"
}
autorizar environment   "${ID_ENV}"   "environment ${ADO_ENVIRONMENT}"
autorizar variablegroup "${ID_GRUPO}" "variable group ${VARIABLE_GROUP}"
autorizar endpoint      "${ID_SC}"    "service connection ${SC_AZURE}"

titulo "RESULTADO"
az pipelines list --query "[].{pipeline:name, id:id}" --output table

cat <<FIM

Para disparar o fluxo: crie a tarefa no Boards, uma branch, altere CODIGO
(mudar so o README desconta pontos), abra o PR e faca o merge na ${ADO_BRANCH}.
Acompanhe em:
  ${ADO_ORG_URL}/$(printf '%s' "${ADO_PROJECT}" | sed 's/ /%20/g')/_build

Depois do primeiro deploy, rode 10_smoke-tests.sh.
FIM
