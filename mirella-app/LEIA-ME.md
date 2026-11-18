# Mirella — app Android de teste (v1.0-teste)

App que liga os **Oakley Meta HSTN** ao seu **n8n**:
óculos (microfone) → app → n8n → resposta em áudio → óculos (alto-falantes).
A câmera dos óculos é usada quando o pedido precisa de foto.

## O que esta versão faz
- Conecta o app ao **Meta AI** (registro do SDK oficial Meta Wearables 1.0.1).
- **Fala com a Mirella**: toque na esfera, fale, ela para sozinha quando você termina e envia ao n8n.
- **Toca a resposta** nos alto-falantes dos óculos.
- **Foto pelos óculos**: quando o n8n pede (`capturar_foto`) ou pelo botão “Foto + pergunta”.
- **Testes** na aba Óculos: áudio, microfone e foto.
- Contexto Pessoal/Profissional enviado em cada pedido.

Ainda não tem: “Oi Mirella” por voz (próxima versão), telas dos módulos com dados do Supabase.

## Antes de começar
1. **Meta AI** instalado no celular, óculos pareados e atualizados.
2. No app Meta AI, ative o **Modo Desenvolvedor** (Configurações → Sobre/versão → toque várias vezes na versão; o caminho pode variar).
3. **Android Studio** recente, **JDK 17**, **Android SDK 36**.
4. Workflow do n8n importado e funcionando (teste com o `curl` do guia do n8n).

## Compilar e instalar
1. Abra a pasta `mirela-android` no Android Studio (File → Open).
2. Espere o Gradle sincronizar (ele baixa o SDK da Meta pelo Maven Central, sem token).
3. Ative a Depuração USB no celular e conecte no computador.
4. Clique em **Run ▶**. Ou gere o APK: Build → Build APK(s) e instale o `app-debug.apk`.

## Primeiro uso no celular
1. Permita **Bluetooth** e **Microfone**.
2. Aba **Ajustes**: cole o endereço do webhook (ex.: `https://seu-n8n/webhook/mirela`) e a chave `X-Mirela-Key`. Salve.
3. Aba **Óculos** → **Conectar ao Meta AI** → aceite no app Meta AI → volte para a Mirella.
4. Rode os testes na ordem: **áudio** → **microfone** → **foto**.
5. Toque na esfera e diga, por exemplo: “gastei quarenta e cinco reais no mercado”.

## Se algo der errado, me mande
- O texto que apareceu na tela (as mensagens de erro são em português e também são faladas).
- Em qual teste falhou.
- Se possível, o Logcat filtrado por `Mirela` ou `mwdat`.

## Ajustes finos conhecidos
- **Corta a fala cedo ou não para de ouvir**: ajuste `VOICE_THRESHOLD` e `SILENCE_TO_STOP_MS` em `GlassesAudio.kt`.
- **Microfone “Celular” em vez de “Óculos”**: os óculos não estão no perfil de chamada Bluetooth; confira o pareamento nas configurações de Bluetooth do Android.
- **Foto falha com permissão**: aceite a permissão de câmera quando o Meta AI pedir.

## Estrutura
```
app/src/main/java/com/mirela/app/
  MainActivity.kt      permissões, inicialização do SDK, pedido de permissão do Meta AI
  MirelaViewModel.kt   fluxo da conversa (ouvir → enviar → falar / foto)
  GlassesAudio.kt      microfone dos óculos (Bluetooth SCO) e reprodução da resposta
  GlassesCamera.kt     foto pela câmera dos óculos (SDK Meta)
  MirelaClient.kt      chamada ao webhook do n8n
  Settings.kt          ajustes salvos no celular
  ui/                  telas no estilo Aurora Líquida
```
Fontes Jura e Outfit sob licença OFL (pasta `licencas-fontes`).

## Gerar o APK sem Android Studio (pelo GitHub)
(Neste repositório o build fica em `.github/workflows/android-mirella.yml`, na raiz.)
1. Crie um repositório **privado** no GitHub (ex.: `mirella-app`).
2. Envie todo o conteúdo da pasta `mirela-android` para ele (botão “Add file → Upload files”, arrastando as pastas; confira se a pasta `.github` foi junto).
3. Abra a aba **Actions**. O build “Gerar APK da Mirella” roda sozinho (leva uns 5 a 10 minutos).
4. Quando ficar verde, abra a execução e baixe **mirella-apk** em “Artifacts”.
5. Descompacte no celular e instale o `app-debug.apk` (permita “instalar apps desconhecidos” quando o Android pedir).
6. Se ficar vermelho, abra o passo “Compilar APK de teste”, copie as linhas com `error:` e me mande.
