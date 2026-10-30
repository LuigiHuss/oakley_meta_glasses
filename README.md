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
