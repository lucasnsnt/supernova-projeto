# Implementação orientada pelo motorista

Contrato de integração: o motorista oferece uma rota fixa por dia, com horários
pareados de ida/volta, instituições e veículo; o aluno entra, responde à
ocorrência e pode sair.

## Tarefas e responsabilidades

- **Backend:** matrícula por convite ativo + instituição atendida; entrada/saída/
  remoção sem aprovação; confirmações apenas para matrículas; manter
  `StudentSchedule`; freeze no início; janela ±30m e confirmações explícitas
  fora dela; regras de capacidade/viabilidade; veículo da rota; migration
  incremental preservando histórico; uma ocorrência corresponde a uma única
  viagem e inviabilidade/capacidade fica em atenção.
- **Frontend:** formulário de rota fixa; roster modal nos cartões de rota; prévia
  real nos cartões de ocorrência; matrícula sem “pedido pendente”; controles de
  início e manifesto congelado; mensagens para rota vazia/inviável.
- **Testes:** convite/instituição, entrada imediata, saída/remoção, agenda sem
  matrícula, `YES`/`NO`/`NO_RESPONSE`, ida/volta, otimização, ±30m, início
  antecipado/tardio, freeze, capacidade e migração/histórico.
- **Revisão:** executar `scripts/verify-transport-flow.sh quick` por padrão e os
  testes focados apenas nas áreas alteradas. Use `all` para o conjunto focado e
  `full` separadamente quando for necessário; `scripts/audit-transport-flow.sh`
  revisa o escopo. O pipeline de produção executa a verificação final antes
  de publicar o backend e o frontend.

## Checklist de aceite

- [ ] Rota fixa contém dia, direção, instituições e veículo configurado.
- [ ] Convite ativo e instituição servida são obrigatórios; entrada é imediata.
- [ ] Aluno pode sair; motorista pode remover; agenda acadêmica continua retida.
- [ ] Sem matrícula não há confirmação/viagem diária.
- [ ] Ida chega até início; volta coleta a partir do fim e respeita espera máxima.
- [ ] Só `YES` vigente entra na prévia/manifesto; endereços e veículo são da rota.
- [ ] Otimizador pode reordenar instituições sem alterar saída agendada.
- [ ] Início em ±30m é direto; fora exige confirmação; cedo não depende do prazo.
- [ ] Início congela passageiros; sem entradas tardias, viagem vazia ou inviável.
- [ ] Histórico preservado por migration incremental e testes existentes verdes.
- [ ] Scripts de verificação e auditoria passam; publicação aguarda revisão.
