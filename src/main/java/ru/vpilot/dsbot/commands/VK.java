package ru.vpilot.dsbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import ru.vpilot.dsbot.Globals;
import ru.vpilot.dsbot.Strings;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.commands.CommandAdapter;
import ru.zont.dsbot2.commands.Input;
import ru.zont.dsbot2.commands.UserInvalidInputException;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VK extends CommandAdapter {
    public static final String REGEX = "[\\w\\d_-]+";

    public VK(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(Input input) {
        Commons.rolesLikeRouter(0, this::set, this::rm, this::get).acceptInput(input);
    }

    private String assertGroup(String inpt) {
        if (inpt.matches(REGEX)) return inpt;

        final Matcher matcher = Pattern.compile("https?://vk\\.com/(" + REGEX + ")/?").matcher(inpt);
        if (!matcher.find())
            throw new UserInvalidInputException(Strings.STR.getString("vk.err.input"));
        return matcher.group(1);
    }

    private void get(Input input) {
        final LinkedList<String> list = Globals.vk2dis.list();

        final ArrayList<EmbedBuilder> builders = new ArrayList<>();
        builders.add(new EmbedBuilder().setTitle("VK").setColor(0x2787F5));
        for (String s: list)
            ZDSBMessages.appendDescriptionSplit("[%1$s](https://vk.com/%1$s)\n".formatted(s), builders);

        ZDSBMessages.sendSplit(input.getChannel(), builders);
    }

    private void rm(Input input) {
        Globals.vk2dis.rm(assertGroup(input.getArg(1)));
        ZDSBMessages.addOK(input.getMessage());
    }

    private void set(Input input) {
        Globals.vk2dis.add(assertGroup(input.getArg(1)));
        ZDSBMessages.addOK(input.getMessage());
    }

    @Override
    public String getCommandName() {
        return "vk";
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
        return "vk (add|set|rm|del|get|list) ...";
    }

    @Override
    public String getDescription() {
        return Strings.STR.getString("comms.media.desc");
    }
}
