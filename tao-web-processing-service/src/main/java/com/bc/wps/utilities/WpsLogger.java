package com.bc.wps.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class WpsLogger {
   private static final Logger wpsLogger = createLogger();

   public static Logger getLogger() {
      return wpsLogger;
   }

   private static Logger createLogger() {
      Logger logger = Logger.getLogger("com.bc.wps");
      logger.setUseParentHandlers(false);
      Handler handler = new ConsoleHandler();
      handler.setFormatter(new LogFormatter());
      logger.addHandler(handler);
      logger.setLevel(Level.ALL);
      return logger;
   }

   private static class LogFormatter extends Formatter {
      private static final SimpleDateFormat LOG_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss,SSS");

      private LogFormatter() {
      }

      public String format(LogRecord logRecord) {
         StringBuilder sb = new StringBuilder(MessageFormat.format("{0} {1} {2}: {3}\n", LOG_TIMESTAMP_FORMAT.format(new Date(logRecord.getMillis())), logRecord.getLevel(), logRecord.getLoggerName(), logRecord.getMessage()));
         Throwable throwable = logRecord.getThrown();
         if (throwable != null) {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            sb.append(writer.toString());
            sb.append("\n");
         }

         return sb.toString();
      }
   }
}
