package br.edu.ifpr.appaurico.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import br.edu.ifpr.appaurico.ui.components.AuricoCard
import br.edu.ifpr.appaurico.ui.components.EarDiagram
import br.edu.ifpr.appaurico.ui.components.PontoAuricular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private fun moment(value: Long) = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
private fun jsonItems(data: JSONObject, key: String): List<JSONObject> = data.optJSONArray(key)?.let { a -> (0 until a.length()).map(a::getJSONObject) } ?: emptyList()
private fun syncLabel(state: String) = when (state) { "synced" -> "Sincronizado"; "error" -> "Erro ao sincronizar — preservado neste dispositivo"; "duplicate" -> "Ocorrência já confirmada em outro dispositivo"; else -> "Aguardando sincronização" }

@Composable
fun PatientApp(vm: PatientViewModel = hiltViewModel()) {
    val account by vm.account.collectAsStateWithLifecycle()
    val onboarding by vm.onboarding.collectAsStateWithLifecycle()
    val allTreatments by vm.treatments.collectAsStateWithLifecycle()
    val allEvents by vm.events.collectAsStateWithLifecycle()
    val allOccurrences by vm.occurrences.collectAsStateWithLifecycle()
    val failures by vm.failures.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val treatments = allTreatments.filter { it.accountId == account }
    val events = allEvents.filter { it.accountId == account }
    val occurrences = allOccurrences.filter { it.accountId == account }
    var selectedId by rememberSaveable(account) { mutableStateOf<String?>(null) }
    val treatment = treatments.firstOrNull { it.id == selectedId } ?: treatments.firstOrNull { it.status == "active" } ?: treatments.firstOrNull()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> result.data?.let(vm::finishLogin) }
    val login = { scope.launch { try { launcher.launch(vm.loginIntent()) } catch (e: Exception) { vm.message.value = e.message ?: "Acesso indisponível" } }; Unit }
    if (onboarding == null) { CircularProgressIndicator(Modifier.padding(32.dp)); return }
    if (onboarding == false) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("AURICO", style = MaterialTheme.typography.headlineLarge)
            Text("Seu acompanhamento, entre as sessões.", style = MaterialTheme.typography.headlineSmall)
            Text("Consulte orientações do profissional e registre como se sente. Os registros ficam disponíveis neste dispositivo mesmo sem internet.")
            Text("O compartilhamento exige uma conta e a aceitação de um convite. Registros antigos e locais não são enviados automaticamente.")
            Button(onClick = vm::onboardingDone, modifier = Modifier.fillMaxWidth()) { Text("Continuar neste dispositivo") }
            OutlinedButton(onClick = login, enabled = vm.repository.session.configured, modifier = Modifier.fillMaxWidth()) { Text("Entrar ou criar conta") }
            if (!vm.repository.session.configured) Text("O acesso online será disponibilizado após a configuração do serviço. O modo local já está disponível.")
            if (message.isNotEmpty()) Text(message)
        }
        return
    }
    val nav = rememberNavController()
    val route by nav.currentBackStackEntryAsState()
    val tabs = listOf("home" to "Início", "checkin" to "Registrar", "plan" to "Plano", "history" to "Histórico", "account" to "Conta")
    Scaffold(bottomBar = { NavigationBar { tabs.forEach { (id, label) -> NavigationBarItem(selected = route?.destination?.route == id, onClick = { nav.navigate(id) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Text(label.take(1)) }, label = { Text(label) }) } } }) { padding ->
        Column(Modifier.padding(padding)) {
            if (message.isNotEmpty()) Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            if (failures.isNotEmpty()) Text("${failures.size} registro(s) não aceito(s) pelo servidor. Consulte o histórico; os dados locais foram preservados.", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                composable("home") {
                    PatientColumn {
                        Text("AURICO", style = MaterialTheme.typography.headlineMedium)
                        Text("Olá. Este é o seu acompanhamento.")
                        if (treatments.isEmpty()) AuricoCard {
                            Text("Nenhum tratamento disponível", style = MaterialTheme.typography.titleLarge)
                            Text("Entre com sua conta e aceite um convite. Depois que o profissional publicar seu plano, ele aparecerá aqui.")
                            OutlinedButton(onClick = { nav.navigate("account") }) { Text("Configurar acesso") }
                        }
                        treatments.forEach { t -> OutlinedButton(onClick = { selectedId = t.id }, modifier = Modifier.fillMaxWidth()) { Text("${if (t.id == treatment?.id) "✓ " else ""}${t.title} · ${if (t.status == "finished") "Encerrado" else "Publicado"}") } }
                        treatment?.let { t -> TreatmentSummary(t, occurrences.filter { it.treatmentId == t.id }, events.filter { it.treatmentId == t.id }, busy, vm::stimulate) }
                        if (account == "local") Text("Modo local. Nenhum dado é compartilhado automaticamente.")
                        else OutlinedButton(onClick = vm::sync, enabled = !busy) { Text(if (busy) "Sincronizando…" else "Sincronizar agora") }
                    }
                }
                composable("checkin") { CheckIn(treatment, busy, vm) }
                composable("plan") { Plan(treatment) }
                composable("history") { History(treatment, events, vm) }
                composable("account") { Account(vm, account, busy, login) }
            }
        }
    }
}
@Composable
private fun PatientColumn(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content) }
@Composable
private fun TreatmentSummary(t: StoredTreatment, slots: List<Occurrence>, events: List<PatientEvent>, busy: Boolean, stimulate: (Occurrence) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(t.id) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.of(t.timezone)).toLocalDate()
    val day = ChronoUnit.DAYS.between(LocalDate.parse(t.startsOn), today) + 1
    val duration = ChronoUnit.DAYS.between(LocalDate.parse(t.startsOn), LocalDate.parse(t.endsOn)) + 1
    val due = slots.filter { it.status == "scheduled" && it.scheduledAt <= now }
    val recorded = events.filter { it.kind == "stimulation" && it.syncState != "error" }.map { JSONObject(it.payload).optString("scheduledId") }.toSet()
    val done = due.count { it.id in recorded }
    val next = slots.firstOrNull { it.status == "scheduled" && it.scheduledAt > now }
    val pending = due.lastOrNull { it.id !in recorded }
    val active = t.status == "active" && t.sharingActive && day in 1..duration
    AuricoCard {
        Text(t.title, style = MaterialTheme.typography.titleLarge)
        Text(if (day < 1) "O acompanhamento começa em ${t.startsOn}" else if (!active) "Acompanhamento encerrado, fora do período ou compartilhamento revogado" else "Dia $day de $duration")
        Text("${done} de ${due.size} ocorrências previstas registradas")
        Text(if (due.isEmpty()) "Ainda não há ocorrências para calcular adesão." else "Adesão por ocorrência: ${done * 100 / due.size}%")
        if (active && next != null) Text("Próxima orientação: ${moment(next.scheduledAt)}")
        events.lastOrNull { it.kind == "symptom" }?.let { Text("Último check-in: ${moment(it.occurredAt)} · ${syncLabel(it.syncState)}") }
        val appointments = jsonItems(JSONObject(t.snapshot), "appointments").filter { it.optString("status") == "scheduled" }
        appointments.firstOrNull()?.let { Text("Retorno: ${moment(Instant.parse(it.getString("scheduled_at")).toEpochMilli())}") }
        if (active && pending != null) {
            Text("Confirme apenas uma estimulação que você realizou, seguindo seu plano.")
            Button(onClick = { stimulate(pending) }, enabled = !busy) { Text("Registrar ocorrência de ${moment(pending.scheduledAt)}") }
        }
    }
}
@Composable
private fun CheckIn(t: StoredTreatment?, busy: Boolean, vm: PatientViewModel) {
    var note by rememberSaveable(t?.id) { mutableStateOf("") }
    val levels = remember(t?.id) { mutableStateMapOf<String, Int>() }
    var localLevel by rememberSaveable { mutableStateOf<Int?>(null) }
    PatientColumn {
        Text("Como você se sente?", style = MaterialTheme.typography.headlineSmall)
        if (t == null) {
            Text("Registro local, sem associação a tratamento. Não será enviado automaticamente.")
            br.edu.ifpr.appaurico.ui.components.SymptomScale(localLevel, { localLevel = it })
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("Observação opcional") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { localLevel?.let { vm.localRecord(it, note) }; localLevel = null; note = "" }, enabled = localLevel != null && !busy) { Text("Salvar neste dispositivo") }
        } else {
            Text(t.title)
            jsonItems(JSONObject(t.snapshot), "symptoms").forEach { s ->
                val id = s.getString("id"); val min = s.getInt("min_value"); val max = s.getInt("max_value")
                AuricoCard {
                    Text(s.getString("name"), style = MaterialTheme.typography.titleMedium)
                    Text("Intensidade: ${levels[id]?.toString() ?: "Não selecionada"} · escala $min–$max")
                    Slider(value = (levels[id] ?: min).toFloat(), onValueChange = { levels[id] = it.roundToInt() }, valueRange = min.toFloat()..max.toFloat(), steps = (max - min - 1).coerceAtLeast(0))
                    OutlinedButton(onClick = { levels[id] = min }) { Text("Selecionar $min") }
                    Button(onClick = { levels[id]?.let { vm.record(t.id, id, it, note) } }, enabled = levels.containsKey(id) && !busy && t.status == "active" && t.sharingActive) { Text("Salvar ${s.getString("name")}") }
                }
            }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("Observação opcional para o registro") }, modifier = Modifier.fillMaxWidth())
        }
    }
}
@Composable
private fun Plan(t: StoredTreatment?) {
    PatientColumn {
        Text("Meu plano", style = MaterialTheme.typography.headlineSmall)
        if (t == null) { Text("Seu plano aparecerá após a publicação pelo profissional."); return@PatientColumn }
        val snapshot = JSONObject(t.snapshot); val version = snapshot.optJSONObject("plan"); val content = version?.optJSONObject("content")
        Text(t.title, style = MaterialTheme.typography.titleLarge)
        Text("${t.startsOn} a ${t.endsOn} · ${t.timezone}")
        Text("Versão publicada: ${version?.optInt("version") ?: 0}")
        content?.optJSONArray("instructions")?.let { a -> for (i in 0 until a.length()) AuricoCard { Text(a.getString(i)) } }
        content?.optJSONArray("times")?.let { Text("Horários: ${(0 until it.length()).joinToString(" · ") { i -> it.getString(i) }}") }
        val points = jsonItems(snapshot, "points").map { PontoAuricular(it.getString("label"), Offset(it.getDouble("x").toFloat(), it.getDouble("y").toFloat())) }
        if (points.isNotEmpty()) {
            EarDiagram(indiceDestacado = -1, pontos = points, modifier = Modifier.fillMaxWidth())
            points.forEach { Text(it.nome) }
        } else Text("Nenhuma anotação de ponto revisada foi incluída neste plano.")
        Text("As orientações são definidas pelo profissional. O aplicativo não realiza diagnóstico nem recomenda protocolos.", style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
private fun History(t: StoredTreatment?, all: List<PatientEvent>, vm: PatientViewModel) {
    var symptom by rememberSaveable(t?.id) { mutableStateOf("") }
    var correction by remember { mutableStateOf<PatientEvent?>(null) }
    var correctedLevel by remember { mutableIntStateOf(0) }
    var from by rememberSaveable(t?.id) { mutableStateOf("") }
    var to by rememberSaveable(t?.id) { mutableStateOf("") }
    var sessionId by rememberSaveable(t?.id) { mutableStateOf("") }
    var reviewLegacy by rememberSaveable { mutableStateOf(false) }
    var importId by remember { mutableStateOf<Long?>(null) }
    var confirmed by remember { mutableStateOf(false) }
    val storedLegacy by vm.legacyRecords.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    val legacy = if (account == "local" || reviewLegacy) storedLegacy else emptyList()
    PatientColumn {
        Text("Histórico", style = MaterialTheme.typography.headlineSmall)
        if (t != null) {
            Text(t.title)
            val symptoms = jsonItems(JSONObject(t.snapshot), "symptoms")
            symptoms.forEach { s -> OutlinedButton(onClick = { symptom = s.getString("id") }) { Text(s.getString("name")) } }
            val selected = symptom.ifEmpty { symptoms.firstOrNull()?.getString("id") ?: "" }
            val sessions = jsonItems(JSONObject(t.snapshot), "sessions")
            OutlinedTextField(from,{ from=it.take(10) },label={Text("Desde (AAAA-MM-DD)")},modifier=Modifier.fillMaxWidth(),isError=from.isNotEmpty() && runCatching { LocalDate.parse(from) }.isFailure)
            OutlinedTextField(to,{ to=it.take(10) },label={Text("Até (AAAA-MM-DD)")},modifier=Modifier.fillMaxWidth(),isError=to.isNotEmpty() && runCatching { LocalDate.parse(to) }.isFailure)
            OutlinedButton(onClick={sessionId=""}) {Text("Todas as sessões")}
            sessions.forEach { session -> OutlinedButton(onClick={sessionId=session.getString("id")}) {Text("Sessão: "+moment(Instant.parse(session.getString("occurred_at")).toEpochMilli()))} }
            val sessionStart=sessions.firstOrNull { it.getString("id")==sessionId }?.getString("occurred_at")?.let(Instant::parse)
            val sessionEnd=sessionStart?.let { start -> sessions.map { Instant.parse(it.getString("occurred_at")) }.firstOrNull { it>start } }
            val matching = all.filter { it.treatmentId == t.id && it.kind == "symptom" && JSONObject(it.payload).optString("symptomId") == selected }
            val replacedAll=matching.map { JSONObject(it.payload).optString("correctionOf") }.toSet()
            val events=matching.filter { e ->
                val day=Instant.ofEpochMilli(e.occurredAt).atZone(ZoneId.of(t.timezone)).toLocalDate().toString()
                (from.isEmpty() || day>=from) && (to.isEmpty() || day<=to) && (sessionStart==null || Instant.ofEpochMilli(e.occurredAt)>=sessionStart) && (sessionEnd==null || Instant.ofEpochMilli(e.occurredAt)<sessionEnd)
            }
            val replaced = replacedAll
            val current = events.filter { it.id !in replaced && it.syncState!="duplicate" }
            val scale=symptoms.firstOrNull { it.getString("id")==selected }
            if(scale!=null) HistoryChart(current,scale.getInt("min_value"),scale.getInt("max_value"))
            if (current.size < 2) Text("Dados insuficientes para comparar a evolução.")
            else Text("Primeiro: ${JSONObject(current.first().payload).getInt("level")} · Último: ${JSONObject(current.last().payload).getInt("level")}. Valores informados por você, sem interpretação clínica automática.")
            events.asReversed().forEach { e ->
                val data = JSONObject(e.payload)
                AuricoCard {
                    Text("${moment(e.occurredAt)} · Intensidade ${data.getInt("level")}")
                    Text(syncLabel(e.syncState))
                    if (data.optString("note").isNotBlank()) Text(data.optString("note"))
                    if (e.id in replaced) Text("Registro corrigido; original preservado.")
                    else if (t.status == "active" && t.sharingActive) OutlinedButton(onClick = { correction = e; correctedLevel = data.getInt("level") }) { Text("Corrigir registro") }
                }
            }
        }
        if(account!="local" && t!=null) OutlinedButton(onClick={reviewLegacy=!reviewLegacy}) {Text(if(reviewLegacy) "Fechar revisão de legados" else "Revisar legados deste dispositivo")}
        if (legacy.isNotEmpty()) {
            Text("Registros locais e legados", style = MaterialTheme.typography.titleLarge)
            Text("Sem associação confirmada a um paciente ou tratamento remoto. Estes registros não são sincronizados.")
            legacy.asReversed().forEach { record ->
                Text("${moment(record.dataHora)} · Nível ${record.nivel} · ${record.nota ?: ""}")
                if(account!="local" && t!=null) OutlinedButton(onClick={importId=record.id;confirmed=false}) {Text("Revisar associação deste registro")}
            }
        }
        if (all.isEmpty() && legacy.isEmpty()) Text("Seus registros aparecerão aqui depois do primeiro check-in.")
    }
    importId?.let { id ->
        val original=storedLegacy.firstOrNull { it.id==id }
        val symptoms=t?.let { jsonItems(JSONObject(it.snapshot),"symptoms") } ?: emptyList()
        val target=symptoms.firstOrNull { it.getString("id")==symptom } ?: symptoms.firstOrNull()
        if(original!=null && t!=null && target!=null) AlertDialog(onDismissRequest={importId=null},title={Text("Confirmar associação de legado")},text={Column {
            Text("Conta: $account\nTratamento: ${t.title}\nSintoma: ${target.getString("name")}\nData original: ${moment(original.dataHora)}\nIntensidade: ${original.nivel}\nObservação: ${original.nota ?: ""}")
            Text("Uma cópia será enviada ao profissional deste tratamento. O original permanece neste dispositivo.")
            Row {Checkbox(confirmed,{confirmed=it});Text("Confirmo que este registro pertence a esta conta e a este tratamento.")}
        }},confirmButton={TextButton(enabled=confirmed,onClick={vm.importLegacy(id,t.id,target.getString("id"));importId=null}) {Text("Associar e colocar na fila")}},dismissButton={TextButton(onClick={importId=null}) {Text("Cancelar")}})
    }
    correction?.let { event ->
        val data = JSONObject(event.payload)
        val s = t?.let { jsonItems(JSONObject(it.snapshot), "symptoms").firstOrNull { it.getString("id") == data.optString("symptomId") } }
        if (s != null) AlertDialog(onDismissRequest = { correction = null }, title = { Text("Corrigir intensidade") }, text = {
            Column { Text("Novo valor: $correctedLevel. O registro anterior será preservado."); Slider(correctedLevel.toFloat(), { correctedLevel = it.roundToInt() }, valueRange = s.getInt("min_value").toFloat()..s.getInt("max_value").toFloat()) }
        }, confirmButton = { TextButton(onClick = { vm.record(event.treatmentId, data.getString("symptomId"), correctedLevel, data.optString("note"), event.id); correction = null }) { Text("Salvar correção") } }, dismissButton = { TextButton(onClick = { correction = null }) { Text("Cancelar") } })
    }
}
@Composable
private fun Account(vm: PatientViewModel, account: String, busy: Boolean, login: () -> Unit) {
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    var token by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    val links by vm.sharingLinks.collectAsStateWithLifecycle()
    var revoke by remember { mutableStateOf<String?>(null) }
    PatientColumn {
        Text("Conta e privacidade", style = MaterialTheme.typography.headlineSmall)
        Text(if (account == "local") "Modo local" else "Conta de paciente conectada")
        if (account == "local") Button(onClick = login, enabled = vm.repository.session.configured && !busy) { Text("Entrar ou criar conta") }
        else {
            Button(onClick = vm::sync, enabled = !busy) { Text("Sincronizar agora") }
            OutlinedButton(onClick=login,enabled=!busy) {Text("Entrar novamente ou trocar conta")}
            OutlinedTextField(token, { token = it.take(100) }, label = { Text("Código do convite") }, modifier = Modifier.fillMaxWidth())
            Text("Aceitar permite ao profissional acompanhar os registros dos tratamentos sob sua responsabilidade. A revogação está disponível no painel de compartilhamento da sua conta.")
            Row { Checkbox(consent, { consent = it }); Text("Autorizo este compartilhamento (versão 2026-11-03).", Modifier.padding(top = 12.dp)) }
            Button(onClick = { vm.accept(token, consent) }, enabled = consent && token.length >= 40 && !busy) { Text("Aceitar convite") }
            OutlinedButton(onClick=vm::loadSharing,enabled=!busy) {Text("Consultar compartilhamentos")}
            links.forEach { link -> Text(link.getString("display_name")+" · "+link.getString("status"));if(link.getString("status")=="active") OutlinedButton(onClick={revoke=link.getString("id")}) {Text("Revogar compartilhamento")} }
            OutlinedButton(onClick = vm::logout, enabled = !busy) { Text("Sair desta conta") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Switch(reminders, vm::setReminders); Text("Lembretes do plano publicado", Modifier.padding(top = 12.dp)) }
        Text("Os horários seguem o plano profissional. O Android pode atrasar os avisos para economizar bateria. Nenhuma orientação clínica aparece na tela bloqueada.")
        revoke?.let { id -> AlertDialog(onDismissRequest={revoke=null},title={Text("Revogar compartilhamento?")},text={Text("O profissional perderá acesso aos seus acompanhamentos. Seu histórico será preservado.")},confirmButton={TextButton(onClick={vm.revokeSharing(id);revoke=null}) {Text("Revogar")}},dismissButton={TextButton(onClick={revoke=null}) {Text("Cancelar")}}) }
        Text("O armazenamento local é protegido pelo dispositivo. Use bloqueio de tela. Dados antigos não são atribuídos automaticamente à conta conectada.")
    }
}

@Composable
private fun HistoryChart(events: List<PatientEvent>, min: Int, max: Int) {
    if(events.size<2) return
    val color=MaterialTheme.colorScheme.primary
    val first=events.first().occurredAt;val last=events.last().occurredAt
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(200.dp).semantics {contentDescription="Gráfico de intensidade, escala $min a $max. Valores detalhados no histórico."}) {
        fun point(e: PatientEvent): Offset = Offset(8.dp.toPx()+((e.occurredAt-first).toFloat()/(last-first).coerceAtLeast(1))*(size.width-16.dp.toPx()),8.dp.toPx()+(1f-(JSONObject(e.payload).getInt("level")-min).toFloat()/(max-min))*(size.height-16.dp.toPx()))
        val path=androidx.compose.ui.graphics.Path()
        events.forEachIndexed { i,e -> val p=point(e); if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y) }
        drawPath(path,color,style=androidx.compose.ui.graphics.drawscope.Stroke(width=3.dp.toPx()))
        events.forEach {drawCircle(color,4.dp.toPx(),point(it))}
    }
    Text("Escala: $min–$max · "+moment(first)+" a "+moment(last),style=MaterialTheme.typography.bodySmall)
}
