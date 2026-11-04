# Arquitetura e execução

## Etapas verificáveis

1. Auditar main e branches, preservar histórico e criar branch isolada.
2. Introduzir modelo relacional, migração Room aditiva e isolamento da demonstração.
3. Implementar identidade, vínculos, planos versionados e autorização por recurso; validar com testes de API/banco.
4. Implementar fila, recibos idempotentes, cursores transacionais e tratamento de correções/revogações.
5. Conectar painel Next.js e app Android aos contratos reais.
6. Executar compilação, lint, testes e revisão dos limites; documentar configuração sem deploy.

## Organização

Android permanece na raiz. O novo fluxo paciente está em `platform/`, separado dos ViewModels e telas acadêmicas, mantendo Compose, Room e Hilt. O backend usa módulos de autenticação, pacientes, tratamentos, registros e relatórios. A camada SQL usa Drizzle para parâmetros e identificadores, com uma interface transacional que permite usar PostgreSQL completo nos testes embarcados.

## Identidade

Keycloak é o provedor local de referência. Clientes web confidencial e Android público usam Authorization Code com PKCE S256. Android usa AppAuth e navegador do sistema; nunca recebe senha. O backend verifica assinatura RS256, emissor, audiência, expiração e papel. IDs de usuários são internos; `issuer + subject` é único. E-mail verificado é exigido para aceitar convite, que expira em sete dias e é de uso único.

A sessão web é um token opaco aleatório em cookie HttpOnly, Secure fora do ambiente local e SameSite=Lax. O banco armazena somente seu hash, com expiração de uma hora. Escritas com cookie exigem Origin igual ao domínio configurado; não há CORS aberto. Estado/nonce/PKCE são validados no callback. A saída elimina a sessão da aplicação; a sessão do provedor de identidade tem ciclo separado.

## Domínio

O SQL versionado é a fonte do esquema. `app_user`, perfis, vínculo e consentimento controlam acesso. Tratamento possui plano, rascunho e versões publicadas imutáveis. Sintomas têm UUID e escala; redefinir um sintoma publicado exige novo ID. Pontos da biblioteca incluem fonte e revisão humana. Sessões e retornos têm relações próprias.

Registros de sintomas são eventos imutáveis; correções referenciam um predecessor único. Estimulações referenciam uma ocorrência única. Chaves compostas impedem atribuir sintomas, sessões ou ocorrências a outro tratamento. Exclusões relacionais são restritas. Vínculos revogados continuam como evidência de consentimento/histórico.

## Agenda e indicador

Datas do plano são locais ao fuso IANA registrado. Ocorrências são instantes UTC gerados na publicação. Limites: 366 dias, oito horários/dia. Horários inexistentes em mudança de horário de verão são omitidos; horários ambíguos geram uma ocorrência, no instante anterior. Uma atualização conserva ocorrências passadas e substitui apenas futuras sem confirmação. O denominador de adesão inclui somente ocorrências não canceladas já vencidas no período; zero ocorrências produz `null`, não 0%.

## Limites operacionais

Este incremento usa monólito único e rate limiting em memória por IP. Escalar para múltiplas instâncias exige limiter compartilhado, política explícita de proxies e testes concorrentes com PostgreSQL de produção. Relatórios/exportações ainda materializam dados do tratamento; impor limites/streaming antes de históricos muito grandes. A trilha de sincronização necessita política de retenção e snapshots antes de limpeza. Não executar limpeza automática sem decisão de retenção aprovada.
