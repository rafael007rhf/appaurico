# AURICO 3 — instruções de desenvolvimento

AURICO é uma plataforma de apoio ao acompanhamento. Não produz diagnósticos, protocolos ou prescrições automáticas. Toda orientação pertence ao profissional responsável.

## Arquitetura

- Android na raiz: Kotlin, Compose Material 3, Hilt, Room, MVVM, Coroutines/Flow, Navigation e WorkManager.
- `patient`: aplicativo real, sem criação automática de tratamentos. `demo`: aplicação e banco separados, para o protótipo acadêmico.
- `backend/`: monólito modular TypeScript, Fastify, PostgreSQL, consultas parametrizadas Drizzle, OIDC/Keycloak.
- `professional-web/`: Next.js App Router, TypeScript, Tailwind/CSS, API no mesmo domínio via proxy.
- `docs/`: auditoria, contratos, segurança, sincronização e estado verificável da entrega.

## Regras

1. Instruções explícitas do usuário prevalecem sobre as restrições antigas do protótipo. Backend, login e sincronização são parte desta arquitetura.
2. O Android persiste acompanhamento e operações da fila na mesma transação; rede não é pré-condição para registrar dados em um plano já recebido.
3. Nunca usar migração destrutiva. Nunca associar registros antigos a uma identidade remota por inferência.
4. A produção não usa `Ciclo` nem o conteúdo clínico ilustrativo do protótipo. Esses fluxos só são acessíveis no flavor `demo`.
5. Autorização é obrigatória no servidor por papel, propriedade e vínculo ativo. UUID não é autorização.
6. Não registrar corpos, tokens, nomes, observações ou registros de saúde em logs técnicos.
7. Não incluir credenciais em Git. O setup local cria arquivos ignorados, com permissões restritas.
8. Preserve eventos e correções. Mudanças de plano publicadas produzem novas versões; não altere o passado.
9. Compartilhamento exige consentimento e deve admitir revogação. Não presuma habilitação profissional.
10. Conteúdo anatômico novo exige fonte e revisão; a biblioteca real começa vazia.
11. Identidade visual: fundo #EDF1EA, principal #2E4034, mostarda #D98A3D, sage #7CA289, texto #1B2620. Textos em português, acessíveis e sem conclusões clínicas automáticas.
12. Execute `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` e as tarefas Android documentadas. Registre falhas e limites; não invente evidência.
13. Não publicar em produção nem modificar recursos remotos de forma destrutiva sem instrução explícita.
