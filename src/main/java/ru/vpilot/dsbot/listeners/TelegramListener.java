package ru.vpilot.dsbot.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import ru.vpilot.dsbot.Main;
import ru.vpilot.dsbot.Strings;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.io.IOException;

public class TelegramListener implements HttpHandler {

    private final ZDSBot.GuildContext context;

    public TelegramListener(ZDSBot.GuildContext context) {
        this.context = context;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Message message = new Message();

            try {
                final String content = Commons.requireJson(exchange);
                if (content == null) return;
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();

                message.text = obj.has("text") ? obj.get("text").getAsString() : null;
                message.caption = obj.has("caption") ? obj.get("caption").getAsString() : null;
                message.chatTitle = obj.has("chatTitle") ? obj.get("chatTitle").getAsString() : null;
                message.photo = obj.has("photo") ? obj.get("photo").getAsInt() : 0;
                message.document = obj.has("document");
                message.video = obj.has("video");
                message.audio = obj.has("audio");

            } catch (IllegalStateException | JsonSyntaxException e) {
//              ErrorReporter.inst().reportError(context, getClass(), e);
                Commons.httpResponse(exchange, "Not a valid JSON", 400);
                return;
            }
            Commons.httpResponse(exchange, "OK", 201);

            handleUpdate(context, message);
        } catch (Throwable e) {
            e.printStackTrace();
            Commons.httpResponse(exchange, "Internal server error: " + ZDSBMessages.describeException(e), 500);
        }
    }

    private static void handleUpdate(ZDSBot.GuildContext context, Message message) {
        if (message == null) return;

        Main.Config config = ConfigCaster.cast(context.getConfig());
        TextChannel channel = context.getTChannel(config.channel_posts.get());
        if (channel == null) throw new NullPointerException("Posts channel");

        String text = message.text();
        StringBuilder append = new StringBuilder();
        if (message.audio())
            append.append(Strings.STR.getString("telega.append.audio")).append("\n");
        if (message.video())
            append.append(Strings.STR.getString("telega.append.video")).append("\n");
        if (message.document())
            append.append(Strings.STR.getString("telega.append.document")).append("\n");
        if (message.photo() > 0)
            for (int i = 0; i < message.photo(); i++)
                append.append(Strings.STR.getString("telega.append.photo")).append("\n");

        if (text == null && append.isEmpty()) return;

        if (text == null) text = "";
        if (!append.isEmpty())
            text += "\n\n" + append;
        if (message.caption() != null)
            text += message.caption();

        EmbedBuilder builder = new EmbedBuilder().setDescription(text).setColor(0x91CCEC);

        if (message.chatTitle() != null)
            builder.setTitle(message.chatTitle());

        channel.sendMessage(builder.build()).queue();
    }

    private static class Message {
        public String text = null;
        public boolean audio = false;
        public boolean video = false;
        public boolean document = false;
        public int photo = 0;
        public String caption = null;
        public String chatTitle = null;

        public String text() {
            return text;
        }

        public boolean audio() {
            return audio;
        }

        public boolean video() {
            return video;
        }

        public boolean document() {
            return document;
        }

        public int photo() {
            return photo;
        }

        public String caption() {
            return caption;
        }

        public String chatTitle() {
            return null;
        }
    }
}
