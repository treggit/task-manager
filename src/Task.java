import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Task {
    private int id;
    private String title, details;
    private boolean done = false;
    private Date deadline;

    private static final int TITLE_MAX_SIZE = 20;
    private static final int DETAILS_MAX_SIZE = 200;
    private static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final String NEWLINE = System.lineSeparator();

    public Task(int id) {
        this.id = id;
    }

    public Task(int id, String title, String details, String deadline) throws TaskException {
        this.id = id;
        if (title == null || title.isEmpty()) {
            this.title = "Untitled";
        } else {
            this.title = title;
        }
        this.details = details;

        if (deadline != null) {
            try {
                this.deadline = new SimpleDateFormat(DATE_FORMAT).parse(deadline);
            } catch (ParseException e) {
                throw new TaskException("Illegal date format");
            }
        }
        if (this.title.length() > TITLE_MAX_SIZE) {
            throw new TaskException("Title size cannot exceed " + TITLE_MAX_SIZE);
        }

        if (this.details != null && this.details.length() > DETAILS_MAX_SIZE) {
            throw new TaskException("Details size cannot exceed " + DETAILS_MAX_SIZE);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getDeadline() {
        if (deadline == null) {
            return "no";
        } else {
            return deadline.toString();
        }
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public boolean isExpired() {
        return deadline != null && new Date().after(deadline);
    }

    public String display() {
        StringBuilder sb = new StringBuilder("############")
                .append(NEWLINE)
                .append("Task ")
                .append(getId());
        if (isDone()) {
            sb.append(" (done)");
        } else {
            if (isExpired()) {
                sb.append(" (expired)");
            }
        }
        sb.append(NEWLINE)
                .append("\"")
                .append(getTitle())
                .append("\"")
                .append(NEWLINE);
        if (getDetails() != null && !getDetails().isEmpty()) {
            sb.append(getDetails())
                    .append(NEWLINE);
        }

        sb.append("Deadline: ")
                .append(getDeadline())
                .append(NEWLINE);

        return sb.toString();
    }
}
