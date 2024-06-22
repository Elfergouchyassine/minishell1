import java.util.ArrayList;
import java.util.List;

public class CommandHistory {
    private List<String> history = new ArrayList<>();
    private int history_index = -1;

    public void add_command(String command) {
        history.add(command);
        history_index = history.size();
    }

    public String get_history() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(i + 1).append(": ").append(history.get(i)).append("\n");
        }
        return sb.toString();
    }

    public String get_previous_command() {
        if (history_index > 0) {
            history_index--;
            return history.get(history_index);
        }
        return null;
    }

    public String get_next_command() {
        if (history_index < history.size() - 1) {
            history_index++;
            return history.get(history_index);
        }
        return null;
    }
}