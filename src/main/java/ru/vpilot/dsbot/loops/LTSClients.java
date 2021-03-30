package ru.vpilot.dsbot.loops;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Globals;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.Commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.vpilot.dsbot.Strings.STR;

public abstract class LTSClients extends LoopAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LTSClients.class);

    public static final String DS_BOT_NAME = "DS Bot";
    private TS3Query query;
    private TS3Api api;

    private Message tsStatus;
    private int lastCount = -1;
    private String title;
    private String channelID;
    private String clientsChannelID;
    private String tsClientsLabel;
    private String footer;

    public LTSClients(ZDSBot.GuildContext context) {
        super(context);
    }

    protected abstract List<String> getTsqConnection();

    public abstract String getTitle();

    public abstract String getCountChannelID();

    protected abstract String getClientsChannelID();

    protected abstract String getCountLabel();

    protected abstract String getFooter();

    @Override
    public void prepare() {
        this.title = getTitle();
        this.channelID = getCountChannelID();
        clientsChannelID = getClientsChannelID();
        tsClientsLabel = getCountLabel();
        footer = getFooter();

        for (String s: getTsqConnection())
            if (s == null) throw new NullPointerException("One of setup fields");

        createQuery();
        prepareMessage();
    }

    private void createQuery() {
        TS3Config config = new TS3Config();
        config.setHost(Globals.tsqHost);
        query = new TS3Query(config);
        query.connect();
        api = query.getApi();
        api.login(Globals.tsqLogin, Globals.tsqPass);
        api.selectVirtualServerById(1, DS_BOT_NAME);
    }

    private void prepareMessage() {
        final TextChannel channel = getContext().getTChannel(clientsChannelID);
        for (Message message: channel.getHistory().retrievePast(50).complete()) {
            final List<MessageEmbed> embeds = message.getEmbeds();
            if (embeds.size() < 1) continue;
            final String title = embeds.get(0).getTitle();
            if (title != null && title.equals(this.title))
                tsStatus = message;
        }
        if (tsStatus != null) return;

        tsStatus = channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(title)
                        .build()).complete();
    }

    @Override
    public void loop() throws Throwable {
        List<Client> clients;
        try {
            clients = api.getClients();
        } catch (Exception e) {
            ErrorReporter.printStackTrace(e, getClass());
            LOG.warn("Failed to get clients, retrying...");
            try {
                try {
                    api.logout();
                    query.exit();
                } catch (Exception ignored) {
                    LOG.warn("Cannot disconnect previous query");
                }
                createQuery();
                clients = api.getClients();
            } catch (Exception exception) {
                LOG.error("Cannot reconnect!");
                throw exception;
            }
        }

        List<Client> finalClients = clients;
        Commons.tryReport(getContext(), getClass(), () -> updTSStatus(finalClients, tsStatus));
        Commons.tryReport(getContext(), getClass(), () ->  updClients(finalClients, channelID));
    }

    private void updClients(List<Client> clients, String channelID) {
        GuildChannel channel = getContext().getChannel(channelID);

        int count = getClientCount(clients);
        if (count == lastCount) return;

        channel.getManager().setName(String.format(tsClientsLabel, count))
                .complete();
        lastCount = count;
    }

    private int getClientCount(List<Client> clients) {
        int i = 0;
        for (Client client: clients)
            if (!client.isServerQueryClient()) i++;
        return i;
    }

    private void updTSStatus(List<Client> clients, Message message) {
        HashMap<Integer, ArrayList<Client>> channels = new HashMap<>();
        for (Client client: clients) {
            if (client.isServerQueryClient()) continue;
            final ArrayList<Client> list = channels.getOrDefault(client.getChannelId(), new ArrayList<>());
            list.add(client);
            channels.put(client.getChannelId(), list);
        }

        final EmbedBuilder builder = new EmbedBuilder()
                .setColor(0x00c8ff)
                .setFooter(footer, "https://icons.iconarchive.com/icons/papirus-team/papirus-apps/256/teamspeak-3-icon.png")
                .setTitle(title);
        if (channels.isEmpty()) builder.setDescription(STR.getString("ts_status.no_one"));
        for (Map.Entry<Integer, ArrayList<Client>> e: channels.entrySet()) {
            StringBuilder sb = new StringBuilder();
            for (Client client: e.getValue())
                sb.append(client.getNickname()).append('\n');
            builder.addField(api.getChannelInfo(e.getKey()).getName(), sb.toString(), false);
        }

        message.editMessage(builder.build()).queue();
    }

    @Override
    public long getPeriod() {
        return 15 * 1000;
    }

    @Override
    public boolean runInGlobal() {
        return true;
    }

    @Override
    public boolean runInLocal() {
        return false;
    }
}
