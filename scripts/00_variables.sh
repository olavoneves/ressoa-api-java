#!/usr/bin/env bash
#
# Ressoa - Global Solution 2026/2 (CitizenTech)
# FIAP - DevOps Tools & Cloud Computing
#
#   RM561940 - Pedro Henrique Dias Franca (DevOps)
#
# Centraliza os nomes de todos os recursos. NENHUMA SENHA AQUI.
# As senhas sao geradas em 02_key-vault.sh, ficam no Key Vault e chegam a
# aplicacao pelo variable group secreto do Azure DevOps (07_variable-group.sh),
# que a pipeline grava no Web App a cada deploy.
#
# Este arquivo nao e executado diretamente: e carregado com `source` pelos
# demais scripts.
#
set -euo pipefail

# O Git Bash converte argumentos que comecam com "/" em caminhos do Windows
# (/subscriptions/... vira C:/Program Files/Git/subscriptions/...). Os
# scripts so passam caminhos para o az, que entende os dois formatos.
export MSYS_NO_PATHCONV=1
export MSYS2_ARG_CONV_EXCL="*"
export PYTHONIOENCODING=utf-8

# --- identificacao -----------------------------------------------------
export RM="rm561940"

# --- assinatura --------------------------------------------------------
# Azure for Students do Olavo (RM563558), onde o RM561940 e Owner.
export SUBSCRIPTION_ID="065b7acf-df9f-4ebf-8e01-829240bb8bea"

# --- regiao ------------------------------------------------------------
# A policy "Allowed resource deployment regions" da Azure for Students
# permite chilecentral, eastus2, southcentralus, northcentralus e
# brazilsouth. brazilsouth e a de menor latencia para a gravacao.
export LOCATION="brazilsouth"

# --- grupo de recursos -------------------------------------------------
export RESOURCE_GROUP="rg-ressoa-gs-${RM}"

# --- cofre e registry (nomes unicos no Azure inteiro) ------------------
export KEYVAULT_NAME="kv-ressoa-${RM}"
export ACR_NAME="acrressoa${RM}"
export ACR_LOGIN_SERVER="${ACR_NAME}.azurecr.io"

# --- banco de dados (Oracle XE em ACI) ---------------------------------
# Imagem propria (dockerfiles/Dockerfile.oracle): Oracle XE 21c + o DDL do
# repositorio (scripts/script-bd.sql, fonte unica do time), aplicado sozinho
# no primeiro boot do container.
export DOCKERFILE_DB="dockerfiles/Dockerfile.oracle"
export IMAGE_DB="ressoa-oracle"
export IMAGE_TAG="v1"

export ACI_DB="${RM}-ressoa-db"
# Rotulo DNS: precisa ser unico dentro da regiao.
export DNS_DB="${RM}-ressoa-db"
export DB_FQDN="${DNS_DB}.${LOCATION}.azurecontainer.io"

# ORACLE_DATABASE pede ao entrypoint da imagem que CRIE um PDB com este
# nome. Nao pode ser "XEPDB1", que ja vem pronto na imagem (ORA-65012).
export ORACLE_DATABASE="RESSOADB"
# Dono das tabelas: o DDL roda conectado como este usuario.
export APP_USER="ressoa"

# Formato thin com barra dupla e nome de SERVICO (nao SID).
export JDBC_URL="jdbc:oracle:thin:@//${DB_FQDN}:1521/${ORACLE_DATABASE}"

# --- aplicacao (App Service Linux B1) ----------------------------------
# So a API Java vai para a Azure; as APIs do app mobile seguem no Render.
export APP_SERVICE_PLAN="asp-ressoa-${RM}"

# API Java (Spring Boot): recebe o deploy da pipeline (azure-pipeline.yml).
export WEBAPP_API="${RM}-ressoa-api"
export WEBAPP_API_RUNTIME="JAVA:21-java21"
# Sobrescrevivel so para rodar o 10 contra a API local (validacao).
export WEBAPP_API_URL="${WEBAPP_API_URL:-https://${WEBAPP_API}.azurewebsites.net}"

# --- nomes dos segredos no Key Vault -----------------------------------
# Apenas os NOMES. Os valores nunca aparecem em arquivo.
export SECRET_ORACLE_PASSWORD="oracle-sys-password"
export SECRET_APP_PASSWORD="oracle-app-password"
export SECRET_JWT="jwt-secret"
# Senha dos usuarios de demonstracao que a API cria com o banco vazio
# (classe DadosIniciais): gestor@, ia-servico@, morador@ e joao@ressoa.app.
export SECRET_SEED="seed-senha-padrao"
# Servicos externos: nao sao gerados, o time informa (02_key-vault.sh pede).
export SECRET_GEMINI="gemini-api-key"
export SECRET_RABBITMQ="rabbitmq-url"

# Usuario GESTOR criado pela carga inicial (fixo no DadosIniciais.java).
export SEED_GESTOR_EMAIL="gestor@ressoa.app"

# --- Azure DevOps ------------------------------------------------------
# Organizacao do Olavo (RM563558), com paralelismo hospedado.
export ADO_ORG="${ADO_ORG:-RM563558}"
export ADO_ORG_URL="https://dev.azure.com/${ADO_ORG}"
export ADO_PROJECT="Ressoa - Global Solution"
export ADO_PROJECT_DESC="Global Solution 2026/2 (CitizenTech) - Ressoa: ultrassom contra larvas do Aedes e plataforma de risco de dengue"
# Repositorio padrao do projeto: recebe o import do codigo Java.
export ADO_REPO="${ADO_REPO:-Ressoa - Global Solution}"
export ADO_BRANCH="main"
# Revisor padrao da main (requisito 03: "seu RM").
export REVISOR_PADRAO="RM561940@fiap.com.br"

# Professores: Basic na organizacao + Project Administrators no projeto.
# Convite pelo UPN do Entra ID da FIAP: o e-mail publico falha com
# "user from outside your directory".
PROFESSORES=(
    "PF0841@fiap.com.br"   # Joao Carlos Menk - DevOps
    "pf1962@fiap.com.br"   # Felipe Braz - Compliance
)

# Service connection Azure Resource Manager (08_service-connection.sh):
# managed identity com federacao, sem App Registration nem segredo.
export SC_AZURE="sc-azure-ressoa"
export MANAGED_IDENTITY="id-ressoa-ado-${RM}"

# Variable group com os segredos que a pipeline grava nas app settings.
export VARIABLE_GROUP="ressoa-secrets"

# Pipeline multi-stage (Build + Release) definida em azure-pipeline.yml.
export PIPELINE_NAME="ressoa-api"
export PIPELINE_YAML="azure-pipeline.yml"
# Environment do stage de Release: guarda o historico de deploys.
export ADO_ENVIRONMENT="ressoa-producao"

# --- tags --------------------------------------------------------------
TAGS=("disciplina=devops-tools-cloud-computing" "entrega=global-solution-2026-2" "projeto=ressoa")

# --- utilitarios usados pelos demais scripts ---------------------------

titulo() {
    echo ""
    echo "======================================================================"
    echo "  $*"
    echo "======================================================================"
}

exigir_comando() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "ERRO: comando '$1' nao encontrado no PATH." >&2
        exit 1
    fi
}

# Le um segredo do Key Vault. O valor vai para stdout e nunca para disco.
# O tr tira o \r que o az.cmd do Windows acrescenta ao fim da linha.
ler_segredo() {
    az keyvault secret show \
        --vault-name "${KEYVAULT_NAME}" \
        --name "$1" \
        --query value \
        --output tsv | tr -d '\r'
}

# Espera ate 10 minutos o /actuator/health da API Java responder UP.
# Na Sprint 4 o Spring Boot levou ~4,5 min para subir num B1.
esperar_api() {
    local tentativa codigo
    echo "Aguardando ${WEBAPP_API_URL}/actuator/health (timeout: 10 min)"
    for tentativa in $(seq 1 60); do
        codigo=$(curl -s -o /dev/null -w "%{http_code}" --max-time 20 \
                      "${WEBAPP_API_URL}/actuator/health" || true)
        if [[ "${codigo}" == "200" ]]; then
            echo ""
            echo "API no ar apos ~$((tentativa * 10)) s."
            return 0
        fi
        printf "."
        sleep 10
    done
    echo ""
    echo "ERRO: a API nao respondeu 200 em 10 minutos (ultimo: ${codigo})." >&2
    echo "Log: az webapp log tail -g ${RESOURCE_GROUP} -n ${WEBAPP_API}" >&2
    return 1
}

# Espera ate 10 minutos o Oracle do ACI imprimir "DATABASE IS READY TO USE".
esperar_oracle() {
    local tentativa log
    echo "Poll em 'az container logs' (timeout: 10 min)"
    for tentativa in $(seq 1 60); do
        log=$(az container logs --resource-group "${RESOURCE_GROUP}" \
                                --name "${ACI_DB}" 2>/dev/null || true)
        if grep -q "DATABASE IS READY TO USE" <<<"${log}"; then
            echo ""
            echo "Oracle pronto apos ~$((tentativa * 10)) s."
            return 0
        fi
        # ORA-00845 = memoria insuficiente; ORA-01990 = password file.
        # Falhar cedo evita esperar o timeout inteiro por um erro ja visivel.
        if grep -qE "ORA-00845|ORA-01990" <<<"${log}"; then
            echo ""
            echo "ERRO: o Oracle abortou com erro conhecido:" >&2
            grep -E "ORA-[0-9]{5}" <<<"${log}" | head -5 >&2
            return 1
        fi
        printf "."
        sleep 10
    done
    echo ""
    echo "ERRO: o Oracle nao ficou pronto em 10 minutos. Ultimas linhas:" >&2
    tail -30 <<<"${log}" >&2
    return 1
}

# --- assinatura ativa --------------------------------------------------
# Todo script que carrega este arquivo opera na assinatura certa, mesmo
# que o `az login` tenha outra como padrao.
if command -v az >/dev/null 2>&1; then
    az account set --subscription "${SUBSCRIPTION_ID}"
fi
