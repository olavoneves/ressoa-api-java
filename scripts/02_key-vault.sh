#!/usr/bin/env bash
#
# 02 - Cria o Azure Key Vault e guarda os segredos da solucao.
#
# GERADOS AQUI (openssl rand, em tempo de execucao):
#   - senha do SYS do Oracle             (so o ACI do banco usa)
#   - senha do usuario ressoa            (banco e API Java)
#   - segredo do JWT                     (assinatura dos tokens da API Java)
#   - senha dos usuarios de demonstracao (SEED_SENHA_PADRAO: a API cria
#     gestor@, ia-servico@, morador@ e joao@ressoa.app com o banco vazio)
#
# INFORMADOS PELO TIME (servicos externos; o script pergunta sem eco):
#   - chave do Google Gemini (Spring AI)
#   - URL do RabbitMQ (CloudAMQP)
#   Deixar em branco pula o segredo; rode de novo quando tiver o valor.
#
# Nenhum valor literal existe em arquivo. O Key Vault e a fonte da verdade:
# o 04 le os segredos do banco e o 07 copia os da aplicacao para o variable
# group secreto do Azure DevOps.
#
# ATENCAO - soft delete: o cofre fica "excluido reversivelmente" por 90 dias
# depois de um `az group delete`. Recriar com o mesmo nome falha ate ele ser
# purgado (az keyvault purge), o que e irreversivel.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az
exigir_comando openssl

titulo "02 - AZURE KEY VAULT"
echo "Cofre : ${KEYVAULT_NAME}"

# Alfanumerico puro: '/', '+' e '=' quebram a URL JDBC e o sqlplus.
gerar_senha() {
    openssl rand -base64 32 | tr -d '/+=\n' | head -c 28
}

# HS256 exige chave de no minimo 32 bytes.
gerar_jwt_secret() {
    openssl rand -base64 64 | tr -d '/+=\n' | head -c 48
}

# Pergunta sem eco: o valor nao fica no historico do shell nem na tela.
perguntar() {
    local valor=""
    # Sem terminal (execucao nao interativa), pula: o 07 aponta o que falta.
    {
        printf "  Valor de '%s' (Enter para pular): " "$1" >/dev/tty
        read -r -s valor </dev/tty
        echo "" >/dev/tty
    } 2>/dev/null || true
    printf '%s' "${valor}"
}

if az keyvault show --name "${KEYVAULT_NAME}" \
                    --resource-group "${RESOURCE_GROUP}" >/dev/null 2>&1; then
    echo "AVISO: o cofre '${KEYVAULT_NAME}' ja existe. Nada a fazer."
else
    if az keyvault list-deleted --query "[?name=='${KEYVAULT_NAME}'].name" \
                                --output tsv 2>/dev/null | grep -q .; then
        echo "ERRO: existe um cofre '${KEYVAULT_NAME}' excluido reversivelmente." >&2
        echo "Para reutilizar o nome: az keyvault purge --name ${KEYVAULT_NAME}" >&2
        echo "Isso e IRREVERSIVEL. Confirme com o responsavel antes." >&2
        exit 1
    fi

    # Access policy (nao RBAC): quem cria o cofre ja ganha acesso aos
    # segredos, sem esperar a propagacao de um role assignment.
    az keyvault create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${KEYVAULT_NAME}" \
        --location "${LOCATION}" \
        --enable-rbac-authorization false \
        --tags "${TAGS[@]}" \
        --output none
    echo "  cofre ........................ criado"
fi

titulo "SEGREDOS"

# Cada segredo so e gravado se ainda nao existir: reexecutar o script nao
# troca a senha de um banco que ja esta no ar.
criar_segredo_se_ausente() {
    local nome="$1" valor_fn="$2" valor
    if az keyvault secret show --vault-name "${KEYVAULT_NAME}" \
                               --name "${nome}" >/dev/null 2>&1; then
        echo "  ${nome} .......... ja existe (mantido)"
        return
    fi
    valor=$(${valor_fn} "${nome}")
    if [[ -z "${valor}" ]]; then
        echo "  ${nome} .......... PULADO (sem valor; o 07 vai cobrar)"
        return
    fi
    az keyvault secret set \
        --vault-name "${KEYVAULT_NAME}" \
        --name "${nome}" \
        --value "${valor}" \
        --output none
    echo "  ${nome} .......... criado"
}

criar_segredo_se_ausente "${SECRET_ORACLE_PASSWORD}" gerar_senha
criar_segredo_se_ausente "${SECRET_APP_PASSWORD}"    gerar_senha
criar_segredo_se_ausente "${SECRET_JWT}"             gerar_jwt_secret
criar_segredo_se_ausente "${SECRET_SEED}"            gerar_senha
criar_segredo_se_ausente "${SECRET_GEMINI}"          perguntar
criar_segredo_se_ausente "${SECRET_RABBITMQ}"        perguntar

titulo "RESULTADO"
az keyvault secret list \
    --vault-name "${KEYVAULT_NAME}" \
    --query "[].{segredo:name, habilitado:attributes.enabled}" \
    --output table

echo ""
echo "Os VALORES dos segredos nunca sao impressos nem gravados em disco."
