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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Globals;
import ru.zont.dsbot2.DescribedException;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.DataList;
import ru.zont.dsbot2.tools.ZDSBStrings;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.vpilot.dsbot.Strings.STR.getString;

public class LDCSServers extends LoopAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LDCSServers.class);
    private static final String CHANNEL_ID = /*"845040939457970207"*/"814559760652304466";
    private static final String DEFAULT_PORT = "10308";

    private final DataList<String> servers = new DataList<>("dcs_servers");
    private final HashMap<String, String> msgMap = new HashMap<>();

    private String cachedData = "{}";
    private long nextCache = 0;

    public LDCSServers(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void loop() throws Throwable {
        final TextChannel channel = getContext().getTChannel(CHANNEL_ID);

        LinkedList<String> updated = new LinkedList<>();
        for (String ip: servers.getData()) {
            final String msg = update(ip, channel);
            if (msg != null) updated.add(msg);
        }

        Commons.clearTextChannel(channel, updated);
    }

    @Override
    public long getPeriod() {
        return 60 * 1000;
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
        if (data == null) {
            final JsonObject cached = JsonParser.parseString(cachedData).getAsJsonObject();
            final int total = cached.has("PLAYERS_COUNT")
                    ? cached.getAsJsonPrimitive("PLAYERS_COUNT").getAsInt()
                    : 0;
            servers.op(list -> list.remove(ip));
            ErrorReporter.inst().reportError(getContext(), getClass(), new DescribedException(getString("dsc.err.noserv.title"), getString("dsc.err.noserv", total)));
            return null;
        }

        String msgID = msgMap.get(ip);
        if (msgID == null) msgID = findOrNewMsg(data, channel);
        if (msgID == null) throw new RuntimeException("Cannot create new message");

        final Message toEdit = channel.retrieveMessageById(msgID).complete();
        toEdit.editMessage(buildMsg(data)).queue();
        return msgID;
    }

    private Message buildMsg(DCSServerData data) {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setTitle(normalizeTitle(data.name))
                        .addField(getString("dcs.players"), "%d / %d".formatted(data.players, data.playersMax), true)
                        .addField(getString("dcs.mission_time"), data.missionTime, true)
                        .addField("dcs.mission", data.mission, true)
                        .setDescription(normalizeDesc(data))
                        .setColor(0x1474A6)
        ).build();
    }

    private String normalizeDesc(DCSServerData data) {
        return getString("dcs.desc.format", data.ip, data.port, ZDSBStrings.trimSnippet(data.description, 715));
    }

    private String normalizeTitle(String name) {
        return ZDSBStrings.trimSnippet(name, 64);
    }

    private String findOrNewMsg(DCSServerData data, TextChannel channel) {
        final List<Message> found = channel.getHistory().retrievePast(100).complete().parallelStream().filter(msg -> {
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
                .setTitle("Fetching data for %s:%s...".formatted(data.ip, data.port))
                .build()).complete();
        return msg != null ? msg.getId() : null;
    }

    private DCSServerData findData(String ip) {
        if (nextCache <= System.currentTimeMillis())
            fetchData();

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

        return null;
    }

    private void fetchData() {
        final String resp;
        try {
            resp = post("https://www.digitalcombatsimulator.com/en/auth/", """
                    {
                        "AUTH_FORM": "Y",
                        "TYPE": "AUTH",
                        "backurl": "/en/personal/server/?ajax=y",
                        "USER_LOGIN": "%s",
                        "USER_PASSWORD": "%s",
                        "USER_REMEMBER": "Y"
                    }
                    """.formatted(Globals.dcsLogin, Globals.dcsPass));
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

    private static String post(String urlString, String payload) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.connect();

        OutputStream os = connection.getOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.write(payload);
        pw.close();

        InputStream is = connection.getInputStream();
        final String res = IOUtils.toString(is, StandardCharsets.UTF_8);
        is.close();
        return res;
    }

    private static class DCSServerData {
        String name;
        String ip;
        String port;
        String mission;
//        long time;
        int players;
        int playersMax;
        String description;
        String missionTime;
    }
}
