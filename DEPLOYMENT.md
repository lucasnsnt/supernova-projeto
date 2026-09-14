# Implantação

## Ambientes

- `dev`: profile padrão, H2 em memória, e-mail exibido no log e frontend em
  `http://localhost:5173`.
- `prod`: PostgreSQL, cookies seguros, API em `127.0.0.1:8081` e origem permitida
  `https://supernova.lucasnsnt.ink`.

O backend de produção responde em `https://api.supernova.lucasnsnt.ink` por meio
do Nginx. Nenhuma porta da aplicação ou do banco deve ser publicada na internet.

## Fluxo Git

Pull requests para `master` e `prod` executam o workflow de CI. Um push em
`prod` também constrói uma imagem imutável, publica no GHCR e atualiza a VPS.
O script de deploy confirma `/actuator/health` e restaura a imagem anterior se a
nova versão não ficar saudável.

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

## Segredos do ambiente `production` no GitHub

- `VPS_HOST`
- `VPS_USER`
- `VPS_SSH_PRIVATE_KEY`
- `VPS_KNOWN_HOSTS`

A variável opcional `VPS_SSH_PORT` assume `22` quando não configurada.

Os segredos da aplicação ficam apenas em `/opt/supernova/supernova.env`. Não os
adicione aos GitHub Secrets, à imagem Docker ou ao repositório.
