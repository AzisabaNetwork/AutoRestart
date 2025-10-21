package net.azisaba.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

public class RestartTask {

    private final AutoRestart plugin;
    private final List<String> restartTimes;
    private final List<Integer> notifyBefore;

    public RestartTask(AutoRestart plugin) {
        this.plugin = plugin;
        this.restartTimes = plugin.getConfig().getStringList("restart-times");
        this.notifyBefore = plugin.getConfig().getIntegerList("notify-before");
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    LocalTime now = LocalTime.now().withSecond(0).withNano(0);

                    for (String timeStr : restartTimes) {
                        LocalTime target;
                        try {
                            target = LocalTime.parse(timeStr);
                        } catch (Exception e) {
                            plugin.getLogger().warning("時間形式が不正です: " + timeStr);
                            continue;
                        }

                        long minutesUntil = Duration.between(now, target).toMinutes();
                        if (minutesUntil < 0) minutesUntil += 24 * 60; // 翌日補正

                        // --- 通知処理 ---
                        for (int before : notifyBefore) {
                            if (minutesUntil == before) {
                                Bukkit.broadcastMessage("§e[AutoRestart] §fサーバーは §c" + before + "分後§f に再起動します。");
                                plugin.getLogger().info("再起動まで " + before + "分。");
                            }
                        }

                        // --- 1分未満カウントダウン（コンソール出力用） ---
                        long secondsUntil = Duration.between(LocalTime.now(), target).getSeconds();
                        if (secondsUntil < 0) secondsUntil += 24 * 3600; // 翌日補正

                        if (secondsUntil == 60) plugin.getLogger().info("再起動まで 1分。");
                        if (secondsUntil == 30) plugin.getLogger().info("再起動まで 30秒。");
                        if (secondsUntil == 10) plugin.getLogger().info("再起動まで 10秒。");

                        // --- 実際の再起動 ---
                        if (minutesUntil == 0 && secondsUntil == 0) {
                            Bukkit.broadcastMessage("§c[AutoRestart] §fサーバーを再起動します。");
                            plugin.getLogger().info("=== サーバーを再起動します ===");
                            Bukkit.shutdown();
                        }
                    }

                } catch (Exception e) {
                    plugin.getLogger().severe("AutoRestart タスク中にエラー: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 毎秒チェック
    }

    public void cancel() {
        // BukkitRunnable の停止はスコープ外のため特に不要
    }
}
