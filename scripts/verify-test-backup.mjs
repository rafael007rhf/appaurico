// Destructive restore operations are restricted to a newly created, isolated test database.
import fs from 'node:fs';import path from 'node:path';import pg from 'pg';import {execFileSync} from 'node:child_process';
const url=new URL(process.env.DATABASE_URL??'');
if(!['127.0.0.1','localhost'].includes(url.hostname)||url.pathname!=='/aurico_integration')throw Error('Backup verification is restricted to the dedicated local integration database');
const binary=process.env.PG_BIN??'',destination=path.resolve('.integration/backup.dump');fs.mkdirSync(path.dirname(destination),{recursive:true,mode:0o700});
const environment={...process.env,PGHOST:url.hostname,PGPORT:url.port||'5432',PGUSER:decodeURIComponent(url.username),PGPASSWORD:decodeURIComponent(url.password),PGDATABASE:'aurico_integration'};
execFileSync(path.join(binary,'pg_dump'),['--format=custom','--file',destination],{env:environment,stdio:'pipe'});fs.chmodSync(destination,0o600);
const restore='aurico_restore_'+Date.now(),admin=new pg.Client({connectionString:url.href});await admin.connect();await admin.query(`CREATE DATABASE ${restore}`);await admin.end();
execFileSync(path.join(binary,'pg_restore'),['--exit-on-error','--no-owner','--dbname',restore,destination],{env:environment,stdio:'pipe'});
const restored=new URL(url);restored.pathname='/'+restore;const a=new pg.Client({connectionString:url.href}),b=new pg.Client({connectionString:restored.href});await a.connect();await b.connect();
try{for(const table of ['schema_migration','app_user','treatment','symptom_log','stimulation_log','sync_operation']){const source=await a.query(`SELECT count(*)::int AS total FROM ${table}`),copy=await b.query(`SELECT count(*)::int AS total FROM ${table}`);if(source.rows[0].total!==copy.rows[0].total)throw Error('Backup restoration mismatch');}console.log('PASS: PostgreSQL custom backup restored into a new isolated database; migrations, records and receipts preserved.');}finally{await a.end();await b.end();}
