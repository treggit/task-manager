import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonUtills {
    public static List<Task> load(final String filename) {
        Gson gson = new Gson();
        List<Task> loaded = new ArrayList<>();
        try (JsonReader reader = gson.newJsonReader(Files.newBufferedReader(Paths.get(filename)))) {
            Task[] raw = gson.fromJson(reader, Task[].class);
            if (raw == null) {
                throw new IOException("file is empty");
            }
            loaded = new ArrayList<>(Arrays.asList(raw));

            System.out.println("Loaded tasks from file " + filename + " successfully");
        } catch (IOException e) {
            System.out.println("Couldn't load tasks list: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            System.out.println("Error parsing json: " + e.getMessage());
        }

        return loaded;
    }

    public static void store(List<Task> tasks, final String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (JsonWriter writer = gson.newJsonWriter(Files.newBufferedWriter(Paths.get(filename)))) {
            gson.toJson(tasks.toArray(), Task[].class, writer);
            System.out.println("Changes were saved successfully");
        } catch (IOException e) {
            System.out.println("Couldn't save changes to disk: " + e.getMessage());
        }
    }
}
