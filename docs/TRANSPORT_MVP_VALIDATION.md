# Validação local do transporte

O fluxo automatizado em `CompleteTransportFlowTests` percorre os endpoints de
cadastro com confirmação de e-mail, aprovação administrativa, criação do veículo
padrão, instituição, agenda de volta, convite, confirmação diária, planejamento,
início e conclusão. Usa H2 com migrations e JWTs emitidos pela aplicação. A entrega
de e-mail e a autenticação no Google são simuladas; as APIs de geocodificação e
rotas são atendidas por um servidor HTTP local com respostas conhecidas.

Essa validação não comprova credenciais, faturamento, cobertura de endereços ou
viabilidade de rotas no Google real. Esses pontos precisam ser verificados em um
ambiente de desenvolvimento configurado antes de declarar o MVP pronto.

## Configuração para serviços reais

Defina no ambiente do backend, sem versionar valores privados:

- `GOOGLE_GEOCODING_ENABLED=true`
- `GOOGLE_GEOCODING_API_KEY`: chave com acesso à Geocoding API.
- `GOOGLE_ROUTE_OPTIMIZATION_ENABLED=true`
- `GOOGLE_CLOUD_PROJECT`: projeto com Route Optimization API habilitada.
- `GOOGLE_APPLICATION_CREDENTIALS`: caminho local de credenciais do Google, ou
  outra fonte de Application Default Credentials aceita pela biblioteca existente.

A geocodificação usa o endereço completo com restrição ao Brasil. Resultados
parciais, ambíguos ou aproximados são recusados. Coordenadas já existentes são
reutilizadas; alterações de endereço devem limpar o par anterior. Cadastro e
edição localizam os endereços quando o serviço está habilitado. O planejamento
resolve também endereços antigos sem coordenadas. Falhas no planejamento mantêm
os participantes e geram uma viagem que precisa de atenção, permitindo recálculo.
Com a geocodificação desabilitada, não são inventadas coordenadas.

Referência: [Geocoding API](https://developers.google.com/maps/documentation/geocoding/guides-v3/requests-geocoding).

## Conferência manual

1. Cadastre o motorista e confirme o e-mail. Antes da aprovação, a API deve recusar
   criação de veículo e convite. Aprove com uma conta administrativa local; o
   status da sessão do motorista é consultado a cada 15 segundos em página ativa.
2. Adicione um veículo. O primeiro é padrão; adicione outro e selecione “Usar como
   padrão” para testar a troca. Gere o convite.
3. Cadastre o aluno, selecione instituição e salve agenda para o dia de teste.
   Confira e aceite o convite. Confirme que os três endereços têm coordenadas:
   base do motorista, residência do aluno e instituição.
4. Aguarde a liberação da confirmação. Para ida, a saída preliminar é uma hora
   antes da aula; para volta, é o horário cadastrado. O prazo de resposta termina
   uma hora antes dessa saída preliminar. Saídas antes de 09h liberam confirmação
   às 20h do dia anterior; as demais às 06h do dia de serviço. A página do aluno
   consulta hoje e amanhã para permitir a resposta na noite anterior.
5. Responda “Sim” antes do prazo. O agendador roda a cada minuto e só planeja depois
   do prazo. Não espere uma viagem imediatamente após responder.
6. Em “Viagens”, confira veículo, ordem das paradas, endereços de embarque e
   desembarque, destinos e estimativas.
   Corrija a causa indicada e recalcule caso haja necessidade de atenção.
7. Inicie, confira “Em andamento” na conta do aluno, conclua e confira “Concluída”
   com horário de conclusão. A tela consulta novamente a cada 15 segundos.
   Selecione a data anterior para verificar o histórico.
8. Mantenha a sessão aberta por mais de 15 minutos e faça uma operação para
   conferir renovação de autenticação. Falhas devem aparecer na tela.

## Comandos

```sh
cd backend
./mvnw test
./mvnw -Dtest=CompleteTransportFlowTests test
```

```sh
cd frontend
npm test
npm run lint
npm run build
```
