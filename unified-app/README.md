# Meta Glasses Unificado v0.3

Um único **APK Android** e um único ícone, com três áreas:

- **Reuniões / áudio**: gravador WAV mono 16 kHz usando o microfone Bluetooth do Oakley (teste HFP/LE do Android), gravação em primeiro plano, lista por título, reprodução e exclusão.
- **Câmera / fotos / vídeos**: CameraAccess da Meta, usando o **DAT SDK** oficial; preview, foto e vídeo com opção de áudio.
- **Biblioteca**: consulta fotos e vídeos salvos automaticamente na galeria do Android, álbuns `Pictures/MetaGlasses/` e `Movies/MetaGlasses/`. As capturas são arquivadas antes de abrir a prévia temporária do CameraAccess.

O aplicativo de câmera usa a implementação original da Meta como base, que já foi testada com seus óculos. O recurso de áudio sem vídeo usa `AudioManager.setCommunicationDevice`/`AudioRecord` e **ainda não foi comprovado no seu HSTN**. Não existe garantia de que o Android exponha o microfone do HSTN a aplicativos de terceiros. O gravador recusa capturar se o Android reportar o microfone do celular.

## Instalação no Android, sem computador

1. Acesse [GitHub Actions](https://github.com/LuigiHuss/oakley_meta_glasses/actions) no repositório privado.
2. Abra **Android - Meta Glasses Unificado v0.3**.
3. Use o artefato de uma execução **verde**: `Meta-Glasses-Unificado-v0.3-APK`.
4. Extraia o ZIP e instale `Meta-Glasses-Unificado-v0.3.apk`.
5. Abra o ícone **Meta Glasses**. Há três botões: **Reuniões e áudio**, **Câmera, fotos e vídeos**, **Biblioteca**.
6. No modo câmera: autorize o **novo app** pelo Meta AI (o identificador do APK é novo) e siga Start Session → Preview → Foto/Vídeo. Confira a galeria após a prévia.
7. No modo áudio: encerre a sessão de câmera para evitar disputa de Bluetooth. Inicie uma gravação curta, confirme o consentimento dos participantes, fale e finalize. Reproduza o arquivo na lista de reuniões.

## Limitações deliberadas desta versão

- Sem transcrição, resumos, busca pelo conteúdo, API OpenAI, Supabase ou conta online. Apenas **busca de reuniões pelo título**.
- Áudios WAV são privados ao aplicativo; **desinstalar pode excluir esses áudios**. Não desinstale sem backup/exportação. Capturas de foto/vídeo na galeria persistem separadamente.
- A reprodução de vídeo/foto abre visualizador Android através de `MediaStore`.
- O áudio de reunião só grava enquanto o sistema permitir uma rota Bluetooth de chamadas. Algumas restrições do Android e do firmware Meta podem impedir essa rota.
- O APK usa ID único `dev.luigihuss.metaglasses.unificado`, portanto não substitui CameraAccess nem Meta Glasses Audio anteriores.
- Os comandos de voz “Hey Meta” não são interceptados; a interface é manual e só usa permissões fornecidas pelo usuário.
- Não é indicado gravar reuniões sem informar as pessoas envolvidas e observar as regras aplicáveis de consentimento e privacidade.

## Como é montado no GitHub Actions

`scripts/build_unified.py` clona o exemplo CameraAccess oficial, incorpora classes próprias de áudio/biblioteca, cria o launcher unificado, preserva a atividade de callback do Meta AI e adiciona salvamento permanente pela API MediaStore.

Workflow: `.github/workflows/android-unificado.yml`.

Se houver falha de build, envie o primeiro erro do job **Compilar Android APK de teste** ou **Unificar camera e audio**, com o link da execução.

### Próximas versões
- Exportação/backup de WAV e mecanismo de transcrição.
- Banco de reuniões, transcrições indexadas com data/hora e pesquisa por conteúdo.
- Resumo, decisões e tarefas com confirmação antes de criar compromissos.
- Segurança e privacidade por perfil, retenção e permissões.
