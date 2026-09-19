package com.nexuscraft.nexuspurgatory;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Locale;

/** {@code /nexuspurgatory} -- admin-only (nexuspurgatory.admin). No pardon/register subcommand
 *  exists on purpose: this plugin has exactly one gate, config.yml's {@code targets} list, and
 *  exactly one way to open it -- edit that file and run {@code reload}. Anything else would be a
 *  second, easier-to-fat-finger way to curse or release a real player, which is the one thing the
 *  user was most emphatic about avoiding. */
final class CurseCommand implements CommandExecutor {

    private final CurseState state;
    private final Runnable onReload;

    CurseCommand(CurseState state, Runnable onReload) {
        this.state = state;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexuspurgatory.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        return switch (sub) {
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            default -> {
                sender.sendMessage(usage());
                yield true;
            }
        };
    }

    private boolean status(CommandSender sender) {
        long now = Instant.now().getEpochSecond();
        boolean any = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!state.isCursed(player.getName())) {
                continue;
            }
            any = true;
            long seconds = state.secondsCursed(player.getUniqueId(), now);
            int stage = state.config().escalationEnabled
                    ? CurseEscalation.stageFor(seconds, state.config().escalationStageThresholdSeconds)
                    : 0;
            sender.sendMessage("§7[NexusPurgatory] §f" + player.getName() + " §7-- cursed " + formatDuration(seconds)
                    + (stage > 0 ? ", escalation stage " + stage : ""));
        }
        if (!any) {
            sender.sendMessage("§7[NexusPurgatory] §fNo configured target is currently online.");
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        onReload.run();
        sender.sendMessage("§7[NexusPurgatory] §fconfig.yml reloaded -- " + state.config().targets.size()
                + " target(s) configured. (Interval settings need a server restart to take effect; "
                + "targets and severity values apply immediately.)");
        return true;
    }

    private static String formatDuration(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + "m " + seconds + "s";
    }

    private static String usage() {
        return "§7Usage: §f/nexuspurgatory <status|reload>";
    }
}
