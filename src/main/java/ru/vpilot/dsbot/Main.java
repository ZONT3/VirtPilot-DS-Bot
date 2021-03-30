package ru.vpilot.dsbot;

import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.api.requests.GatewayIntent;
import ru.vpilot.dsbot.commands.Media;
import ru.vpilot.dsbot.http.EmbedWebhook;
import ru.vpilot.dsbot.http.ReportHandler;
import ru.vpilot.dsbot.listeners.Greetings;
import ru.vpilot.dsbot.loops.LMedia;
import ru.vpilot.dsbot.loops.LMemberList;
import ru.vpilot.dsbot.loops.LTSClients;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.ZDSBotBuilder;
import ru.zont.dsbot2.commands.implement.Clear;
import ru.zont.dsbot2.commands.implement.Help;
import ru.zont.dsbot2.commands.implement.Say;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
        public final Entry role_checked = new Entry("0");
        public final Entry message_checkpoint = new Entry("0");
        public final Entry channel_streams = new Entry("0");
        public final Entry channel_posts = new Entry("0");
        public final Entry channel_video = new Entry("0");
        public final Entry channel_report = new Entry("0");
        public final Entry channel_ts = new Entry("0");
        public final Entry channel_ts_clients = new Entry("0");

        public Config() {
            super.prefix = new Entry("p.");
            super.channel_log = new Entry("814472065574109184", true);
            super.version = new Entry(getVersion(), true);
            super.version_str = new Entry("VirtPil BOT v.%s", true);
            super.approved_guilds = new Entry("785203451797569626,620965426381324288", true);
        }
    }

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        handleArgs(args);

        ZDSBotBuilder builder = new ZDSBotBuilder(args[0])
                .defaultSetup()
                .setConfig(new Config())
                .addCommands(Help.class,
                        Clear.class, Say.class,
                        Media.class
                )
                .addLoops(LMemberList.class, LMedia.class, LTSClients.class)
                .setTechAdmins(List.of("375638389195669504", "331524458806247426"))
                .addListeners(new Greetings());

        builder.getJdaBuilder().enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);

        ZDSBot bot = builder.build();
        bot.getJda().awaitReady();


        setupWebServer(bot.getVoidGuildContext());
    }

    private static void handleArgs(String[] args) throws LoginException {
        if (args.length < 4) throw new LoginException("Not enough args");

        Globals.TWITCH_API_SECRET = args[1];
        Globals.GOOGLE_API = args[2];

        String[] split = args[3].split(";");
        if (split.length != 3) throw new LoginException("TSQuery Connect string is invalid");
        Globals.tsqHost  = split[0];
        Globals.tsqLogin = split[1];
        Globals.tsqPass  = split[2];
    }

    private static void setupWebServer(ZDSBot.GuildContext bot) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 13370), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/postForm", new ReportHandler(bot));
        server.createContext("/embed", new EmbedWebhook(bot));
        server.setExecutor(threadPoolExecutor);
        server.start();
    }
}
