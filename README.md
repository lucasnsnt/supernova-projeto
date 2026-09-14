# Supernova

Monorepo da aplicação web de transporte estudantil.

## Estrutura

```text
backend/   API Java 21 com Spring Boot
docs/      decisões funcionais e técnicas
infra/     implantação e configuração da VPS
frontend/  aplicação web (próxima etapa)
```

## Backend

Para executar os testes:

```bash
cd backend
./mvnw test
```

Cada aplicação possui pipeline, imagem e implantação independentes. Os workflows
usam filtros de caminho para executar somente quando seu diretório ou sua
infraestrutura relacionada for alterada.
