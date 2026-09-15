# Desenvolvimento local

As mudanças são implementadas e verificadas localmente. Commit não exige push,
merge ou deploy. Produção só deve receber alterações depois da validação local.

## Requisitos

- Java 21 (o Maven Wrapper está no repositório).
- Node.js 24 e npm.
- Não precisa de Docker, PostgreSQL, Vercel, SMTP ou credenciais de produção.

## Backend — terminal 1

Na raiz do repositório:

```bash
cd backend
cp .env.local.properties.example .env.local.properties
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

A API fica em `http://127.0.0.1:8080`. O perfil `local` usa H2 em arquivo
(`backend/.local/`), com migrations Flyway. Os dados sobrevivem a reinícios e
não se misturam com o banco da VPS. Os testes continuam usando bancos isolados.

Os códigos de cadastro são exibidos no terminal do backend. Eles **não** são
enviados por e-mail neste ambiente. Solicite o código no frontend, copie os seis
dígitos do log e insira no formulário. Não use esse modo em produção.

Para ter um administrador local, descomente `app.local-admin.email` e
`app.local-admin.password` em `.env.local.properties`, usando credenciais
exclusivas de desenvolvimento. Ele é criado no início apenas se o e-mail não
existir. Reiniciar não altera senha nem papel de contas existentes. Esse
inicializador não é ativado no perfil `prod`.

## Frontend — terminal 2

Na raiz do repositório:

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha `/api` para `BACKEND_LOCAL`
(`http://127.0.0.1:8080`). A porta do frontend é fixa; ele avisa se estiver ocupada.
Se alterar `.env.local`, reinicie o Vite. Não configure `BACKEND_LOCAL` com a API
de produção: ações de teste poderiam modificar dados reais.

## Verificações antes de publicar

```bash
cd backend
./mvnw test
```

Em outro terminal, na raiz:

```bash
cd frontend
npm run lint
npm test
npm run build
```

Teste manualmente cadastro, código, login/logout, agenda, notificações e telas
de cada papel. A otimização Google fica desabilitada localmente: testar rotas
reais depende de habilitar a integração com credenciais específicas de teste.

Nunca copie chaves, senhas ou arquivos da VPS para o repositório. Os arquivos
privados `.env.local`, `.env.local.properties` e o banco local estão no gitignore.
Não remova o banco local sem precisar: isso apaga suas contas e dados de teste.

## Vercel

- Root Directory: `frontend`.
- Production Branch: `prod`.
- `URL_BACKEND`: origem HTTPS da API de cada ambiente, sem `/api`.
- `BACKEND_LOCAL`: não é usada no deploy, pode ser removida de Production.

As configurações da Vercel são avaliadas no build. Não é necessário executar
Vercel para desenvolver localmente. Não coloque segredos em variáveis `VITE_*`:
esses valores são públicos no navegador.
