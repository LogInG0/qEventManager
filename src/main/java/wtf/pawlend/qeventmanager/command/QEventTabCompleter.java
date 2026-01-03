package wtf.pawlend.qeventmanager.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import wtf.pawlend.qeventmanager.api.EventInfo;
import wtf.pawlend.qeventmanager.qEventManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QEventTabCompleter implements TabCompleter {

    private final qEventManager plugin;
    private final List<String> subCommands = Arrays.asList(
            "help", "list", "vote", "start", "stop", "rand", "startvote",
            "reload", "status", "schedules", "forceinterval"
    );

    public QEventTabCompleter(qEventManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.addAll(subCommands.stream()
                    .filter(cmd -> cmd.startsWith(input))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (subCommand.equals("start") || subCommand.equals("vote")) {
                completions.addAll(plugin.getEventRegistry().getAllEventInfo().stream()
                        .map(EventInfo::getId)
                        .filter(id -> id.toLowerCase().startsWith(input))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}