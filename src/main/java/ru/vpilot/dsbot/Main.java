package ru.vpilot.dsbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import ru.vpilot.dsbot.loops.LMemberList;
import ru.vpilot.dsbot.tools.MemberList;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.ZDSBotBuilder;
import ru.zont.dsbot2.commands.implement.Clear;
import ru.zont.dsbot2.commands.implement.Help;
import ru.zont.dsbot2.tools.ZDSBMessages;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    private static String getVersion() {
        Properties properties = new Properties();
        try {
            properties.load(Main.class.getResourceAsStream("/version.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties.getProperty("version", "UNKNOWN");
    }

    public static class Config extends ru.zont.dsbot2.Config {
        public final Entry channel_list = new Entry("0");
        public final Entry roles_list = new Entry("0");

        public Config() {
            super.prefix = new Entry("p.");
            super.channel_log = new Entry("814472065574109184", true);
            super.version = new Entry(getVersion(), true);
            super.version_str = new Entry("VirtPil BOT v.%s", true);
            super.approved_guilds = new Entry("785203451797569626,620965426381324288", true);
        }
    }

    public static void main(String[] args) throws LoginException, InterruptedException {
        ZDSBotBuilder builder = new ZDSBotBuilder(args[0])
                .defaultSetup()
                .setConfig(new Config())
                .addCommands(Help.class,
                        Clear.class
                )
                .addLoops(LMemberList.class)
                .addListeners();

        builder.getJdaBuilder().enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);

        ZDSBot bot = builder.build();
//
//        Guild guild = bot.getJda().getGuilds().stream().filter(g -> g.getId().equals("785203451797569626")).findAny().get();
//        ZDSBot.GuildContext gc = bot.forGuild(guild);
//        assert gc != null;
//        TextChannel ch = bot.getTChannel(gc.getGlobalConfig().channel_log.get());
//        ArrayList<EmbedBuilder> builders = MemberList.Msg.list(guild, List.of("785204064900218882", "785233488725934162", "814496006770917466"));
//        ZDSBMessages.sendSplit(ch, builders);
    }
}
