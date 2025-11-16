package net.azisaba.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AutoRestart extends JavaPlugin {

    private List<LocalTime> restartTimes = new ArrayList<>();
    private List<Integer> notifyMinutes = new ArrayList<>();
    private Map<LocalTime, List<String>> scheduledCommands = new HashMap<>();
    private Set<String> executedCommandsToday = new HashSet<>();
    private boolean running = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        startScheduler();
        getLogger().info("AutoRestart enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AutoRestart disabled.");
    }

    private void loadSettings() {
        reloadConfig();
        restartTimes.clear();
        notifyMinutes.clear();
        scheduledCommands.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // restart-times
        for (String s : getConfig().getStringList("restart-times")) {
            try {
                restartTimes.add(LocalTime.parse(s, formatter));
            } catch (Exception e) {
                getLogger().warning("Invalid time format: " + s);
            }
        }

        // notify-before
        notifyMinutes.addAll(getConfig().getIntegerList("notify-before"));

        // scheduled-commands
        ConfigurationSection sec = getConfig().getConfigurationSection("scheduled-commands");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String timeStr = sec.getString(key + ".time");
                String command = sec.getString(key + ".command");

                try {
                    LocalTime time = LocalTime.parse(timeStr, formatter);
                    scheduledCommands.computeIfAbsent(time, k -> new ArrayList<>()).add(command);
                } catch (Exception e) {
                    getLogger().warning("Invalid scheduled command time: " + timeStr);
                }
            }
        }

        executedCommandsToday.clear();
    }

    private void startScheduler() {
        if (running) return;
        running = true;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            LocalTime now = LocalTime.now().withSecond(0).withNano(0);

            // 毎日00:00でコマンド実行済みフラグをリセット
            if (now.equals(LocalTime.MIDNIGHT)) {
                executedCommandsToday.clear();
            }

            checkRestartNotifications(now);
            checkScheduledCommands(now);

        }, 20L, 20L); // 1秒に1回実行
    }

    private void checkScheduledCommands(LocalTime now) {
        List<String> commands = scheduledCommands.get(now);
        if (commands == null) return;

        String key = now.toString();
        if (executedCommandsToday.contains(key)) return;

        executedCommandsToday.add(key);

        for (String cmd : commands) {
            getLogger().info("[AutoRestart] " + now + " コマンド実行: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private void checkRestartNotifications(LocalTime now) {
        for (LocalTime rt : restartTimes) {

            long diffSeconds = java.time.Duration.between(now, rt).getSeconds();

            // --- notify-before（分前の全体通知） ---
            if (notifyMinutes.contains((int)(diffSeconds / 60))) {
                Bukkit.broadcastMessage(ChatColor.GOLD
                        + "[AutoRestart] サーバーは " + (diffSeconds / 60) + "分後 に再起動します。");
            }

            // --- 固定の秒前通知（コンソール出力） ---
            if (diffSeconds == 60 || diffSeconds == 30 || diffSeconds == 10) {
                getLogger().info("Restart in " + diffSeconds + " seconds.");
            }

            // --- 再起動実行 ---
            if (diffSeconds == 0) {
                getLogger().info("Restart time reached. Restarting...");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("autorestart")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                sender.sendMessage(ChatColor.GREEN + "[AutoRestart] 設定をリロードしました。");

                sender.sendMessage(ChatColor.GREEN + "次回の再起動時刻:");
                restartTimes.forEach(t -> sender.sendMessage(" - " + t));

                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "/autorestart reload - 設定をリロードします");
            return true;
        }

        return false;
    }
}
