#!/usr/bin/env bash
#
# 06b - Protege a main do Azure Repos (requisito 03 do enunciado):
#
#   1. revisor obrigatorio : PR precisa de 1 aprovacao. O autor pode aprovar
#                            a propria PR (requisito 05: "simulacao").
#   2. work item vinculado : PR sem work item do Boards nao entra.
#   3. revisor padrao      : RM561940 entra sozinho em toda PR para a main.
#
# Com a politica, push direto na main e recusado: o Build (trigger na main)
# so roda depois do merge de uma PR (requisito 04).
#
# PRE-REQUISITO: codigo importado no Azure Repos (ADO_REPO) com a branch main.
# Nao cria recurso cobrado. Reexecutavel: politica ja existente e mantida.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "06b - POLITICAS DA ${ADO_BRANCH}"
az devops configure --defaults organization="${ADO_ORG_URL}" project="${ADO_PROJECT}"

ID_REPO=$(az repos show --repository "${ADO_REPO}" --query id --output tsv | tr -d '\r')
if ! az repos ref list --repository "${ADO_REPO}" --filter "heads/${ADO_BRANCH}" \
        --query "[0].name" --output tsv | grep -q .; then
    echo "ERRO: a branch '${ADO_BRANCH}' nao existe em '${ADO_REPO}'. Importe o codigo antes." >&2
    exit 1
fi
echo "Repositorio: ${ADO_REPO} (${ID_REPO})"

# IDs fixos dos tipos de politica do Azure Repos.
TIPO_REVISORES="fa4e907d-c16b-4a4c-9dfa-4906e5d171dd"    # Minimum number of reviewers
TIPO_WORK_ITEM="40e92b44-2fe1-4dd6-b3d8-74a9c21d0c6e"    # Work item linking
TIPO_REVISOR_PADRAO="fd2167ab-b0be-447a-8ec8-39368250530e" # Required reviewers

politica_existe() {
    az repos policy list --repository-id "${ID_REPO}" --branch "${ADO_BRANCH}" \
        --query "[?type.id=='$1'].id" --output tsv | grep -q .
}

titulo "1. REVISOR OBRIGATORIO"
if politica_existe "${TIPO_REVISORES}"; then
    echo "  ja existe (mantida)"
else
    az repos policy approver-count create \
        --repository-id "${ID_REPO}" --branch "${ADO_BRANCH}" \
        --blocking true --enabled true \
        --minimum-approver-count 1 \
        --creator-vote-counts true \
        --allow-downvotes false \
        --reset-on-source-push false \
        --output none
    echo "  criada: 1 aprovacao, o autor pode aprovar a propria PR"
fi

titulo "2. WORK ITEM VINCULADO"
if politica_existe "${TIPO_WORK_ITEM}"; then
    echo "  ja existe (mantida)"
else
    az repos policy work-item-linking create \
        --repository-id "${ID_REPO}" --branch "${ADO_BRANCH}" \
        --blocking true --enabled true \
        --output none
    echo "  criada: PR sem work item e bloqueada"
fi

titulo "3. REVISOR PADRAO ${REVISOR_PADRAO}"
if politica_existe "${TIPO_REVISOR_PADRAO}"; then
    echo "  ja existe (mantida)"
else
    # Pelo JSON, e nao por `az repos policy required-reviewer create`: so
    # assim da para ligar o creatorVoteCounts. Sem ele, uma PR aberta pelo
    # proprio RM561940 ficaria bloqueada esperando a aprovacao dele mesmo.
    ID_REVISOR=$(az devops user show --user "${REVISOR_PADRAO}" --query id --output tsv | tr -d '\r')
    # Ao lado do script, e nao em /tmp: no Git Bash o az.cmd nao enxerga o
    # /tmp emulado. O arquivo nao tem segredo nenhum.
    CONFIG="$(dirname "$0")/.politica.tmp.json"
    trap 'rm -f "${CONFIG}"' EXIT
    cat > "${CONFIG}" <<EOF
{
  "isEnabled": true,
  "isBlocking": true,
  "type": { "id": "${TIPO_REVISOR_PADRAO}" },
  "settings": {
    "requiredReviewerIds": [ "${ID_REVISOR}" ],
    "minimumApproverCount": 1,
    "creatorVoteCounts": true,
    "message": "Revisor padrao da main (requisito 03 da Global Solution)",
    "scope": [ { "repositoryId": "${ID_REPO}", "refName": "refs/heads/${ADO_BRANCH}", "matchKind": "exact" } ]
  }
}
EOF
    az repos policy create --policy-configuration "${CONFIG}" --output none
    echo "  criada: ${REVISOR_PADRAO} incluido em toda PR"
fi

titulo "RESULTADO"
az repos policy list --repository-id "${ID_REPO}" --branch "${ADO_BRANCH}" \
    --query "[].{politica:type.displayName, bloqueante:isBlocking, ativa:isEnabled}" \
    --output table
