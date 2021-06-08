package ru.vpilot.dsbot.loops;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Globals;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.DataList;
import ru.zont.dsbot2.tools.ZDSBStrings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.vpilot.dsbot.Strings.STR.getString;

public class LDCSServers extends LoopAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LDCSServers.class);
    private static final String CHANNEL_ID = "845040939457970207"/*"814502474509451295"*/;
    public static final String DEFAULT_PORT = "10308";

    public static final DataList<String> servers = new DataList<>("dcs_servers");
    private final HashMap<String, String> msgMap = new HashMap<>();

    private static String cachedData = "{}";
    private static long nextCache = 0;

    public LDCSServers(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void loop() throws Throwable {
        final TextChannel channel = getContext().getTChannel(CHANNEL_ID);

        LinkedList<String> updated = new LinkedList<>();
        for (String ip: servers.getData()) {
            String msg = null;
            try {
                msg = update(ip, channel);
            } catch (Exception e) {
                ErrorReporter.inst().reportError(getContext(), getClass(), e);
            }
            if (msg != null) updated.add(msg);
        }

        Commons.clearTextChannel(channel, updated);
    }

    @Override
    public long getPeriod() {
        return 5 * 60 * 1000;
    }

    @Override
    public boolean runInGlobal() {
        return true;
    }

    @Override
    public boolean runInLocal() {
        return false;
    }

    private String update(String ip, TextChannel channel) {
        final DCSServerData data = findData(ip);

        String msgID = msgMap.get(ip);
        Message toEdit = tryFindMsg(msgID, channel);

        if (msgID == null || toEdit == null) {
            msgID = findOrNewMsg(data, channel);
            toEdit = tryFindMsg(msgID, channel);
        }
        if (msgID == null || toEdit == null)
            throw new RuntimeException("Cannot create new message");

        msgMap.put(ip, msgID);

        toEdit.editMessage(buildMsg(data, ip)).queue();
        return msgID;
    }

    private Message tryFindMsg(String id, TextChannel channel) {
        if (id != null) {
            Message msg = null;
            try {
                msg = channel.retrieveMessageById(id).complete();
            } catch (Throwable ignored) { }
            return msg;
        }
        return null;
    }

    private Message buildMsg(DCSServerData data, String ip) {
        if (data != null) {
            return new MessageBuilder(
                    new EmbedBuilder()
                            .setTitle(normalizeTitle(data.name))
                            .addField(getString("dcs.players"), "%d / %d".formatted(data.players, data.playersMax), true)
                            .addField(getString("dcs.mission_time"), data.missionTime, true)
                            .addField(getString("dcs.mission"), data.mission, true)
                            .setDescription(normalizeDesc(data))
                            .setColor(0x1474A6)
            ).build();

        } else {
            final JsonObject cached = JsonParser.parseString(cachedData).getAsJsonObject();
            final int total = cached.has("PLAYERS_COUNT")
                    ? cached.getAsJsonPrimitive("PLAYERS_COUNT").getAsInt()
                    : 0;

            return new MessageBuilder(
                    new EmbedBuilder()
                            .setTitle(getString("dsc.err.noserv.title"))
                            .setDescription(getString("dsc.err.noserv", total, ip))
                            .setColor(0x1474A6)
            ).build();
        }
    }

    private String normalizeDesc(DCSServerData data) {
        return getString("dcs.description.format", data.ip, data.port, ZDSBStrings.trimSnippet(data.description.replaceAll("<br />", "\n").replaceAll("<.+?>", ""), 715));
    }

    public static String normalizeTitle(String name) {
        return ZDSBStrings.trimSnippet(name, 64);
    }

    private String findOrNewMsg(DCSServerData data, TextChannel channel) {
        final List<Message> found = channel.getHistory().retrievePast(100).complete().parallelStream().filter(msg -> {
            if (data == null) return false;
            final List<MessageEmbed> embeds = msg.getEmbeds();
            if (embeds.size() > 0) {
                final String title = embeds.get(0).getTitle();
                return title != null && title.equals(normalizeTitle(data.name));
            } else return false;
        }).collect(Collectors.toList());

        if (found.size() == 1) {
            final Message msg = found.get(0);
            final String id = msg.getId();
            if (!msgMap.containsValue(id))
                return id;
        }


        final Message msg = channel.sendMessage(new EmbedBuilder()
                .setTitle("Fetching data for %s:%s...".formatted(data != null ? data.ip : "???", data != null ? data.port : "?"))
                .build()).complete();
        return msg != null ? msg.getId() : null;
    }

    public static DCSServerData findData(String ip) {
        if (nextCache <= System.currentTimeMillis())
            fetchData();

        try {
            for (JsonElement server: JsonParser.parseString(cachedData).getAsJsonObject().get("SERVERS").getAsJsonArray()) {
                final JsonObject svObj = server.getAsJsonObject();
                if (svObj.has("IP_ADDRESS")) {
                    String port = svObj.has("PORT") ? svObj.get("PORT").getAsString() : DEFAULT_PORT;
                    final String svIP = svObj.getAsJsonPrimitive("IP_ADDRESS").getAsString();

                    if ("%s:%s".formatted(svIP, port).equals(ip)) {
                        final DCSServerData data = new DCSServerData();
                        data.name = svObj.getAsJsonPrimitive("NAME").getAsString();
                        data.ip = svIP;
                        data.port = port;
                        data.mission = svObj.getAsJsonPrimitive("MISSION_NAME").getAsString();
                        data.players = svObj.getAsJsonPrimitive("PLAYERS").getAsInt();
                        data.playersMax = svObj.getAsJsonPrimitive("PLAYERS_MAX").getAsInt();
                        data.description = svObj.getAsJsonPrimitive("DESCRIPTION").getAsString();
                        data.missionTime = svObj.getAsJsonPrimitive("MISSION_TIME_FORMATTED").getAsString();

                        if ("No".equals(data.description))
                            data.description = getString("dcs.description.no");

                        return data;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot parse data", e);
        }
        return null;
    }

    private static void fetchData() {
        final String resp;
        try {
            resp = post("https://www.digitalcombatsimulator.com/en/personal/server/?login=yes&ajax=y", new LinkedList<>() {{
                add(new BasicNameValuePair("AUTH_FORM", "Y"));
                add(new BasicNameValuePair("TYPE", "AUTH"));
                add(new BasicNameValuePair("backurl", "/en/personal/server/?ajax=y"));
                add(new BasicNameValuePair("USER_LOGIN", Globals.dcsLogin));
                add(new BasicNameValuePair("USER_PASSWORD", Globals.dcsPass));
                add(new BasicNameValuePair("USER_REMEMBER", "Y"));
            }});
        } catch (IOException e) {
            LOG.error("Cannot fetch DCS Servers.", e);
            return;
        }

        if (resp != null && !resp.isBlank()) {
            cachedData = resp;
            nextCache = System.currentTimeMillis() + 5000;
        } else {
            LOG.error("No response from dcs website");
        }
    }

    private static String post(String urlString, List<NameValuePair> payload) throws IOException {
        final CloseableHttpClient client = HttpClients.createDefault();

        final HttpPost post = new HttpPost(urlString);
        post.setEntity(new UrlEncodedFormEntity(payload));
        post.setHeader("Accept", "application/x-www-form-urlencoded");
        post.setHeader("Content-type", "application/x-www-form-urlencoded");

        final CloseableHttpResponse resp = client.execute(post);
        final String res = IOUtils.toString(resp.getEntity().getContent(), StandardCharsets.UTF_8);
        resp.close();

        return res;
    }

    public static class DCSServerData {
        public String name;
        public String ip;
        public String port;
        public String mission;
        //        public long time;
        public int players;
        public int playersMax;
        public String description;
        public String missionTime;
    }
}
