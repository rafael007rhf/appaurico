import https from 'node:https';import http from 'node:http';import fs from 'node:fs';
const tls=new URL('../infra/local-tls/',import.meta.url);
const server=https.createServer({key:fs.readFileSync(new URL('server.key',tls)),cert:fs.readFileSync(new URL('server.crt',tls))},(req,res)=>{
 const port=req.url?.startsWith('/identity/')?8080:req.url?.startsWith('/api/v1/')||['/health','/ready'].includes(req.url)?4000:3000;
 const upstream=http.request({host:'127.0.0.1',port,path:req.url,method:req.method,headers:{...req.headers,host:'localhost:8443','x-forwarded-proto':'https','x-forwarded-host':'localhost:8443','x-forwarded-port':'8443'}},response=>{res.writeHead(response.statusCode??502,response.headers);response.pipe(res);});
 upstream.on('error',()=>{res.writeHead(503,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(JSON.stringify({error:{code:'SERVICE_UNAVAILABLE'}}));});req.pipe(upstream);
});server.listen(8443,'127.0.0.1',()=>console.log('Development TLS gateway: https://localhost:8443'));for(const sig of ['SIGINT','SIGTERM'])process.on(sig,()=>server.close());
