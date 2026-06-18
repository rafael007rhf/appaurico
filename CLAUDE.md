# CLAUDE.md — Semente

> Guia do projeto para o Claude Code. Leia este arquivo antes de cada tarefa e siga os padrões aqui descritos. Se uma instrução de um prompt conflitar com este arquivo, peça confirmação antes de prosseguir.

## 1. O que é o app

**Semente** é um aplicativo Android nativo de **apoio ao paciente de auriculoterapia** durante o intervalo entre as sessões.

Na auriculoterapia, o paciente sai da sessão com sementes (esferas) fixadas na orelha e precisa **estimulá-las várias vezes ao dia** até o retorno. A adesão a essa tarefa é frágil e a evolução dos sintomas raramente é registrada. O app resolve isso oferecendo: lembretes de estimulação, registro diário do sintoma (escala 0–10), gráfico de evolução, agenda de retorno e um relatório para o profissional que conduz o tratamento.

**Enquadramento (importante para o contexto do projeto):** isto é uma **inovação de serviço** — melhora um serviço de saúde existente. O app **não substitui** a avaliação do profissional; é ferramenta de apoio. Mantenha esse princípio na copy da interface.

**Contexto acadêmico:** projeto para o VI SeCIF / XIV IFTech (IFPR — Instituto Federal do Paraná, Campus Curitiba). O artefato será **demonstrado ao vivo** numa feira, possivelmente **sem internet**. Portanto: tudo funciona **100% offline e local**. Sem backend, sem nuvem, sem login.

## 2. Stack e decisões fixas

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Arquitetura:** MVVM em camadas (data / domain / ui), padrão recomendado pelo Google
- **Injeção de dependência:** Hilt
- **Persistência:** Room (banco local, sem rede)
- **Assíncrono:** Kotlin Coroutines + Flow
- **Navegação:** Navigation Compose
- **Notificações/lembretes:** AlarmManager + BroadcastReceiver + NotificationManager
- **minSdk:** 26 · **build:** Kotlin DSL (`build.gradle.kts`), version catalog (`libs.versions.toml`)
- **Package base:** `br.edu.ifpr.semente`

Não introduza outras bibliotecas (Retrofit, Firebase, etc.) sem perguntar — o app é offline por princípio.

## 3. Estrutura de pastas (mapa-alvo)

Coloque cada arquivo no diretório correspondente. A pasta nasce junto com o arquivo.

```
br/edu/ifpr/semente/
├─ MainActivity.kt                 // @AndroidEntryPoint, hospeda o NavHost
├─ SementeApp.kt                   // @HiltAndroidApp (Application)
├─ core/notification/
│  ├─ ReminderScheduler.kt         // agenda alarmes
│  └─ ReminderReceiver.kt          // recebe o alarme e emite a notificação
├─ data/
│  ├─ local/
│  │  ├─ SementeDatabase.kt
│  │  ├─ dao/ SymptomLogDao.kt, ReminderDao.kt
│  │  └─ entity/ SymptomLogEntity.kt, ReminderEntity.kt
│  └─ repository/ SymptomRepository.kt, ReminderRepository.kt
├─ domain/model/ SymptomLog.kt, Reminder.kt
├─ ui/
│  ├─ theme/ Color.kt, Theme.kt, Type.kt
│  ├─ navigation/ SementeNavHost.kt, Routes.kt
│  ├─ components/ EarDiagram.kt, SymptomScale.kt, EvolutionChart.kt, SementeBottomBar.kt
│  └─ screens/
│     ├─ onboarding/ OnboardingScreen.kt
│     ├─ home/ HomeScreen.kt, HomeViewModel.kt
│     ├─ reminder/ ReminderScreen.kt
│     ├─ log/ LogScreen.kt, LogViewModel.kt
│     ├─ evolution/ EvolutionScreen.kt, EvolutionViewModel.kt
│     ├─ agenda/ AgendaScreen.kt, AgendaViewModel.kt
│     └─ professional/ ProfessionalScreen.kt, ProfessionalViewModel.kt
└─ di/ AppModule.kt
```

## 4. Padrão de uma tela (sempre o mesmo)

Cada tela com lógica segue: **Screen (Composable) → ViewModel → Repository → DAO (Room)**.

- A `Screen` é stateless: recebe o estado e lambdas de evento; não acessa repositório direto.
- O `ViewModel` expõe um `UiState` via `StateFlow` e trata os eventos.
- O `Repository` é a única porta para os dados; **expõe domain models, nunca entities**.
- Telas sem estado persistente (Onboarding, Reminder) podem dispensar ViewModel.

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
- DAOs expõem leitura como `Flow<List<…>>` (a UI observa e reage).
- Converters do Room para enums/datas quando necessário.
- O mapeamento entity ↔ domain fica no repository.

## 6. Telas (escopo do MVP)

1. **Onboarding** — abertura, propósito, botão "Começar".
2. **Home** — próxima estimulação, CTA "Estimular agora", ciclo atual (dia X de N), adesão, atalho para registrar e link "visão do profissional".
3. **Reminder** — diagrama da orelha com o ponto destacado, instrução, "marcar ponto como feito" (registra a estimulação).
4. **Log** — pergunta o nível de ansiedade (escala 0–10), nota opcional, **salva no Room**.
5. **Evolution** — gráfico de linha **a partir dos registros reais** do banco + resumo (início, hoje, variação).
6. **Agenda** — próximo retorno e configuração dos lembretes (estimulação e retorno).
7. **Professional** — visão de leitura: adesão, tendência do sintoma, registros recentes, botão "compartilhar relatório", aviso de que não substitui avaliação clínica.

**Fora do MVP (não implementar agora):** contas/login, backend, sincronização em nuvem, múltiplos pacientes, o profissional como usuário autenticado. Se um prompt pedir algo assim, avise que está fora do escopo definido.

## 7. Identidade visual

Paleta (defina em `ui/theme/Color.kt`):
- Fundo sage `#EDF1EA` · Verde floresta (primária) `#2E4034` · Mostarda (ação/destaque) `#D98A3D` · Sage (dados/positivo) `#7CA289` · Texto `#1B2620` · Linhas `#E2E8DF`
- Alerta de sintoma alto: `#C2603F`

A cor mostarda remete às **sementes de mostarda** usadas na prática — use-a para CTAs e para o ponto auricular destacado. O **diagrama da orelha** é o elemento de identidade; desenhe-o com `Canvas` em `EarDiagram.kt`.

> "Semente" é um **nome provisório**. Centralize o nome em um único lugar (string resource `app_name`) para troca fácil.

## 8. Copy (texto da interface)

- Português do Brasil, tom calmo e claro, frases curtas.
- Voz ativa nos botões, dizendo o que acontece: "Salvar registro", "Estimular agora", "Marcar como feito".
- O mesmo verbo do começo ao fim de um fluxo (botão "Salvar" → confirmação "Registro salvo").
- Sem jargão técnico voltado ao usuário. Nada de emojis.
- Estados vazios são convite à ação ("Faça seu primeiro registro"), não decoração.

## 9. Qualidade mínima

- Compila ao final de cada tarefa. Se não compilar, conserte antes de encerrar.
- Acessibilidade: `contentDescription` em ícones, alvos de toque ≥ 48dp, respeitar fonte grande.
- Sem credenciais, sem código de rede.
- Comente apenas o não óbvio (ex.: agendamento de alarme); o resto deve se explicar pelo nome.

## 10. Como trabalhar comigo

- Faça **uma etapa por vez** e pare para eu testar antes de seguir.
- Ao terminar uma etapa, diga em uma linha o que mudou e o que devo verificar.
- Em decisão ambígua, pergunte em vez de assumir.
- Não refatore arquivos fora do escopo da tarefa atual sem avisar.