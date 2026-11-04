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
docker compose -f compose.yaml -f compose.https-local.yaml up -d
npm run migrate -w backend
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

## Cenário automatizado nativo

O cenário não injeta token fictício. Ele usa a configuração AppAuth real do Android, PKCE e código emitido pelo Keycloak, envia pelos clientes reais, persiste no Room e verifica os registros no backend. A tela React é carregada em WebView de teste com sessão profissional real para verificar a apresentação dos registros. Não se trata de homologação da navegação do navegador do usuário ou de aparelho físico.

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
