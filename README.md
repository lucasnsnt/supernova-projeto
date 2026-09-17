# transmoovi

Monorepo da aplicação web de transporte estudantil.

## Estrutura

```text
backend/   API Java 21 com Spring Boot
docs/      decisões funcionais e técnicas
infra/     implantação e configuração da VPS
frontend/  aplicação web React e TypeScript
```

## Backend

Para rodar front e backend localmente, com banco e códigos de e-mail isolados da
VPS, siga [Desenvolvimento local](docs/LOCAL_DEVELOPMENT.md).

Para executar os testes:

```bash
cd backend
./mvnw test
```

Cada aplicação possui pipeline, imagem e implantação independentes. Os workflows
usam filtros de caminho para executar somente quando seu diretório ou sua
infraestrutura relacionada for alterada.
