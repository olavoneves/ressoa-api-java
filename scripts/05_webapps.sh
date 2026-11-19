#!/usr/bin/env bash
#
# 05 - Cria o App Service Plan (Linux B1) e o Web App da API Java
#      (Spring Boot, Java 21 SE), que recebe o JAR do stage de Release do
#      azure-pipeline.yml. So a API Java vai para a Azure: as APIs do app
#      mobile seguem no Render.
#
# Este script so prepara o "terreno" e NAO grava segredo nenhum: URL,
# usuario e senha do banco, JWT_SECRET etc. sao aplicados pela pipeline a
# cada deploy, a partir do variable group secreto (07_variable-group.sh).
# Ate o primeiro deploy, o Web App mostra a pagina padrao do App Service.
#
# Fuso: JAVA_OPTS=-Duser.timezone=America/Sao_Paulo (o App Service roda em
# UTC e a variavel TZ e ignorada pela imagem Java).
#
# Tempo de subida: WEBSITES_CONTAINER_START_TIME_LIMIT=600. O padrao e 230 s
# e, na Sprint 4, o Spring Boot levou ~4,5 min no B1: passando do limite o
# App Service mata a aplicacao e reinicia em loop.
#
# Ressoa - Global Solution 2026/2 - RM561940
#
set -euo pipefail
source "$(dirname "$0")/00_variables.sh"

exigir_comando az

titulo "05 - APP SERVICE"
echo "Plano       : ${APP_SERVICE_PLAN} (Linux B1)"
echo "API Java    : ${WEBAPP_API} (${WEBAPP_API_RUNTIME})"

titulo "APP SERVICE PLAN"
if az appservice plan show --resource-group "${RESOURCE_GROUP}" \
                           --name "${APP_SERVICE_PLAN}" >/dev/null 2>&1; then
    echo "AVISO: o plano '${APP_SERVICE_PLAN}' ja existe. Nada a fazer."
else
    # B1: Always On e 1,75 GB. O F1 dorme, tem cota diaria de CPU e 1 GB.
    az appservice plan create \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${APP_SERVICE_PLAN}" \
        --location "${LOCATION}" \
        --is-linux \
        --sku B1 \
        --tags "${TAGS[@]}" \
        --output none
    echo "  plano ........................ criado"
fi

# criar_webapp <nome> <runtime>: cria (se faltar) e aplica a configuracao
# comum de seguranca e disponibilidade.
criar_webapp() {
    local nome="$1" runtime="$2"
    titulo "WEB APP ${nome}"
    if az webapp show --resource-group "${RESOURCE_GROUP}" --name "${nome}" >/dev/null 2>&1; then
        echo "AVISO: o Web App '${nome}' ja existe."
    else
        az webapp create \
            --resource-group "${RESOURCE_GROUP}" \
            --plan "${APP_SERVICE_PLAN}" \
            --name "${nome}" \
            --runtime "${runtime}" \
            --tags "${TAGS[@]}" \
            --output none
        echo "  web app ...................... criado"
    fi

    az webapp config set \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${nome}" \
        --always-on true \
        --min-tls-version 1.2 \
        --ftps-state Disabled \
        --output none
    az webapp update \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${nome}" \
        --https-only true \
        --output none
    az webapp log config \
        --resource-group "${RESOURCE_GROUP}" \
        --name "${nome}" \
        --docker-container-logging filesystem \
        --output none
    echo "  Always On, HTTPS only, TLS 1.2, FTPS desligado, log de container"
}

criar_webapp "${WEBAPP_API}" "${WEBAPP_API_RUNTIME}"

# App settings que nao sao segredo. Os segredos chegam pela pipeline.
az webapp config appsettings set \
    --resource-group "${RESOURCE_GROUP}" \
    --name "${WEBAPP_API}" \
    --settings "JAVA_OPTS=-Duser.timezone=America/Sao_Paulo" \
               "WEBSITES_CONTAINER_START_TIME_LIMIT=600" \
    --output none
echo "  JAVA_OPTS e WEBSITES_CONTAINER_START_TIME_LIMIT gravadas"

titulo "RESULTADO"
az webapp list --resource-group "${RESOURCE_GROUP}" \
    --query "[].{webapp:name, estado:state, url:defaultHostName}" \
    --output table
