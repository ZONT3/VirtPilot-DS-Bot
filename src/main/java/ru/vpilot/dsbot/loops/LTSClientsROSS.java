package ru.vpilot.dsbot.loops;

import ru.vpilot.dsbot.Globals;
import ru.vpilot.dsbot.Main;
import ru.vpilot.dsbot.Strings;
import ru.zont.dsbot2.ZDSBot;

import java.util.Arrays;
import java.util.List;

public class LTSClientsROSS extends LTSClients {
    public LTSClientsROSS(ZDSBot.GuildContext context) {
        super(context);
    }

    @Override
    protected List<String> getTsqConnection() {
        return Arrays.asList(Globals.tsqHost, Globals.tsqLogin, Globals.tsqPass);
    }

    @Override
    public String getTitle() {
        return Strings.STR.getString("ts_status.title");
    }

    @Override
    public String getCountChannelID() {
        return ((Main.Config) getContext().getConfig()).channel_ts_clients.get();
    }

    @Override
    protected String getClientsChannelID() {
        return ((Main.Config) getContext().getConfig()).channel_ts.get();
    }

    @Override
    protected String getCountLabel() {
        return Strings.STR.getString("ts_clients");
    }

    @Override
    protected String getFooter() {
        return Strings.STR.getString("ts_status.footer");
    }
}
