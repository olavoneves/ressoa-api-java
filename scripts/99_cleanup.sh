#!/usr/bin/env bash
#
# 99 - Remove TODOS os recursos Azure da solucao (o grupo inteiro).
#
# QUANDO EXECUTAR: depois que a nota saiu e ninguem mais precisa da solucao
# no ar. Antes disso, prefira 97_pausar.sh: e a rede de seguranca para
# regravar o video e para a apresentacao presencial (23 a 27/11).
#
# O que NAO e removido: o projeto no Azure DevOps. O professor corrige
# olhando Boards, Repos e o historico das pipelines - sem acesso ao Azure
# DevOps a nota e zero. A service connection fica orfa (a identidade
# federada some com o grupo).
#
# Exige confirmacao digitada: `az group delete` e irreversivel.
#
# ATENCAO - Key Vault: o cofre fica em soft delete por 90 dias. Recriar com
# o mesmo nome falha ate ele ser purgado.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "99 - REMOCAO DOS RECURSOS"
echo "Grupo de recursos: ${RESOURCE_GROUP}"

if ! az group show --name "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo "O grupo '${RESOURCE_GROUP}' nao existe. Nada a fazer."
    exit 0
fi

echo ""
echo "Sera removido:"
az resource list --resource-group "${RESOURCE_GROUP}" \
    --query "[].{nome:name, tipo:type}" --output table

echo ""
echo "Isto e IRREVERSIVEL. O banco nao tem volume: os dados vao junto."
echo "Depois disto, o Release falha ate os scripts 01 a 08 rodarem de novo."
echo ""
read -r -p "Digite o nome do grupo para confirmar: " CONFIRMACAO

if [[ "${CONFIRMACAO}" != "${RESOURCE_GROUP}" ]]; then
    echo "Confirmacao nao confere. Nada foi removido."
    exit 1
fi

titulo "REMOVENDO"
az group delete --name "${RESOURCE_GROUP}" --yes --no-wait
echo "Remocao iniciada em background (--no-wait)."
echo "Acompanhe com: az group list --output table"
echo "Lembrete: o cofre '${KEYVAULT_NAME}' fica em soft delete por 90 dias."
