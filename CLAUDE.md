# CLAUDE.md — Aurico

> Guia do projeto. Leia este arquivo antes de cada tarefa e siga os padrões aqui descritos. Se uma instrução de um prompt conflitar com este arquivo, peça confirmação antes de prosseguir.

## 1. O que é o app

**Aurico** é um aplicativo Android nativo de **apoio ao paciente de auriculoterapia** durante o intervalo entre as sessões.

Na auriculoterapia, o paciente sai da sessão com sementes (esferas) fixadas na orelha e precisa **estimulá-las várias vezes ao dia** até o retorno. A adesão a essa tarefa é frágil e a evolução dos sintomas raramente é registrada. O app apoia esse acompanhamento oferecendo lembretes de estimulação, registro diário do sintoma (escala 0–10), gráfico de evolução, agenda de retorno e um relatório para o profissional que conduz o tratamento.

**Enquadramento:** isto é uma **inovação de serviço** — melhora um serviço de saúde existente. O app **não substitui** a avaliação do profissional; é uma ferramenta de apoio. Mantenha esse princípio na copy da interface.

**Contexto acadêmico:** projeto para o VI SeCIF / XIV IFTech (IFPR — Instituto Federal do Paraná, Campus Curitiba). O artefato será **demonstrado ao vivo** numa feira, possivelmente **sem internet**. Portanto: tudo funciona **100% offline e local**. Sem backend, sem nuvem, sem login.

## 2. Stack e decisões fixas

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Arquitetura:** MVVM em camadas (data / domain / ui)
- **Injeção de dependência:** Hilt
- **Persistência:** Room (banco local, sem rede)
- **Assíncrono:** Kotlin Coroutines + Flow
- **Navegação:** Navigation Compose
- **Notificações/lembretes:** AlarmManager + BroadcastReceiver + NotificationManager
- **minSdk:** 26 · **build:** Kotlin DSL (`build.gradle.kts`), version catalog (`libs.versions.toml`)
- **Package base:** `br.edu.ifpr.appaurico`

Não introduza bibliotecas de rede ou backend (Retrofit, Firebase etc.) sem uma decisão explícita — o app é offline por princípio.

## 3. Estrutura de pastas

```text
br/edu/ifpr/appaurico/
├─ MainActivity.kt
├─ AuricoApp.kt
├─ core/
│  ├─ cycle/ Ciclo.kt
│  └─ notification/
│     ├─ BootReceiver.kt
│     ├─ Notificacoes.kt
│     ├─ ReminderScheduler.kt
│     └─ ReminderReceiver.kt
├─ data/
│  ├─ local/
│  │  ├─ AuricoDatabase.kt
│  │  ├─ dao/ SymptomLogDao.kt, ReminderDao.kt, StimulationDao.kt
│  │  └─ entity/ SymptomLogEntity.kt, ReminderEntity.kt, StimulationEntity.kt
│  └─ repository/ SymptomRepository.kt, ReminderRepository.kt, StimulationRepository.kt
├─ domain/model/ SymptomLog.kt, Reminder.kt, ReminderType.kt, Stimulation.kt
├─ ui/
│  ├─ theme/ Color.kt, Theme.kt, Type.kt, Dimens.kt
│  ├─ navigation/ AuricoNavHost.kt, Routes.kt
│  ├─ components/ EarDiagram.kt, SymptomScale.kt, EvolutionChart.kt, AuricoBottomBar.kt, AuricoCard.kt
│  └─ screens/
│     ├─ onboarding/ OnboardingScreen.kt
│     ├─ home/ HomeScreen.kt, HomeViewModel.kt
│     ├─ reminder/ ReminderScreen.kt, ReminderViewModel.kt
│     ├─ log/ LogScreen.kt, LogViewModel.kt
│     ├─ evolution/ EvolutionScreen.kt, EvolutionViewModel.kt
│     ├─ agenda/ AgendaScreen.kt, AgendaViewModel.kt
│     ├─ professional/ ProfessionalScreen.kt, ProfessionalViewModel.kt
│     └─ settings/ SettingsScreen.kt
└─ di/ AppModule.kt
```

## 4. Padrão de uma tela

Cada tela com lógica segue: **Screen (Composable) → ViewModel → Repository → DAO (Room)**.

- A `Screen` recebe estado e lambdas de evento; não acessa repositório diretamente.
- O `ViewModel` expõe um `UiState` via `StateFlow` e trata os eventos.
- O `Repository` é a única porta para os dados e expõe domain models, nunca entities.
- Telas sem estado persistente podem dispensar ViewModel.

Exemplo de contrato de estado:

```kotlin
data class LogUiState(
    val nivelSelecionado: Int? = null,
    val nota: String = "",
    val salvando: Boolean = false,
    val salvo: Boolean = false,
)
```

## 5. Camada de dados

- `SymptomLogEntity`: `id`, `dataHora: Long`, `nivel: Int` (0..10), `nota: String?`
- `ReminderEntity`: `id`, `tipo: ReminderType` (ESTIMULACAO, RETORNO), `horario`, `ativo: Boolean`
- `StimulationEntity`: registra as estimulações realizadas para cálculo de adesão.
- DAOs expõem leitura como `Flow<List<…>>` quando a UI precisa observar mudanças.
- O mapeamento entity ↔ domain fica no repository.
- Os dados de acompanhamento permanecem locais e são excluídos de backup e transferência.

## 6. Telas e escopo do MVP

1. **Onboarding** — abertura, propósito, botão "Começar".
2. **Home** — próxima estimulação, CTA "Estimular agora", ciclo atual, adesão, atalho para registrar e link para a visão do profissional.
3. **Reminder** — diagrama da orelha com o ponto destacado, instrução e ação para marcar a estimulação como feita.
4. **Log** — pergunta o nível do sintoma em escala 0–10, nota opcional e salva no Room.
5. **Evolution** — gráfico de linha a partir dos registros reais do banco + resumo de início, hoje e variação.
6. **Agenda** — próximo retorno e ativação/desativação dos lembretes de estimulação e retorno.
7. **Professional** — visão de leitura com adesão, tendência do sintoma, registros recentes, compartilhamento de relatório e aviso de que não substitui avaliação clínica.
8. **Settings** — ajustes disponíveis no protótipo.

O ciclo atual é **demonstrativo**: sete dias, horários predefinidos e retorno calculado localmente. Não apresentar o MVP como uma agenda clínica totalmente configurável.

**Fora do MVP:** contas/login, backend, sincronização em nuvem, múltiplos pacientes e profissional como usuário autenticado.

## 7. Identidade visual

Paleta em `ui/theme/Color.kt`:
- Fundo sage `#EDF1EA`
- Verde floresta (primária) `#2E4034`
- Mostarda (ação/destaque) `#D98A3D`
- Sage (dados/positivo) `#7CA289`
- Texto `#1B2620`
- Linhas `#E2E8DF`
- Alerta de sintoma alto `#C2603F`

A cor mostarda remete às **sementes de mostarda** usadas na prática. O diagrama da orelha é um dos elementos centrais da identidade.

O nome oficial deste protótipo é **Aurico** e deve permanecer centralizado em `@string/app_name` para consistência.

## 8. Copy

- Português do Brasil, tom calmo e claro, frases curtas.
- Voz ativa nos botões: "Salvar registro", "Estimular agora", "Marcar como feito".
- Usar o mesmo verbo do começo ao fim de um fluxo.
- Sem jargão técnico voltado ao usuário.
- Estados vazios devem convidar à ação.
- Não prometer diagnóstico, tratamento ou substituição do profissional.

## 9. Notificações e privacidade

- Android 13+ exige `POST_NOTIFICATIONS` em runtime.
- Os lembretes usam `AlarmManager`; quando alarme exato não está disponível, há fallback para alarme inexato.
- `BootReceiver` restaura os lembretes ativos após reinicialização do dispositivo.
- O app não usa rede, backend ou nuvem.
- Backup e transferência dos dados locais devem permanecer desativados/excluídos.

## 10. Qualidade mínima

- Compilar ao final de cada tarefa e corrigir erros antes de encerrar.
- Acessibilidade: `contentDescription` quando aplicável, alvos de toque adequados e suporte a fonte grande.
- Sem credenciais e sem código de rede.
- Comentar apenas o não óbvio.
- Não refatorar arquivos fora do escopo sem necessidade.
