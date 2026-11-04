# AURICO 3.0 — plataforma de acompanhamento

Android offline-first, API REST e painel profissional, desenvolvidos a partir do aplicativo existente. Este incremento é destinado à revisão e homologação; não houve deploy em produção.

## Executar localmente

Requisitos: Node 22.16+, npm, Docker Compose; para Android, JDK 21 e SDK 36.1 / Build Tools 36.0.0.

```sh
npm ci
node scripts/setup-local.mjs
docker compose up -d
npm run migrate -w backend
npm run dev -w backend
# Outro terminal:
npm run dev -w professional-web
```

O setup cria credenciais aleatórias em arquivos ignorados, com permissão 0600, sem imprimir os valores. Recusa sobrescrever configurações existentes. Não executar `docker compose down -v` em bancos que precisem ser preservados.

- Painel: http://127.0.0.1:3000
- Provedor local: http://127.0.0.1:8080 (admin; senha no `.env`, chave `KEYCLOAK_ADMIN_PASSWORD`)
- E-mails de verificação locais: http://127.0.0.1:8025
- API: http://127.0.0.1:4000/api/v1/openapi

Cadastros públicos recebem papel paciente. Uma conta profissional precisa receber o papel `professional` na administração do provedor, antes do primeiro acesso à API. Isso libera o ambiente profissional, mas não marca habilitação como verificada. Não existe senha padrão, conta privilegiada semeada ou login que ignore autenticação.

Convide pelo e-mail do paciente, entregue o código pelo canal autorizado, confirme o e-mail no provedor e aceite o convite na área de compartilhamento. Crie um plano no painel, revise e publique. Nenhum e-mail de convite é enviado automaticamente.

## Android

```sh
./gradlew :app:assemblePatientDebug :app:assembleDemoDebug
./gradlew :app:testPatientDebugUnitTest :app:lintPatientDebug
./gradlew :app:connectedPatientDebugAndroidTest
```

`patient` mantém o pacote original e `semente.db`; `demo` usa `.demo` e outro banco. Sem serviço configurado, o paciente tem onboarding, registros locais e consulta de legados, sem tratamento fictício.

Para integrar com API e identidade em HTTPS:

```sh
./gradlew :app:assemblePatientDebug \
  -PauricoApiUrl=https://api.seu-ambiente.example \
  -PauricoOidcIssuer=https://identidade.seu-ambiente.example/realms/aurico
```

Cliente público: `aurico-android`; redirect `br.edu.ifpr.appaurico:/oauth2redirect`; PKCE S256, audiência `aurico-api`, escopos openid/profile/email/offline_access. Use certificado confiável; a configuração HTTP do Compose é para desenvolvimento web no computador, não uma liberação de HTTP no Android.

## Verificar

```sh
npm run typecheck
npm run lint
npm test
npm run build
npm audit
```

[Auditoria](docs/AUDIT.md), [arquitetura](docs/ARCHITECTURE.md), [API](docs/API.md), [sincronização e recuperação](docs/SYNC-AND-MIGRATION.md), [segurança e pendências](docs/SECURITY.md), [evidências da entrega](docs/VERIFICATION.md).

## Escopo e pendências

Implementados: autorização por vínculo; convites/consentimento/revogação; tratamento com rascunho e publicação versionada; sintomas com escalas; biblioteca de anotações revisadas; sessões e retorno; registros/correções; ocorrências únicas e adesão; sync idempotente com cursor; dashboard; painel responsivo; PDF local; exportação pessoal; app paciente com persistência/fila por conta e migração preservadora.

Adicionados: importação assistida de legados com confirmação e prevenção de repetição; filtros/gráfico Android por período e intervalo de sessão; comparação descritiva entre sessões no painel; solicitações de direitos, revisão cadastral e revisão de representação com fila operacional auditada; login real testado com Keycloak local.

Ainda dependem de homologação: aprovação da política de retenção/exclusão, procedimento efetivo de purge e backups, comprovação profissional na fonte competente, habilitação do acesso de responsáveis e testes de notificações em aparelhos físicos. A proposta inicial está em [POLICY-DRAFT.md](docs/POLICY-DRAFT.md). Consulte o relatório de verificação antes de assumir cobertura ou prontidão para produção.
