# AutoRestart
**Version:** 1.2.0<br>
**Native Minecraft Version:** 1.16.5<br>
**Author:** pino223<br>
**LICENSE:** [GPL-3.0](LICENSE)<br>
## 概要
指定した時間にサーバーをstopする<br>
また、指定した時間に設定したコマンドを実行する
## 設定
configにて
- **restart-time:**<br>
24時間制で指定した時間にサーバーをstopする
- **notify-before:**<br>
ここに書いた分前に全体チャット、コンソールに通知する
- **scheduled-commands:**<br>
  好きなコマンドを指定した時刻に実行する<br>
  `time:` 時刻指定<br>
  `command: ""` コマンド指定 
## コマンド
**/autorestart reload**<br>
設定を再読み込みする
## 権限
autorestart.reload
