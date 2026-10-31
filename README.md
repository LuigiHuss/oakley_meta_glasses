# Meta Glasses — Oakley Meta HSTN

Projeto privado para integrar um aplicativo Android próprio aos óculos **Oakley Meta HSTN** usando o **Meta Wearables Device Access Toolkit (DAT)**.

## Estado atual

- [x] Modo de desenvolvedor habilitado no Meta AI (Android).
- [x] DAT presente nos óculos: versão exibida 1.0.0.0.
- [x] Repositório GitHub autorizado para colaboração.
- [ ] Compilar o **APK de diagnóstico**.
- [ ] Instalar o APK no Android e concluir o registro no Meta AI.
- [ ] Iniciar uma sessão com os óculos e testar captura de foto/vídeo.
- [ ] Desenvolver app próprio: voz, memória, finanças, tarefas, calendário, mídia.

## Primeiro APK: diagnóstico oficial

Para reduzir risco nesta primeira etapa, o GitHub Actions compila diretamente o exemplo **CameraAccess** publicado pela Meta. Ele permite testar a autorização dos óculos, iniciar uma sessão, visualizar o vídeo e capturar fotos.

O arquivo em `.github/workflows/android-diagnostico.yml` automatiza a compilação de um **APK debug**, sem exigir computador. Ele busca o exemplo oficial no GitHub da Meta e compila usando as dependências declaradas pelo projeto original. **Este APK é de diagnóstico, não é ainda o aplicativo personalizado Meta Glasses.**

### Como baixar pelo celular

1. Abra a aba **Actions** do repositório.
2. Selecione **Android — APK de diagnóstico Meta**.
3. Abra a execução mais recente com **ícone verde** (sucesso).
4. Na seção **Artifacts**, baixe `Meta-Glasses-Diagnostico-APK`.
5. Extraia o ZIP no Android e instale `MetaGlasses-Diagnostico.apk`. Se o Android solicitar, autorize temporariamente a instalação pelo aplicativo de arquivos/navegador utilizado.
6. Com os óculos ligados, próximos e o Meta AI aberto, inicie o aplicativo, toque em **Connect** e conclua a autorização no Meta AI.
7. Volte ao app, toque em **Start Session**, depois **Preview**; teste foto e vídeo.

> Um APK debug instalado por fora da Play Store exige autorização manual do Android. Instale apenas o artefato gerado na sua própria execução do GitHub Actions.

## Diagnóstico se der erro

- Confira se o HSTN está ligado, com hastes abertas, perto do celular e conectado pelo Meta AI.
- Verifique se **Modo de Desenvolvedor** continua ativado e aparece uma versão de **DAT SDK** no Meta AI.
- Certifique-se de permitir o Bluetooth e as permissões solicitadas na autorização dos óculos.
- Se a compilação falhar, abra a execução em **Actions** e copie a primeira mensagem de erro em vermelho.

## Segurança e limites

- O APK inicial usa credenciais de desenvolvimento `0` / `0`, apenas com **Developer Mode** ativo; não são chaves privadas.
- **Não** coloca chave OpenAI, senha ou credenciais reais de usuário no código.
- **Não** contém banco de dados, sincronização, serviços externos ou assistente ChatGPT.
- Fotos e vídeos produzidos no teste ficam sob controle do aplicativo de exemplo; não há upload para nosso backend.
- O exemplo oficial é compilado a partir do branch `main` da Meta. Após o primeiro build validado, registraremos o commit exato do código upstream e o fixaremos para reproduções posteriores.
- O uso do SDK oficial está sujeito à disponibilidade regional e às permissões do dispositivo.

## Código-fonte e referência

- Meta DAT Android: https://github.com/facebook/meta-wearables-dat-android
- Exemplo CameraAccess: https://github.com/facebook/meta-wearables-dat-android/tree/main/samples/CameraAccess
- Documentação: https://wearables.developer.meta.com/docs/develop/
- O exemplo possui licença própria no repositório original. Nosso workflow não copia nem publica os fontes da Meta neste repositório.


## Versão experimental v0.2 — perfil LOW24 para comparar fluidez

A partir do commit de otimização, o workflow **Android - Camera Access otimizado v0.2** gera um segundo APK, `MetaGlasses-Otimizado-LOW24.apk`, com estes ajustes na cópia local do CameraAccess oficial:

- **Stream de vídeo**: LOW (360 x 640), 24 FPS solicitados e compressão HEVC mantida.
- **Microfone inicialmente desligado**: não solicita a rota HFP do áudio durante o Preview. Para gravar **com áudio**, habilite manualmente o ícone de microfone antes de iniciar Record; nessa condição, o compartilhamento de banda Bluetooth pode voltar a causar engasgos.
- **Aplicativo separado**: `applicationId = dev.luigihuss.metaglasses.optimized` para permitir instalar lado a lado com o Camera Access anterior; será necessário autorizar o novo app no Meta AI (Developer Mode ligado).
- O script `scripts/optimize_cameraaccess.py` confere as linhas de código esperadas e interrompe a compilação se a Meta alterar essa parte do exemplo.
- O build continua puxando código atual da Meta, registra o commit original no log e publica o APK no **Artifacts** da execução.

### Comparação recomendada (mesmo local e iluminação)

1. No app **original**: Preview por 20 s com microfone desativado, depois vídeo de 15 s com áudio; observe travadas e dessincronização.
2. No **otimizado v0.2**: conecte os óculos no novo app, inicie sessão e Preview com o microfone desligado; repita os 20 s.
3. Ligue o ícone do microfone e grave 15 s com áudio; salve/compartilhe, reproduza externamente e observe se imagem e voz estão sincronizadas.
4. Compare os arquivos em reprodução normal no celular. O novo perfil terá resolução inferior; melhora de fluidez **não é garantida** se o gargalo for o link Bluetooth/HFP ou irregularidade dos timestamps.
5. Compartilhe o resultado do teste antes de novas mudanças. Preserve o app original para rollback.

### Download privado no Android

Acesse [Actions](https://github.com/LuigiHuss/oakley_meta_glasses/actions), escolha a execução verde do workflow **Android - Camera Access otimizado v0.2**, baixe o artefato `Meta-Glasses-Otimizado-LOW24-APK`, extraia o ZIP e instale `MetaGlasses-Otimizado-LOW24.apk`.

**Limites:** a gravação via DAT Android não é igual à gravação nativa dos óculos; o vídeo ainda depende da transmissão Bluetooth. O modo LOW24 é um teste de banda/latência, não uma solução definitiva. Não há envio de mídia, áudio ou dados financeiros à nuvem.
