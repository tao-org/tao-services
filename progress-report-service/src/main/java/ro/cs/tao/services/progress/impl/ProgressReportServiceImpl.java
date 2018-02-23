package ro.cs.tao.services.progress.impl;

import org.springframework.stereotype.Service;
import ro.cs.tao.messaging.*;
import ro.cs.tao.services.interfaces.ProgressReportService;

import java.util.*;

@Service("progressReportService")
public class ProgressReportServiceImpl extends Notifiable implements ProgressReportService {
    private static final String TOPIC_PATTERN = "(.+)progress";
    private static final Map<String, TaskProgress> tasksInProgress = Collections.synchronizedMap(new LinkedHashMap<>());

    public ProgressReportServiceImpl() {
        Messaging.subscribe(this, TOPIC_PATTERN);
    }

    @Override
    protected void onMessageReceived(Message message) {
        logger.info(message.getItem(Message.PAYLOAD_KEY));
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
