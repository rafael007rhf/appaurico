const url=process.argv[2]??'https://localhost:8443/ready';
for(let i=0;i<60;i++){try{const response=await fetch(url);if(response.ok){console.log('Service ready.');process.exit(0);}}catch{}await new Promise(r=>setTimeout(r,1000));}
throw Error('Service readiness timeout: check the local service logs.');
