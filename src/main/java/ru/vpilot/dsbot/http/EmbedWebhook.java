package ru.vpilot.dsbot.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Main.Config;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.tools.Commons;

import java.io.IOException;

public class EmbedWebhook implements HttpHandler {
    private static Logger LOG = LoggerFactory.getLogger(EmbedWebhook.class);
    private ZDSBot.GuildContext context;

    public EmbedWebhook(ZDSBot.GuildContext bot) {
        this.context = bot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Config config = ConfigCaster.cast(context.getConfig());
        TextChannel tChannel = context.getTChannel(config.channel_posts.get());
        if (tChannel == null) {
            Commons.httpResponse(exchange, "Cannot find channel to send", 500);
            return;
        }

        String json = Commons.requireJson(exchange);
        if (json == null) {
            LOG.warn("Illegal request");
            return;
        }
        JsonObject r = JsonParser.parseString(json).getAsJsonObject();

        String content = r.has("content")
                ? r.getAsJsonPrimitive("content").getAsString()
                : "";

        JsonArray arr = r.get("embeds").getAsJsonArray();
        if (arr.size() == 0) throw new IllegalArgumentException();

        DataObject dataObject = DataObject.fromJson(arr.get(0).getAsJsonObject().toString());
        MessageEmbed embed = new EntityBuilder(context.getBot().getJda()).createMessageEmbed(dataObject);

        tChannel.sendMessage(new MessageBuilder(content).setEmbed(embed).build()).queue();
    }
}
