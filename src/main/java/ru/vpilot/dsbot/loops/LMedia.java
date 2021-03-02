package ru.vpilot.dsbot.loops;

import com.github.twitch4j.helix.domain.Stream;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot2.ConfigCaster;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.loops.LoopAdapter;
import ru.zont.dsbot2.tools.DataList;
import ru.zont.dsbot2.tools.ZDSBMessages;

import static ru.vpilot.dsbot.Main.*;
import static ru.vpilot.dsbot.tools.TMedia.*;
import static ru.vpilot.dsbot.tools.TMedia.data;

public class LMedia extends LoopAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(LMedia.class);

    private static final DataList<String> committed = new DataList<>("mediaCommitted");

    public LMedia(ZDSBot.GuildContext context) {
        super(context);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    public void loop() throws Throwable {
        Config config = ConfigCaster.cast(getContext().getConfig());
        TextChannel channelStr = getContext().getTChannel(config.channel_streams.get());
        TextChannel channelVid = getContext().getTChannel(config.channel_video.get());
        if (channelStr == null || channelVid == null) {
            LOG.warn("Media channel(s) not stated");
            return;
        }

        committed.op(list -> {
            for (String s: data.getData()) {
                String[] media = s.split(":");
                if (media.length < 2) {
                    LOG.error("Corrupt media entry occurred");
                    continue;
                }
                try {
                    switch (media[0]) {
                        case "ttv" -> {
                            for (Stream stream: TTV.getStreams(media[1])) {
                                if (list.contains(stream.getId())) continue;

                                channelStr.sendMessage(ZDSBMessages.wrapEmbed(Msg.ttvStream(stream),
                                                "", String.format("<@&%s>", config.role_checked.get()))).queue();
                                list.add(stream.getId());
                            }
                        }
                        case "yt" -> {
                            for (SearchResult video: YT.getVideos(media[1])) {
                                String bc = video.getSnippet().getLiveBroadcastContent();
                                String identity;
                                if ("upcoming".equals(bc))
                                    identity = video.getId().getVideoId() + ":ucs";
                                else identity = video.getId().getVideoId();

                                if (list.contains(identity)) continue;

                                switch (bc) {
                                    case "live"     -> channelStr
                                            .sendMessage(ZDSBMessages.wrapEmbed(Msg.ytStream(video),
                                                    "", String.format("<@&%s>", config.role_checked.get()))).queue();
                                    case "upcoming" -> channelStr.sendMessage(Msg.ytStreamPlan(video)).queue();
                                    default         -> channelVid.sendMessage(     Msg.ytVideo(video)).queue();
                                }

                                list.add(identity);
                            }
                        }
                    }
                } catch (Throwable e) {
                    ErrorReporter.inst().reportError(getContext(), LMedia.class, e);
                }
            }
        });
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
