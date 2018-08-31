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
package ro.cs.tao.services.progress.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.messaging.*;
import ro.cs.tao.services.interfaces.ProgressReportService;

import java.util.*;
import java.util.regex.Pattern;

@Service("progressReportService")
public class ProgressReportServiceImpl extends Notifiable implements ProgressReportService {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(.+)progress");
    private static final Map<String, TaskProgress> tasksInProgress = Collections.synchronizedMap(new LinkedHashMap<>());

    public ProgressReportServiceImpl() {
        Messaging.subscribe(this, TOPIC_PATTERN);
    }

    @Override
    protected void onMessageReceived(Message message) {
        logger.fine(message.getItem(Message.PAYLOAD_KEY));
        String contents = message.getItem(Message.PAYLOAD_KEY);
        String taskName;
        if (contents.startsWith("Started") && !contents.contains(":")) {
            taskName = contents.substring(8);
            tasksInProgress.put(taskName, new TaskProgress(taskName, 0, null));
        } else if (contents.startsWith("Completed") && !contents.contains(":")) {
            taskName = contents.substring(10);
            tasksInProgress.remove(taskName);
        } else if (contents.startsWith("Started")) {
            taskName = contents.substring(8, contents.indexOf(":"));
            if (tasksInProgress.containsKey(taskName)) {
                double mainProgress = tasksInProgress.get(taskName).getProgress();
                tasksInProgress.put(taskName,
                        new TaskProgress(taskName,
                                mainProgress,
                                new SubTaskProgress(contents.substring(contents.indexOf(":") + 1),
                                        0.0)));
            }
        } else if (contents.startsWith("Completed")) {
            final String mainTask = contents.substring(10, contents.indexOf(":"));
            final TaskProgress taskProgress = tasksInProgress.get(mainTask);
            if (taskProgress != null) {
                double mainProgress = taskProgress.getProgress();
                tasksInProgress.put(mainTask,
                        new TaskProgress(mainTask,
                                mainProgress,
                                new SubTaskProgress(contents.substring(contents.indexOf(":") + 1),
                                        100.0)));
            }
        } else if (contents.startsWith("[")) {
            // "[%s: %s] - %s: %s"
            final int firstSeparator = contents.indexOf(":");
            final String mainTask = contents.substring(1, firstSeparator);
            final double mainProgress = Double.parseDouble(contents.substring(firstSeparator + 2,
                    contents.indexOf("]", firstSeparator)));
            final int secondSeparator = contents.lastIndexOf(":");
            final String subTask = contents.substring(contents.indexOf("-") + 2, secondSeparator);
            final double subTaskProgress = Double.parseDouble(contents.substring(secondSeparator + 2));
            tasksInProgress.put(mainTask, new TaskProgress(mainTask, mainProgress, subTask, subTaskProgress));
        } else {
            final int firstSeparator = contents.indexOf(":");
            final String mainTask = contents.substring(0, firstSeparator);
            final double mainProgress = Double.parseDouble(contents.substring(firstSeparator + 2));
            tasksInProgress.put(mainTask, new TaskProgress(mainTask, mainProgress, null));
        }
    }

    @Override
    public List<TaskProgress> getRunningTasks() {
        return new ArrayList<>(tasksInProgress.values());
    }
}
