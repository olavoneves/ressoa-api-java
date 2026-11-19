#!/usr/bin/env bash
#
# 04 - Cria o ACI do BANCO DE DADOS (Oracle XE 21c em container).
#
#   - imagem do nosso ACR (03_acr.sh), com o DDL do projeto embutido: as
#     tabelas de scripts/script-bd.sql nascem no primeiro boot
#   - senhas por --secure-environment-variables: com --environment-variables
#     elas apareceriam em texto claro no `az container show` e no Portal
#   - FQDN publico na porta 1521: o Web App alcanca o banco por ele, e o
#     video faz o SELECT direto no banco a partir da maquina de quem grava
#
# RISCO ACEITO - banco publico: o ACI nao aceita NSG/whitelist em IP
# publico. Mitigacoes: senhas aleatorias de 28 caracteres no Key Vault,
# usuario da aplicacao sem privilegio de DBA, e o ACI parado fora das
# sessoes (97_pausar.sh).
#
# SEM VOLUME (na Sprint 3 o Oracle sobre Azure Files falhou com ORA-01990).
# Recriar ou religar o container zera os dados; o DDL e reaplicado sozinho.
#
# 2 vCPU / 4 GB: o Oracle XE aloca SGA de 1,6 GB e nao inicia com menos.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "04 - ACI DO BANCO DE DADOS"
echo "ACI      : ${ACI_DB}"
echo "Imagem   : ${ACR_LOGIN_SERVER}/${IMAGE_DB}:${IMAGE_TAG}"
echo "Recursos : 2 vCPU / 4 GB, sem volume"
echo "FQDN     : ${DB_FQDN}"

if az container show --resource-group "${RESOURCE_GROUP}" \
                     --name "${ACI_DB}" >/dev/null 2>&1; then
    echo ""
    echo "AVISO: o ACI '${ACI_DB}' ja existe. Para recria-lo (APAGA OS DADOS):"
    echo "    az container delete -g ${RESOURCE_GROUP} -n ${ACI_DB} --yes"
    exit 0
fi

titulo "LEITURA DE CREDENCIAIS (runtime, nada em disco)"
ACR_USERNAME=$(az acr credential show --name "${ACR_NAME}" \
                   --query username --output tsv | tr -d '\r')
ACR_PASSWORD=$(az acr credential show --name "${ACR_NAME}" \
                   --query "passwords[0].value" --output tsv | tr -d '\r')
echo "  credencial do ACR ............ lida"

ORACLE_PASSWORD=$(ler_segredo "${SECRET_ORACLE_PASSWORD}")
APP_USER_PASSWORD=$(ler_segredo "${SECRET_APP_PASSWORD}")
echo "  senhas do Key Vault .......... lidas"

titulo "CRIACAO DO CONTAINER GROUP"
az container create \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${ACI_DB}" \
    --image "${ACR_LOGIN_SERVER}/${IMAGE_DB}:${IMAGE_TAG}" \
    --location "${LOCATION}" \
    --os-type Linux \
    --cpu 2 \
    --memory 4 \
    --registry-login-server "${ACR_LOGIN_SERVER}" \
    --registry-username "${ACR_USERNAME}" \
    --registry-password "${ACR_PASSWORD}" \
    --ports 1521 \
    --ip-address Public \
    --dns-name-label "${DNS_DB}" \
    --restart-policy Always \
    --environment-variables \
        APP_USER="${APP_USER}" \
        ORACLE_DATABASE="${ORACLE_DATABASE}" \
    --secure-environment-variables \
        ORACLE_PASSWORD="${ORACLE_PASSWORD}" \
        APP_USER_PASSWORD="${APP_USER_PASSWORD}" \
    --output none

# O az 2.84 tirou o --tags do `az container create`: as tags entram depois.
az resource tag --is-incremental \
    --ids "$(az container show --resource-group "${RESOURCE_GROUP}" --name "${ACI_DB}" \
             --query id --output tsv | tr -d '\r')" \
    --tags "${TAGS[@]}" \
    --output none

unset ACR_PASSWORD ORACLE_PASSWORD APP_USER_PASSWORD

titulo "AGUARDANDO O ORACLE FICAR PRONTO"
esperar_oracle

# O DDL roda antes do "READY": erro de SQL aparece no log como ORA-xxxxx.
if az container logs --resource-group "${RESOURCE_GROUP}" --name "${ACI_DB}" \
        | grep -E "ORA-[0-9]{5}"; then
    echo "AVISO: houve erro de SQL no boot (acima). Confira o script-bd.sql." >&2
fi

titulo "RESULTADO"
az container show \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${ACI_DB}" \
    --query "{aci:name, estado:instanceView.state, fqdn:ipAddress.fqdn, ip:ipAddress.ip}" \
    --output table

echo ""
echo "URL JDBC (vai para o variable group no 07):"
echo "  ${JDBC_URL}"
