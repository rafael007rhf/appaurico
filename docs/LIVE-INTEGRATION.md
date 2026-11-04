# Integração real — ambiente de desenvolvimento e homologação

## Estado e escopo

Branch: `feature/aurico-live-integration`, criada a partir de `182b164fa24f5863b7da7af5cb392d1525b395a4`, sem alterações locais pendentes. A base já possuía o domínio e a fila; o APK era local por falta de URLs. Este incremento conecta os serviços, adiciona cadastro profissional autenticado, revogação no Android, polling do painel e um cenário instrumentado contra PostgreSQL e Keycloak reais.

## Inicialização reproduzível com Docker

Requisitos: Node 22, Docker Compose, JDK 21, SDK Android 36.1 e emulador API 35. Use somente dados sintéticos neste ambiente.

```sh
npm ci
node scripts/setup-local.mjs        # apenas na primeira configuração
node scripts/setup-https-local.mjs
# O segundo arquivo define hostname e caminho HTTPS da identidade.
docker compose -f compose.yaml -f compose.https-local.yaml up -d --wait
node --env-file=backend/.env.https --import tsx backend/src/migrate.ts
```

Em terminais separados, a partir da raiz do repositório:

```sh
NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt" node --env-file=backend/.env.https --import tsx backend/src/server.ts
npm run dev -w professional-web
node scripts/https-local-proxy.mjs
```

Endereço único: **https://localhost:8443**. API: `/api/v1`; identidade: `/identity/realms/aurico`; readiness: `/ready`. O certificado é assinado por uma CA local gerada no computador, válida por 30 dias. Nenhuma chave privada entra no APK ou no Git. Para acesso humano no navegador, instale conscientemente a CA pública de desenvolvimento no ambiente de teste; não ignore intersticiais de segurança. Para ambientes remotos, use domínio e certificado de CA pública.

A alternativa usada neste computador sem Docker é PostgreSQL nativo obtido via pacote de desenvolvimento `embedded-postgres`, distribuição oficial Keycloak e o mesmo gateway TLS. As tabelas e migrações são PostgreSQL reais; PGlite continua restrito à suíte rápida de API.

## Endereço no Android

Para emulador ou aparelho de desenvolvimento conectado por USB:

```sh
adb reverse tcp:8443 tcp:8443
./gradlew :app:assemblePatientDebug \
  -PauricoApiUrl=https://localhost:8443 \
  -PauricoOidcIssuer=https://localhost:8443/identity/realms/aurico
```

Aqui `localhost` só funciona porque `adb reverse` encaminha explicitamente a porta do dispositivo ao computador. Sem esse encaminhamento, o localhost do aparelho não é o computador. O APK debug confia apenas na CA local para esse domínio; assinaturas, validade e hostname TLS continuam sendo verificados. O build de release e os builds que não executaram o setup usam somente CAs do sistema. Não há opção para aceitar qualquer certificado.

Para um aparelho fora do USB/rede de teste, configure URLs HTTPS públicas reais da API e do provedor. Elas dependem de domínio, hospedagem e credenciais; este trabalho não publica serviços de produção.

## Contas e fluxo manual

1. Abra o painel e use o cadastro do provedor. Confirme o e-mail (Mailpit em desenvolvimento).
2. Na conta ainda sem vínculos de paciente, escolha “Criar meu perfil profissional”. A API cria o perfil e audita a mudança; o cadastro permanece não verificado. Contas que já têm vínculos/tratamentos de paciente precisam de outra identidade.
3. Crie o convite por e-mail. No Android, entre com a identidade de paciente correspondente e aceite com consentimento explícito.
4. No painel, selecione o paciente, crie/revise/publice o plano. No Android, sincronize; o plano é persistido no Room.
5. Faça check-in e confirme uma ocorrência. Desligue Wi-Fi/dados e faça outro registro; ele deve ficar pendente. Reconecte e sincronize.
6. O painel consulta os dados persistidos e atualiza em até 30 segundos quando está visível. Sessões e retornos continuam no mesmo tratamento.
7. A tela Conta do Android permite consultar e revogar vínculos. A revogação bloqueia os acessos profissionais e é recebida no próximo pull.
8. Em Pacientes, a lista de convites pendentes permite cancelar um código antes da aceitação. Somente o criador pode listar ou cancelar seus convites; o cancelamento não altera um vínculo já aceito.

## Cenário automatizado nativo

O cenário não injeta token fictício. Ele usa a configuração AppAuth real do Android, PKCE e código emitido pelo Keycloak, envia pelos clientes reais, persiste no Room e verifica os registros no backend. A tela React é carregada em WebView de teste com sessão profissional real para verificar a apresentação dos registros. Não se trata de homologação da navegação do navegador do usuário ou de aparelho físico.

Antes do cenário, prepare o banco isolado e, no terminal do backend, pare apenas o processo da API e reinicie com a configuração de integração (os demais serviços continuam):

```sh
node scripts/init-integration-database.mjs
NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt" node --env-file=backend/.env.integration --import tsx backend/src/migrate.ts
NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt" node --env-file=backend/.env.integration --import tsx backend/src/server.ts
```

Em outro terminal:

```sh
NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt" node scripts/wait-local.mjs
NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt" node --env-file=.env scripts/prepare-integration.mjs
./gradlew :app:assemblePatientDebug :app:assemblePatientDebugAndroidTest \
  -PauricoApiUrl=https://localhost:8443 \
  -PauricoOidcIssuer=https://localhost:8443/identity/realms/aurico
node scripts/run-android-integration.mjs
```

O runner só aceita emulador dedicado. As credenciais sintéticas ficam em arquivo ignorado 0600, são copiadas para o diretório privado do app de teste e removidas ao terminar. Nenhuma senha ou token é compilado no APK. O teste normal de CI pula esse cenário quando não existe o argumento explícito `liveIntegration=true`.

## Preparação para implantação

Use `backend/.env.production.example`. O PostgreSQL gerenciado precisa de validação TLS completa; configure certificados exigidos pelo provedor, sem desabilitar a verificação. Execute as migrações como etapa controlada, com backup anterior e usuário apropriado. Mantenha a API atrás de reverse proxy; o processo atual escuta loopback. O painel usa proxy no mesmo domínio e não requer CORS aberto.

Valide `/ready`, callback web HTTPS e redirect Android exatos no provedor, SMTP real para cadastro/recuperação, rotação de segredos, auditoria, retenção e política de backup. Antes de atualização, faça backup PostgreSQL com ferramenta compatível, cifre o arquivo e teste restauração em banco isolado. Nunca envie dumps a Git. Não usar a CA local ou o `start-dev` do Keycloak em produção.

## Resultado executado nesta entrega

- PostgreSQL nativo real: cluster persistente local e banco exclusivo `aurico_integration`, com migrações 0001 e 0002 executadas.
- Keycloak 26.8.1 real, gateway HTTPS e painel Next.js de produção em execução local durante o teste.
- Emulador Android API 35 ARM64: `LivePlatformIntegrationTest` executado, uma execução aprovada, zero falhas, zero cenários pulados. A fase do cenário levou aproximadamente dez segundos, além da instalação/build.
- Fluxo coberto: identidade paciente autenticada no Android por AppAuth/PKCE; profissional cadastrado via API autenticada; convite cancelado rejeitado e convite válido aceito; vínculo persistido; plano publicado; Room recebendo dados; um registro online e um offline; estimulação; replay idempotente; sessão registrada; nova versão de plano recebida sem perda dos eventos; retorno e sessões no snapshot sem notas privadas; profissional não vinculado recusado; painel React renderizando os registros e cancelando um convite pelo botão real; revogação; logout e preservação de histórico.
- Conferência independente no PostgreSQL após o cenário aprovado: dois registros de sintomas e uma estimulação do cenário; tratamentos das tentativas sintéticas anteriores permaneceram isolados e identificados como teste.
- Os testes anteriores encontraram um erro real de contrato de timestamps do driver PostgreSQL. A API agora serializa datas em ISO 8601, inclusive dentro de JSONB de sincronização já existente. O normalizador é idempotente e não altera texto de observações ou datas sem horário.
- Cadastro público também exercitado via páginas reais do Keycloak: criação de conta, recebimento no Mailpit, confirmação de e-mail, definição de senha e callback com sessão autenticada na API. Esse teste usa uma identidade sintética própria; o teste instrumentado prepara identidades pela administração do provedor.
- Suíte rápida: 19 testes de API/serialização aprovados, incluindo cancelamento de convites e isolamento entre criadores. Build, lint e testes JVM Android passaram nas verificações locais. O teste nativo não substitui homologação da navegação por navegador externo e de aparelhos físicos.

## Backup e restauração

A operação real deve usar uma ferramenta `pg_dump` compatível com a versão do servidor. Configure host/usuário/TLS e autenticação por serviço ou arquivo `.pgpass` com permissão 0600; não passe senha na linha de comando. Exemplo, com as variáveis PostgreSQL já configuradas:

```sh
# Destino em volume cifrado, restrito e fora do repositório.
pg_dump --format=custom --file=/CAMINHO_PROTEGIDO/aurico.dump
# Restauração de validação em banco NOVO e isolado, nunca sobre produção.
createdb aurico_restore_validation
pg_restore --exit-on-error --no-owner --dbname=aurico_restore_validation /CAMINHO_PROTEGIDO/aurico.dump
```

Compare versões de migração, contagens, índices, restrições e um fluxo autorizado antes de considerar o backup recuperável. A política de retenção e a proteção do arquivo precisam ser aprovadas pela operação. O script `scripts/verify-test-backup.mjs` automatiza a comparação somente para o banco local de integração; requer os binários de cliente PostgreSQL em `PG_BIN`. Ele não foi executado neste host, cujo pacote nativo contém o servidor, mas não `pg_dump`/`pg_restore`.
