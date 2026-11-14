# Ressoa — API principal (Java)

[![CI](https://github.com/olavoneves/ressoa-api-java/actions/workflows/ci.yml/badge.svg)](https://github.com/olavoneves/ressoa-api-java/actions/workflows/ci.yml)

> **Matar a larva com som antes que a seca vire epidemia.**
> Global Solution FIAP 2026/2 — CitizenTech · Disciplina **Java Advanced**

O **Ressoa** combina um aparelho de ultrassom (ESP32 + transdutor piezoelétrico) que mata larvas do *Aedes aegypti* dentro de tambores e caixas d'água com uma **plataforma de risco** que diz onde instalar e quando agir. Esta API é o núcleo da plataforma: contas, bairros, aparelhos, avaliações de risco, orientação gerada por IA e alertas aos moradores.

**Cidadão beneficiado:** morador de periferia com abastecimento de água intermitente (prioridade a bebês, gestantes e idosos). **Segundo público:** agentes de endemias e gestores da vigilância em saúde.

## Integrantes

| Nome | RM |
|---|---|
| Olavo Porto Neves | RM563558 |
| Pedro Henrique Dias França | RM561940 |
| Luiz Gustavo Gonçalves | RM564495 |
| Altamir Lima | RM562906 |
| Felipe Conte | RM562248 |

## Arquitetura

```text
[App Mobile] ──JWT──► API Java (este repo) ──► Oracle ◄── API .NET (telemetria) ◄── [Aparelho ESP32]
                         │   │
[Serviço de IA] ─────────┘   ├──Spring AI──► Google Gemini (orientação / plano de ação)
  POST /api/avaliacoes       └──publica "risco.atualizado"──► RabbitMQ ──► consumidor ──► NOTIFICACAO
```

- **Spring Boot 4.1 · Java 21** · Spring Data JPA · Spring Security (JWT HS256, resource server) · Spring AI (Gemini) · Spring AMQP · springdoc-openapi · Actuator.
- Organização por funcionalidade: `auth`, `usuario`, `bairro`, `dispositivo`, `avaliacao`, `orientacao`, `notificacao`, `mensageria`, `shared`.
- Erros padronizados em **Problem Details (RFC 9457)**; paginação com envelope `{ content, page, size, totalElements, totalPages, last }`.
- Modelo de dados: `scripts/script-bd.sql` (fonte única do DDL; usado também pela API .NET e pela disciplina de Banco).

### Requisito 5 — Arquitetura distribuída: **Opção A (RabbitMQ)**

Quando uma avaliação de risco muda o nível de um bairro, a API publica o evento `risco.atualizado` (exchange `ressoa.eventos`). Um consumidor assíncrono grava uma notificação para cada morador do bairro quando o risco sobe, exibida no app. Fila com DLQ e consumo idempotente.

## Como rodar

**Pré-requisitos:** JDK 21. O Maven vem pelo wrapper (`./mvnw`), não precisa instalar.

1. Suba Oracle e RabbitMQ locais (ou use o Oracle da FIAP e o CloudAMQP):
   ```bash
   docker compose -f infra/docker-compose.yml up -d oracle rabbitmq   # compose no repositório hub
   ```
2. Crie o esquema executando `scripts/script-bd.sql` no seu usuário Oracle.
3. Copie `.env.example` para `.env` e preencha (`DB_*`, `JWT_SECRET` com 32+ caracteres, `GEMINI_API_KEY`, `RABBITMQ_URL`).
4. Rode:
   ```bash
   # Linux/macOS (Git Bash)
   set -a && source .env && set +a && ./mvnw spring-boot:run
   # Windows PowerShell
   Get-Content .env | % { if ($_ -match '^([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }; .\mvnw spring-boot:run
   ```
5. Acesse **http://localhost:8080/swagger-ui.html**.

**Testes:** `./mvnw test` — rodam com H2 em memória, sem Oracle, LLM ou RabbitMQ (prontos para o CI).

### Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão Oracle |
| `JWT_SECRET` | Segredo HS256 (≥ 32 bytes), **o mesmo da API .NET** |
| `JWT_EXPIRACAO_MINUTOS` | Validade do token (padrão 480) |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | Google Gemini (padrão `gemini-2.5-flash`) |
| `AI_CHAT_PROVIDER` | `google-genai` (padrão) ou `none` (usa orientação de contingência) |
| `RABBITMQ_URL` | URI AMQP (ex.: CloudAMQP `amqps://...`) |
| `SEED_HABILITADO`, `SEED_SENHA_PADRAO` | Carga de dados de demonstração com banco vazio |

## Usuários de teste

Criados automaticamente com o banco vazio (`SEED_HABILITADO=true`). Senha: o valor de `SEED_SENHA_PADRAO` (padrão de desenvolvimento: `Ressoa@2026`).

| E-mail | Papel | Observação |
|---|---|---|
| `gestor@ressoa.app` | GESTOR | Gestora da vigilância (Jardim Ângela, SP) |
| `morador@ressoa.app` | CIDADAO | Maria — 2 aparelhos no Jardim Ângela |
| `joao@ressoa.app` | CIDADAO | João — 1 aparelho no Grajaú |
| `ia-servico@ressoa.app` | GESTOR | Conta do serviço de IA (grava avaliações) |

## Endpoints principais

| Método e rota | Acesso | Descrição |
|---|---|---|
| `POST /api/auth/registro` | público | Cadastro do morador (já devolve token) |
| `POST /api/auth/login` | público | `{ token, tipo, expiraEm, papel, usuario }` |
| `GET /api/auth/me` | autenticado | Perfil do usuário |
| `GET /api/bairros`, `GET /api/bairros/{id}` | público | Lista paginada (filtros `nome`, `municipio`, `uf`, `nivel`) |
| `POST/PUT/DELETE /api/bairros` | GESTOR | CRUD de bairros |
| `GET/POST/PUT/DELETE /api/dispositivos` | CIDADAO (os seus) · GESTOR (todos) | CRUD paginado de aparelhos |
| `/swagger-ui.html`, `/actuator/health` | público | Documentação e saúde |

*(Avaliações de risco, orientação por IA e notificações: em desenvolvimento.)*

## Deploy

Azure App Service via Azure DevOps (disciplina de DevOps). Link: *a definir*.

## Declaração de uso de IA

| Ferramenta | Uso |
|---|---|
| Claude Code (Anthropic) | Apoio na estruturação do projeto, geração e revisão de código, testes e documentação. Todo o código foi revisado e é explicado pela equipe. |
| Google Gemini (via Spring AI) | Funcionalidade do produto: gera a orientação ao morador e o plano de ação do gestor a partir do nível de risco. |
