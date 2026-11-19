#!/usr/bin/env bash
#
# 01 - Registra os resource providers e cria o Grupo de Recursos.
#
# Todos os recursos da solucao nascem por Azure CLI e moram neste grupo:
# Key Vault, ACR, ACI do banco, App Service Plan, o Web App da API Java e a managed
# identity da service connection.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "01 - GRUPO DE RECURSOS"
echo "Nome       : ${RESOURCE_GROUP}"
echo "Regiao     : ${LOCATION}"
echo "Assinatura : ${SUBSCRIPTION_ID}"

titulo "REGISTRO DOS SERVICOS UTILIZADOS"
# Como no criar-infra.sh das aulas: numa assinatura nova os providers nascem
# desregistrados e o primeiro `create` falha com MissingSubscriptionRegistration.
for provedor in Microsoft.KeyVault Microsoft.ContainerRegistry \
                Microsoft.ContainerInstance Microsoft.Web \
                Microsoft.ManagedIdentity; do
    az provider register --namespace "${provedor}" --wait --output none
    echo "  ${provedor} ...... registrado"
done

titulo "GRUPO"
if az group show --name "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo "AVISO: o grupo '${RESOURCE_GROUP}' ja existe. Nada a fazer."
else
    az group create \
        --name "${RESOURCE_GROUP}" \
        --location "${LOCATION}" \
        --tags "${TAGS[@]}" \
        --output none
    echo "  grupo ........................ criado"
fi

titulo "RESULTADO"
az group show --name "${RESOURCE_GROUP}" \
    --query "{nome:name, regiao:location, estado:properties.provisioningState}" \
    --output table
