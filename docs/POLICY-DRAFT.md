# Política inicial AURICO — rascunho 2026-11-04

Status: proposta para homologação, não aprovada juridicamente. Nenhum valor deste documento equivale a prazo legal obrigatório ou certificação clínica.

## Responsabilidade e finalidade

O mantenedor do serviço precisa identificar a pessoa jurídica responsável, seu contato de privacidade e os papéis de controlador/operador antes da abertura a pacientes reais. O AURICO organiza planos aprovados por profissionais e registros informados pelos pacientes. Não realiza diagnóstico, prescrição ou avaliação automática de eficácia. Não usa dados de saúde para publicidade, treinamento de modelos ou analytics de terceiros.

A base legal precisa ser definida por finalidade e agente responsável. O consentimento de vínculo controla compartilhamento na aplicação e não é tratado como solução automática para todas as obrigações legais. Dados de saúde são sensíveis; término, conservação excepcional e direitos do titular exigem avaliação da situação concreta. Referência: [LGPD, texto atualizado na Câmara dos Deputados](https://www2.camara.leg.br/legin/fed/lei/2018/lei-13709-14-agosto-2018-787077-normaatualizada-pl.html).

## Compartilhamento e revogação

O profissional acessa somente tratamentos sob sua responsabilidade e com vínculo ativo. O paciente aceita convite destinado ao e-mail verificado. Cada aceite e revogação fica auditado. Revogar bloqueia novos acessos profissionais; não apaga silenciosamente os registros pessoais. Dispositivos offline recebem a revogação ao reconectar. Cópias exportadas exigem proteção pelo destinatário e não podem ser revogadas tecnicamente à distância.

## Retenção proposta

- Credenciais web: sessão expira em uma hora; tokens opacos são armazenados por hash. OIDC segue o ciclo do provedor.
- Convites: sete dias de validade; tokens expirados não autorizam associação.
- Logs técnicos: proposta de 30 dias, sem corpos, nomes, observações ou tokens. A instalação deve configurar rotação e acesso restrito antes da operação.
- Pedidos de direitos: proposta de revisão operacional inicial em até cinco dias úteis, sem apresentá-la como prazo legal geral. O responsável determina o prazo aplicável e registra justificativa.
- Dados de acompanhamento, consentimentos e auditoria: nenhum descarte automático até definição formal do tipo de registro, finalidade, obrigações aplicáveis e prazo aprovado. Esta retenção provisória não autoriza conservação indefinida; a aprovação da política é condição de entrada em produção.
- Backups: proposta de janela operacional de 30 dias após definição jurídica. A exclusão deve ser propagada na restauração por lista de supressões, e o acesso às cópias deve ser restrito.

## Exclusão e direitos

O paciente pode exportar seus dados e abrir pedido de exclusão. O pedido recebe protocolo e estado consultável. A equipe valida identidade, escopo e obrigações de retenção; informa eventuais impedimentos e oferece resposta fundamentada. Enquanto não existir decisão aprovada, a API não executa purge nem marca exclusão como concluída. A retirada de consentimento de compartilhamento está disponível independentemente do pedido de exclusão. A implantação deve oferecer contato humano, possibilidade de contestação e procedimento de exclusão verificado em banco, backups e provedor de identidade.

## Verificação profissional

Cadastro público não comprova habilitação. O profissional informa registro, jurisdição e referência privada de evidência. Um operador com papel específico `aurico-operations`, atribuído no provedor de identidade, consulta a fonte competente e registra sua decisão; o solicitante não aprova o próprio pedido. Não coletar documentos completos por campos livres. A classificação “verificado” significa revisão cadastral documentada, não promessa de eficácia ou autorização para qualquer atividade clínica. Revogação da habilitação e revalidação periódica precisam de procedimento operacional próprio.

## Crianças, adolescentes e responsáveis

Proposta de lançamento: atendimento remoto da plataforma restrito a adultos até aprovação do fluxo de responsáveis. Não usar declaração de idade isolada como prova de representação. Solicitações de revisão de responsável podem ser registradas, mas não concedem acesso a prontuários ou habilitam contas dependentes. A abertura desse serviço exige verificar representação, capacidade de compreensão, finalidade, minimização, informações adequadas à idade, manifestação do adolescente quando pertinente e revisão do melhor interesse no caso concreto. A ANPD exige a prevalência do melhor interesse na avaliação do tratamento de dados de crianças e adolescentes. [Enunciado CD/ANPD nº 1/2023](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-divulga-enunciado-sobre-o-tratamento-de-dados-pessoais-de-criancas-e-adolescentes).

## Incidentes e entrada em produção

Suspender credenciais comprometidas, preservar evidências sem conteúdo de saúde em logs, avaliar impacto com responsáveis e definir comunicações legais aplicáveis. A aprovação exige responsáveis nomeados, base legal/finalidades, retenção por classe, contratos com fornecedores, revisão de segurança, restauração de backup testada, conteúdo clínico revisado e homologação Android em dispositivos representativos.

Os endereços locais de desenvolvimento são `http://127.0.0.1:3000` (web), `http://127.0.0.1:4000` (API) e `http://127.0.0.1:8080/realms/aurico` (identidade). Endereços HTTPS externos dependem de domínio e infraestrutura contratados; não foram inventados nem publicados.
