# Frontend transmoovi

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

O arquivo `vercel.mjs` mantém as rotas da SPA funcionando em acessos diretos e
encaminha `/api/*` para a origem definida em `URL_BACKEND`.

Configure `URL_BACKEND=https://api.supernova.lucasnsnt.ink` em Production, sem
`/api` no final. Para Preview, defina explicitamente a API de testes desejada;
não há fallback silencioso para produção. Alterar a variável requer novo deploy.

`BACKEND_LOCAL` é usada somente pelo Vite local, em `.env.local`, e não precisa
estar em Production na Vercel. O navegador continua chamando `/api` no próprio
domínio. Nenhuma dessas variáveis usa o prefixo `VITE_`, portanto não é incorporada
ao bundle como variável pública. O endereço da API, entretanto, não é um segredo.

Veja o fluxo completo de desenvolvimento em [../docs/LOCAL_DEVELOPMENT.md](../docs/LOCAL_DEVELOPMENT.md).
