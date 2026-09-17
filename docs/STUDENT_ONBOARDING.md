# Validação local do cadastro e entrada do aluno

Acesse o front local e entre com uma conta de aluno. A página **Meu cadastro**
fica disponível no menu, incluindo a navegação inferior em telas pequenas.

## Checklist manual

1. Confira pendências na página Hoje: instituição, endereço, agenda e transporte.
2. Edite dados e endereço; salve e recarregue. CEP deve ter oito dígitos e UF
   duas letras. Alterar o endereço limpa coordenadas antigas.
3. Selecione uma instituição cadastrada pelo administrador e salve pelo menos
   um horário de ida ou volta na Agenda.
4. Com motorista aprovado, crie rota fixa para um dia, horários de ida/volta,
   veículo e instituição do aluno; gere convite ativo. No aluno, informe o código
   e entre na rota.
   A entrada é imediata e não há aprovação. Convite inválido/expirado ou
   instituição não atendida deve ser recusado.
5. Saia da rota e confirme que a agenda acadêmica permanece. Sem matrícula,
   ela não deve gerar confirmação diária nem viagem. Entre novamente e confirme
   que o motorista consegue remover o aluno.
6. Saia e entre com outro aluno: dados do anterior não podem aparecer.
7. Repita os passos principais em largura de celular e confira os quatro itens
   da navegação inferior.

## Escopo e limites

- Cadastro completo segue a regra do backend: endereço, instituição e pelo menos
  um horário. Matrícula em rota é uma pendência adicional do transporte.
- A agenda acadêmica fica retida para validar chegada até o início da aula (ida),
  coleta a partir do fim (volta) e espera máxima existente; sozinha não cria trips.
- Esta tela não faz geocodificação. Rotas usam endereços válidos e configuração
  separada do MVP.

Validação automatizada (a partir da raiz): `scripts/verify-transport-flow.sh` ou,
diretamente, `cd frontend && npm test && npm run lint && npm run build` e
`cd backend && ./mvnw -Dtest=CompleteTransportFlowTests test`.
