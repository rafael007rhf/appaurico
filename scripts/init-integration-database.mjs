import fs from 'node:fs';import path from 'node:path';import pg from 'pg';
const root=path.resolve(new URL('..',import.meta.url).pathname),source=fs.readFileSync(path.join(root,'backend/.env.https'),'utf8');
const connection=source.match(/^DATABASE_URL=(.*)$/m)?.[1];if(!connection)throw Error('Missing development database URL');const url=new URL(connection);
if(!['localhost','127.0.0.1'].includes(url.hostname))throw Error('Test database setup is restricted to the local isolated server');
const client=new pg.Client({connectionString:connection});await client.connect();try{if(!(await client.query("SELECT 1 FROM pg_database WHERE datname='aurico_integration'")).rows.length)await client.query('CREATE DATABASE aurico_integration');}finally{await client.end();}
url.pathname='/aurico_integration';fs.writeFileSync(path.join(root,'backend/.env.integration'),source.replace(/^DATABASE_URL=.*$/m,'DATABASE_URL='+url.href),{mode:0o600});console.log('Dedicated integration database prepared; development data is preserved.');
