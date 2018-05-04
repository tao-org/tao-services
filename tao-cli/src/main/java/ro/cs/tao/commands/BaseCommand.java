/*
 * Copyright (C) 2017 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package ro.cs.tao.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public abstract class BaseCommand {
    private final String name;
    private final String restFragment;
    private final String httpMethod;
    private final Options options;

    public BaseCommand(String name, String urlFragment, String httpMethod) {
        this.name = name;
        this.restFragment = urlFragment;
        this.httpMethod = httpMethod;
        this.options = initOptions();
    }

    public String getName() { return name; }

    public int execute(String[] arguments) throws Exception {
        int retCode;
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(this.options, arguments);
        String masterUrl = commandLine.getOptionValue("master", "localhost:8080");
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("password");
        String authToken = "Basic " + new String(Base64.getEncoder().encode((user + ":" + password).getBytes()));
        String baseUrl = "http://" + masterUrl + "/";
        Map<String, Object> parameters = readParameters(commandLine);
        try {
            retCode = postRequest(baseUrl + this.restFragment + "/", authToken, this.httpMethod, parameters);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            retCode = 1;
        }
        return retCode;
    }

    /**
     * An argument is described by the following sequence:
     * Short code ; Args cardinality ; Value hint ; Value separator (if multiple) ; Optional ; Description
     */
    protected abstract String[][] getArgumentsDefinition();

    private Options initOptions() {
        String[][] argumentsDefinition = getArgumentsDefinition();
        Options options = new Options();
        options.addOption(buildOption(new String[] { "user", "1", "string", "", "false", "The master node user" }));
        options.addOption(buildOption(new String[] { "password", "1", "string", "", "false", "The master node password" }));
        options.addOption(buildOption(new String[] { "master", "1", "string", "", "true", "The master node name or IP address. Defaults to localhost:8080" }));
        if (argumentsDefinition != null) {
            for (String[] values : argumentsDefinition) {
                options.addOption(buildOption(values));
            }
        }
        return options;
    }

    protected abstract Map<String, Object> readParameters(CommandLine commandLine);

    private Option buildOption(String[] values) {
        Option.Builder optionBuilder = Option.builder(values[0].trim())
                .longOpt(values[0].trim())
                .argName(values[2].trim())
                .desc(values[5].trim())
                .required(!Boolean.parseBoolean(values[4].trim()));
        String cardinality = values[1].trim();
        switch (cardinality) {
            case "1":
                optionBuilder.hasArg();
                break;
            case "n":
                optionBuilder.hasArgs().valueSeparator(values[3].trim().charAt(1));
                break;
            default:
                break;
        }
        return optionBuilder.build();
    }

    private int postRequest(String url, String authToken, String method, Map<String, Object> parameters) throws IOException {
        URL urlObj;
        HttpURLConnection conn;
        int retCode;
        switch (method) {
            case "POST":
                urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                if (authToken != null) {
                    conn.setRequestProperty("Authorization", authToken);
                }
                if (parameters != null) {
                    String contents = expandParameters(parameters);
                    if (!contents.startsWith("{")) {
                        contents = "{" + contents;
                    }
                    if (!contents.endsWith("}")) {
                        contents += "}";
                    }
                    try (OutputStream outStream = conn.getOutputStream()) {
                        outStream.write(contents.getBytes());
                        outStream.flush();
                    }
                }
                retCode = conn.getResponseCode() == 200 ? 0 : 2;
                byte[] buffer = new byte[4096];
                int read = -1;
                try (InputStream inStream = conn.getInputStream()) {
                    while((read = inStream.read(buffer)) != -1) {
                        System.out.print(new String(Arrays.copyOfRange(buffer, 0, read)));
                    }
                    System.out.println();
                }
                break;
            case "GET":
            default:
                if (parameters != null) {
                    StringBuilder urlBuilder = new StringBuilder(url);
                    for (Object value : parameters.values()) {
                        urlBuilder.append(value.toString()).append("/");
                    }
                    url = urlBuilder.toString();
                }
                urlObj = new URL(url);
                conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                if (authToken != null) {
                    conn.setRequestProperty("Authorization", authToken);
                }
                retCode = conn.getResponseCode() == 200 ? 0 : 2;
                InputStream inputStream;
                if (retCode == 0) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream();
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(gson.toJson(jp.parse(line)));
                    }
                }
                break;
        }
        return retCode;
    }

    private String expandParameters(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();
        if (params.size() == 1) {
            String value = params.values().stream().findFirst().get().toString();
            if (value.startsWith("{") && value.endsWith("}")) {
                builder.append(value);
                params.remove(params.keySet().stream().findFirst().get());
            }
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            builder.append("\"").append(entry.getKey()).append(":");
            Object value = entry.getValue();
            if (value instanceof String) {
                builder.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                builder.append("{").append(expandParameters((Map<String, Object>) value)).append("}");
            } else {
                builder.append(value);
            }
            builder.append(",");
        }
        String out = builder.toString();
        return out.substring(0, out.length() - 1);
    }

}
