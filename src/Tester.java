import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class Tester {

    private static final String TOKENS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz     ";
    private static SecureRandom rnd = new SecureRandom();
    private static final String list1 = "todo-list.json";
    private static final String list2 = "todo-list.json2";
    private static final String list3 = "todo-list.json3";
    private static final List<String> modifications = Arrays.asList("add", "remove", "done");
    private static final List<String> lists = Arrays.asList("all", "done", "undone", "expired");

    private String randomString(int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++) {
            char c = TOKENS.charAt(rnd.nextInt(TOKENS.length()));
            while (i > 0 && Character.isWhitespace(c) && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                c = TOKENS.charAt(rnd.nextInt(TOKENS.length()));
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private int randomInt(int limit) {
        return rnd.nextInt(limit);
    }

    private String randomDate() {
        return (10 + randomInt(10)) + "/" + (1 + randomInt(12)) + "/" + (2000 + randomInt(30));
    }

    private Task generateTask(int limitId) {
        int id = randomInt(limitId);
        String title = randomString(10);
        String details = randomString(randomInt(256));
        boolean done = randomInt(2) == 0;

        String deadline = randomDate();

        Task task = null;
        try {
            task = new Task(id, title, details, deadline);
            task.setDone(done);
        } catch (TaskException ignored) {
        }

        return task;
    }

    private List<Task> generateTasks(final int number) {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            Task task = null;
            while (task == null) {
                task = generateTask(number);
            }
            tasks.add(task);
        }

        return tasks;
    }

    private List<Task> basicGeneration(int number) {
        List<Task> tasks = new ArrayList<>();
        List<Task> t1 = generateTasks(number);
        List<Task> t2 = generateTasks(number);
        List<Task> t3 = generateTasks(number);

        JsonUtills.store(t1, list1);
        JsonUtills.store(t2, list2);
        JsonUtills.store(t3, list3);

        tasks.addAll(t1);
        tasks.addAll(t2);
        tasks.addAll(t3);

        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).setId(i + 1);
        }

        while (tasks.size() > TaskManager.MAX_TASKS_NUMBER) {
            tasks.remove(tasks.size() - 1);
        }

        return tasks;
    }

    private String getExpectedList(List<Task> tasks, Set<String> flags) {
        StringBuilder expected = new StringBuilder();
        for (Task task : tasks) {
            if (flags.contains("all") || (flags.contains("done") && task.isDone())
                    || (flags.contains("undone") && !task.isDone()) || (flags.contains("expired") && task.isExpired() && !task.isDone())) {
                expected.append(task.display());
            }
        }

        return expected.toString();
    }

    private TaskManager initManager() {
        TaskManager manager = new TaskManager();
        manager.processRequest("load " + list2 + " " + list3);
        return manager;
    }

    private void check(List<Task> tasks, TaskManager manager, Set<String> listOptions) {
        final String actual = manager.list(listOptions);
        final String expected = getExpectedList(tasks, listOptions);
        Assert.assertEquals(actual, expected);
    }

    @Test
    public void loadTest() {
        int testNumber = 20;
        while (testNumber-- > 0) {
            final int number = 100;
            List<Task> tasks = basicGeneration(number);

            TaskManager manager = initManager();

            check(tasks, manager, new HashSet<>());
        }
    }

    @Test
    public void loadLargeTest() {
        final int number = TaskManager.MAX_TASKS_NUMBER / 2;
        List<Task> tasks = basicGeneration(number);

        TaskManager manager = initManager();

        check(tasks, manager, new HashSet<>());

    }

    private int getAvailableId(List<Task> tasks) {
        return (tasks.isEmpty() ? 0 : tasks.get(tasks.size() - 1).getId()) + 1;
    }

    private String generateRequest(List<Task> tasks, Set<Integer> toRemove, Set<Integer> wasDone) {
        final String req = modifications.get(rnd.nextInt(3));
        if (req.equals("add")) {
            String title = randomString(10);
            String details = randomString(randomInt(256));
            String deadline = randomDate();

            try {
                tasks.add(new Task(getAvailableId(tasks), title, details, deadline));
            } catch (TaskException ignored) {
            }

            return "add " + randomString(10) + " -t " + title + " -dt " + details + " -dl " + deadline + " " + randomString(10);
        }

        if (req.equals("remove")) {
            final int ids = 1 + randomInt(3);
            StringBuilder sb = new StringBuilder("remove");
            for (int i = 0; i < ids; i++) {
                int id = 1 + randomInt(tasks.size() - 1);
                sb.append(" ").append(id);
                toRemove.add(id);
            }

            return sb.toString();
        }

        if (req.equals("done")) {
            final int ids = 1 + randomInt(3);
            StringBuilder sb = new StringBuilder("done");
            for (int i = 0; i < ids; i++) {
                int id = 1 + randomInt(tasks.size() - 1);
                sb.append(" ").append(id);
                wasDone.add(id);
            }

            return sb.toString();
        }

        return null;
    }

    @Test
    public void modificationsTest() {
        int testNumber = 100;

        final int size = 100;
        final int opNumber = 50;

        while (testNumber-- > 0) {
            List<Task> tasks = basicGeneration(size);
            Set<Integer> toRemove = new HashSet<>();
            Set<Integer> wasDone = new HashSet<>();
            TaskManager manager = initManager();
            for (int i = 0; i < opNumber; i++) {
                manager.processRequest(generateRequest(tasks, toRemove, wasDone));
            }

            manager.storeChanges(true);

            tasks = tasks.stream()
                    .filter(task -> !toRemove.contains(task.getId()))
                    .collect(Collectors.toList());

            tasks.forEach(task -> {
                if (wasDone.contains(task.getId())) {
                    task.setDone(true);
                }
            });

            Set<String> listOptions = new HashSet<>();
            listOptions.add(lists.get(randomInt(lists.size())));
            listOptions.add(lists.get(randomInt(lists.size())));
            check(tasks, manager, listOptions);
        }
    }
}
