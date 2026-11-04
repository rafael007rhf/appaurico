# Auditoria da integração — 4 de novembro de 2026

Base preservada: `182b164fa24f5863b7da7af5cb392d1525b395a4`. Branch nova: `feature/aurico-live-integration`. Não havia alterações não commitadas na criação.

| Componente | Antes | Incremento |
|---|---|---|
| Banco | SQL/migrações e testes embarcados; nenhum PostgreSQL operacional neste host | PostgreSQL nativo real, migrações executadas, banco de teste exclusivo |
| Identidade | OIDC/PKCE implementado e testado fora do Android | AppAuth executado no emulador contra identidade real em HTTPS |
| Cadastro profissional | Dependia de atribuição manual de papel | Adesão autenticada e auditada para contas sem relacionamentos de paciente; sem elevar verificação ou privilégios operacionais |
| Aplicativo paciente | APK sem URLs | APK compilado para gateway HTTPS de desenvolvimento com confiança restrita à CA local |
| Room/sync | Persistência/fila implementadas; sem evidência nativa contra servidor real | Cenário completo executado no emulador, com registro offline, replay e conferência no servidor |
| Datas de rede | Driver PostgreSQL podia entregar timestamps em formato SQL | ISO 8601 estável em linhas e JSONB já armazenado; teste de regressão |
| Vínculos | Revogação disponível na API/web | Consulta e revogação também na tela Conta Android |
| Convites pendentes | Expiração e uso único, sem cancelamento pelo criador | Listagem paginada e cancelamento auditado na API/painel; teste de isolamento e aceitação recusada |
| Painel | Consultas reais, atualização manual | Polling moderado quando visível; renderização dos registros comprovada em WebView de teste |
| Retentativas | Lotes incompletos podiam permanecer em loop | Resposta precisa confirmar exatamente os IDs enviados; fila preservada ao recusar confirmação incompleta |
| Desenvolvimento | Compose HTTP sem instrução prática de acesso Android | Overlay HTTPS, CA local restrita, scripts de fixture/runner e encaminhamento ADB explícito |
| CI | Verificava camadas separadas | Job com PostgreSQL e Keycloak reais e teste instrumentado do fluxo integrado |

O conteúdo de teste é sintético e explicitamente identificado. Nenhum paciente real foi utilizado. A API mantém autorização por papel, propriedade e vínculo; o cadastro profissional não confirma habilitação. A CA de desenvolvimento e as credenciais não são versionadas. O APK contém apenas a CA pública de debug e URLs, sem chaves privadas, senha ou token.

O endereço público de produção continua dependente de hospedagem/domínio e configuração dos serviços externos. Nenhuma implantação de produção ou alteração na main foi realizada. Instruções e limites exatos em `LIVE-INTEGRATION.md`.
