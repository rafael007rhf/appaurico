# Verificação da entrega — 4 de novembro de 2026

## Evidências executadas no ambiente local

| Verificação | Resultado |
|---|---|
| TypeScript backend e Next.js | `npm run typecheck` passou |
| ESLint backend, testes e painel | `npm run lint` passou |
| API + PostgreSQL embarcado | 15 testes passaram, zero falhas |
| Build backend e painel | `npm run build` passou; Next.js gerou a rota `/` |
| Dependências npm | `npm audit`: zero vulnerabilidades conhecidas, incluindo ferramentas de desenvolvimento |
| Paciente Android | `assemblePatientDebug` passou e gerou APK |
| Demonstração Android | `assembleDemoDebug` passou e gerou APK separado |
| Lint Android paciente | Passou: zero erros e 42 avisos, incluindo versões/deprecações e questões herdadas de UI |
| Testes JVM Android | Três testes passaram: exemplo herdado, migração Room 2→3 e importação de legado com confirmação, deduplicação e preservação do original |
| Migração Room real sob Robolectric API 35 | Preservou IDs, intensidade, observação, estimulação e lembrete de um banco v2; nenhuma fila/associação remota criada |
| APK dos testes instrumentados | Compilado; não executado em emulador ou aparelho |
| Prévia web | HTTP 200; tela inicial observada no navegador, com estado de serviço indisponível sem API/identidade em execução |
| Setup local | Geração de configurações/credenciais em arquivos ignorados; sem valores impressos |

Os testes de API usam PostgreSQL via PGlite e JWTs RS256 assinados por chave efêmera de teste. Não há usuário ou paciente fictício no setup de aplicação. Foram exercitados: token ausente/forjado/audiência inválida; convite/e-mail/consentimento/uso único; acesso indevido por outro paciente e profissional; rascunho privado; publicação e controle de versão; escala inválida; retry/idempotência; correções; confirmação em múltiplos dispositivos; paginação do cursor; PDF gerado no servidor; notas profissionais restritas; CSRF; revogação; DST e denominador vazio.

## Limites que permanecem

- Docker não estava instalado; Compose não foi executado. O login real foi validado usando a distribuição oficial do Keycloak local, como descrito abaixo.
- Android foi compilado sem URLs externas configuradas. O APK entregue abre em modo local; integração remota exige recompilar com os parâmetros HTTPS do README.
- Não foram executados testes em aparelhos físicos: permissões, reinicialização, atraso de alarmes, restrições de bateria e acessibilidade ainda precisam de homologação.
- O teste de migração JVM passou; a variante instrumentada equivalente foi compilada, mas requer aparelho/emulador.
- A validação de API não substitui testes concorrentes de carga com PostgreSQL externo nem revisão independente de segurança.
- Não houve deploy, merge em `main`, uso de pacientes reais ou envio de convites/e-mails a pessoas.

## Pendências funcionais explícitas

A execução efetiva de exclusão/retenção, habilitação do acesso de responsáveis e revisão jurídica/profissional da política permanecem pendentes. Importação assistida, gráficos/filtros e comparação por sessões e os protocolos de governança estão implementados. A base da plataforma está implementada, mas não é correto considerar concluídos todos os requisitos da missão nem autorizar uso clínico real com base somente nestes testes.

## Incremento de pendências

O login foi exercitado contra Keycloak 26.8.1 local real (sem Docker): discovery, Authorization Code com PKCE, state/nonce, assinatura por JWKS, sessão e logout passaram. Foi corrigida a composição do papel padrão no realm. O teste reproduzível está em `backend/tests/oidc.integration.ts` e exige os arquivos `.env` locais; usa identidade sintética descartável e banco de teste efêmero. O resultado anterior do CI teve TypeScript aprovado e Android interrompido na instalação do pacote removido `tools`; o workflow agora solicita explicitamente `platform-tools`.

Importação assistida, filtros/gráfico Android e comparação por sessão foram implementados. A migração PostgreSQL 0002 acrescenta solicitações de governança; acesso operacional exige papel `aurico-operations` atribuído no provedor. Exclusão e representação não podem ser aprovadas como ações automáticas por mudança de status. A política é uma proposta sem declaração de conformidade. Os limites da seção inicial devem ser lidos com estas atualizações.
