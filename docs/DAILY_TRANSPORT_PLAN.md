# Planejamento da operação diária

## Rota fixa do motorista

O motorista cria uma rota recorrente para um dia da semana, com os horários
pareados de ida/volta, instituições atendidas e o veículo configurado. As
saídas publicadas são fixas; cada ocorrência nasce dessa rota, nunca de uma
agenda escolar isolada.
O aluno só entra com convite ativo daquele motorista e instituição atendida.
Entrada é imediata, sem aprovação; o aluno pode sair e o motorista pode removê-lo.
A agenda acadêmica permanece para validar horários, mas sozinha não cria viagens.

## Confirmação diária

Somente alunos matriculados em uma rota geram confirmação diária. O aluno responde
`YES` ou `NO`; ausência até o prazo vira `NO_RESPONSE`, e apenas `YES` entra no
manifesto. A confirmação pode ser liberada antes do prazo e o motorista pode
iniciar antes dele. A rota define `responseDeadlineTime` próprio; esse valor
configurado é a fonte de verdade da ocorrência (incluindo a janela de liberação,
com os horários padrão de 20:00 no dia anterior/06:00 no dia de serviço quando
assim configurados).
Fuso operacional: `America/Bahia`.

## Planejamento das viagens

Após o prazo, calcula-se a ocorrência somente com `YES` matriculados naquela rota.
A ocorrência é uma única viagem; capacidade, trajeto e horários determinam sua
viabilidade. Na ida, casa é coleta e instituição é entrega até o início da aula.
Na volta, instituição é coleta a partir do fim da aula e casa é entrega; espera
máxima: 30 minutos.

Solução impossível não remove confirmados silenciosamente: fica `NEEDS_ATTENTION`
com as restrições não atendidas.

## Atuação do motorista

Cartões de rota abrem modal com o elenco. Cartões de ocorrência abrem prévia de
otimização real usando só passageiros `YES`, endereços de casa/instituição e o
veículo da rota. O otimizador pode reordenar instituições por tempo/geografia,
sem alterar a saída fixa.

Início dentro de ±30 minutos (inclusive limites) é direto. Fora exige confirmação
explícita: **Iniciar antecipadamente** antes ou **Iniciar agora** depois. Pode
iniciar cedo antes do prazo. Ao iniciar, congela os `YES` atuais: sem entradas
tardias, e viagens vazias ou inviáveis não podem iniciar.

O veículo da rota é aplicado à ocorrência. Alterações e remoções notificam
internamente os participantes. O MVP usa Google Route Optimization API isolada
do domínio; prévia recalcula ao abrir e a rota definitiva congela no início.

## Estados

```text
PENDING -> YES | NO | NO_RESPONSE
PLANNING -> PLANNED -> IN_PROGRESS -> COMPLETED
                    +--------------> CANCELLED
       +----------------------------> NEEDS_ATTENTION
```

## Fora do primeiro MVP

- calendário escolar, feriados e exceções planejadas;
- push, SMS ou email operacional;
- agenda completa de turnos do motorista;
- gestão documental avançada da aprovação do motorista.
