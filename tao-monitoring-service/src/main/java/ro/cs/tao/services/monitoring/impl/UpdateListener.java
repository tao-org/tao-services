package ro.cs.tao.services.monitoring.impl;

public interface UpdateListener {
    public class AskForPassword extends Event {
        public AskForPassword() {
            super(null);
        }
    }
    public enum Stream { STDOUT, STDERR };
    public static abstract class Event {
        final Stream stream;
        public Event(Stream stream) {
            this.stream = stream;
        }

        public Stream getStream() {
            return stream;
        }
    };
    public static class BytesEvent extends Event {
        public final byte[] bytes;
        public BytesEvent(Stream stream, byte[] bytes) {
            super(stream);
            this.bytes = bytes;
        }
    }
    public static class EofEvent extends Event {
        public EofEvent(Stream stream) {
            super(stream);
        }
    }
    public static class ErrorEvent extends Event {
        private Exception error;
        public ErrorEvent(Exception error) {
            super(null);
            this.error = error;
        }

        public Exception getError() {
            return error;
        }
    }
    public void update(Event bytesEvent);
}