import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaskManager {
    private List<Task> tasks = new ArrayList<>();
    private final Set<Integer> toRemove = new HashSet<>();
    private final Set<Integer> wasDone = new HashSet<>();

    public static final String TODO_LIST_FILE = "todo-list.json";
    public static final int MAX_TASKS_NUMBER = 100000;
    private static final Set<String> ADD_REQUEST_OPTIONS = new HashSet<>(Arrays.asList("-t", "-dt", "-dl"));
    private static final int UNSTORED_CHANGES_BUFFER_SIZE = 20;
    private int modifications = 0;

    public TaskManager() {
        load(TODO_LIST_FILE);
    }

    List<Task> load(final String filename) {
        List<Task> loaded = JsonUtills.load(filename);

        if (loaded.isEmpty()) {
            System.out.println("Warning: " + filename + " does not contain tasks");
        }
        int left = MAX_TASKS_NUMBER - tasks.size();
        if (loaded.size() > left) {
            while (loaded.size() > left) {
                loaded.remove(loaded.size() - 1);
            }
            System.out.println("Warning: not all tasks were loaded as the maximum possible tasks number is " + MAX_TASKS_NUMBER);
        }

        for (Task task : loaded) {
            task.setId(getAvailableId());
            tasks.add(task);
        }

        return loaded;
    }

    /* Alternative way to implement load from json. Using this one, we can avoid problems of very huge files, passed to enter
       But it looks just ... ugly
     */

//    private List<Task> load(final String filename) {
//        List<Task> loaded = new ArrayList<>();
//        try (JsonReader reader = gson.newJsonReader(Files.newBufferedReader(Paths.get(filename)))) {
//            Task task;
//            reader.beginArray();
//            while (loaded.size() + tasks.size() < MAX_TASKS_NUMBER && reader.hasNext()) {
//                task = gson.fromJson(reader, Task.class);
//                if (!taskExists(task.getId())) {
//                    loaded.add(task);
//                }
//            }
//
//            if (gson.fromJson(reader, Task.class) != null) {
//                System.out.println("Warning: not all tasks were loaded as the maximum possible tasks number is " + MAX_TASKS_NUMBER);
//            }
//            System.out.println("Loaded tasks from file " + filename + " successfully");
//        } catch (IOException e) {
//            System.out.println("Couldn't load tasks list: " + e.getMessage());
//        } catch (IllegalStateException ignored) {
//
//        }
//
//        if (loaded.isEmpty()) {
//            System.out.println("Warning: " + filename + " is empty");
//        }
//
//        return loaded;
//    }

    private void pushChanges() {
        tasks = tasks.stream()
                .filter(task -> !toRemove.contains(task.getId()))
                .collect(Collectors.toList());

        for (Task task : tasks){
            if (wasDone.contains(task.getId())) {
                task.setDone(true);
            }
        }

        toRemove.clear();
        wasDone.clear();
    }


    /* I guess, this function could be delegated to another class, which might work asynchronously and store the changes time after time.
       I would implement it, if I had little more time
     */

     void storeChanges(boolean forced) {
        if (forced || modifications > UNSTORED_CHANGES_BUFFER_SIZE) {
            System.out.println("Saving latest changes to " + TODO_LIST_FILE + "...");
            pushChanges();
            JsonUtills.store(tasks, TODO_LIST_FILE);
            modifications = 0;
        }
    }

    private void save(Task task) {
        if (tasks.size() == MAX_TASKS_NUMBER) {
            System.out.println("The task is not saved as the maximum possible tasks number is " + MAX_TASKS_NUMBER);
        }
        tasks.add(task);
        modifications++;
        storeChanges(false);
        System.out.println("Task was added successfully");
    }

    private int getAvailableId() {
        return (tasks.isEmpty() ? 1 : tasks.get(tasks.size() - 1).getId() + 1);
    }

    void add(final String req) {
        try {
            Map<String, String> optionsVals = new Parser(ADD_REQUEST_OPTIONS).parse(req);
            String title = optionsVals.get("-t");
            String details = optionsVals.get("-dt");
            String deadline = optionsVals.get("-dl");

            save(new Task(getAvailableId(), title, details, deadline));
        } catch (ParserException | TaskException e) {
            System.out.println("Couldn't parse add request options: " + e.getMessage());
        }
    }

    private boolean taskExists(Task task) {
        return (Collections.binarySearch(tasks, task, Comparator.comparingInt(Task::getId)) >= 0);
    }

    void remove(int id) {
        if (!taskExists(new Task(id))) {
            System.out.println("No task with id " + id);
            return;
        }
        wasDone.remove(id);
        toRemove.add(id);
        modifications++;
        storeChanges(false);
        System.out.println("Task " + id + " was removed successfully");
    }


    String list(Set<String> flags) {
        if (flags.isEmpty()) {
            flags.add("all");
        }
        StringBuilder sb = new StringBuilder();
        pushChanges();
        for (Task task : tasks) {
            if (flags.contains("all") || (flags.contains("done") && task.isDone())
            || (flags.contains("undone") && !task.isDone()) || (flags.contains("expired") && task.isExpired() && !task.isDone())) {
                sb.append(task.display());
            }
        }

        return sb.toString();
    }

    void markAsDone(int id) {
        if (!taskExists(new Task(id))) {
            System.out.println("No task with id " + id);
        }
        wasDone.add(id);
        modifications++;
        storeChanges(false);
        System.out.println("Task " + id + " was marked as done");
    }

    private Stream<String> getArgs(final String request) {
        return Arrays.stream(request
                .split(" "))
                .filter(Predicate.not(String::isEmpty));
    }

    public boolean processRequest(final String request) {
        if (request.startsWith("exit")) {
            return false;
        }

        if (request.startsWith("load")) {
            getArgs(request.substring(4))
                    .forEach(this::load);
            return true;
        }

        if (request.startsWith("add")) {
            add(request.substring(3));
            return true;
        }

        try {
            if (request.startsWith("remove")) {
                getArgs(request.substring(6))
                        .map(Integer::valueOf)
                        .forEach(this::remove);
                return true;
            }

            if (request.startsWith("done")) {
                getArgs(request.substring(4))
                        .map(Integer::valueOf)
                        .forEach(this::markAsDone);
                return true;
            }
        } catch (NumberFormatException e) {
            System.out.println("Only id numbers expected in this request");
        }


        if (request.startsWith("list")) {
            System.out.print(list(getArgs(request.substring(4)).collect(Collectors.toSet())));
            return true;
        }

        System.out.println("Unsupported operation");

        return true;
    }

    public static void main(String[] args) {
        TaskManager taskManager = new TaskManager();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                try {
                    if (!taskManager.processRequest(reader.readLine())) {
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Couldn't fetch the request: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage() + "; program will be terminated");
        } finally {
            taskManager.storeChanges(true);
        }
    }

}
