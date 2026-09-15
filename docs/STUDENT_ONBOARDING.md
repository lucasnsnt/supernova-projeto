# Validação local do cadastro do aluno

Acesse o front local e entre com uma conta de aluno. A página **Meu cadastro**
fica disponível no menu, incluindo a navegação inferior em telas pequenas.

## Checklist manual

1. Confira as pendências na página Hoje: instituição, endereço, agenda e motorista.
2. Edite nome, telefone, nascimento e endereço; salve e recarregue a página para
   verificar a persistência. CEP deve ter oito dígitos e UF duas letras.
3. Selecione uma instituição cadastrada pelo administrador. Se a lista estiver
   vazia, cadastre uma instituição com a conta administrativa primeiro.
4. Na Agenda, salve pelo menos um horário de ida ou volta. Retorne à página Hoje
   e confira que a pendência de agenda desapareceu.
5. Com uma conta de motorista aprovado, gere um convite. Na conta do aluno,
   informe o código, confira o nome do motorista e confirme o vínculo.
6. Confira que editar o código remove a confirmação do convite anterior. Um
   código inválido ou expirado deve mostrar erro sem permitir confirmação.
7. Confira o motorista atual e o histórico. Em Encerrar vínculo, escolha Manter
   vínculo e verifique que nada mudou; depois confirme o encerramento e confira
   que a pendência de motorista reaparece.
8. Saia e entre com outro aluno: dados do aluno anterior não devem aparecer.
9. Repita os passos principais em largura de celular e confira os quatro itens
   da navegação inferior.

## Escopo e limites

- Cadastro completo segue a regra do backend: endereço, instituição e pelo
  menos um horário. Motorista ativo é uma pendência adicional para o transporte.
- Endereço inalterado preserva coordenadas; alteração de rua, número, bairro,
  cidade, UF ou CEP limpa coordenadas antigas para evitar rota incorreta.
- Esta tela não faz geocodificação. A configuração e validação das rotas com
  Google continuam sendo uma etapa separada do MVP.
- Os testes de componentes usam respostas simuladas da API; não substituem a
  validação manual com backend local nem validam a configuração de produção.

Validação automatizada: `cd frontend && npm test && npm run lint && npm run build`
e `cd backend && ./mvnw test`.
