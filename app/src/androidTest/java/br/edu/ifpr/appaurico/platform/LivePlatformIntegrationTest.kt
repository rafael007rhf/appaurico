package br.edu.ifpr.appaurico.platform

import android.net.Uri
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import br.edu.ifpr.appaurico.MainActivity
import br.edu.ifpr.appaurico.BuildConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthorizationResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Runs only against dedicated local TLS services and synthetic fixture identities. No login bypass. */
class LivePlatformIntegrationTest {
    @Test fun authenticatedAndroidRoomOfflineSyncAndProfessionalPanel() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveIntegration")=="true")
        check(BuildConfig.API_URL=="https://localhost:8443")
        val context=instrumentation.targetContext
        val fixture=JSONObject(File(context.filesDir,"integration.json").readText())
        val repository=EntryPointAccessors.fromApplication(context,PatientWorkerDependencies::class.java).repository()
        val session=repository.session
        val http=OkHttpClient.Builder().followRedirects(false).callTimeout(30,TimeUnit.SECONDS).build()
        val proCookie=fixture.getString("professionalCookie")
        fun api(path:String,method:String="GET",body:JSONObject?=null,cookie:String=proCookie,expected:Int=200):JSONObject {
            val req=Request.Builder().url(BuildConfig.API_URL+"/api/v1"+path).header("Cookie",cookie).header("Origin",BuildConfig.API_URL)
                .method(method,if(method=="GET")null else (body?.toString()?:"{}").toRequestBody("application/json".toMediaType())).build()
            return http.newCall(req).execute().use { response -> assertEquals("API $path",expected,response.code);JSONObject(response.body?.string()?:"{}") }
        }
        session.logout()
        val request=session.authorizationRequest()
        val login=http.newCall(Request.Builder().url(request.toUri().toString()).build()).execute()
        val cookies=login.headers.values("Set-Cookie").joinToString("; "){it.substringBefore(';')}
        val html=login.body!!.string();login.close()
        val action=Regex("<form[^>]*action=\"([^\"]+)\"").find(html)!!.groupValues[1].replace("&amp;","&")
        assertEquals("localhost",Uri.parse(action).host)
        val form=FormBody.Builder().add("username",fixture.getString("patientUsername")).add("password",fixture.getString("patientPassword")).add("credentialId","").build()
        val authorized=http.newCall(Request.Builder().url(action).header("Cookie",cookies).post(form).build()).execute()
        assertEquals(302,authorized.code)
        val redirect=Uri.parse(authorized.header("Location"));authorized.close()
        assertEquals(request.state,redirect.getQueryParameter("state"))
        session.finishLogin(AuthorizationResponse.Builder(request).fromUri(redirect).build().toIntent())
        val account=session.account.first();assertNotEquals("local",account)
        val cancelled=api("/patient-invitations","POST",JSONObject().put("email",fixture.getString("patientUsername")+"@example.invalid"),expected=201)
        api("/patient-invitations/"+cancelled.getString("id"),"DELETE",cookie=fixture.getString("strangerCookie"),expected=404)
        api("/patient-invitations/"+cancelled.getString("id"),"DELETE")
        try {
            session.requestFor(account,"/patient-links/accept","POST",JSONObject().put("token",cancelled.getString("token")).put("consent",true).put("policyVersion","2026-11-03"))
            fail("Cancelled invitation must not be accepted")
        } catch (failure:ApiFailure) {assertEquals(404,failure.status)}
        session.requestFor(account,"/patient-links/accept","POST",JSONObject().put("token",fixture.getString("invitation")).put("consent",true).put("policyVersion","2026-11-03"))
        val patients=api("/patients").getJSONArray("items")
        assertTrue((0 until patients.length()).any {patients.getJSONObject(it).getString("id")==account})
        val today=LocalDate.now(ZoneOffset.UTC).toString()
        val plan=JSONObject().put("title","INTEGRATION TEST plan "+fixture.getString("suffix")).put("startsOn",today).put("endsOn",today).put("timezone","UTC")
            .put("symptoms",JSONArray().put(JSONObject().put("id",fixture.getString("symptomId")).put("name","INTEGRATION TEST symptom").put("min",0).put("max",10)))
            .put("times",JSONArray(listOf("00:00","23:59"))).put("instructions",JSONArray().put("Synthetic integration instruction; not clinical guidance."))
            .put("pointIds",JSONArray()).put("followUpAt",Instant.now().plusSeconds(86400).toString())
        val treatment=api("/treatments","POST",JSONObject().put("patientId",account).put("plan",plan),expected=201).getString("id")
        api("/treatments/$treatment/publish","POST",JSONObject().put("version",1))
        repository.synchronize(account)
        assertEquals(treatment,repository.dao.allTreatments(account).first {it.id==treatment}.id)
        repository.recordSymptom(account,treatment,fixture.getString("symptomId"),3,"Synthetic online integration record")
        val slot=repository.dao.allOccurrences(account).first {it.treatmentId==treatment && it.scheduledAt<=System.currentTimeMillis()}
        repository.recordStimulation(account,slot)
        repository.synchronize(account)
        assertEquals(1,api("/treatments/$treatment/symptom-logs").getJSONArray("items").length())
        assertEquals(1,api("/treatments/$treatment/stimulations").getJSONArray("items").length())
        WorkManager.getInstance(context).cancelAllWork().result.get(20,TimeUnit.SECONDS)
        fun shell(command:String){instrumentation.uiAutomation.executeShellCommand(command).use { java.io.FileInputStream(it.fileDescriptor).readBytes() }}
        shell("cmd connectivity airplane-mode enable");shell("svc wifi disable");shell("svc data disable")
        val connectivity=context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        var replay: OutboxOperation?=null
        try {
            repeat(60) { if(connectivity.activeNetwork!=null)Thread.sleep(500) }
            assertNull("Test must be offline before recording",connectivity.activeNetwork)
            repository.recordSymptom(account,treatment,fixture.getString("symptomId"),7,"Synthetic offline integration record")
            val queued=repository.dao.pending(account);assertTrue(queued.isNotEmpty());replay=queued.first()
            assertTrue(repository.dao.allEvents(account).any {it.syncState=="pending"})
            assertEquals(1,api("/treatments/$treatment/symptom-logs").getJSONArray("items").length())
        } finally {shell("cmd connectivity airplane-mode disable");shell("svc wifi enable");shell("svc data enable")}
        repeat(60) { if(connectivity.activeNetwork==null)Thread.sleep(500) }
        assertNotNull("Test network must reconnect before synchronizing and loading WebView",connectivity.activeNetwork)
        val pending=replay!!
        repository.synchronize(account)
        assertTrue(repository.dao.pending(account).isEmpty())
        val duplicate=JSONObject().put("deviceId",session.deviceId()).put("operations",JSONArray().put(JSONObject(pending.payload)))
        repeat(2){session.requestFor(account,"/sync/push","POST",duplicate)}
        assertEquals(2,api("/treatments/$treatment/symptom-logs").getJSONArray("items").length())
        api("/treatments/$treatment/sessions","POST",JSONObject().put("occurredAt",Instant.now().toString()).put("notes","Synthetic private professional note"),expected=201)
        val revision=api("/treatments/$treatment").getJSONObject("treatment").getInt("version")
        val revisedPlan=JSONObject(plan.toString()).put("title","INTEGRATION TEST revised plan")
        val edited=api("/treatments/$treatment","PATCH",JSONObject().put("version",revision).put("plan",revisedPlan))
        api("/treatments/$treatment/publish","POST",JSONObject().put("version",edited.getInt("version")))
        repository.synchronize(account)
        val received=repository.dao.treatment(account,treatment)!!
        assertEquals("INTEGRATION TEST revised plan",received.title)
        val snapshot=JSONObject(received.snapshot)
        assertEquals(1,snapshot.getJSONArray("sessions").length())
        assertFalse(snapshot.getJSONArray("sessions").getJSONObject(0).has("notes"))
        Instant.parse(snapshot.getJSONArray("appointments").getJSONObject(0).getString("scheduled_at"))
        assertEquals(2,api("/treatments/$treatment").getJSONArray("versions").length())
        assertEquals(2,repository.dao.allEvents(account).count {it.treatmentId==treatment && it.kind=="symptom"})
        assertEquals(404,run {api("/treatments/$treatment",cookie=fixture.getString("strangerCookie"),expected=404);404})
        val dashboard=api("/dashboard");assertTrue(dashboard.getJSONArray("recent").length()>0)
        val invitationEmail="integration-ui-"+fixture.getString("suffix")+"@example.invalid"
        val uiInvitation=api("/patient-invitations","POST",JSONObject().put("email",invitationEmail),expected=201)
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        var web:WebView?=null
        var pageFailure="none"
        try {
            val ready=CountDownLatch(1)
            scenario.onActivity {activity->web=WebView(activity).apply {
                settings.javaScriptEnabled=true;settings.domStorageEnabled=true
                webViewClient=object:android.webkit.WebViewClient(){
                    override fun onReceivedError(view:WebView,request:android.webkit.WebResourceRequest,error:android.webkit.WebResourceError){if(request.isForMainFrame)pageFailure="network:"+error.errorCode}
                    override fun onReceivedHttpError(view:WebView,request:android.webkit.WebResourceRequest,response:android.webkit.WebResourceResponse){if(request.isForMainFrame)pageFailure="http:"+response.statusCode}
                    override fun onReceivedSslError(view:WebView,handler:android.webkit.SslErrorHandler,error:android.net.http.SslError){pageFailure="tls:"+error.primaryError;handler.cancel()}
                }
            };activity.setContentView(web);CookieManager.getInstance().setCookie(BuildConfig.API_URL,"$proCookie; Path=/api/v1; Secure; HttpOnly"){ready.countDown()} }
            assertTrue(ready.await(10,TimeUnit.SECONDS))
            instrumentation.runOnMainSync {web!!.loadUrl(BuildConfig.API_URL)}
            var displayed=false
            repeat(90){if(!displayed){val result=CountDownLatch(1);instrumentation.runOnMainSync {web!!.evaluateJavascript("document.body.innerText.includes('INTEGRATION TEST patient') && document.body.innerText.includes('Registros recentes')"){ displayed=it=="true";result.countDown() }};result.await(3,TimeUnit.SECONDS);if(!displayed)Thread.sleep(1000)}}
            var pageState=""
            val diagnostic=CountDownLatch(1)
            instrumentation.runOnMainSync {web!!.evaluateJavascript("JSON.stringify({ready:document.readyState,textLength:document.body.innerText.length,login:document.body.innerText.includes('Entrar com segurança'),dashboard:document.body.innerText.includes('Registros recentes')})"){pageState=it;diagnostic.countDown()}}
            diagnostic.await(5,TimeUnit.SECONDS)
            assertTrue("Professional React panel must render synchronized patient records ($pageFailure; $pageState)",displayed)
            fun javascript(script:String):String {
                val result=CountDownLatch(1);var value=""
                instrumentation.runOnMainSync {web!!.evaluateJavascript(script){value=it;result.countDown()}}
                assertTrue(result.await(5,TimeUnit.SECONDS));return value
            }
            fun waitForPage(predicate:String):Boolean {
                repeat(60){if(javascript(predicate)=="true")return true;Thread.sleep(500)}
                return false
            }
            javascript("[...document.querySelectorAll('nav button')].find(button=>button.textContent.includes('Pacientes')).click()")
            assertTrue("Pending invitation must appear in the real professional panel",waitForPage("document.body.innerText.includes('$invitationEmail')"))
            javascript("[...document.querySelectorAll('button')].find(button=>button.getAttribute('aria-label')==='Cancelar convite para $invitationEmail').click()")
            assertTrue("Cancelled invitation must disappear from the panel",waitForPage("!document.body.innerText.includes('$invitationEmail')"))
            val remaining=api("/patient-invitations").getJSONArray("items")
            assertFalse((0 until remaining.length()).any {remaining.getJSONObject(it).getString("id")==uiInvitation.getString("id")})
        } finally {scenario.onActivity {web?.destroy()};scenario.close()}
        val links=session.requestFor(account,"/patient-links").getJSONArray("items")
        session.requestFor(account,"/patient-links/"+links.getJSONObject(0).getString("id"),"DELETE")
        api("/treatments/$treatment",expected=404)
        repository.synchronize(account)
        assertFalse(repository.dao.treatment(account,treatment)!!.sharingActive)
        assertEquals(2,repository.dao.allEvents(account).count {it.treatmentId==treatment && it.kind=="symptom"})
        session.logout();assertEquals("local",session.account.first())
        assertTrue(repository.dao.pending("unrelated-account").isEmpty())
    }
}
