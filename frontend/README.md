# Frontend Supernova

Aplicação web responsiva construída com React, TypeScript e Vite.

## Desenvolvimento

Com o backend executando na porta `8080`:

```bash
npm install
npm run dev
```

O servidor de desenvolvimento encaminha `/api` para o backend. Antes de abrir
uma sessão, o cliente obtém o token CSRF; o access token permanece apenas na
sessão do navegador e o refresh token continua no cookie HttpOnly.

## Verificação

```bash
npm run lint
npm test
npm run build
```

## Deploy na Vercel

Ao importar o repositório na Vercel, configure:

- **Root Directory:** `frontend`
- **Framework Preset:** Vite
- **Production Branch:** `prod`

O arquivo `vercel.json` mantém as rotas da SPA funcionando em acessos diretos e
encaminha `/api/*` para o backend de produção. Nenhuma variável de ambiente é
necessária para essa configuração inicial.
