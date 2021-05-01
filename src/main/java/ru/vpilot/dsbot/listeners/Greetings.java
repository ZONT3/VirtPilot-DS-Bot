package ru.vpilot.dsbot.listeners;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static ru.vpilot.dsbot.Strings.*;

public class Greetings extends ListenerAdapter {
    public static final String ID_ROLE_CHECKED   = "809358190813773836";
    public static final String ID_ROLE_MEDIA     = "819138179722248193";
    public static final String ID_ROLE_FGN       = "836898707793510400";
    public static final String ID_CHANNEL_CP     = "637552217762562050";
    public static final String ID_MSG_CP         = "814552497468473376";
    public static final String ID_MSG_MEDIA      = "826390523325579275";
    public static final String ID_MSG_FGN        = "838065483395498028";
    public static final String ID_CHANNEL_CP_MSG = "814043680775471136";
    public static final String ID_GUILD          = "620965426381324288";
    public static final String EMOJI             = "U+1F6EC";
    private Role roleChecked;
    private Role roleMedia;
    private Role roleFGN;

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) { // TODO Instantiate all the shit
        Guild guild = event.getGuild();
        final Message message = guild.getTextChannelById(ID_CHANNEL_CP_MSG).retrieveMessageById(ID_MSG_CP).complete();

        message.addReaction(EMOJI).complete();

        roleChecked = event.getJDA().getRoleById(ID_ROLE_CHECKED);
        roleMedia = event.getJDA().getRoleById(ID_ROLE_MEDIA);
        roleFGN = event.getJDA().getRoleById(ID_ROLE_FGN);
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot()) return;
        switch (event.getMessageId()) {
            case ID_MSG_CP -> addRole(event.getGuild(), event.getMember(), roleChecked);
            case ID_MSG_MEDIA -> addRole(event.getGuild(), event.getMember(), roleMedia);
            case ID_MSG_FGN -> addRole(event.getGuild(), event.getMember(), roleFGN);
        }

    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        if (event.getUser() != null && event.getUser().isBot()) return;
        switch (event.getMessageId()) {
            case ID_MSG_CP -> rmRole(event.getGuild(), event.getMember(), roleChecked);
            case ID_MSG_MEDIA -> rmRole(event.getGuild(), event.getMember(), roleMedia);
            case ID_MSG_FGN -> rmRole(event.getGuild(), event.getMember(), roleFGN);
        }
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

    private synchronized void addRole(Guild guild, Member member, Role role) {
        guild.addRoleToMember(member, role).complete();
    }

    private synchronized void rmRole(Guild guild, Member member, Role role) {
        guild.removeRoleFromMember(member, role).complete();
    }
}
