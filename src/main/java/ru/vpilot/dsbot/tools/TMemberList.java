package ru.vpilot.dsbot.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import ru.vpilot.dsbot.Strings;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TMemberList {

    public static class Msg {
        public static ArrayList<EmbedBuilder> list(Guild guild, List<String> roles) {
            ArrayList<EmbedBuilder> builders = new ArrayList<>();

            for (String role: roles) {
                Role roleObj = guild.getRoleById(role);
                if (roleObj == null) continue;

                String roleTitle = Strings.STR.getString("memlist.title", roleObj.getAsMention()) + "\n";
                builders.add(new EmbedBuilder().setColor(roleObj.getColorRaw()));
                String[] strings = listRole(guild, role, Strings.STR.getString("memlist.role.prefix"), MessageEmbed.TEXT_MAX_LENGTH - roleTitle.length() - 5);

                for (String string: strings)
                    ZDSBMessages.appendDescriptionSplit(roleTitle + string, builders);
            }

            return builders;
        }

        private static String[] listRole(Guild g, String role, String prefix, int maxlen) {
            List<Member> members = g.findMembers(
                    m -> m.getRoles().stream().anyMatch(
                            r -> r.getId().equals(role)
                    )
            ).get();

            LinkedList<String> list = new LinkedList<>();
            StringBuilder sb = new StringBuilder();
            for (Member member: members) {
                String app = prefix + member.getAsMention() + " `%s`".formatted(member.getUser().getAsTag()) + "\n";
                if (app.length() + sb.length() > maxlen) {
                    list.add(sb.toString());
                    sb = new StringBuilder(app);
                } else sb.append(app);
            }
            if (!sb.toString().isEmpty())
                list.add(sb.toString());

            return list.toArray(new String[0]);
        }
    }
}
