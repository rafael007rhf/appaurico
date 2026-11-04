# Segurança e limites de uso

## Controles implementados

OIDC com PKCE, validação de JWT, sessão web HttpOnly, proteção de Origin para CSRF, cookies Secure fora do loopback, consultas parametrizadas, validação Zod, limite de corpo/lote/frequência, autorização por recurso e vínculo, auditoria de acessos e alterações, ausência de conteúdo de saúde em logs técnicos e respostas sem cache. Pontos e orientações não são gerados automaticamente.

Android: credenciais em arquivo atômico cifrado com chave do Keystore; backup/transferência desativados; dados separados por conta; notificações neutras e privadas. O banco Room usa a proteção de armazenamento do Android e não possui criptografia adicional do banco neste incremento. Esse limite deve ser avaliado para dispositivos compartilhados, comprometidos ou sujeitos a exigências de proteção adicionais.

O ambiente Compose é de desenvolvimento e expõe serviços somente no loopback. O acesso HTTP de desenvolvimento não é permitido quando `NODE_ENV=production`. O aplicativo Android exige HTTPS e mantém validação padrão de certificados. Não foi incluída opção para ignorar TLS.

## Antes de pacientes reais

A implementação não declara conformidade com LGPD, habilitação profissional ou enquadramento regulatório. É necessária avaliação jurídica e profissional de finalidade, base legal, responsabilidades de controlador/operador, atendimento de direitos, retenção, menores de idade, conteúdo clínico e enquadramento do produto. O aceite técnico de convite não substitui essa definição.

Ainda precisam de implementação/validação operacional: fluxo completo de solicitação e execução de exclusão conforme retenção legal; política e rotinas de retenção; gestão/verificação de habilitação profissional; regras para responsáveis e menores; recuperação de banco e backup cifrado testado; monitoração sem dados de saúde; revisão de segurança independente; rotação de segredos e resposta a incidentes. Não existe endpoint de exclusão em massa que apague prontuários sem essas decisões.

O rate limiter atual é em memória por processo, adequado ao monólito de desenvolvimento. A produção exige dimensionamento, proxy confiável explícito, proteção distribuída se houver réplicas e revisão de disponibilidade. Exportações e PDFs devem ser tratados como dados sensíveis pelo usuário e pela operação.

## Revisão de dados

Nenhum paciente é semeado pelo setup. Os únicos usuários/registros sintéticos são criados dentro dos testes automatizados, no banco de teste efêmero. A biblioteca clínica começa vazia. Os convites não enviam mensagens automaticamente.
