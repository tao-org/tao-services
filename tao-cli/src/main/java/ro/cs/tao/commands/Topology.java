/*
 * Copyright (C) 2018 CS ROMANIA
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

import org.apache.commons.cli.CommandLine;

import java.util.HashMap;
import java.util.Map;

public final class Topology {

    public static final BaseCommand List = new BaseCommand("topology-list", "topology", "GET") {
        @Override
        protected String[][] getArgumentsDefinition() {
            return null;
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            return null;
        }
    };

    public static final BaseCommand AddNode = new BaseCommand("topology-add", "topology", "POST") {
        @Override
        protected String[][] getArgumentsDefinition() {
            //Short code ; Args cardinality ; Value hint ; Value separator (if multiple) ; Optional ; Description
            return new String[][] {
                    new String[] { "remoteHost", "1", "string", "", "false", "The new node name or IP address" },
                    new String[] { "remoteUser", "1", "string", "", "false", "The account that has admin privileges on the new node" },
                    new String[] { "remotePass", "1", "string", "", "false", "The password on the new node" },
                    new String[] { "processors", "1", "integer", "", "true", "The number of processors installed on the new node. Defaults to 1" },
                    new String[] { "memory", "1", "integer", "", "true", "The RAM memory (in GB) installed on the new node. Defaults to 4" },
                    new String[] { "disk", "1", "integer", "", "true", "The disk space (in GB) installed on the new node. Defaults to 100" },
                    new String[] { "description", "1", "string", "", "true", "The description of the new node. Defaults to 'New TAO node'" }
            };
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("hostName", commandLine.getOptionValue("remoteHost"));
            parameters.put("userName", commandLine.getOptionValue("remoteUser"));
            parameters.put("userPass", commandLine.getOptionValue("remotePass"));
            parameters.put("processorCount", Integer.parseInt(commandLine.getOptionValue("processors", "1")));
            parameters.put("memorySizeGB", Integer.parseInt(commandLine.getOptionValue("memory", "4")));
            parameters.put("diskSpaceSizeGB", Integer.parseInt(commandLine.getOptionValue("disk", "100")));
            parameters.put("active", Boolean.TRUE);
            return parameters;
        }
    };

    public static final BaseCommand DeleteNode = new BaseCommand("topology-remove", "topology", "DELETE") {
        @Override
        protected String[][] getArgumentsDefinition() {
            //Short code ; Args cardinality ; Value hint ; Value separator (if multiple) ; Optional ; Description
            return new String[][] {
                    new String[] { "remoteHost", "1", "string", "", "false", "The new node name or IP address" },
            };
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("hostName", commandLine.getOptionValue("remoteHost"));
            return parameters;
        }
    };
}
