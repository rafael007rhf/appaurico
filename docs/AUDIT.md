# Auditoria inicial — 3 de novembro de 2026

Base: `rafael007rhf/appaurico`, `main` em `eb43000c5cf66c8a2adb8372c2dc8520f6d655ba`. A branch de trabalho é `feature/aurico-3-platform`.

A inspeção abrangeu a árvore do repositório, configurações Gradle, manifesto, recursos, código Kotlin de produção/debug/release, entidades, DAOs, repositórios, ViewModels, componentes Compose e testes. Não havia backend, painel web, CI de plataforma ou testes de domínio; apenas testes de exemplo Android/JUnit.

| Área | Evidência na base | Decisão |
|---|---|---|
| Room | `AuricoDatabase` v2, três tabelas, `exportSchema=false` | v3 aditiva, exportação do esquema, teste com banco v2 contendo registros |
| Migração | `AppModule` chamava `fallbackToDestructiveMigration(dropAllTables=true)` | Removido; esquemas desconhecidos falham sem apagar dados |
| Ciclo | `Ciclo`: sete dias, quatro horários, início relativo que mantém dia 5 | Retido no fluxo demo; produção usa planos publicados persistidos |
| Adesão | Contagem de todos os registros dividida por horários demonstrativos | Ocorrências estáveis, unicidade no servidor e conjunto de IDs confirmados |
| Profissional | `ProfessionalViewModel` usava “Maria (exemplo)” e leitura local | UI antiga acessível apenas no app demo; painel separado com autorização |
| Notificações | Horários fixos, instrução de 30 segundos, alarmes exatos | Conteúdo neutro, verificação de plano/conta e alarmes inexatos |
| Onboarding | Sempre `Routes.ONBOARDING` | Preferência persistente no app paciente |
| Demonstração | Seeder de debug podia limpar os mesmos DAOs usados pelo app | Flavor/package `.demo`, banco `aurico-demo.db`; paciente mantém `semente.db` |
| Dados antigos | Sem paciente, tratamento, sintoma ou ocorrência identificável | Mantidos como registros locais/legados, sem envio automático |
| Segurança | Backup e transferência já desativados | Mantidos; credenciais novas no Android Keystore, conteúdo sem analytics |
| Instruções | `CLAUDE.md` proibia backend/autenticação | Substituído pelas regras da plataforma, preservando offline-first |

## Comparação de branches

- `aurico-iftech-final`: arquivos idênticos à `main` na auditoria. Não foi feito merge redundante.
- `aurico-ui-v2`: nove arquivos diferentes, principalmente composição de telas, navegação, slider e timer. A barra de navegação com rótulos e indicador de seleção foi aproveitada. Não foram importados o cronômetro clínico fixo, dados ilustrativos ou dependências de `Ciclo` para a produção.
- As telas antigas continuam no repositório para a demonstração; a entrada `MainActivity` escolhe explicitamente o ambiente pelo flavor.

## Ambiente de execução

O host inicialmente não possuía Git funcional, Node, JDK, SDK Android ou Docker. Node 22, JDK 21 e SDK Android foram preparados isoladamente fora do repositório. O checkout preservou a história Git usando isomorphic-git. O banco dos testes de API é PostgreSQL embarcado via PGlite, não uma imitação dos repositórios. O estado final das verificações está em `VERIFICATION.md`.
