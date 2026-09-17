# Validação local do transporte

`CompleteTransportFlowTests` deve percorrer cadastro, aprovação administrativa,
veículo, instituição, agenda acadêmica, convite ativo, rota fixa de um dia com
ida/volta,
entrada imediata, confirmação diária, prévia/otimização, início e conclusão.
Também deve provar que aluno sem matrícula não gera ocorrência, que motorista
remove aluno e que início congela o manifesto `YES`. H2, JWT, email, Google e
provedores de mapas são simulados por migrations/fixtures locais.

Essa validação não comprova credenciais, faturamento, cobertura de endereços ou
viabilidade no Google real. Verifique esses pontos em ambiente de desenvolvimento
configurado antes de declarar o MVP pronto. Nunca versione segredos.

## Conferência manual

1. Cadastre motorista, confirme email e aprove com administrador. Antes da
   aprovação, criação de veículo e convite devem ser recusados.
2. Crie veículo (o configurado na rota), instituição e convite ativo. Configure
   rota fixa de um dia com horários de ida/volta, instituições e saída; não crie
   viagem manual.
3. Cadastre aluno, endereço, instituição e agenda de ida/volta. Entre na rota com
   convite: a entrada deve ser imediata. Convite expirado e instituição não servida
   devem falhar. Saída do aluno e remoção pelo motorista devem funcionar.
4. Retire a matrícula: a agenda permanece, mas não há confirmação/viagem. Reinsira
   e responda `YES`; `NO` e `NO_RESPONSE` não entram no manifesto.
5. Valide ida até início da aula, volta a partir do fim e espera máxima de 30m.
   O prazo usa o `responseDeadlineTime` configurado na rota; liberação segue os
   horários configurados (20:00/06:00 são os padrões documentados).
6. Na visão do motorista, cartão de rota abre roster modal; cartão de ocorrência
   abre prévia real com apenas `YES`, endereços casa/instituição e veículo da rota.
   Instituições podem ser reordenadas por tempo/geografia, sem mudar saída fixa.
7. Inicie em ±30m sem confirmação; fora, exija **Iniciar antecipadamente** ou
   **Iniciar agora**. Inicie cedo antes do prazo e confirme freeze: sem late entrants,
   sem viagem vazia ou inviável.
8. Confira “Em andamento”, conclua e confira “Concluída” com histórico. Mantenha
   sessão aberta por mais de 15 minutos e teste renovação de autenticação.

## Comandos

```sh
scripts/verify-transport-flow.sh backend
scripts/verify-transport-flow.sh all
```

Para provedores reais, consulte `docs/LOCAL_DEVELOPMENT.md`; o teste local não
faz chamadas externas nem exige credenciais.
