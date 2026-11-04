import {execFileSync} from 'node:child_process';import path from 'node:path';import fs from 'node:fs';
const root=path.resolve(new URL('..',import.meta.url).pathname),adb=process.env.ADB??'adb';
const serial=process.env.ANDROID_SERIAL??execFileSync(adb,['devices'],{encoding:'utf8'}).split('\n').find(x=>/^emulator-\d+\s+device/.test(x))?.split(/\s/)[0];
if(!serial?.startsWith('emulator-'))throw Error('Integration runner requires a dedicated emulator. It never installs fixtures on a personal device.');
const run=(args)=>execFileSync(adb,['-s',serial,...args],{stdio:'inherit'});
run(['reverse','tcp:8443','tcp:8443']);
const apk=path.join(root,'app/build/outputs/apk/patient/debug/app-patient-debug.apk');run(['install','-r',apk]);run(['shell','pm','grant','br.edu.ifpr.appaurico','android.permission.POST_NOTIFICATIONS']);
const fixture=path.join(root,'.integration/fixture.json');if(!fs.existsSync(fixture))throw Error('Run prepare-integration.mjs first');
run(['push',fixture,'/data/local/tmp/aurico-integration.json']);
try{run(['shell','run-as','br.edu.ifpr.appaurico','mkdir','-p','files']);run(['shell','run-as','br.edu.ifpr.appaurico','cp','/data/local/tmp/aurico-integration.json','files/integration.json']);}finally{run(['shell','rm','-f','/data/local/tmp/aurico-integration.json']);}
try{execFileSync(path.join(root,'gradlew'),[':app:connectedPatientDebugAndroidTest','-PauricoApiUrl=https://localhost:8443','-PauricoOidcIssuer=https://localhost:8443/identity/realms/aurico','-Pandroid.testInstrumentationRunnerArguments.liveIntegration=true','-Pandroid.testInstrumentationRunnerArguments.class=br.edu.ifpr.appaurico.platform.LivePlatformIntegrationTest','--console=plain'],{cwd:root,stdio:'inherit',env:{...process.env,ANDROID_SERIAL:serial}});}finally{run(['shell','run-as','br.edu.ifpr.appaurico','rm','-f','files/integration.json']);}
