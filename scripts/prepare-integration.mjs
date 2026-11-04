// Creates isolated, clearly labelled synthetic identities using the real development IdP.
import fs from 'node:fs';import path from 'node:path';import {randomBytes,randomUUID} from 'node:crypto';
const base='https://localhost:8443',issuer=base+'/identity/realms/aurico';
const request=async(url,options={})=>{const response=await fetch(url,{...options,redirect:'manual'});return response;};
const token=await request(base+'/identity/realms/master/protocol/openid-connect/token',{method:'POST',body:new URLSearchParams({grant_type:'password',client_id:'admin-cli',username:'admin',password:process.env.KEYCLOAK_ADMIN_PASSWORD})});if(!token.ok)throw Error('Development identity administrator unavailable');
const admin=(await token.json()).access_token,adminHeaders={Authorization:`Bearer ${admin}`,'Content-Type':'application/json'};
const suffix=randomBytes(6).toString('hex'),users=[];
for(const kind of ['professional','patient','stranger']){const username=`integration-${kind}-${suffix}`,password=randomBytes(24).toString('hex');const r=await request(base+'/identity/admin/realms/aurico/users',{method:'POST',headers:adminHeaders,body:JSON.stringify({username,email:username+'@example.invalid',firstName:'INTEGRATION TEST',lastName:kind,enabled:true,emailVerified:true,credentials:[{type:'password',value:password,temporary:false}]})});if(r.status!==201)throw Error('Synthetic identity creation failed');users.push({kind,username,password,location:r.headers.get('location')});}
async function login(user){
 const start=await request(base+'/api/v1/auth/login');if(start.status!==302)throw Error('Login unavailable');const flow=start.headers.getSetCookie().find(x=>x.startsWith('aurico_flow=')).split(';')[0];
 const form=await request(start.headers.get('location'));const html=await form.text(),action=html.match(/<form[^>]*action="([^"]+)"/)?.[1]?.replaceAll('&amp;','&');if(!action||new URL(action).origin!==base)throw Error('Invalid provider login origin');
 const cookie=form.headers.getSetCookie().map(x=>x.split(';')[0]).join('; ');
 const authorized=await request(action,{method:'POST',headers:{Cookie:cookie},body:new URLSearchParams({username:user.username,password:user.password,credentialId:''})});if(authorized.status!==302)throw Error('Provider authorization failed');
 const target=authorized.headers.get('location');if(new URL(target).origin!==base)throw Error('Unexpected callback');const callback=await request(target,{headers:{Cookie:flow}});if(callback.status!==302)throw Error('OIDC callback failed');return callback.headers.getSetCookie().find(x=>x.startsWith('aurico_session=')).split(';')[0];
}
const pro=users.find(x=>x.kind==='professional'),patient=users.find(x=>x.kind==='patient'),stranger=users.find(x=>x.kind==='stranger');
const proCookie=await login(pro),strangerCookie=await login(stranger);
async function api(route,method='GET',body,cookie=proCookie){const r=await request(base+'/api/v1'+route,{method,headers:{Cookie:cookie,Origin:base,...(body?{'Content-Type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{})});if(!r.ok)throw Error(`Integration setup endpoint failed: ${route} (${r.status})`);return r.json();}
await api('/me/enroll-professional','POST',{confirmed:true});
await api('/me/enroll-professional','POST',{confirmed:true},strangerCookie);if((await api('/me')).role!=='professional')throw Error('Professional enrollment did not persist');
const invitation=await api('/patient-invitations','POST',{email:patient.username+'@example.invalid'});
const fixture={base,issuer,patientUsername:patient.username,patientPassword:patient.password,professionalCookie:proCookie,strangerCookie,invitation:invitation.token,suffix,symptomId:randomUUID(),testIdentityLocations:users.map(x=>x.location)};
const dir=path.resolve(process.env.E2E_FIXTURE_DIR??'.integration');fs.mkdirSync(dir,{recursive:true,mode:0o700});fs.writeFileSync(path.join(dir,'fixture.json'),JSON.stringify(fixture),{mode:0o600});console.log('Prepared synthetic integration identities and single-use invitation. Credentials are in an ignored 0600 fixture file.');
