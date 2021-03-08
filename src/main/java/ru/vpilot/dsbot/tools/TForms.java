package ru.vpilot.dsbot.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import ru.vpilot.dsbot.Main;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.util.ArrayList;
import java.util.Map;

public class TForms {
    public static void newForm(ZDSBot.GuildContext context, JsonObject form) {
        ArrayList<EmbedBuilder> builders = new ArrayList<>();
        builders.add(new EmbedBuilder().setColor(0xf01010));

        for (Map.Entry<String, JsonElement> entry: form.entrySet()) {
            if ("#Title".equalsIgnoreCase(entry.getKey())) {
                builders.get(builders.size() - 1)
                        .setTitle(entry.getValue().getAsJsonObject().get("data").getAsString());
                continue;
            }
            ZDSBMessages.appendDescriptionSplit(parseField(entry.getKey(), entry.getValue().getAsJsonObject().get("data")), builders);
        }

        Main.Config config = ConfigCaster.cast(context.getConfig());
        ZDSBMessages.sendSplit(context.getTChannel(config.channel_report.get()), builders);
//        Tools.tryFindTChannel(Commons.getReportsChannelID(), bot.jda).sendMessage(builder.build()).complete();
    }

    private static String parseField(String key, JsonElement data) {
        String res;
        if (data.isJsonArray()) {
            StringBuilder builder = new StringBuilder();
            boolean f = true;
            for (JsonElement d: data.getAsJsonArray()) {
                if (!f) builder.append(", ");
                else f = false;
                if (d.isJsonArray())
                    builder.append(d.getAsJsonArray().toString());
                else if (d.isJsonPrimitive())
                    builder.append(d.getAsString());
            }
            res = builder.toString();
        } else res = data.getAsString();
        return String.format("**%s**\n%s\n", key, res);
    }
}
