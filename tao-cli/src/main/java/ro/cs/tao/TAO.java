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

import ro.cs.tao.commands.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TAO {

    private static final Map<String, BaseCommand> commands;

    static {
        commands = new HashMap<>();
        commands.put(Topology.List.getName(), Topology.List);
        commands.put(Topology.AddNode.getName(), Topology.AddNode);
        commands.put(Topology.DeleteNode.getName(), Topology.DeleteNode);
        commands.put(Container.List.getName(), Container.List);
        commands.put(Workflow.List.getName(), Workflow.List);
        commands.put(Workflow.Export.getName(), Workflow.Export);
        commands.put(Component.List.getName(), Component.List);
        commands.put(Component.Export.getName(), Component.Export);
        commands.put(Component.Import.getName(), Component.Import);
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 3) {
            BaseCommand command = commands.get(args[0]);
            if (command == null) {
                System.err.println("Unsupported command");
                System.exit(1);
            }
            System.exit(command.execute(Arrays.copyOfRange(args, 1, args.length)));
        }
        System.exit(0);
    }

}
