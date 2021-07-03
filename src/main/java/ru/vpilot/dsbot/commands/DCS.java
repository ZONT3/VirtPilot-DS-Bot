package ru.vpilot.dsbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import ru.vpilot.dsbot.Strings;
import ru.vpilot.dsbot.loops.LDCSServers;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.commands.CommandAdapter;
import ru.zont.dsbot2.commands.Input;
import ru.zont.dsbot2.commands.UserInvalidInputException;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;
import ru.zont.dsbot2.tools.ZDSBStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.vpilot.dsbot.loops.LDCSServers.servers;

public class DCS extends CommandAdapter {
    private static final String PATTERN_IPv4 = "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(:\\d{1,5})?$";

    public DCS(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(Input input) {
        Commons.rolesLikeRouter(0, this::set, this::rm, this::get).acceptInput(input);
    }

    @Override
    public String getCommandName() {
        return "dcs";
    }

    @Override
    public boolean checkPermission(Input input) {
        return Commons.rolesLikePermissions(input, List.of("get", "list"));
    }

    @Override
    public boolean allowPM() {
        return false;
    }

    @Override
    public boolean allowForeignGuilds() {
        return false;
    }

    @Override
    public String getSynopsis() {
        return "dcs (add|set|rm|del|get|list) ...";
    }

    @Override
    public String getDescription() {
        return Strings.STR.getString("comms.dcs.desc");
    }

    private void set(Input input) {
        String link = getLink(input);
        servers.op(list -> {
            String ref = toReference(link);
            if (!list.contains(ref))
                list.add(ref);
        });
        ZDSBMessages.addOK(input.getMessage());
        getContext().getBot().getVoidGuildContext().tickLoop(LDCSServers.class);
    }

    private void rm(Input input) {
        String link = getLink(input);
        boolean bool = servers.op(new ArrayList<>(), list -> {
            if (list.remove(toReference(link))) {
                ZDSBMessages.addOK(input.getMessage());
                return true;
            } else {
                ZDSBMessages.printError(input.getChannel(), Strings.STR.getString("err"), Strings.STR.getString("dcs.err.rm"));
                return false;
            }
        });
        if (bool) getContext().getBot().getVoidGuildContext().tickLoop(LDCSServers.class);
    }

    private void get(Input input) {
        ArrayList<EmbedBuilder> builders = new ArrayList<>();
        builders.add(new EmbedBuilder().setTitle(Strings.STR.getString("dcs.list")));
        servers.getData().parallelStream().sorted().forEach(s -> ZDSBMessages.appendDescriptionSplit(listEntry(s), builders));
        ZDSBMessages.sendSplit(input.getChannel(), builders);
    }

    private String listEntry(String ip) {
        final LDCSServers.DCSServerData data = LDCSServers.findData(ip);

        return String.format(" - `%s` %s\n", ip, data != null ? data.name : "**Cannot find server**");
    }

    private String toReference(String link) {
        final Matcher matcher = Pattern.compile(PATTERN_IPv4).matcher(link);
        if (!matcher.find()) throw new UserInvalidInputException(Strings.STR.getString("dcs.err.ip"));

        final String[] split = link.split(":");
        if (split.length <= 1) link = link + ":" + LDCSServers.DEFAULT_PORT;
        return link;
    }

    @NotNull
    private String getLink(Input input) {
        String link = input.getArg(1);
        if (link == null) throw new UserInvalidInputException(ZDSBStrings.STR.getString("err.insufficient_args"));
        return link;
    }
}
