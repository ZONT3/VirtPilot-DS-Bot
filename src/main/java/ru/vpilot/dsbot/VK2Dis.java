package ru.vpilot.dsbot;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;

import java.io.*;
import java.util.LinkedList;

public class VK2Dis {
    private static final Logger LOG = LoggerFactory.getLogger(VK2Dis.class);
    private final ZDSBot.GuildContext context;
    private final File config;
    private final String key;
    private final String path;
    private Process process;
    private StreamGobbler stdout;
    private StreamGobbler stderr;
    private Thread watchdog;

    public VK2Dis(ZDSBot.GuildContext context, String key, String executablePath, String configPath) {
        this.context = context;
        this.key = key;
        path = executablePath;
        config = new File(configPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process != null) notifyUpdate();
        }));
    }

    public synchronized void start() {
        if (checkEXE()) return;
        try {
            if (process != null || stdout != null || stderr != null || watchdog != null && watchdog.isAlive())
                stop();
            LOG.info("Starting VK2Dis...");
            process = Runtime.getRuntime().exec(new String[]{"npm.cmd", "start"}, null, new File(path));
            createWatchdog();
            stdout = new StreamGobbler(process.getInputStream());
            stderr = new StreamGobbler(process.getErrorStream());
        } catch (Exception e) {
            ErrorReporter.inst().reportError(context, getClass(), e);
        }
    }

    private void createWatchdog() {
        watchdog = new Thread(() -> {
            try {
                process.waitFor();
                start();
            } catch (InterruptedException ignored) { }
        }, "VK2Dis Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    public synchronized void stop() {
        if (checkEXE()) return;

        if (watchdog != null) {
            watchdog.interrupt();
            try { watchdog.join();
            } catch (InterruptedException ignored) { }
        }

        try {
            if (process != null)
                process.destroy();
        } catch (Exception e) {
            LOG.error("ERROR on stopping VK2Dis", e);
        }
        try {
            if (stderr != null)
                stderr.interrupt();
        } catch (Exception e) {
            LOG.error("ERROR on stopping VK2Dis", e);
        }
        try {
            if (stdout != null)
                stdout.interrupt();
        } catch (Exception e) {
            LOG.error("ERROR on stopping VK2Dis", e);
        }

        LOG.info("Waiting for process to end...");
        try {
            if (process != null)
                process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOG.info("Done.");

        process = null;
        stderr = null;
        stdout = null;
        watchdog = null;
    }

    private boolean checkEXE() {
        if (!new File(path, "package.json").isFile()) {
            LOG.warn("Cannot find executable");
            return true;
        }
        return false;
    }

    public LinkedList<String> list() {
        final LinkedList<String> res = new LinkedList<>();
        for (JsonElement cluster: getClusters(parseConfig()))
            res.add(getClusterID(cluster));

        return res;
    }

    public void rm(String name) {
        final JsonObject config = parseConfig();
        final JsonArray clusters = getClusters(config);
        boolean done = false;
        for (int i = 0; i < clusters.size(); i++) {
            if (getClusterID(clusters.get(i)).equals(name)) {
                clusters.remove(i);
                done = true;
                break;
            }
        }

        if (!done) return;

        write(config);
    }

    public void add(String group) {
        final JsonObject config = parseConfig();
        final JsonArray clusters = getClusters(config);
        clusters.add(JsonParser.parseString("""
                {
                    "vk": {
                        "token": "%s",
                        "longpoll": false,
                        "group_id": "%s",
                        "keywords": [],
                        "words_blacklist": [],
                        "filter": true,
                        "donut": false,
                        "ads": false,
                        "interval": 30
                    },
                    "discord": {
                        "webhook_urls": [
                            "http://localhost:13370/embed/"
                        ],
                        "username": "",
                        "avatar_url": "",
                        "content": "",
                        "color": "#5181B8",
                        "author": true,
                        "copyright": true
                    }
                }
                """.formatted(key, group)));
        write(config);
    }

    private synchronized void write(JsonObject config) {
        try {
            //stop();

            final String json = new GsonBuilder().setPrettyPrinting().create().toJson(config);
            final PrintWriter printWriter = new PrintWriter(new FileWriter(this.config));
            printWriter.write(json);
            printWriter.flush();
            printWriter.close();

            //start();
            notifyUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyUpdate() {
        if (process == null || !process.isAlive())
            start();

        final PrintStream ps = new PrintStream(process.getOutputStream());
        ps.println("upd");
        ps.flush();
    }

    private String getClusterID(JsonElement cluster) {
        return cluster.getAsJsonObject().get("vk").getAsJsonObject().get("group_id")
                .getAsJsonPrimitive().getAsString();
    }

    private JsonArray getClusters(JsonObject root) {
        return root.get("clusters").getAsJsonArray();
    }

    private JsonObject parseConfig() {
        try {
            return JsonParser.parseReader(new FileReader(config)).getAsJsonObject();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class StreamGobbler extends Thread {
        private final InputStream is;

        private StreamGobbler(InputStream is) {
            this.is = is;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ( !interrupted() && (line = br.readLine()) != null)
                    LOG.info(line);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
