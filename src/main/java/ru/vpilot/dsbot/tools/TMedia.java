package ru.vpilot.dsbot.tools;

import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import ru.vpilot.dsbot.Globals;
import ru.zont.dsbot2.NotImplementedException;
import ru.zont.dsbot2.tools.Data;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.vpilot.dsbot.Strings.*;

public class TMedia {
    public static final String ICON_YT = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/YouTube_full-color_icon_%282017%29.svg/1024px-YouTube_full-color_icon_%282017%29.svg.png";
    public static final String ICON_TTV = "https://assets.help.twitch.tv/Glitch_Purple_RGB.png";

    public static final int SOURCE_YT = 1;
    public static final int SOURCE_TTV = 2;
    public static final String PREFIX_TTV = "ttv:";

    public static final Data<String> data = new Data<>("media");
    public static final String USRPREFIX_TTV = "https://www.twitch.tv/";

    public static String toReference(String link) {
        if (Twitch.isUID(link))
            return PREFIX_TTV + link;

        Matcher matcher = Pattern.compile("https?://([^.]*\\.)?(youtube\\.com|twitch\\.tv).*").matcher(link);
        if (!matcher.find()) return null;
        int src = matcher.group(2).equals("youtube.com") ? SOURCE_YT : SOURCE_TTV;

        String prefix, id;
        switch (src) {
            case SOURCE_TTV -> {
                prefix = "ttv";
                id = Twitch.getID(link);
            }
            case SOURCE_YT -> {
                prefix = "yt";
                id = YouTube.getID(link);
            }
            default -> {
                return null;
            }
        }
        return prefix + ":" + id;
    }

    public static class YouTube {
        public static String getID(String link) {
            throw new NotImplementedException();
        }
    }

    public static class Twitch {

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
            if (!matcher.find()) throw new IllegalArgumentException("Not a Twitch link");
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
            User user = Twitch.getUser(stream.getUserName());
            if (user == null) throw new RuntimeException("Cannot fetch user");

            String name = stream.getUserName();
            String link = USRPREFIX_TTV + stream.getUserId();
            String aThumb = user.getProfileImageUrl();
            String title = stream.getTitle();
            String thumb = stream.getThumbnailUrl(240, 135);

            return stream(name, link, aThumb, title, link, thumb, "");
        }

        @NotNull
        private static MessageEmbed stream(
                String name, String aLink, String aThumb,
                String title, String link, String thumb, String desc) {
            return new EmbedBuilder()
                    .setAuthor(name, aLink, aThumb)
                    .setTitle(STR.getString("media.stream.new.title"), link)
                    .setDescription(String.format(STR.getString("media.stream.new.desc"), title, desc))
                    .setImage(thumb)
                    .setThumbnail(ICON_TTV)
                    .setColor(0x6441A4)
                    .build();
        }
    }
}
