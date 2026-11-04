# Verificação da entrega — 4 de novembro de 2026

## Evidências executadas no ambiente local

| Verificação | Resultado |
|---|---|
| TypeScript backend e Next.js | `npm run typecheck` passou |
| ESLint backend, testes e painel | `npm run lint` passou |
| API + PostgreSQL embarcado | 12 testes passaram, zero falhas |
| Build backend e painel | `npm run build` passou; Next.js gerou a rota `/` |
| Dependências npm | `npm audit`: zero vulnerabilidades conhecidas, incluindo ferramentas de desenvolvimento |
| Paciente Android | `assemblePatientDebug` passou e gerou APK |
| Demonstração Android | `assembleDemoDebug` passou e gerou APK separado |
| Lint Android paciente | Passou: zero erros e 42 avisos, incluindo versões/deprecações e questões herdadas de UI |
| Testes JVM Android | Dois testes passaram: exemplo herdado e migração Room 2→3 |
| Migração Room real sob Robolectric API 35 | Preservou IDs, intensidade, observação, estimulação e lembrete de um banco v2; nenhuma fila/associação remota criada |
| APK dos testes instrumentados | Compilado; não executado em emulador ou aparelho |
| Prévia web | HTTP 200; tela inicial observada no navegador, com estado de serviço indisponível sem API/identidade em execução |
| Setup local | Geração de configurações/credenciais em arquivos ignorados; sem valores impressos |

Os testes de API usam PostgreSQL via PGlite e JWTs RS256 assinados por chave efêmera de teste. Não há usuário ou paciente fictício no setup de aplicação. Foram exercitados: token ausente/forjado/audiência inválida; convite/e-mail/consentimento/uso único; acesso indevido por outro paciente e profissional; rascunho privado; publicação e controle de versão; escala inválida; retry/idempotência; correções; confirmação em múltiplos dispositivos; paginação do cursor; PDF gerado no servidor; notas profissionais restritas; CSRF; revogação; DST e denominador vazio.

## Limites que permanecem

- Docker não estava instalado; o Compose e o login real com Keycloak não foram executados neste computador. O código de integração existe, mas não há evidência de login ponta a ponta com o provedor real.
- Android foi compilado sem URLs externas configuradas. O APK entregue abre em modo local; integração remota exige recompilar com os parâmetros HTTPS do README.
- Não foram executados testes em aparelhos físicos: permissões, reinicialização, atraso de alarmes, restrições de bateria e acessibilidade ainda precisam de homologação.
- O teste de migração JVM passou; a variante instrumentada equivalente foi compilada, mas requer aparelho/emulador.
- A validação de API não substitui testes concorrentes de carga com PostgreSQL externo nem revisão independente de segurança.
- Não houve deploy, merge em `main`, uso de pacientes reais ou envio de convites/e-mails a pessoas.

## Pendências funcionais explícitas

Importação assistida dos legados, gráficos/filtros Android avançados, comparação entre sessões, fluxo operacional de exclusão/retenção, responsáveis/menores e verificação profissional permanecem pendentes. A base da plataforma está implementada, mas não é correto considerar concluídos todos os requisitos da missão nem autorizar uso clínico real com base somente nestes testes.
