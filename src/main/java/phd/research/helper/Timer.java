package phd.research.helper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Jordan Doyle
 */

public class Timer {

    private final DateTimeFormatter formatter;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Timer() {
        formatter = DateTimeFormatter.ofPattern("dd/MM/yy-HH:mm:ss");
    }

    @SuppressWarnings("unused")
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @SuppressWarnings("unused")
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @SuppressWarnings("unused")
    public DateTimeFormatter getFormatter() {
        return this.formatter;
    }

    public String start() {
        return start(false);
    }

    public String start(boolean reset) {
        if (this.startTime != null && this.endTime == null) {
            throw new RuntimeException("Timer has been started but not ended, cannot restart before timer ends.");
        }

        if (this.startTime != null) {
            if (reset) {
                resetDateTimer();
            } else {
                throw new RuntimeException("Timer already started. Use reset parameter to restart the timer.");
            }
        }

        this.startTime = LocalDateTime.now();
        return this.formatter.format(this.startTime);
    }

    public String end() {
        if (this.startTime == null) {
            throw new RuntimeException("Timer has not been started, cannot end timer.");
        }

        if (this.endTime != null) {
            throw new RuntimeException("Timer already ended. Reset timer and use start method.");
        }

        this.endTime = LocalDateTime.now();
        return this.formatter.format(this.endTime);
    }


    public long secondsDuration() {
        if (this.startTime == null) {
            throw new RuntimeException("Timer has not been started, cannot get duration.");
        }

        if (this.endTime == null) {
            throw new RuntimeException("Timer has not been ended, cannot get duration.");
        }

        return Duration.between(this.startTime, this.endTime).getSeconds();
    }

    public void resetDateTimer() {
        this.startTime = null;
        this.endTime = null;
    }
}
