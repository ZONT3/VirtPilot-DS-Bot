package ru.vpilot.dsbot.loops;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import ru.vpilot.dsbot.Main.Config;
import ru.vpilot.dsbot.tools.TMemberList;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LMemberList extends LoopAdapter {
    public LMemberList(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void loop() throws Throwable {
        Config config = ConfigCaster.cast(getContext().getConfig());
        TextChannel channel = getContext().getTChannel(config.channel_list.get());
        String roles = config.roles_list.get();
        if (channel == null || roles.isEmpty() || roles.equals("0")) return;

        ArrayList<EmbedBuilder> list = TMemberList.Msg.list(
                getContext().getGuild(),
                Commons.getIDs(roles));

        List<Message> messages = channel.getHistory().retrievePast(50).complete();
        for (Message msg: messages) {
            List<MessageEmbed> embeds = msg.getEmbeds();
            if (embeds.size() == 1) {
                EmbedBuilder similar = findSimilar(embeds.get(0), list);
                if (similar != null) {
                    msg.editMessage(similar.build()).queue();
                    list.remove(similar);
                    continue;
                }
            }
            msg.delete().queue();
        }

        ZDSBMessages.sendSplit(channel, list);
    }

    private static EmbedBuilder findSimilar(MessageEmbed msg, List<EmbedBuilder> findIn) {
        String description = msg.getDescription();
        if (description == null) return null;

        int i = description.indexOf("\n");
        if (i < 0) return null;

        String s = description.substring(0, i);
        List<EmbedBuilder> list = findIn.stream().filter(b -> {
            String d = b.build().getDescription();
            return d != null && d.startsWith(s);
        }).collect(Collectors.toList());


        if (list.size() != 1) return null;
        else return list.get(0);
    }

    @Override
    public long getPeriod() {
        return 12 * 60 * 60 * 1000;
    }

    @Override
    public boolean runInGlobal() {
        return false;
    }

    @Override
    public boolean runInLocal() {
        return true;
    }
}
