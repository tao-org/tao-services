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
import ro.cs.tao.messaging.progress.*;
import ro.cs.tao.serialization.JsonMapper;
import ro.cs.tao.services.interfaces.ProgressReportService;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("progressReportService")
public class ProgressReportServiceImpl extends Notifiable implements ProgressReportService {
    private static final Pattern DOWNLOAD_PATTERN = Topic.getCategoryPattern(Topic.PROGRESS);
    private static final Pattern TRANSFER_PATTERN = Topic.getCategoryPattern(Topic.TRANSFER_PROGRESS);
    private static final Map<String, TaskProgress> tasksInProgress = Collections.synchronizedMap(new LinkedHashMap<>());
    private String previousMessage;

    public ProgressReportServiceImpl() {
        Messaging.subscribe(this, DOWNLOAD_PATTERN);
        Messaging.subscribe(this, TRANSFER_PATTERN);
    }

    @Override
    protected void onMessageReceived(Message message) {
        final String contents = message.getData();
        if (!Objects.equals(contents, this.previousMessage)) {
            logger.finest(contents);
            this.previousMessage = contents;
        }
        String taskName;
        final String category = message.getTopic();
        TaskProgress taskProgress;
        if (message instanceof ActivityStart) {
            taskName = ((ActivityStart) message).getTaskName();
            taskProgress = new TaskProgress(taskName, category, 0);
            taskProgress.setInfo(message.getItems());
            tasksInProgress.put(taskName, taskProgress);
        } else if (message instanceof ActivityEnd) {
            tasksInProgress.remove(((ActivityEnd) message).getTaskName());
        } else if (message instanceof SubActivityStart) {
            SubActivityStart casted = (SubActivityStart) message;
            taskName = casted.getTaskName();
            if (tasksInProgress.containsKey(taskName)) {
                double mainProgress = tasksInProgress.get(taskName).getProgress();
                taskProgress = new TaskProgress(taskName, category, mainProgress, casted.getSubTaskName(), 0.0);
                taskProgress.setInfo(message.getItems());
                tasksInProgress.put(taskName, taskProgress);
            }
        } else if (message instanceof SubActivityEnd) {
            SubActivityEnd casted = (SubActivityEnd) message;
            taskName = casted.getTaskName();
            final TaskProgress existingProgress = tasksInProgress.get(taskName);
            if (existingProgress != null) {
                double mainProgress = existingProgress.getProgress();
                if (1.0 - mainProgress < 0.001) {
                    tasksInProgress.remove(taskName);
                } else {
                    taskProgress = new TaskProgress(taskName, category, mainProgress, casted.getSubTaskName(), 1.0);
                    taskProgress.setInfo(message.getItems());
                    tasksInProgress.put(taskName, taskProgress);
                }
            }
        } else if (message instanceof SubActivityProgress) {
            SubActivityProgress casted = (SubActivityProgress) message;
            taskName = casted.getTaskName();
            final TaskProgress existingProgress = tasksInProgress.get(taskName);
            if (existingProgress != null) {
                double mainProgress = existingProgress.getProgress();
                if (casted.getSubTaskProgress() == 1.0 && (1.0 - mainProgress < 0.001)) {
                    tasksInProgress.remove(taskName);
                } else {
                    taskProgress = new TaskProgress(taskName, category, casted.getTaskProgress(), casted.getSubTaskName(), casted.getSubTaskProgress());
                    taskProgress.setInfo(message.getItems());
                    tasksInProgress.put(taskName, taskProgress);
                }
            }
        } else if (message instanceof ActivityProgress) {
            ActivityProgress casted = (ActivityProgress) message;
            taskName = casted.getTaskName();
            taskProgress = new TaskProgress(taskName, category, casted.getProgress());
            taskProgress.setInfo(casted.getItems());
            if (message instanceof DownloadProgress) {
                taskProgress.addInfo("speed", ((DownloadProgress) message).getTransferSpeedMB());
            }
            tasksInProgress.put(taskName, taskProgress);
        }
    }

    @Override
    public List<TaskProgress> getRunningTasks(String category, String jsonFilter) {
        Filter filter = null;
        if (jsonFilter != null) {
            try {
                filter = JsonMapper.instance().readerFor(Filter.class).readValue(jsonFilter);
            } catch (Exception e) {
                logger.warning(String.format("Invalid filter [%s]", e.getMessage()));
            }
        }
        Stream<TaskProgress> results;
        if (category != null && !category.isEmpty()) {
            results = tasksInProgress.values().stream().filter(p -> category.equals(p.getCategory()));
        } else {
            results = tasksInProgress.values().stream();
        }
        if (filter != null) {
            final Filter f = filter;
            results = results.filter(p -> {
                String value = p.getInfo(f.getName());
                return value != null && value.equals(f.getValue());
            });
        }
        return results.collect(Collectors.toList());
    }
}
