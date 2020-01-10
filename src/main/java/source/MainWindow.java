package source;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindow {
    private static ResourceBundle strings;
    private JTextField pathTextField;
    private JButton applyButton;
    private JTextField renameTextField;
    private JPanel mainPanel;
    private JButton openButton;
    private JList<String> fileList;
    private JList<String> newFileList;
    private JTextField origTextField;
    private JScrollPane oldScrollPane;
    private JScrollPane newScrollPane;
    private JButton statisticsButton;
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
            if (pathTextField.getText().trim().length() <= 0) {
                JOptionPane.showMessageDialog(null, strings.getString("no_directory_dialog"));
                return;
            }
            if (model.size() <= 0) {
                JOptionPane.showMessageDialog(null, strings.getString("empty_list_dialog"));
                return;
            }
            Map<String, String> renames = updateRenaming();
            if (renames.size() <= 0) {
                JOptionPane.showMessageDialog(null, strings.getString("no_files_dialog"));
                return;
            }
            StringBuilder fileString = new StringBuilder();
            for (Map.Entry<String, String> entry : renames.entrySet()) {
                fileString.append(entry.getKey()).append(" \u2192 ").append(entry.getValue()).append("\r\n");
            }
            int res = JOptionPane.showConfirmDialog(null, strings.containsKey("confirm_dialog") + "\r\n" + fileString);
            String path = pathTextField.getText();
            if (res == JOptionPane.OK_OPTION) {
                List<String> errorList = new ArrayList<>();
                for (Map.Entry<String, String> entry : renames.entrySet()) {
                    String src = path + "\\" + entry.getKey();
                    String dst = path + "\\" + entry.getValue();
                    File file = new File(src);
                    boolean success = file.renameTo(new File(dst));
                    if (success) {
                        System.out.println("Renamed " + src + " to " + dst);
                    } else {
                        System.out.println("Unable to rename " + src + " to " + dst);
                        errorList.add(src + " \u2192 " + dst);
                    }
                }
                if (!errorList.isEmpty()) {
                    JOptionPane.showConfirmDialog(null, strings.containsKey("rename_error_dialog") + ":\r\n" + errorList.toString());
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
        oldScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentEvent -> newScrollPane.getVerticalScrollBar().setValue(oldScrollPane.getVerticalScrollBar().getValue()));
        newScrollPane.getVerticalScrollBar().addAdjustmentListener(adjustmentEvent -> oldScrollPane.getVerticalScrollBar().setValue(newScrollPane.getVerticalScrollBar().getValue()));
        statisticsButton.addActionListener(actionEvent -> {
            if (pathTextField.getText().trim().length() > 0) {
                String path;
                path = pathTextField.getText().trim();
                List<Directory> dirList = getDirectorySizes(path);
                StringBuilder tmp = new StringBuilder();
                for (Directory dir : dirList) {
                    tmp.append(dir.getName()).append(": \t");
                    double value = dir.getSize().doubleValue();
                    String fileSize = value + " B";
                    if (value >= 1000) {
                        // Filesize > 1 KB
                        value = value / 1000;
                        value = Math.round(value * 100.0) / 100.0;
                        fileSize = value + " KB";

                        if (value >= 1000) {
                            // Filesize > 1 MB
                            value = value / 1000;
                            value = Math.round(value * 100.0) / 100.0;
                            fileSize = value + " MB";

                            if (value >= 1000) {
                                // Filesize > 1 GB
                                value = value / 1000;
                                value = Math.round(value * 100.0) / 100.0;
                                fileSize = value + " GB";
                            }
                        }

                    }
                    tmp.append(fileSize).append("\n");
                }
                JOptionPane.showMessageDialog(null, tmp.toString());
            }

        });
    }

    /**
     * Determines the size of each directory in the given path
     *
     * @param path Path to directory which should be analyzed
     * @return List of files and directories with corresponding size, ordered by size
     */
    private static List<Directory> getDirectorySizes(String path) {
        File directory = new File(path);
        List<Directory> fList = new ArrayList<>();

        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile()) {
                fList.add(new Directory(file.getName(), file.length()));
            } else if (file.isDirectory()) {
                Long directorySize = getFolderSize(file);
                fList.add(new Directory(file.getName(), directorySize));
            } else {
                System.out.println(strings.containsKey("UnsupportedFileType") + ": " + file.getName());
            }
        }

        class CustomComparator implements Comparator<Directory> {
            @Override
            public int compare(Directory o1, Directory o2) {
                return o2.getSize().compareTo(o1.getSize());
            }
        }

        Collections.sort(fList, new CustomComparator());

        return fList;
    }

    /**
     * Allocates all files in a directory and sum up their size
     *
     * @param directory Directory to be measured
     * @return Size of the directory
     */
    private static long getFolderSize(File directory) {
        long length = 0;
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isFile())
                length += file.length();
            else
                length += getFolderSize(file);
        }
        return length;
    }

    public static void main(String[] args) {
        // Load Strings
        String language = "";
        try {
            language = System.getProperty("user.language");
            String filename = "strings_" + language;
            strings = ResourceBundle.getBundle(filename);
        } catch (MissingResourceException ex) {
            System.out.println("Could not load language " + language + ": " + ex.toString());
            strings = ResourceBundle.getBundle("strings_en");
        }

        // Create Frame
        JFrame frame = new JFrame(strings.getString("program_title"));

        // Set os layout
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            //Doesn't matter
        }

        // Create window
        frame.setContentPane(new MainWindow().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(500, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Loads content of directory and displays it
     *
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
                if (replacement.length() > 0) {
                    renames.put(name, tmp);
                }
                name = tmp;
            }
            model2.addElement(name);
        }
        return renames;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setAlignmentX(0.5f);
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, -1, -1, mainPanel.getFont())));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(5, 5, 0, 5), -1, -1));
        mainPanel.add(panel1, BorderLayout.NORTH);
        pathTextField = new JTextField();
        pathTextField.setEditable(false);
        panel1.add(pathTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        openButton = new JButton();
        this.$$$loadButtonText$$$(openButton, ResourceBundle.getBundle("strings").getString("choose"));
        panel1.add(openButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 5, 0, 5), -1, -1));
        mainPanel.add(panel2, BorderLayout.CENTER);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("strings").getString("files"));
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("strings").getString("preview"));
        panel2.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        oldScrollPane = new JScrollPane();
        oldScrollPane.setName("");
        panel2.add(oldScrollPane, new GridConstraints(1, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        fileList = new JList();
        oldScrollPane.setViewportView(fileList);
        newScrollPane = new JScrollPane();
        newScrollPane.setName("");
        panel2.add(newScrollPane, new GridConstraints(1, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        newFileList = new JList();
        newScrollPane.setViewportView(newFileList);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 3, new Insets(0, 5, 5, 5), -1, -1));
        mainPanel.add(panel3, BorderLayout.SOUTH);
        renameTextField = new JTextField();
        panel3.add(renameTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        applyButton = new JButton();
        this.$$$loadButtonText$$$(applyButton, ResourceBundle.getBundle("strings").getString("apply"));
        panel3.add(applyButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        origTextField = new JTextField();
        panel3.add(origTextField, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("strings").getString("search_text"));
        panel3.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("strings").getString("replacement"));
        panel3.add(label4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statisticsButton = new JButton();
        this.$$$loadButtonText$$$(statisticsButton, ResourceBundle.getBundle("strings").getString("statistics"));
        panel3.add(statisticsButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(oldScrollPane);
        label2.setLabelFor(newScrollPane);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
