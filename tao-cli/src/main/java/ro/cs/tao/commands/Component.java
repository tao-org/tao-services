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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Component {

    public static final BaseCommand List = new BaseCommand("component-list", "component", "GET") {
        @Override
        protected String[][] getArgumentsDefinition() {
            return null;
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            return null;
        }
    };

    public static final BaseCommand Export = new BaseCommand("component-export", "component", "GET") {
        @Override
        protected String[][] getArgumentsDefinition() {
            return new String[][] {
                    new String[] { "id", "1", "string", "", "false", "The component identifier" }
            };
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", commandLine.getOptionValue("id"));
            return parameters;
        }
    };

    public static final BaseCommand Import = new BaseCommand("component-import", "component/import", "POST") {
        @Override
        protected String[][] getArgumentsDefinition() {
            return new String[][] {
                    new String[] { "in", "1", "file", "", "false", "The component descriptor file" }
            };
        }

        @Override
        protected Map<String, Object> readParameters(CommandLine commandLine) {
            Map<String, Object> parameters = new HashMap<>();
            String fileName = commandLine.getOptionValue("in");
            try {
                parameters.put("in", new String(Files.readAllBytes(Paths.get(fileName))));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return parameters;
        }
    };
}
