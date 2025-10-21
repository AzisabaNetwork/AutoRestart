package net.azisaba.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AutoRestart extends JavaPlugin {

    private LocalTime restartTime;
    private List<Integer> notifyBeforeList;
    private BukkitRunnable restartTask;
    private BukkitRunnable countdownTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        startRestartScheduler();
        getLogger().info("AutoRestart が有効になりました。");
    }

    @Override
    public void onDisable() {
        if (restartTask != null) restartTask.cancel();
        if (countdownTask != null) countdownTask.cancel();
        getLogger().info("AutoRestart が無効になりました。");
    }

    private void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();

        // 再起動時刻（例: 04:00）
        String timeString = config.getString("restart-time", "04:00");
        restartTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));

        // 通知タイミング（分単位）
        notifyBeforeList = config.getIntegerList("notify-before");
        if (notifyBeforeList.isEmpty()) {
            notifyBeforeList = Arrays.asList(5, 1); // デフォルト
        }

        getLogger().info("再起動予定時刻: " + restartTime);
        getLogger().info("通知タイミング(分前): " + notifyBeforeList);
    }

    private void startRestartScheduler() {
        if (restartTask != null) restartTask.cancel();

        restartTask = new BukkitRunnable() {
            private final Set<Integer> notifiedMinutes = new HashSet<>();

            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                long secondsUntilRestart = java.time.Duration.between(now, restartTime).getSeconds();

                // 翌日補正
                if (secondsUntilRestart < 0) {
                    secondsUntilRestart += 24 * 60 * 60;
                }

                // 通知処理（分前）
                for (int minutesBefore : notifyBeforeList) {
                    long targetSeconds = minutesBefore * 60L;
                    if (secondsUntilRestart <= targetSeconds && !notifiedMinutes.contains(minutesBefore)) {
                        String msg = "§e[AutoRestart] §fサーバーは §c" + minutesBefore + "分後§fに再起動します。";
                        Bukkit.broadcastMessage(msg);
                        getLogger().info("[AutoRestart] " + minutesBefore + "分前通知を送信しました。");
                        notifiedMinutes.add(minutesBefore);
                    }
                }

                // カウントダウン開始（60秒未満で一度だけ）
                if (secondsUntilRestart <= 60 && countdownTask == null) {
                    startCountdown();
                }

                // 再起動処理
                if (secondsUntilRestart <= 0) {
                    Bukkit.broadcastMessage("§c[AutoRestart] サーバーを再起動します！");
                    cancel();
                    Bukkit.shutdown();
                }
            }
        };

        // 1秒ごとにチェック
        restartTask.runTaskTimer(this, 20L, 20L);
    }

    private void startCountdown() {
        countdownTask = new BukkitRunnable() {
            int seconds = 60;

            @Override
            public void run() {
                if (seconds == 60 || seconds == 30 || seconds == 10) {
                    getLogger().info("[AutoRestart] サーバーは " + seconds + "秒後に再起動します。");
                }
                if (seconds <= 0) {
                    cancel();
                }
                seconds--;
            }
        };
        countdownTask.runTaskTimer(this, 0L, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("autorestart")) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loadSettings();
            if (restartTask != null) restartTask.cancel();
            if (countdownTask != null) countdownTask.cancel();
            startRestartScheduler();

            sender.sendMessage("§a[AutoRestart] 設定をリロードしました。");
            sender.sendMessage("§7次回再起動時刻: §b" + restartTime);
            sender.sendMessage("§7通知タイミング(分前): §b" + notifyBeforeList);
            return true;
        }

        sender.sendMessage("§e/autorestart reload §7- 設定をリロードします。");
        return true;
    }
}
