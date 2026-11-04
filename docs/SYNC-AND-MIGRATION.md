# Sincronização, migração e recuperação

## Transação local e isolamento

Cada evento novo de tratamento e sua operação UUID entram na mesma transação Room. A fila guarda o corpo completo do envio e o identificador de conta. WorkManager exige conectividade e usa backoff exponencial; também há sincronização periódica e manual. Falhas de rede não removem eventos. Operações recusadas ficam identificadas e preservadas para análise; não são reenviadas infinitamente.

A sessão é serializada pelo AppAuth e cifrada com AES-GCM usando chave gerada pelo Android Keystore, em arquivo atômico no diretório sem backup. Um mutex serializa sincronização e troca/saída de conta. O worker consulta a conta vigente e só envia sua fila. Registros de outras contas permanecem no dispositivo, mas a UI filtra pela conta ativa. Não são associados à próxima conta.

## Protocolo

1. Pull de alterações de autoridade, inclusive revogações.
2. Push de até 50 operações por lote; cada operação usa sua própria transação.
3. Autenticação, propriedade, vínculo, plano, escala e ocorrência são validados.
4. O recibo `(user_id, operation_id)` guarda hash do corpo normalizado e resultado. Repetição do mesmo corpo retorna o recibo; corpo diferente gera conflito.
5. Confirmações simultâneas de uma ocorrência retornam o registro canônico e não aumentam adesão.
6. Ack atualiza evento e fila na mesma transação local.
7. Novo pull traz registros dos outros dispositivos. Cursor e dados importados são persistidos juntos.

O cursor é uma revisão por paciente. Escritas adquirem lock na linha do paciente antes de alterar recursos e incrementar a revisão; a ordem de commit corresponde à ordem de leitura. Não se usa timestamp nem sequência global sem garantia de commit para avançar o cursor.

## Conflitos

- Planos: autoridade do servidor; rascunho usa versão otimista, publicação cria histórico.
- Sintomas: eventos imutáveis, correção com predecessor único. Correções concorrentes conflitantes são recusadas e permanecem locais.
- Estimulações: unicidade por ocorrência, inclusive entre dispositivos.
- Preferência de lembretes: local por dispositivo, sem sincronização automática.
- Revogação: acesso profissional bloqueado no servidor; cópias pessoais do paciente permanecem; novos envios daquele vínculo são recusados. Revogação offline só é conhecida após conexão.
- Encerramento: cancela agenda futura; eventos novos enviados após encerramento são recusados, preservando a fila local para revisão. Não são descartados.

## Room 2 → 3

`semente.db` e as tabelas `symptom_log`, `stimulation`, `reminder` são preservados byte a byte quanto aos valores. A migração cria tabelas da plataforma e seus índices. Não transforma dados antigos em registros remotos, não inventa paciente e não muda intensidades/datas.

Os dados antigos aparecem na seção “Registros locais e legados”. A associação assistida e importação para tratamento remoto **ainda não foi implementada**: precisa de seleção de conta/tratamento/sintoma, prévia e confirmação explícita. Até lá, nenhum legado entra na fila.

A demonstração usa outro pacote e `aurico-demo.db`. Seus controles de limpeza não alcançam `semente.db`.

## Recuperação

- Não desinstalar nem limpar os dados do aplicativo ao encontrar falha de migração.
- Não usar fallback destrutivo ou reinstalação como reparo.
- Em dispositivo autorizado, interromper o app e preservar banco, WAL e SHM juntos antes de qualquer intervenção. Esses arquivos contêm dados sensíveis e não devem ir para Git ou logs.
- Reproduzir a falha em cópia protegida e adicionar migração incremental específica, com teste de preservação.
- A versão 1 não tinha esquema exportado na base inspecionada. Não foi presumida uma migração 1→2; bancos de versões não suportadas falham preservando dados e exigem auditoria do esquema real.
- O procedimento de backup de produção do PostgreSQL, restauração e retenção exige desenho operacional antes da implantação. Não há purge automático neste incremento.
