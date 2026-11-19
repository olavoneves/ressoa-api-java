#!/usr/bin/env bash
#
# 03 - Cria o Azure Container Registry e publica nele a imagem do banco.
#
# O banco roda em container na nuvem (ACI), opcao aceita pelo enunciado
# (requisito 08: "Container (ACI/ACR)"). A imagem e a de
# dockerfiles/Dockerfile.oracle: Oracle XE 21c + o DDL de scripts/script-bd.sql.
#
# Build: primeiro tenta `az acr build` (o ACR compila na nuvem, sem Docker
# local). Assinaturas Azure for Students costumam ter o ACR Tasks bloqueado
# (TasksOperationsNotAllowed); nesse caso cai para `docker build` + `push`
# com o Docker Desktop local.
#
# SKU Basic: suficiente para uma imagem e o mais barato.
# admin-enabled: o ACI autentica no registry com usuario/senha de admin.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

# Contexto do build = raiz do repositorio (o Dockerfile copia scripts/).
# `pwd -W` (Git Bash) devolve C:/...: com MSYS_NO_PATHCONV o az.cmd do
# Windows nao entende /c/...; no Linux/macOS cai no `pwd` comum.
RAIZ="$(cd "$(dirname "$0")/.." && { pwd -W 2>/dev/null || pwd; })"
IMAGEM="${IMAGE_DB}:${IMAGE_TAG}"

titulo "03 - AZURE CONTAINER REGISTRY"
echo "Nome       : ${ACR_NAME}"
echo "SKU        : Basic"
echo "Imagem     : ${IMAGEM} (${DOCKERFILE_DB})"

if az acr show --name "${ACR_NAME}" --resource-group "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo "AVISO: o registry '${ACR_NAME}' ja existe. Nada a fazer."
else
    az acr create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${ACR_NAME}" \
        --sku Basic \
        --location "${LOCATION}" \
        --admin-enabled true \
        --tags "${TAGS[@]}" \
        --output none
    echo "  registry ..................... criado"
fi

titulo "BUILD DA IMAGEM DO BANCO"
# Sempre reconstroi: se o script-bd.sql mudou, a imagem precisa acompanhar.
if az acr build \
       --registry "${ACR_NAME}" \
       --image "${IMAGEM}" \
       --file "${RAIZ}/${DOCKERFILE_DB}" \
       "${RAIZ}"; then
    echo "  imagem compilada no ACR (az acr build)."
else
    echo ""
    echo "AVISO: az acr build falhou (ACR Tasks bloqueado?). Usando o Docker local."
    exigir_comando docker
    az acr login --name "${ACR_NAME}"
    docker build \
        --file "${RAIZ}/${DOCKERFILE_DB}" \
        --tag "${ACR_LOGIN_SERVER}/${IMAGEM}" \
        "${RAIZ}"
    docker push "${ACR_LOGIN_SERVER}/${IMAGEM}"
    echo "  imagem compilada localmente e enviada ao ACR."
fi

titulo "RESULTADO"
echo "Login Server: ${ACR_LOGIN_SERVER}"
az acr repository show-tags --name "${ACR_NAME}" \
                            --repository "${IMAGE_DB}" \
                            --output table
