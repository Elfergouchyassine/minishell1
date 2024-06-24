import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleShell extends JFrame {
    private CommandHistory ch = new CommandHistory();
    private String home_path = System.getProperty("user.home");
    private String current_path = System.getProperty("user.dir");
    private File dir = new File(current_path);
    private JTextArea outputArea;
    private JTextField commandField;

    public SimpleShell() {
        setTitle("Simple Shell");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        commandField = new JTextField();

        commandField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = commandField.getText();
                if (!line.equals("")) {
                    processCommand(line);
                    commandField.setText("");
                }
            }
        });

        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    String previousCommand = ch.get_previous_command();
                    if (previousCommand != null) {
                        commandField.setText(previousCommand);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    String nextCommand = ch.get_next_command();
                    if (nextCommand != null) {
                        commandField.setText(nextCommand);
                    }
                }
            }
        });

        JButton executeButton = new JButton("Execute");
        executeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String line = commandField.getText();
                if (!line.equals("")) {
                    processCommand(line);
                    commandField.setText("");
                }
            }
        });

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(commandField, BorderLayout.CENTER);
        inputPanel.add(executeButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        updatePrompt();
    }

    private void updatePrompt() {
        outputArea.append(current_path + "$ ");
    }

    private void processCommand(String line) {
        outputArea.append(line + "\n");
        if (line.equals("exit") || line.equals("quit")) {
            System.exit(0);
        }

        ch.add_command(line);
        ArrayList<String> command_parts = new ArrayList<>();
        String main_operation = prepare_command(line, command_parts);

        try {
            if (line.startsWith("./") && line.endsWith(".sh")) {
                executeScriptFile(line.substring(2)); // Execute script file
            } else if (line.contains("|")) {
                handlePipelining(line);
            } else if (line.contains(">") || line.contains(">>") || line.contains("<")) {
                handleRedirection(line);
            } else {
                executeCommand(line, command_parts, main_operation);
            }
        } catch (Exception e) {
            outputArea.append("Error: " + e.getMessage() + "\n");
        }

        updatePrompt();
    }
    private void executeScriptFile(String fileName) throws IOException {
        File scriptFile = new File(dir, fileName);
        if (!scriptFile.exists() || !scriptFile.isFile() || !fileName.endsWith(".sh")) {
            outputArea.append("Error: Script file does not exist or is not a valid .sh file\n");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
        String line;
        while ((line = reader.readLine()) != null) {
            processCommand(line);
        }
        reader.close();
    }



    private void executeCommand(String line, ArrayList<String> command_parts, String main_operation) throws IOException {
        if (main_operation.equals("history")) {
            outputArea.append(ch.get_history() + "\n");
        } else if (main_operation.equals("cd")) {
            if (command_parts.size() < 4) {
                outputArea.append("invalid cd command! hint: you need to provide new directory\n");
                return;
            }

            String new_path = command_parts.get(3).replaceAll("^\"|\"$", "");
            if (new_path.equals("~") || new_path.equals("$HOME")) {
                new_path = home_path;
            }

            Path p = Paths.get(new_path);
            File new_dir;
            if (p.isAbsolute())
                new_dir = new File(new_path);
            else
                new_dir = new File(dir, new_path);

            if (!new_dir.exists() || !new_dir.isDirectory()) {
                outputArea.append("Error: invalid directory requested\n");
            } else {
                dir = new_dir;
                current_path = dir.getAbsolutePath();
            }
        } else if (main_operation.equals("ls")) {
            listDirectory(dir);
        } else if (main_operation.equals("cat")) {
            if (command_parts.size() < 4) {
                outputArea.append("invalid cat command! hint: you need to provide a file name\n");
                return;
            }

            String file_name = command_parts.get(3);
            File file = new File(dir, file_name);

            if (!file.exists() || !file.isFile()) {
                outputArea.append("Error: file does not exist\n");
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line_content;
                while ((line_content = reader.readLine()) != null) {
                    outputArea.append(line_content + "\n");
                }
                reader.close();
            }
        } else if (main_operation.equals("grep")) {
            if (command_parts.size() < 5) {
                outputArea.append("invalid grep command! hint: you need to provide a word and a file name\n");
                return;
            }

            String word = command_parts.get(3);
            String file_name = command_parts.get(4);
            File file = new File(dir, file_name);

            if (!file.exists() || !file.isFile()) {
                outputArea.append("Error: file does not exist\n");
            } else {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line_content;
                while ((line_content = reader.readLine()) != null) {
                    outputArea.append(highlightWord(line_content, word) + "\n");
                }
                reader.close();
            }
        } else if (main_operation.equals("touch")) {
            if (command_parts.size() < 4) {
                outputArea.append("invalid touch command! hint: you need to provide a file name\n");
                return;
            }

            String file_name = command_parts.get(3);
            File file = new File(dir, file_name);
            if (file.exists()) {
                outputArea.append("Error: file already exists\n");
            } else {
                file.createNewFile();
                outputArea.append("File " + file_name + " created successfully\n");
            }
        } else if (main_operation.equals("rm")) {
            if (command_parts.size() < 4) {
                outputArea.append("invalid rm command! hint: you need to provide a file name\n");
                return;
            }

            String file_name = command_parts.get(3);
            File file = new File(dir, file_name);

            if (!file.exists()) {
                outputArea.append("Error: file does not exist\n");
            } else {
                if (file.delete()) {
                    outputArea.append("File " + file_name + " deleted successfully\n");
                } else {
                    outputArea.append("Error: failed to delete file\n");
                }
            }
        } else {
            ProcessBuilder pb = new ProcessBuilder(command_parts);
            pb.directory(dir);
            Process process = pb.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String ln;
            while ((ln = br.readLine()) != null)
                outputArea.append(ln + "\n");
            br.close();

            current_path = dir.getCanonicalPath();
        }
    }

    private void handleRedirection(String line) throws IOException {
        String[] parts;
        boolean append = false;
        if (line.contains(">>")) {
            parts = line.split(">>");
            append = true;
        } else if (line.contains(">")) {
            parts = line.split(">");
        } else if (line.contains("<")) {
            parts = line.split("<");
        } else {
            return;
        }

        String commandPart = parts[0].trim();
        String filePart = parts[1].trim();

        if (line.contains(">") || line.contains(">>")) {
            executeCommandWithOutputRedirection(commandPart, filePart, append);
        } else if (line.contains("<")) {
            executeCommandWithInputRedirection(commandPart, filePart);
        }
    }

    private void executeCommandWithOutputRedirection(String commandPart, String filePart, boolean append) throws IOException {
        File outputFile = new File(dir, filePart);
        ArrayList<String> command_parts = new ArrayList<>();
        String main_operation = prepare_command(commandPart, command_parts);

        if (main_operation.equals("cat") && command_parts.size() > 3) {
            String inputFileName = command_parts.get(3);
            File inputFile = new File(dir, inputFileName);

            if (!inputFile.exists() || !inputFile.isFile()) {
                outputArea.append("Error: input file does not exist\n");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, append));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();
        } else {
            ProcessBuilder pb = new ProcessBuilder(command_parts);
            pb.directory(dir);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, append));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();
        }
    }

    private void executeCommandWithInputRedirection(String commandPart, String filePart) throws IOException {
        File inputFile = new File(dir, filePart);
        if (!inputFile.exists() || !inputFile.isFile()) {
            outputArea.append("Error: input file does not exist\n");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        ArrayList<String> command_parts = new ArrayList<>();
        String main_operation = prepare_command(commandPart, command_parts);

        ProcessBuilder pb = new ProcessBuilder(command_parts);
        pb.directory(dir);
        Process process = pb.start();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }
        reader.close();
        writer.close();

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        while ((line = br.readLine()) != null) {
            outputArea.append(line + "\n");
        }
        br.close();
    }

    private void handlePipelining(String line) throws IOException {
        String[] commands = line.split("\\|");
        if (commands.length != 2) {
            outputArea.append("Error: Only simple pipelining with one pipe is supported.\n");
            return;
        }

        ArrayList<String> command1_parts = new ArrayList<>();
        prepare_command(commands[0].trim(), command1_parts);
        ArrayList<String> command2_parts = new ArrayList<>();
        prepare_command(commands[1].trim(), command2_parts);

        if (!command2_parts.get(2).equals("grep")) {
            outputArea.append("Error: Only 'grep' is supported as the second command in a pipeline.\n");
            return;
        }

        String word = command2_parts.get(3);

        if (command1_parts.get(2).equals("cat") && command1_parts.size() > 3) {
            String file_name = command1_parts.get(3);
            File file = new File(dir, file_name);

            if (!file.exists() || !file.isFile()) {
                outputArea.append("Error: input file does not exist\n");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line_content;
            while ((line_content = reader.readLine()) != null) {
                if (line_content.contains(word)) {
                    outputArea.append(highlightWord(line_content, word) + "\n");
                }
            }
            reader.close();
        } else {
            outputArea.append("Error: Only 'cat' command is supported as the first command in a pipeline.\n");
        }
    }


    private String prepare_command(String command_line, ArrayList<String> str_list) {
        String[] str_arr = command_line.split(" "); // parsing
        str_list.add("cmd");
        str_list.add("/c");
        boolean quotation = false;
        for (String str : str_arr) {
            if (quotation) {
                int last_index = str_list.size() - 1;
                str_list.set(last_index, str_list.get(last_index) + " " + str);
            } else {
                str_list.add(str);
            }
            if (str.startsWith("\""))
                quotation = true;
            if (str.endsWith("\""))
                quotation = false;
        }
        return str_list.get(2);
    }

    private void listDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                outputArea.append(file.getName() + "\n");
            }
        }
    }

    private String highlightWord(String line, String word) {
        Pattern pattern = Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<" + matcher.group() + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


}
