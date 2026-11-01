# Meta Glasses Audio v0.1 — gravador de reuniões (Android)

## Objetivo

Gravar reuniões por **microfone Bluetooth dos Oakley Meta HSTN**, **sem vídeo**.
Esta versão serve como **prova de compatibilidade do HFP/LE Audio** no Android.
Ela não substitui o Meta AI e **não usa o DAT SDK**: utiliza APIs de áudio padrão
do Android (`AudioManager.setCommunicationDevice` + `AudioRecord`). O sistema
pode não expor o microfone dos óculos a aplicativos de terceiros; precisamos
validar isso no dispositivo real.

### Recursos implementados
- Iniciar gravação **manualmente** após confirmação de consentimento.
- Checar permissão `RECORD_AUDIO` / `BLUETOOTH_CONNECT`.
- Solicitar rota Bluetooth de chamadas e conferir `AudioRecord.getRoutedDevice()`.
- **Falha segura:** se a rota real não for Bluetooth, **não grava pelo microfone do celular**.
- Grava WAV PCM mono 16 kHz (aprox. 1,9 MB/min) em pasta privada do aplicativo.
- Continua gravando com tela apagada via foreground service tipo `microphone`, iniciado com o app visível.
- Notificação permanente com ação **Parar**; ao finalizar gravação, grava cabeçalho WAV.
- Lista e reproduz reuniões por data; pesquisa por **título**, exclusão com confirmação.

### Não implementado ainda
- Transcrição automática e pesquisa por palavras efetivamente ditas.
- Resumo, extração de decisões, tarefas, agenda, integração ChatGPT ou Supabase.
- Exportação externa/sincronização/backup dos WAV (não desinstale o app para preservar as gravações).
- Início por voz (“Hey Meta”), gravação automática ou de chamadas.
- Identificação individual de falantes e redução de ruídos aprimorada.

## Compilar pelo celular

1. Abra a aba [Actions](https://github.com/LuigiHuss/oakley_meta_glasses/actions).
2. Procure **Android - Meta Glasses Audio v0.1** e abra a execução mais recente com sucesso.
3. Em **Artifacts**, baixe `Meta-Glasses-Audio-v0.1-APK`.
4. Extraia o ZIP, instale `Meta-Glasses-Audio-v0.1.apk` e abra **Meta Glasses Audio**.
5. Confirme as permissões de microfone/Bluetooth e, se desejar, notificações.
6. Com o HSTN conectado ao Android em áudio de **chamadas**, nomeie a reunião e toque em **Iniciar**.
7. Faça um teste curto, diga algumas frases, toque em **Parar e salvar**; reproduza o item na lista.

**Nota:** builds debug do GitHub têm chave de assinatura temporária. Se Android rejeitar uma futura atualização por assinatura diferente, salve seus áudios primeiro; desinstalar pode apagar os WAV privados.

## Regras de armazenamento e pesquisa futura

O WAV fica em `getFilesDir()/recordings/<timestamp>.wav`; títulos e estado em `SharedPreferences(meetings)`.
Para a pesquisa por conteúdo, a fase 2 adicionará índices duráveis de trechos transcritos com timestamps e
vínculo a cada reunião; a fase 3 poderá usar banco de dados privado com RLS e criptografia.
No futuro, pesquisar “o que decidimos no encontro de terça?” requer indexar a transcrição,
não apenas a gravação em WAV.

## Limitações e privacidade

O perfil Bluetooth de chamadas pode ser mono, com processamento para priorizar a voz de quem usa os óculos,
reduzindo vozes distantes em uma reunião. O app informa somente a **rota de áudio reportada pelo Android**:
é necessário ouvir a gravação para confirmar qualidade e detectar silêncio digital.
A gravação requer informar os participantes e cumprir as regras locais de consentimento.
Nenhuma mídia é enviada para servidores por esta versão.

Referências oficiais:
- https://developer.android.com/develop/background-work/services/fgs/service-types
- https://developer.android.com/reference/android/media/AudioManager#setCommunicationDevice(android.media.AudioDeviceInfo)
- https://developers.meta.com/wearables/faq/
