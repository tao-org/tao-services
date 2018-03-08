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
package ro.cs.tao;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class TAO {

    private static Options options;

    static {
        options = new Options();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(TAO.class.getResourceAsStream("parameters.def")))) {
            HashMap<String, List<String[]>> groups = new HashMap<>();
            reader.lines()
                    .filter(l -> {
                        String trimmed = l.trim();
                        return !trimmed.startsWith("#") &&
                                !(trimmed.startsWith("\r") || trimmed.startsWith("\n"));
                    })
                    .forEach(s -> {
                        String[] tokens = s.split(";");
                        if (tokens.length == 8) {
                            String key = tokens[0].trim();
                            if (!groups.containsKey(key)) {
                                groups.put(key, new ArrayList<>());
                            }
                            groups.get(key).add(tokens);
                        }
                    });
            for (String key : groups.keySet()) {
                if ("n/a".equals(key)) {
                    groups.get(key).forEach(values -> options.addOption(buildOption(values)));
                } else {
                    OptionGroup group = new OptionGroup();
                    groups.get(key).forEach(values -> group.addOption(buildOption(values)));
                    options.addOptionGroup(group);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(Integer.MAX_VALUE);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("TAO", options);
            System.exit(0);
        }
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        System.exit(execute(commandLine));
    }

    private static int execute(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);
        return execute(commandLine);
    }

    private static int execute(CommandLine commandLine) {
        int retCode = 0;
        String master = commandLine.getOptionValue("master", "localhost:8080");
        String user = commandLine.getOptionValue("user");
        String password = commandLine.getOptionValue("pass");
        String authToken = "Basic " + new String(Base64.getEncoder().encode((user + ":" + password).getBytes()));
        String baseUrl = "http://" + master + "/";
        String command = commandLine.getOptionValue("cmd");
        Map<String, Object> parameters = new HashMap<>();
        try {
            switch (command) {
                case "node-add":
                    parameters.put("hostName", commandLine.getOptionValue("remoteHost"));
                    parameters.put("userName", commandLine.getOptionValue("remoteUser"));
                    parameters.put("userPass", commandLine.getOptionValue("remotePass"));
                    parameters.put("processorCount", Integer.parseInt(commandLine.getOptionValue("processors", "1")));
                    parameters.put("memorySizeGB", Integer.parseInt(commandLine.getOptionValue("memory", "4")));
                    parameters.put("diskSpaceSizeGB", Integer.parseInt(commandLine.getOptionValue("disk", "100")));
                    parameters.put("active", Boolean.TRUE);
                    retCode = postRequest(baseUrl + "topology/", authToken, "POST", parameters);
                    break;
                case "node-delete":
                    parameters.put("hostName", commandLine.getOptionValue("remoteHost"));
                    retCode = postRequest(baseUrl + "topology/", authToken, "DELETE", parameters);
                    break;
                default:
                    retCode = 1;
            }
        } catch (Exception ex) {
            System.out.println("Could not process the command. Are the services started?");
            retCode = 1;
        }
        return retCode;
    }

    private static int postRequest(String url, String authToken, String method, Map<String, Object> parameters) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        if (authToken != null) {
            conn.setRequestProperty("Authorization", authToken);
        }
        String contents = "{" + expandParameters(parameters) + "}";
        try (OutputStream outStream = conn.getOutputStream()) {
            outStream.write(contents.getBytes());
            outStream.flush();
        }
        return conn.getResponseCode() == 200 ? 0 : 2;
    }

    private static String expandParameters(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();
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

    private static Option buildOption(String[] values) {
        Option.Builder optionBuilder = Option.builder(values[1].trim())
                .longOpt(values[2].trim())
                .argName(values[4].trim())
                .desc(values[7].trim())
                .required(!Boolean.parseBoolean(values[6].trim()));
        String cardinality = values[3].trim();
        switch (cardinality) {
            case "1":
                optionBuilder.hasArg();
                break;
            case "n":
                optionBuilder.hasArgs().valueSeparator(values[5].trim().charAt(1));
                break;
            default:
                break;
        }
        return optionBuilder.build();
    }
}
