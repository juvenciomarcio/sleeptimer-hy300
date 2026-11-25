# Sleep Timer HY300 (timer + desligamento por ADB)

## Gerar o APK
A) Android Studio: abrir a pasta > Build > Build APK(s).
B) Sem Android Studio: crie um repositório no GitHub com o conteúdo desta pasta;
   em Actions > "Build APK" > Run workflow; baixe "SleepTimerHY300-apk".

## Preparar o projetor (uma vez)
1. Ativar Opções do desenvolvedor e "Depuração USB".
2. ADB por rede na porta 5555 (do PC, uma vez): adb tcpip 5555
   (alguns HY300 já vêm com isso ativo; se mudar de rede/reiniciar, repita).
3. Instalar o APK, abrir, tocar "Autorizar ADB (1ª vez)" e marcar "Sempre permitir".
4. Escolher a duração. Ao final o app pausa a mídia e desliga (svc power shutdown / reboot -p).
   Se o ADB falhar, cai para "apagar tela" (administrador de dispositivo ou root).
