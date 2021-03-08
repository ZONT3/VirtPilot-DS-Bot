package ru.vpilot.dsbot.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import ru.vpilot.dsbot.tools.TForms;
import ru.zont.dsbot2.ErrorReporter;
import ru.zont.dsbot2.ZDSBot;
import ru.zont.dsbot2.tools.ZDSBMessages;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReportHandler implements HttpHandler {
    private final ZDSBot.GuildContext context;

    public ReportHandler(ZDSBot.GuildContext context) {
        this.context = context;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"post".equalsIgnoreCase(exchange.getRequestMethod())) {
                response(exchange, "Only POST method is acceptable", 400);
                return;
            }

            final List<String> contentType = exchange.getRequestHeaders().get("Content-type");
            if (contentType.size() < 1 || !contentType.contains("application/json")) {
                response(exchange, "Content-type should be JSON", 400);
                return;
            }

            try {
                final String content = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
                new Committer(content).start();
            } catch (JsonSyntaxException e) {
                ErrorReporter.inst().reportError(context, getClass(), e);
                response(exchange, "Not a valid JSON", 400);
                return;
            }

            response(exchange, "OK", 200);
        } catch (Throwable e) {
            e.printStackTrace();
            response(exchange, "Internal server error: " + ZDSBMessages.describeException(e), 500);
        }
    }

    private void response(HttpExchange exchange, String response, int code) throws IOException {
        exchange.sendResponseHeaders(code, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    public class Committer extends Thread {
        private final JsonObject form;

        public Committer(String formStr) throws JsonSyntaxException {
            super("Form Committer");
            form = new Gson().fromJson(formStr, JsonObject.class);
        }

        @Override
        public void run() {
            TForms.newForm(context, form);
        }
    }
}
