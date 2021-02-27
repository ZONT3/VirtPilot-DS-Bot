package ru.vpilot.dsbot.loops;

import com.github.twitch4j.helix.domain.Stream;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Main;
import ru.vpilot.dsbot.tools.TMedia;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.Data;

import static ru.vpilot.dsbot.Main.*;
import static ru.vpilot.dsbot.tools.TMedia.data;

public class LMedia extends LoopAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LMedia.class);

    private static final Data<String> committed = new Data<>("mediaCommitted");

    public LMedia(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void loop() throws Throwable {
        Config config = ConfigCaster.cast(getContext().getConfig());
        TextChannel channel = getContext().getTChannel(config.channel_media.get());
        if (channel == null) {
            LOG.warn("Media channel not stated");
            return;
        }

        for (String s: data.get()) {
            String[] media = s.split(":");
            if (media.length < 2) {
                LOG.error("Corrupt media entry occurred");
                continue;
            }
            switch (media[0]) {
                case "ttv" -> {
                    for (Stream stream: TMedia.Twitch.getStreams(media[1])) {
                        if (committed.get().contains(stream.getId())) continue;

                        channel.sendMessage(TMedia.Msg.ttvStream(stream)).queue();
                        committed.op(l -> l.add(stream.getId()));
                    }
                }
            }
        }
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
}
