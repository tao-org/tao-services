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
import ro.cs.tao.messaging.Message;
import ro.cs.tao.messaging.Messaging;
import ro.cs.tao.messaging.Notifiable;
import ro.cs.tao.messaging.TaskProgress;
import ro.cs.tao.messaging.progress.*;
import ro.cs.tao.services.interfaces.ProgressReportService;

import java.util.*;
import java.util.regex.Pattern;

@Service("progressReportService")
public class ProgressReportServiceImpl extends Notifiable implements ProgressReportService {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(.+)progress");
    private static final Map<String, TaskProgress> tasksInProgress = Collections.synchronizedMap(new LinkedHashMap<>());
    private String previousMessage;

    public ProgressReportServiceImpl() {
        Messaging.subscribe(this, TOPIC_PATTERN);
    }

    @Override
    protected void onMessageReceived(Message message) {
        final String contents = message.getItem(Message.PAYLOAD_KEY);
        if (!Objects.equals(contents, this.previousMessage)) {
            logger.fine(contents);
            this.previousMessage = contents;
        }
        String taskName;
        if (message instanceof ActivityStartMessage) {
            taskName = ((ActivityStartMessage) message).getTaskName();
            tasksInProgress.put(taskName, new TaskProgress(taskName, 0));
        } else if (message instanceof ActivityEndMessage) {
            tasksInProgress.remove(((ActivityEndMessage) message).getTaskName());
        } else if (message instanceof SubActivityStartMessage) {
            SubActivityStartMessage casted = (SubActivityStartMessage) message;
            taskName = casted.getTaskName();
            if (tasksInProgress.containsKey(taskName)) {
                double mainProgress = tasksInProgress.get(taskName).getProgress();
                tasksInProgress.put(taskName,
                                    new TaskProgress(taskName, mainProgress, casted.getSubTaskName(), 0.0));
            }
        } else if (message instanceof SubActivityEndMessage) {
            SubActivityEndMessage casted = (SubActivityEndMessage) message;
            taskName = casted.getTaskName();
            final TaskProgress taskProgress = tasksInProgress.get(taskName);
            if (taskProgress != null) {
                double mainProgress = taskProgress.getProgress();
                tasksInProgress.put(taskName,
                                    new TaskProgress(taskName, mainProgress, casted.getSubTaskName(), 100.0));
            }
        } else if (message instanceof SubActivityProgressMessage) {
            SubActivityProgressMessage casted = (SubActivityProgressMessage) message;
            taskName = casted.getTaskName();
            final String subTask = casted.getSubTaskName();
            tasksInProgress.put(taskName,
                                new TaskProgress(taskName, casted.getTaskProgress(), subTask, casted.getSubTaskProgress()));
        } else if (message instanceof ActivityProgressMessage) {
            ActivityProgressMessage casted = (ActivityProgressMessage) message;
            taskName = casted.getTaskName();
            tasksInProgress.put(taskName, new TaskProgress(taskName, casted.getProgress()));
        }
    }

    @Override
    public List<TaskProgress> getRunningTasks() {
        return new ArrayList<>(tasksInProgress.values());
    }
}
