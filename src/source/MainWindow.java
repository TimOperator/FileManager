package source;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;

public class MainWindow {
    private JTextField pathTextField;
    private JButton applyButton;
    private JTextField renameTextField;
    private JPanel mainPanel;
    private JButton openButton;
    private JList<String> fileList;
    private JList<String> newFileList;
    private JTextField origTextField;
    private JFileChooser fileChooser;
    private DefaultListModel<String> model;
    private DefaultListModel<String> model2;

    private MainWindow() {
        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        model = new DefaultListModel<>();
        fileList.setModel(model);
        model2 = new DefaultListModel<>();
        newFileList.setModel(model2);

        applyButton.addActionListener(actionEvent -> {
            Map<String, String> renames = updateRenaming();
            StringBuilder fileString = new StringBuilder();
            for (Map.Entry<String, String> entry : renames.entrySet()) {
                fileString.append(entry.getKey()).append(" \u2192 ").append(entry.getValue()).append("\r\n");
            }
            int res = JOptionPane.showConfirmDialog(null, "Are you sure you want to rename these files and directories?\r\n" + fileString);
            String path = pathTextField.getText();
            if (res == JOptionPane.OK_OPTION) {
                for (Map.Entry<String, String> entry : renames.entrySet()) {
                    String src = path + "\\" + entry.getKey();
                    String dst = path + "\\" + entry.getValue();
                    File file = new File(src);
                    boolean success = file.renameTo(new File(dst));
                    if (success) {
                        System.out.println("Renamed " + src + " to " + dst);
                    } else {
                        System.out.println("Unable to rename " + src + " to " + dst);
                    }
                }

                //reload content
                loadFiles(new File(pathTextField.getText()));
            }
        });
        openButton.addActionListener(actionEvent -> {
            int returnVal = fileChooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File folder = fileChooser.getSelectedFile();
                loadFiles(folder);
                updateRenaming();
            }
        });
        fileList.addListSelectionListener(listSelectionEvent -> {
            int index = fileList.getSelectedIndex();
            if (index >= 0 && index < model.getSize()) {
                origTextField.setText(model.getElementAt(index));
            }
        });
        renameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                updateRenaming();
            }
        });
        origTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                updateRenaming();
            }
        });
    }

    /**
     * Loads content of directory and displays it
     * @param folder Folder to analyse
     */
    private void loadFiles(File folder) {
        pathTextField.setText(folder.getAbsolutePath());

        model.clear();

        List<String> directories = new ArrayList<>();
        List<String> files = new ArrayList<>();

        for (final File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                directories.add(file.getName() + "/");
            } else {
                files.add(file.getName());
            }
        }
        for (Object fileName : directories) {
            model.addElement(fileName.toString());
        }
        for (Object fileName : files) {
            model.addElement(fileName.toString());
        }
    }

    /**
     * Update the list of renamed files
     */
    private Map<String, String> updateRenaming() {
        model2.clear();
        Map<String, String> renames = new HashMap<>();
        String str = origTextField.getText();
        String replacement = renameTextField.getText();
        for (int i = 0; i < model.size(); i++) {
            String name = model.getElementAt(i);
            if (name.contains(str) && str.length() > 0) {
                String tmp = name.replace(str, replacement);
                // prevent empty name
                if (tmp.length() <= 0) tmp = name;
                // prevent '/' as name
                if (tmp.equals("/")) tmp = name;
                renames.put(name, tmp);
                name = tmp;
            } else if (str.length() == 0) {
                String tmp = replacement + name;
                renames.put(name, tmp);
                name = tmp;
            }
            model2.addElement(name);
        }
        return renames;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("FileManager 1.0");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            //Doesn't matter
        }
        frame.setContentPane(new MainWindow().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(500, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
