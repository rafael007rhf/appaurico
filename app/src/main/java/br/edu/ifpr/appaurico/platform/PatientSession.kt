package br.edu.ifpr.appaurico.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import br.edu.ifpr.appaurico.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.openid.appauth.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val Context.patientPreferences by preferencesDataStore("aurico_patient_preferences")
@Singleton
class PatientSession @Inject constructor(@ApplicationContext private val context: Context) {
    val mutex = Mutex()
    private val accountKey = stringPreferencesKey("account")
    private val onboardKey = booleanPreferencesKey("onboarding")
    private val reminderKey = booleanPreferencesKey("reminders")
    private val preferences = context.patientPreferences
    val account = preferences.data.map { it[accountKey] ?: "local" }
    val onboarding = preferences.data.map { it[onboardKey] ?: false }
    val reminders = preferences.data.map { it[reminderKey] ?: false }
    private val file = AtomicFile(File(context.noBackupFilesDir, "identity.v1"))
    private val service by lazy { AuthorizationService(context) }
    private val http = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).followRedirects(false).build()
    val configured get() = BuildConfig.API_URL.startsWith("https://") && BuildConfig.OIDC_ISSUER.startsWith("https://")
    suspend fun completeOnboarding() { preferences.edit { it[onboardKey] = true } }
    suspend fun setReminders(enabled: Boolean) { preferences.edit { it[reminderKey] = enabled } }
    suspend fun deviceId(): String {
        val key = stringPreferencesKey("device_id")
        var result = ""
        preferences.edit { values -> result = values[key] ?: UUID.randomUUID().toString(); values[key] = result }
        return result
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("aurico_identity_v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("aurico_identity_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun write(accountId: String, state: AuthState) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val data = JSONObject().put("accountId", accountId).put("auth", state.jsonSerializeString()).toString().toByteArray()
        val encrypted = cipher.doFinal(data)
        val out = file.startWrite()
        try { out.write(cipher.iv.size); out.write(cipher.iv); out.write(encrypted); file.finishWrite(out) }
        catch (e: Exception) { file.failWrite(out); throw e }
    }
    private fun read(): JSONObject? {
        if (!file.baseFile.exists()) return null
        val bytes = file.readFully(); val length = bytes[0].toInt()
        require(length in 12..16 && bytes.size > length + 1)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 1 + length))) }
        return JSONObject(String(cipher.doFinal(bytes.copyOfRange(1 + length, bytes.size))))
    }
    suspend fun loginIntent(): Intent {
        check(configured) { "O acesso online ainda não está configurado nesta versão." }
        val config = suspendCancellableCoroutine<AuthorizationServiceConfiguration> { continuation ->
            AuthorizationServiceConfiguration.fetchFromIssuer(Uri.parse(BuildConfig.OIDC_ISSUER)) { value, error ->
                if (continuation.isActive) { if (value != null) continuation.resume(value) else continuation.resumeWithException(error ?: IllegalStateException("Identidade indisponível")) }
            }
        }
        val request = AuthorizationRequest.Builder(config, BuildConfig.OIDC_CLIENT_ID, ResponseTypeValues.CODE, Uri.parse("br.edu.ifpr.appaurico:/oauth2redirect"))
            .setScopes("openid", "profile", "email", "offline_access").build()
        return service.getAuthorizationRequestIntent(request)
    }
    suspend fun finishLogin(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent) ?: throw IllegalStateException("Acesso cancelado ou inválido.")
        val state = AuthState(response, AuthorizationException.fromIntent(intent))
        val tokens = suspendCancellableCoroutine<TokenResponse> { continuation ->
            service.performTokenRequest(response.createTokenExchangeRequest()) { token, error ->
                if (continuation.isActive) { if (token != null) continuation.resume(token) else continuation.resumeWithException(error ?: IllegalStateException("Falha no acesso")) }
            }
        }
        state.update(tokens, null)
        val me = request("/me", "GET", null, tokens.accessToken ?: error("Sem credencial"))
        require(me.getString("role") == "patient") { "Use uma conta de paciente neste aplicativo." }
        val id = me.getString("id")
        withContext(Dispatchers.IO) { write(id, state) }
        preferences.edit { it[accountKey] = id; it[onboardKey] = true }
    }
    suspend fun logout() { withContext(Dispatchers.IO) { file.delete() }; preferences.edit { it[accountKey] = "local"; it[reminderKey] = false } }
    suspend fun requestFor(accountId: String, path: String, method: String = "GET", body: JSONObject? = null): JSONObject {
        val stored = withContext(Dispatchers.IO) { read() } ?: error("Entre novamente para sincronizar.")
        check(stored.getString("accountId") == accountId) { "A conta foi alterada." }
        val state = AuthState.jsonDeserialize(stored.getString("auth"))
        val token = suspendCancellableCoroutine<String> { continuation ->
            state.performActionWithFreshTokens(service) { access, _, error ->
                if (continuation.isActive) { if (access != null) continuation.resume(access) else continuation.resumeWithException(error ?: IllegalStateException("Entre novamente")) }
            }
        }
        withContext(Dispatchers.IO) { write(accountId, state) }
        return request(path, method, body, token)
    }
    private suspend fun request(path: String, method: String, body: JSONObject?, token: String): JSONObject = withContext(Dispatchers.IO) {
        check(configured)
        val url = BuildConfig.API_URL.trimEnd('/') + "/api/v1" + path
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token")
            .method(method, if (method == "GET") null else (body?.toString() ?: "{}").toRequestBody("application/json".toMediaType())).build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string() ?: "{}"
            if (!response.isSuccessful) throw ApiFailure(response.code)
            JSONObject(text)
        }
    }
}
class ApiFailure(val status: Int) : Exception("Não foi possível sincronizar (HTTP $status). Seus registros continuam neste dispositivo.")
