# Diagramas técnicos do Transmoovi

Este pacote foi produzido por leitura estática do código, das migrations Flyway
V1–V8 e dos documentos funcionais existentes. Nenhum arquivo da aplicação foi
alterado. Os diagramas representam o estado observado em 18/09/2026.

## Como abrir e editar

- Importe os arquivos de `editaveis/` em <https://app.diagrams.net/> ou no
  aplicativo desktop diagrams.net. Caixas, textos, grupos e conectores são
  objetos editáveis.
- Use `svg/` para documentação e páginas web; SVG mantém texto vetorial.
- Use `png/` para prévias, mensagens e ferramentas que não renderizam SVG.
- `pdf/transmoovi-diagramas.pdf` reúne as quatro vistas para leitura e impressão.

## 01 — Arquitetura geral

Mostra os três atores, o cliente React/PWA, a API Spring Boot, o núcleo de
transporte, banco, notificações e integrações externas. A leitura principal é da
esquerda para a direita. As caixas inferiores resumem a implantação: frontend na
Vercel e backend/banco protegidos na VPS.

## 02 — Modelo de dados

Apresenta o esquema efetivo depois da migration V8. Cada caixa informa a chave
primária, chaves estrangeiras e restrições de unicidade mais importantes. As
setas apontam para a tabela referenciada; os rótulos `1`, `0..1`, `0..*` e `1..*`
indicam cardinalidade. As áreas coloridas são domínios conceituais para leitura,
não schemas físicos no PostgreSQL.

Pontos importantes: `drivers` e `students` especializam `users` com a mesma PK;
`trip_locations` é uma relação 1:1 com `trips`; `trip_participants` congela o
manifesto associando viagem, aluno e confirmação; uma ocorrência moderna é única
por rota recorrente, data e direção.

## 03 — Fluxo de geolocalização

A parte superior cobre cadastro e prévia: o navegador envia o endereço, o
backend consulta Pelias/ORS e valida confiança, país e limites das coordenadas.
O salvamento persiste latitude/longitude quando o chamador resolve o endereço.
A parte central mostra o planejamento com casa, instituição e base operacional,
seguido da Google Route Optimization API. A parte inferior mostra o GPS do
motorista atualizando a última localização de uma viagem em andamento.

Uma falha de geocodificação ou otimização não elimina passageiros confirmados:
a ocorrência fica em `NEEDS_ATTENTION`.

## 04 — Operação da viagem

Leia pelas faixas horizontais: configuração, ocorrência diária, planejamento e
execução. A rota fixa e a matrícula habilitam confirmações. Apenas respostas
`YES` elegíveis entram no planejamento. O início congela o manifesto; respostas
pendentes viram `NO_RESPONSE`. Dentro da janela inclusiva de ±30 minutos o início
é direto; fora dela o motorista precisa confirmar explicitamente. Durante
`IN_PROGRESS`, o navegador compartilha posição e o motorista conclui paradas e a
viagem.

## Convenções e limites

- Roxo: cliente/cadastro; azul: aplicação e oferta; verde: dados/estado válido;
  ciano: geolocalização externa; âmbar: decisão/atenção; vermelho: falha.
- Os diagramas privilegiam relações e regras relevantes ao fluxo solicitado;
  métodos auxiliares, DTOs e índices de desempenho não são exibidos integralmente.
- O Google Maps JavaScript renderiza mapas no frontend; geocodificação ocorre no
  backend via OpenRouteService/Pelias; otimização ocorre via Google Route
  Optimization API.

## Fontes principais analisadas

- `backend/src/main/resources/db/migration/V1__create_initial_schema.sql` a
  `V8__driver_led_occurrences.sql`
- entidades em `backend/src/main/java/.../models/`
- `GeocodingService`, `TripPlanningService`, `RouteOperationService`,
  `TripTrackingService`, `DailyConfirmationService` e `DailyTransportScheduler`
- controllers de aluno, motorista e geocodificação
- `docs/DAILY_TRANSPORT_PLAN.md`, `docs/DRIVER_LED_IMPLEMENTATION.md`,
  `docs/STUDENT_ONBOARDING.md` e `docs/TRANSPORT_MVP_VALIDATION.md`

