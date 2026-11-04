# AURICO 3.0 — paciente, profissional e API integrados

Android offline-first, painel Next.js, API TypeScript/PostgreSQL e identidade OIDC. A integração de desenvolvimento foi exercitada no Android real em emulador contra serviços reais, incluindo registro offline e apresentação dos dados no painel. Não houve deploy de produção.

[Auditoria desta integração](docs/INTEGRATION-AUDIT.md) · [Guia completo e resultados](docs/LIVE-INTEGRATION.md).

## Iniciar banco, identidade, backend e painel

Requisitos: Node 22.16+, npm e Docker Compose. O host desta entrega não tinha Docker; a validação local usou PostgreSQL nativo e distribuição Keycloak, com o mesmo protocolo e migrações. O CI reproduz o cenário via Compose.

```sh
npm ci
node scripts/setup-local.mjs       # SOMENTE na primeira configuração
node scripts/setup-https-local.mjs
docker compose -f compose.yaml -f compose.https-local.yaml up -d --wait
export NODE_EXTRA_CA_CERTS="$PWD/infra/local-tls/ca.crt"
node --env-file=backend/.env.https --import tsx backend/src/migrate.ts
node --env-file=backend/.env.https --import tsx backend/src/server.ts
```

Em outros terminais:

```sh
npm run dev -w professional-web
node scripts/https-local-proxy.mjs
```

Use **https://localhost:8443** como endereço do painel. API: `/api/v1`, identidade: `/identity/realms/aurico`, readiness: `/ready`. O endereço HTTP interno impresso pelo Next.js não é o endereço canônico deste fluxo HTTPS. E-mails de desenvolvimento ficam no Mailpit: http://127.0.0.1:8025.

A CA pública local precisa ser confiada conscientemente no navegador de teste; não ignore erros TLS. Ela é criada em `infra/local-tls/ca.crt`. Chaves e configurações sensíveis ficam ignoradas e com permissões restritas. O setup inicial recusa sobrescrever credenciais. Não executar `docker compose down -v` em bancos que precisem ser preservados.

## Cadastro e acompanhamento

Cadastre-se pelo provedor e confirme o e-mail. Uma conta ainda sem vínculos/tratamentos de paciente pode escolher **Criar meu perfil profissional** no painel. A mudança é autenticada e auditada; a habilitação permanece não verificada. Uma conta que já é usada como paciente deve usar outra identidade para o ambiente profissional.

O profissional cria convite para o e-mail do paciente e entrega o código por canal autorizado. O paciente autentica no Android, aceita o vínculo e recebe os planos publicados, sem cadastrá-los manualmente. Registros persistem no Room antes de qualquer envio. O painel lê o PostgreSQL e atualiza em até 30 segundos enquanto está visível. A tela Conta do Android permite consultar e revogar compartilhamentos. Não há pacientes semeados no setup normal.

O papel `aurico-operations` é separado, atribuído no provedor apenas a operadores autorizados. Não é concedido pelo cadastro profissional.

Convites pendentes aparecem em Pacientes e podem ser cancelados pelo profissional criador antes da aceitação. Os códigos são de uso único; cancelar um convite não revoga um vínculo já aceito.

## APK paciente conectado

Requisitos Android: JDK 21, SDK 36.1, Build Tools 36.0.0.

```sh
adb reverse tcp:8443 tcp:8443
./gradlew :app:assemblePatientDebug \
  -PauricoApiUrl=https://localhost:8443 \
  -PauricoOidcIssuer=https://localhost:8443/identity/realms/aurico
```

O APK é `app/build/outputs/apk/patient/debug/app-patient-debug.apk`. Localhost só chega ao computador quando o encaminhamento ADB acima está ativo. O app debug confia na CA local apenas para localhost; o navegador externo do login também precisa confiar nessa CA no aparelho de teste. Não existe opção para aceitar certificados inválidos. Para uso remoto, forneça URLs públicas HTTPS reais por esses mesmos parâmetros e use certificados públicos confiáveis.

`patient` mantém o pacote original e `semente.db`; `demo` usa `.demo` e outro banco. `app-demo-debug.apk` é a demonstração acadêmica; `app-patient-debug-androidTest.apk` contém testes e não é o app de uso. Builds sem URLs configuradas continuam oferecendo modo local e não inventam tratamentos.

Cliente Android: público `aurico-android`, Authorization Code/PKCE S256, redirect `br.edu.ifpr.appaurico:/oauth2redirect`, audiência `aurico-api`. Nenhuma senha, chave privada ou token é compilado no APK.

## Testar a integração real

O procedimento do cenário A–E está em [LIVE-INTEGRATION.md](docs/LIVE-INTEGRATION.md). Com o ambiente HTTPS acima em execução:

```sh
node scripts/init-integration-database.mjs
# Execute migrações e backend com backend/.env.integration para isolar os testes.
node --env-file=backend/.env.integration --import tsx backend/src/migrate.ts
node --env-file=.env scripts/prepare-integration.mjs
./gradlew :app:assemblePatientDebug :app:assemblePatientDebugAndroidTest \
  -PauricoApiUrl=https://localhost:8443 \
  -PauricoOidcIssuer=https://localhost:8443/identity/realms/aurico
node scripts/run-android-integration.mjs
```

O runner exige emulador dedicado e usa identidades sintéticas e arquivo privado de fixture, sem autenticação fictícia. O backend deve estar conectado ao banco `aurico_integration` durante esse teste. Nunca use pacientes reais nas fixtures.

Verificações regulares:

```sh
npm run typecheck
npm run lint
npm test
npm run build
npm audit
./gradlew :app:testPatientDebugUnitTest :app:lintPatientDebug
```

## Produção e limites

`backend/.env.production.example` lista as credenciais externas. Domínio/HTTPS público, PostgreSQL gerenciado com TLS, identidade de produção e SMTP precisam ser provisionados e configurados. Nada disso foi presumido nem publicado. A CA local e o Keycloak `start-dev` não são configurações de produção.

Persistem as exigências de homologação física de alarmes/permissões/bateria, revisão independente de segurança, aprovação jurídica/profissional, definição operacional de retenção/exclusão e habilitação do acesso de responsáveis. A proposta está em [POLICY-DRAFT.md](docs/POLICY-DRAFT.md).

Documentação: [API](docs/API.md), [arquitetura](docs/ARCHITECTURE.md), [migração e recuperação](docs/SYNC-AND-MIGRATION.md), [segurança](docs/SECURITY.md), [histórico das verificações](docs/VERIFICATION.md).
