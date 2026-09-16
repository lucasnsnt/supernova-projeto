# Implantação

## Ambientes

- `dev`: profile padrão, H2 em memória, e-mail exibido no log e frontend em
  `http://localhost:5173`.
- `prod`: PostgreSQL, cookies seguros, API em `127.0.0.1:8081` e origem permitida
  `https://supernova.lucasnsnt.ink`.

O backend está em `backend/` e sua imagem é publicada no GHCR com o sufixo
`-backend`. Em produção, responde em `https://api.supernova.lucasnsnt.ink` por meio
do Nginx. Nenhuma porta da aplicação ou do banco deve ser publicada na internet.
A API possui limite de 384 MB e 0,75 CPU para coexistir com os demais serviços
da VPS sem poder consumir todos os recursos do host.

## Fluxo Git

Alterações em `backend/**` nos pull requests para `master` e `prod` executam o
workflow de CI do backend. Um push em `prod` que altere `backend/**`,
`frontend/**` ou `infra/vps/**` constrói uma imagem imutável e atualiza a VPS.
O script confirma `/actuator/health` e restaura a imagem anterior se necessário;
somente depois da saúde do backend ser confirmada o frontend é promovido na Vercel.

Para publicar uma versão, faça um pull request de `master` para `prod`.

## Bootstrap da VPS

Copie `infra/vps/bootstrap.sh` para a VPS e execute uma única vez com `sudo`.
Ele cria:

- banco e usuário PostgreSQL exclusivos;
- `/opt/supernova/supernova.env` com credenciais geradas aleatoriamente;
- virtual host e limites de requisição do Nginx.

Depois que o registro DNS existir, gere o certificado conforme a instrução
exibida pelo script. O arquivo de ambiente inicia com `MAIL_DELIVERY=log`; altere
para `smtp` somente depois de preencher as configurações de e-mail. Ao habilitar
o SMTP, defina também `MAIL_HEALTH_ENABLED=true` para que o Actuator monitore a
conexão com o provedor.

## Otimização de rotas da Google

Habilite a Route Optimization API no projeto Google Cloud e conceda à conta de
serviço usada pela aplicação permissão para consumir a API. Copie somente o JSON
da conta de serviço para `/opt/supernova/google-service-account.json`; o arquivo
é montado no container como segredo e nunca deve ser adicionado ao repositório.

No `/opt/supernova/supernova.env`, configure:

```dotenv
GOOGLE_ROUTE_OPTIMIZATION_ENABLED=true
GOOGLE_CLOUD_PROJECT=id-do-projeto
```

Enquanto `GOOGLE_ROUTE_OPTIMIZATION_ENABLED=false`, o backend permanece
funcional, mas novos planejamentos ficam em `NEEDS_ATTENTION` até a integração
ser habilitada e o motorista solicitar o recálculo.

## Geocodificação e primeiro administrador

O Compose habilita a geocodificação ORS em produção. Mantenha `ORS_API_KEY`
somente em `/opt/supernova/supernova.env`.

Para o primeiro acesso administrativo, pode ser criado temporariamente o arquivo
`/home/lucas/supernova-bootstrap.env` com as variáveis `ADMIN_BOOTSTRAP_*`.
O segundo `env_file` é opcional. Remova o arquivo e recrie o container assim que
o primeiro login for confirmado; a conta persistida no PostgreSQL permanece.

## Segredos do ambiente `production` no GitHub

- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_PRIVATE_KEY`
- `VPS_KNOWN_HOSTS`

A variável opcional `VPS_SSH_PORT` assume `22` quando não configurada.

Os segredos da aplicação ficam apenas em `/opt/supernova/supernova.env`. Não os
adicione aos GitHub Secrets, à imagem Docker ou ao repositório.
