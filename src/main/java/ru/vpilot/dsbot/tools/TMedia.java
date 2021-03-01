package ru.vpilot.dsbot.tools;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.SearchResult;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Globals;
import ru.vpilot.dsbot.loops.LMedia;
import ru.zont.dsbot2.tools.Data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.vpilot.dsbot.Strings.*;

public class TMedia {
    private static final Logger LOG = LoggerFactory.getLogger(LMedia.class);

    public static final String ICON_YT = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/YouTube_full-color_icon_%282017%29.svg/1024px-YouTube_full-color_icon_%282017%29.svg.png";
    public static final String ICON_TTV = "https://assets.help.twitch.tv/Glitch_Purple_RGB.png";

    public static final int SOURCE_YT = 1;
    public static final int SOURCE_TTV = 2;
    public static final String PREFIX_TTV = "ttv:";
    public static final String PREFIX_YT = "yt:";

    public static final Data<String> data = new Data<>("media");
    public static final String USRPREFIX_TTV = "https://www.twitch.tv/";
    public static final String USRPREFIX_YT = "https://www.youtube.com/channel/";
    public static final String VIDPREFIX_YT = "https://www.youtube.com/watch?v=";

    public static String toReference(String link) {
        if (TTV.isUID(link))
            return PREFIX_TTV + link;

        Matcher matcher = Pattern.compile("https?://([^.]*\\.)?(youtube\\.com|twitch\\.tv).*").matcher(link);
        if (!matcher.find()) return null;
        int src = matcher.group(2).equals("youtube.com") ? SOURCE_YT : SOURCE_TTV;

        String prefix, id;
        try {
            switch (src) {
                case SOURCE_TTV -> {
                    prefix = PREFIX_TTV;
                    id = TTV.getID(link);
                }
                case SOURCE_YT -> {
                    prefix = PREFIX_YT;
                    id = YT.getID(link);
                }
                default -> {
                    return null;
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return prefix + id;
    }

    public static class YT {
        private static HashMap<String, LocalTime> contentFetchMap = new HashMap<>();
        private static final LocalTime[] updatePoints = {
                LocalTime.of(15, 5), LocalTime.of(18, 5),
                LocalTime.of(21, 5), LocalTime.of(22, 5),
                LocalTime.of(23, 5)
        };

        private static YouTube api = null;

        private static void setup() throws GeneralSecurityException, IOException {
            api = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), null).setApplicationName("ds-bot").build();
        }

        private static void checkSetup() {
            if (api == null) {
                try {
                    setup();
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public static String getID(String link) {
            Pattern pattern = Pattern.compile("https?://(\\w+\\.)?youtube\\.com/channel/([\\w-]+)(/.*)?");
            Matcher matcher = pattern.matcher(link);
            if (!matcher.find()) {
                String nLink = findCanonicalLink(link);
                matcher = null;
                if (nLink != null) {
                    matcher = pattern.matcher(nLink);
                    if (!matcher.find()) matcher = null;
                }
                if (matcher == null)
                    throw new IllegalArgumentException("Not a YouTube channel link");
            }
            return matcher.group(2);
        }

        private static String findCanonicalLink(String link) {
            try {
                Document document = Jsoup.connect(link).get();
                Elements e = document.body().getElementsByAttributeValue("rel", "canonical");
                if (e.size() > 0 && e.hasAttr("href"))
                    return e.attr("href");
                return null;
            } catch (Throwable t) {
                return null;
            }
        }

        public static List<SearchResult> getVideos(String id) throws IOException {
            if (!checkPeriod(id)) return Collections.emptyList();
            checkSetup();

            try {
                return api.search().list("snippet")
                        .setKey(Globals.GOOGLE_API)
                        .setChannelId(id)
                        .setOrder("date")
                        .execute().getItems();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public static Channel getChannelSnippet(String id) {
            checkSetup();

            final List<Channel> snippet;
            try {
                snippet = api.channels().list("snippet")
                        .setKey(Globals.GOOGLE_API)
                        .setId(id)
                        .execute().getItems();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return snippet.size() == 1 ? snippet.get(0) : null;
        }

        private static boolean checkPeriod(String id) {
            LocalTime lastContentFetch = contentFetchMap.getOrDefault(id, null);
            LocalTime now = LocalTime.now();
            if (lastContentFetch == null || getPos(now) != getPos(lastContentFetch)) {
                contentFetchMap.put(id, now);
                return true;
            } else return false;
        }

        private static LocalTime getPos(LocalTime time) {
            LocalTime res = updatePoints[updatePoints.length - 1];
            for (LocalTime p: updatePoints) {
                if (time.isBefore(p))
                    return res;
                else res = p;
            }
            return res;
        }
    }

    public static class TTV {
        private static TwitchHelix helix = null;

        private static void setup() {
            helix = TwitchHelixBuilder.builder()
                    .withClientId(Globals.TWITCH_API_CLIENT_ID)
                    .withClientSecret(Globals.TWITCH_API_SECRET)
                    .build();
        }

        private static void checkSetup() {
            if (helix == null) {
                setup();
                if (helix == null)
                    throw new IllegalStateException("Cannot setup Twitch Helix");
            }
        }

        public static List<Stream> getStreams(String user) {
            checkSetup();

            return helix.getStreams(
                    null, null, null,
                    null, null, null, null,
                    Collections.singletonList(user)
            ).execute().getStreams();
        }

        public static String getID(String link) {
            checkSetup();

            if (isUID(link)) return link;
            Matcher matcher = Pattern.compile("https?://(\\w+\\.)?twitch\\.tv/(\\w+)(/.*)?").matcher(link);
            if (!matcher.find()) throw new IllegalArgumentException("Not a Twitch channel link");
            return matcher.group(2);
        }

        private static boolean isUID(String link) {
            return link.matches("\\w+") && verifyUID(link);
        }

        public static boolean verifyUID(String user) {
            return getUser(user) != null;
        }

        public static User getUser(String user) {
            checkSetup();

            List<User> users = helix.getUsers(null, null, Collections.singletonList(user)).execute().getUsers();
            if (users.size() != 1) return null;
            return users.get(0);
        }
    }

    public static class Msg {
        public static MessageEmbed ttvStream(Stream stream) {
            String userName = stream.getUserName();
            User user = TTV.getUser(userName);
            if (user == null) throw new RuntimeException("Cannot fetch user");

            String link   = USRPREFIX_TTV + userName;
            String aThumb = user.getProfileImageUrl();
            String title  = stream.getTitle();
            String thumb  = stream.getThumbnailUrl(240, 135);

            return media(userName, link, aThumb, link, thumb,
                    STR.getString("media.stream.new.title"),
                    String.format(STR.getString("media.stream.new.desc"), title, ""), ICON_TTV, 0x6441A4
            );
        }

        public static MessageEmbed ytStream(SearchResult result) {
            return ytMedia(result, STR.getString("media.stream.new.title"));
        }

        public static MessageEmbed ytStreamPlan(SearchResult result) {
            return ytMedia(result, STR.getString("media.stream.plan.title"));
        }

        public static MessageEmbed ytVideo(SearchResult result) {
            return ytMedia(result, STR.getString("media.video.new.title"));
        }

        private static MessageEmbed ytMedia(SearchResult result, String embedTitle) {
            String channelId = result.getSnippet().getChannelId();
            Channel channel = YT.getChannelSnippet(channelId);
            if (channel == null) throw new RuntimeException("Cannot fetch channel");

            String name   = channel.getSnippet().getTitle();
            String aLink  = USRPREFIX_YT + channelId;
            String aThumb = channel.getSnippet().getThumbnails().getDefault().getUrl();
            String title  = result.getSnippet().getTitle();
            String desc   = result.getSnippet().getDescription();
            String thumb  = result.getSnippet().getThumbnails().getDefault().getUrl();
            String link   = VIDPREFIX_YT + result.getId().getVideoId();

            return media(name, aLink, aThumb, link, thumb, embedTitle,
                    String.format(STR.getString("media.stream.new.desc"), title, desc), ICON_YT, 0xFF0202);

        }

        @NotNull
        private static MessageEmbed media(
                String name, String aLink, String aThumb,
                String link, String thumb,
                String embedTitle, String embedDesc,
                String icon, int color) {
            return new EmbedBuilder()
                    .setAuthor(name, aLink, aThumb)
                    .setTitle(embedTitle, link)
                    .setDescription(embedDesc)
                    .setImage(thumb)
                    .setThumbnail(icon)
                    .setColor(color)
                    .build();
        }
    }
}
