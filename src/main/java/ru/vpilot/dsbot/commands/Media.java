package ru.vpilot.dsbot.commands;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vpilot.dsbot.Strings;
import ru.vpilot.dsbot.tools.TMedia;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.commands.CommandAdapter;
import ru.zont.dsbot2.commands.Input;
import ru.zont.dsbot2.commands.UserInvalidInputException;
import ru.zont.dsbot2.tools.Commons;
import ru.zont.dsbot2.tools.ZDSBMessages;
import ru.zont.dsbot2.tools.ZDSBStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ru.vpilot.dsbot.tools.TMedia.*;

public class Media extends CommandAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Media.class);

    public Media(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    public void onCall(Input input) {
        Commons.rolesLikeRouter(0, this::set, this::rm, this::get).acceptInput(input);
    }

    private void set(Input input) {
        String link = getLink(input);
        data.op(list -> {
            String ref = toReference(link);
            if (!list.contains(ref))
                list.add(ref);
        });
        ZDSBMessages.addOK(input.getMessage());
    }

    private void rm(Input input) {
        String link = getLink(input);
        data.op(list -> {
            if (list.remove(toReference(link)))
                ZDSBMessages.addOK(input.getMessage());
            else ZDSBMessages.printError(input.getChannel(), Strings.STR.getString("err"), Strings.STR.getString("media.err.rm"));
        });
    }

    private void get(Input input) {
        ArrayList<EmbedBuilder> builders = new ArrayList<>();
        builders.add(new EmbedBuilder().setTitle(Strings.STR.getString("media.list.title")));
        data.getData().stream().sorted().forEach(s -> ZDSBMessages.appendDescriptionSplit(listEntry(s), builders));
        ZDSBMessages.sendSplit(input.getChannel(), builders);
    }

    private String listEntry(String s) {
        String[] media = s.split(":");
        if (media.length < 2) {
            LOG.error("Corrupt media entry occurred");
            return " - ???";
        }

        String prefix, name, src;
        switch (media[0]) {
            case "ttv" -> {
                prefix = USRPREFIX_TTV;
                src = "[TTV]";
                name = media[1];
            }
            case "yt" -> {
                prefix = USRPREFIX_YT;
                src = "[YT]";
                try {
                    name = Objects.requireNonNull(YT.getChannelSnippet(media[1])).getSnippet().getTitle();
                } catch (Throwable e) {
                    ErrorReporter.inst().reportError(getContext(), getClass(), e);
                    String err;
                    if (e.getCause() != null && e.getCause() instanceof GoogleJsonResponseException)
                        err = "ERROR: No Quota (%s)";
                    else err = "ERROR (%s)";
                    name = String.format(err, media[1]);
                }
            }
            default -> { return " - ???"; }
        }

        return String.format(" - `%5s` [%s](%s)\n", src, name, prefix + media[1]);
    }

    private String toReference(String link) {
        String s = TMedia.toReference(link);
        if (s == null) throw new UserInvalidInputException(Strings.STR.getString("media.err.link"));
        return s;
    }

    @NotNull
    private String getLink(Input input) {
        String link = input.getArg(1);
        if (link == null) throw new UserInvalidInputException(ZDSBStrings.STR.getString("err.insufficient_args"));
        return link;
    }

    @Override
    public String getCommandName() {
        return "media";
    }

    @Override
    public boolean checkPermission(Input input) {
        return Commons.rolesLikePermissions(input, List.of("get", "list"));
    }

    @Override
    public boolean allowPM() {
        return false;
    }

    @Override
    public boolean allowForeignGuilds() {
        return false;
    }

    @Override
    public String getSynopsis() {
        return "media (add|set|rm|del|get|list) ...";
    }

    @Override
    public String getDescription() {
        return Strings.STR.getString("comms.media.desc");
    }
}
