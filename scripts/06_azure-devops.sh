#!/usr/bin/env bash
#
# 06 - Projeto no Azure DevOps e acesso dos professores.
#
# PRE-REQUISITOS (uma unica vez, pelo portal):
#   1. Organizacao ADO_ORG criada em https://dev.azure.com (nao existe
#      comando de CLI para criar organizacao).
#   2. `az login` com uma conta administradora da organizacao. A extensao
#      azure-devops do az usa o mesmo login: nao precisa de PAT.
#
# O QUE O ENUNCIADO COBRA E ONDE ESTA AQUI
#   - Projeto privado, com Git ...................................... passo 1
#   - Professor convidado: Organizacao Basic, Projeto Project
#     Administrators (sem acesso ao Azure DevOps a nota e zero) ..... passo 2
#
# Nao cria recurso cobrado: pode rodar antes do provisionamento.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "06 - AZURE DEVOPS"
echo "Organizacao : ${ADO_ORG_URL}"
echo "Projeto     : ${ADO_PROJECT}"

if ! az extension show --name azure-devops >/dev/null 2>&1; then
    az extension add --name azure-devops --only-show-errors
fi
az devops configure --defaults organization="${ADO_ORG_URL}"

# --- 1. projeto --------------------------------------------------------
titulo "PASSO 1 - PROJETO"
if az devops project show --project "${ADO_PROJECT}" >/dev/null 2>&1; then
    echo "AVISO: o projeto '${ADO_PROJECT}' ja existe. Nada a fazer."
else
    az devops project create \
        --name "${ADO_PROJECT}" \
        --description "${ADO_PROJECT_DESC}" \
        --visibility private \
        --source-control git \
        --process Scrum \
        --output none
    echo "  projeto ...................... criado"
fi

az devops configure --defaults organization="${ADO_ORG_URL}" project="${ADO_PROJECT}"

az devops project show --project "${ADO_PROJECT}" \
    --query "{projeto:name, visibilidade:visibility, processo:capabilities.processTemplate.templateName, controleVersao:capabilities.versioncontrol.sourceControlType}" \
    --output table

# --- 2. professores ----------------------------------------------------
titulo "PASSO 2 - ACESSO DOS PROFESSORES"

GRUPO_ADMIN=$(az devops security group list --project "${ADO_PROJECT}" \
                  --query "graphGroups[?displayName=='Project Administrators'].descriptor | [0]" \
                  --output tsv | tr -d '\r')

for professor in "${PROFESSORES[@]}"; do
    # "express" e o nome interno do nivel de acesso Basic. Stakeholder nao
    # enxerga Repos nem o detalhe das pipelines.
    if az devops user show --user "${professor}" >/dev/null 2>&1; then
        az devops user update --user "${professor}" --license-type express --output none
        echo "  ${professor} ...... ja estava na organizacao (Basic garantido)"
    else
        az devops user add \
            --email-id "${professor}" \
            --license-type express \
            --send-email-invite true \
            --output none
        echo "  ${professor} ...... convidado (Basic)"
    fi

    # Adicionar quem ja e membro devolve erro: a conferencia vem logo abaixo.
    az devops security group membership add \
        --group-id "${GRUPO_ADMIN}" \
        --member-id "${professor}" \
        --output none 2>/dev/null || true
done

titulo "RESULTADO"
echo "Nivel de acesso na organizacao:"
for professor in "${PROFESSORES[@]}"; do
    az devops user show --user "${professor}" \
        --query "{usuario:user.principalName, acesso:accessLevel.licenseDisplayName}" \
        --output tsv
done

echo ""
echo "Membros de Project Administrators:"
MEMBROS=$(az devops security group membership list --id "${GRUPO_ADMIN}" \
              --query "*.principalName" --output tsv | tr -d '\r')
echo "${MEMBROS}" | sed 's/^/  /'

FALTANDO=0
for professor in "${PROFESSORES[@]}"; do
    if ! grep -qix "${professor}" <<<"${MEMBROS}"; then
        echo "ERRO: ${professor} nao esta em Project Administrators." >&2
        FALTANDO=1
    fi
done
exit "${FALTANDO}"
