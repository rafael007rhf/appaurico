import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),tls=path.join(root,'infra/local-tls');
fs.mkdirSync(tls,{recursive:true,mode:0o700});
if(!fs.existsSync(path.join(tls,'server.crt'))){
 fs.writeFileSync(path.join(tls,'ca.cnf'),'[req]\ndistinguished_name=dn\nprompt=no\nx509_extensions=v3_ca\n[dn]\nCN=AURICO local development CA\n[v3_ca]\nbasicConstraints=critical,CA:TRUE\nkeyUsage=critical,keyCertSign,cRLSign\nsubjectKeyIdentifier=hash\n');
 execFileSync('openssl',['req','-x509','-newkey','rsa:3072','-nodes','-keyout',path.join(tls,'ca.key'),'-out',path.join(tls,'ca.crt'),'-days','30','-config',path.join(tls,'ca.cnf'),'-extensions','v3_ca'],{stdio:'ignore'});
 execFileSync('openssl',['req','-newkey','rsa:3072','-nodes','-keyout',path.join(tls,'server.key'),'-out',path.join(tls,'server.csr'),'-subj','/CN=localhost'],{stdio:'ignore'});
 fs.writeFileSync(path.join(tls,'server.ext'),'subjectAltName=DNS:localhost,IP:127.0.0.1\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n');
 execFileSync('openssl',['x509','-req','-in',path.join(tls,'server.csr'),'-CA',path.join(tls,'ca.crt'),'-CAkey',path.join(tls,'ca.key'),'-CAcreateserial','-out',path.join(tls,'server.crt'),'-days','30','-extfile',path.join(tls,'server.ext')],{stdio:'ignore'});
 for(const file of ['server.key','ca.key'])fs.chmodSync(path.join(tls,file),0o600);
}
const env=fs.readFileSync(path.join(root,'backend/.env'),'utf8').replace(/^OIDC_ISSUER=.*$/m,'OIDC_ISSUER=https://localhost:8443/identity/realms/aurico').replace(/^WEB_ORIGIN=.*$/m,'WEB_ORIGIN=https://localhost:8443').replace(/^ALLOW_LOCAL_HTTP=.*$/m,'ALLOW_LOCAL_HTTP=false');
fs.writeFileSync(path.join(root,'backend/.env.https'),env,{mode:0o600});
const realmFile=path.join(root,'infra/realm.generated.json'),realm=JSON.parse(fs.readFileSync(realmFile,'utf8'));
const web=realm.clients.find(c=>c.clientId==='aurico-web');web.redirectUris=[...new Set([...web.redirectUris,'https://localhost:8443/api/v1/auth/callback'])];web.webOrigins=[...new Set([...web.webOrigins,'https://localhost:8443'])];fs.writeFileSync(realmFile,JSON.stringify(realm,null,2),{mode:0o600});
const debug=path.join(root,'app/src/debug/res/raw');fs.mkdirSync(debug,{recursive:true});fs.copyFileSync(path.join(tls,'ca.crt'),path.join(debug,'aurico_local_ca.crt'));
fs.mkdirSync(path.join(root,'app/src/debug/res/xml'),{recursive:true});fs.writeFileSync(path.join(root,'app/src/debug/res/xml/aurico_network.xml'),'<network-security-config><base-config cleartextTrafficPermitted="false"><trust-anchors><certificates src="system" /></trust-anchors></base-config><domain-config><domain includeSubdomains="false">localhost</domain><trust-anchors><certificates src="system" /><certificates src="@raw/aurico_local_ca" /></trust-anchors></domain-config></network-security-config>');
console.log('Local TLS material prepared. Public CA copied to ignored debug resource; private keys remain outside APK and Git.');
