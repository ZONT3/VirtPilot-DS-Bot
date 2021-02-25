package ru.vpilot.dsbot.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import ru.vpilot.dsbot.Strings;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.util.ArrayList;
import java.util.List;

public class MemberList {

    public static class Msg {
        public static ArrayList<EmbedBuilder> list(Guild guild, List<String> roles) {
            ArrayList<EmbedBuilder> builders = new ArrayList<>();

            for (String role: roles) {
                Role roleObj = guild.getRoleById(role);
                if (roleObj == null) continue;

                String roleTitle = Strings.STR.getString("memlist.title", roleObj.getAsMention()) + "\n";
                builders.add(new EmbedBuilder().setColor(roleObj.getColorRaw()));
                String s = listRole(guild, role, Strings.STR.getString("memlist.role.prefix"));

                String[] strings = Commons.splitLength(s, MessageEmbed.TEXT_MAX_LENGTH - roleTitle.length() - 5);
                for (String string: strings)
                    ZDSBMessages.appendDescriptionSplit(roleTitle + string, builders);
            }

            return builders;
        }

        private static String listRole(Guild g, String role, String prefix) {
            List<Member> members = g.findMembers(
                    m -> m.getRoles().stream().anyMatch(
                            r -> r.getId().equals(role)
                    )
            ).get();

            StringBuilder sb = new StringBuilder();
            for (Member member: members)
                sb.append(prefix)
                        .append(member.getAsMention())
                        .append("\n");
            return sb.toString();
        }
    }
}
