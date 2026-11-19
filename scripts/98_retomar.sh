#!/usr/bin/env bash
#
# 98 - RETOMA os recursos pausados por 97_pausar.sh.
#
# O Oracle volta zerado (ACI sem volume), com as tabelas recriadas pelo DDL
# da imagem. Depois deste script, rode 10_smoke-tests.sh.
#
# Lembre de rodar 97_pausar.sh ao terminar.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando curl

titulo "98 - RETOMANDO RECURSOS"

# Banco primeiro: o Oracle leva minutos para abrir o PDB, e a API falha no
# boot se subir antes dele.
echo "Iniciando o ACI ${ACI_DB}..."
az container start --resource-group "${RESOURCE_GROUP}" --name "${ACI_DB}" --output none

echo "Voltando ${APP_SERVICE_PLAN} para B1..."
az appservice plan update --resource-group "${RESOURCE_GROUP}" \
    --name "${APP_SERVICE_PLAN}" --sku B1 --output none

az webapp config set --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_API}" --always-on true --output none
echo "Always On religado no Web App."

titulo "AGUARDANDO O ORACLE FICAR PRONTO"
esperar_oracle

echo "Iniciando o Web App ${WEBAPP_API}..."
az webapp start --resource-group "${RESOURCE_GROUP}" --name "${WEBAPP_API}" --output none

titulo "AGUARDANDO A API JAVA"
esperar_api

titulo "PROXIMO PASSO"
echo "    ./10_smoke-tests.sh"
