# Planejamento da operação diária

## Confirmação do aluno

Cada ocorrência da agenda gera uma confirmação diária independente da viagem.
O aluno responde uma única vez com `YES` ou `NO`. A ausência de resposta até o
prazo fica registrada como `NO_RESPONSE`; somente `YES` participa da viagem.

O sistema calcula uma saída preliminar considerando todos os alunos esperados.
O prazo termina uma hora antes dessa saída e não é prorrogado quando recusas
tornam a rota mais curta.

- Saída anterior às 09:00: confirmação disponível às 20:00 do dia anterior.
- Saída a partir das 09:00: confirmação disponível às 06:00 do próprio dia.
- Fuso operacional: `America/Bahia`.

## Planejamento das viagens

No encerramento do prazo, o sistema cria viagens somente com os alunos que
responderam `YES`. A quantidade de viagens é resultado da viabilidade de
capacidade, trajeto e horários, e não um número fixo.

Na ida, a casa é a coleta e a instituição é a entrega, cujo limite é o horário
da agenda. Na volta, a instituição é a coleta, disponível no horário da agenda,
e a casa é a entrega. A espera máxima depois da aula é de 30 minutos.

Se uma solução não for possível, nenhum aluno confirmado é removido
silenciosamente. O planejamento fica em `NEEDS_ATTENTION` e informa as
restrições que não puderam ser atendidas.

## Atuação do motorista

A viagem é criada pelo sistema e não depende de aceite. O motorista confirma ou
ajusta o horário de saída. Até 30 minutos antes, ele pode adiantar ou atrasar em
até 30 minutos quando a rota continuar viável. Depois desse limite, somente um
atraso com motivo pode ser informado. O cancelamento é permitido até o início e
sempre exige motivo.

O motorista possui um veículo padrão, aplicado automaticamente às novas
viagens. Ele pode substituir o veículo de uma viagem antes de iniciá-la, sem
alterar o padrão, desde que a capacidade seja suficiente. Mudanças de horário,
veículo e cancelamento geram notificações internas aos participantes.

O endereço operacional do motorista é a origem da ida e o destino da volta. Ele
pode coincidir com o endereço cadastrado ou ser configurado separadamente.

## Rotas e notificações

O MVP usará a Google Route Optimization API. A integração fica isolada do
domínio para permitir troca de fornecedor. Respostas são exibidas em tempo real,
mas a rota externa não é recalculada a cada resposta. A otimização definitiva
ocorre no encerramento do prazo e é repetida apenas para mudanças operacionais
relevantes.

As notificações são persistentes e internas à aplicação web. Não haverá SMS,
email operacional ou push do dispositivo no MVP.

## Estados

```text
PENDING -> YES
        -> NO
        -> NO_RESPONSE
```

```text
PLANNING -> PLANNED -> IN_PROGRESS -> COMPLETED
       |         |
       |         +-----------------> CANCELLED
       +---------------------------> NEEDS_ATTENTION
```

## Fora do primeiro MVP

- calendário escolar, feriados e exceções planejadas;
- rastreamento GPS ao vivo;
- push, SMS ou email operacional;
- agenda completa de turnos do motorista;
- gestão documental avançada da aprovação do motorista.
