package ru.vpilot.dsbot.listeners;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static ru.zont.dsbot2.tools.ZDSBMessages.*;
import static ru.zont.dsbot2.tools.ZDSBStrings.*;

public class Greetings extends ListenerAdapter {
    public static final String ID_ROLE           = "809358190813773836";
    public static final String ID_CHANNEL_CP     = "637552217762562050";
    public static final String ID_MSG_CP         = "814382443695046706";
    public static final String ID_CHANNEL_CP_MSG = "814043680775471136";
    public static final String ID_GUILD          = "620965426381324288";
    private String checkpointID;
    private Role role;

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) { // TODO Instantiate all the shit
        checkpointID = ID_MSG_CP;
        Guild guild = event.getGuild();
        final Message message = guild.getTextChannelById(ID_CHANNEL_CP_MSG).retrieveMessageById(checkpointID).complete();

        addOK(message);

        role = event.getJDA().getRoleById(ID_ROLE);
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        if (!event.getMessageId().equals(checkpointID)) return;
        if (!event.getReactionEmote().getEmoji().equals(EMOJI_OK)) return;
        addRole(event.getGuild(), event.getMember());
    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        if (event.getUser() != null && event.getUser().isBot()) return;
        if (!event.getMessageId().equals(checkpointID)) return;
        if (!event.getReactionEmote().getEmoji().equals(EMOJI_OK)) return;
        rmRole(event.getGuild(), event.getMember());
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!event.getGuild().getId().equals(ID_GUILD)) return;
        final TextChannel channel = event.getGuild().getTextChannelById(ID_CHANNEL_CP);
        final TextChannel bureau  = event.getGuild().getTextChannelById(ID_CHANNEL_CP_MSG);

        if (channel == null || bureau == null) return;

        if (channel.getGuild().getIdLong() != event.getGuild().getIdLong()) return;

        final String memberMention = event.getUser().getAsMention();
        final String bureauMention = bureau.getAsMention();
        channel.sendMessage(String.format(STR.getString("checkpoint.greetings"), memberMention)).queue();
        event.getUser().openPrivateChannel().complete()
                .sendMessage(String.format(STR.getString("checkpoint.greetings.pm"),
                        memberMention, bureauMention)).queue();
    }

    private synchronized void addRole(Guild guild, Member member) {
        guild.addRoleToMember(member, role).complete();
    }

    private synchronized void rmRole(Guild guild, Member member) {
        guild.removeRoleFromMember(member, role).complete();
    }
}
