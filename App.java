package com.res;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Residence Management System.
 *
 * A small Swing desktop app. Students log in to report maintenance issues for
 * their room; admin/management logs in to view reports and mark them resolved,
 * and to manage students, the campus map (blocks and rooms) and the list of
 * issue types.
 *
 * All data is persisted to a flat file (see {@link DataStore}) so it survives
 * between runs. Input is validated and the user always gets feedback on whether
 * an action succeeded or why it failed.
 */
public class App extends JFrame {

    // In-memory state (loaded from / saved to disk via DataStore).
    // LinkedHashMap keeps insertion order stable for predictable saves and UI.
    private Map<String, String[]> students = new LinkedHashMap<>();      // ID -> {Name, PIN, Block, Room}
    private List<String[]> reports = new ArrayList<>();                  // {StudentNo, Block, Room, Issue, Description, Status}
    private List<String> availableIssues = new ArrayList<>();
    private Map<String, List<String>> campusMap = new LinkedHashMap<>(); // Block -> list of rooms

    // Admin credentials are a fixed sentinel kept in source, not in the data file.
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PIN = "adminpin";

    private final DataStore store = new DataStore(Paths.get("data", "resdata.txt"));

    private JPanel cardPanel;
    private final CardLayout cardLayout = new CardLayout();
    private DefaultTableModel adminReportsTableModel;
    private JComboBox<String> studentFormBlockBox, studentFormRoomBox, studentFormIssueBox;

    public App() {
        loadOrSeed();

        setTitle("Residence Management System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardPanel = new JPanel(cardLayout);
        cardPanel.add(createLoginPanel(), "Login");

        add(cardPanel);
        setVisible(true);
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    /** Load saved data, or seed default sample data on first run. */
    private void loadOrSeed() {
        try {
            DataStore.Data d = store.load();
            boolean empty = d.students.isEmpty() && d.campusMap.isEmpty()
                    && d.issues.isEmpty() && d.reports.isEmpty();
            if (empty) {
                seedDefaults();
                persist();
            } else {
                students.putAll(d.students);
                campusMap.putAll(d.campusMap);
                availableIssues.addAll(d.issues);
                reports.addAll(d.reports);
            }
        } catch (IOException ex) {
            // Could not read the data file: fall back to defaults so the app still runs.
            seedDefaults();
        }
    }

    private void seedDefaults() {
        students.put("212345", new String[]{"John Doe", "1234", "Block A", "Room 101"});
        campusMap.put("Block A", new ArrayList<>(List.of("Room 101", "Room 102")));
        campusMap.put("Block B", new ArrayList<>(List.of("Room 201", "Room 202")));
        availableIssues.addAll(List.of("Broken Light", "Leaking Pipe", "Door Lock Fault", "No Hot Water"));
    }

    /** Write current state to disk. Surfaces an error dialog if it fails. */
    private void persist() {
        DataStore.Data d = new DataStore.Data();
        d.students = students;
        d.campusMap = campusMap;
        d.issues = availableIssues;
        d.reports = reports;
        try {
            store.save(d);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save data to disk: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------------

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Welcome to RES-MGMT");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0; panel.add(new JLabel("Username/Student ID:"), gbc);
        JTextField userField = new JTextField(15);
        gbc.gridx = 1; panel.add(userField, gbc);

        gbc.gridy = 2; gbc.gridx = 0; panel.add(new JLabel("PIN/Password:"), gbc);
        JPasswordField pinField = new JPasswordField(15);
        gbc.gridx = 1; panel.add(pinField, gbc);

        JButton loginBtn = new JButton("Login");
        Runnable doLogin = () -> authenticate(userField.getText().trim(), new String(pinField.getPassword()));
        loginBtn.addActionListener(e -> doLogin.run());
        // Allow pressing Enter in either field to log in.
        userField.addActionListener(e -> doLogin.run());
        pinField.addActionListener(e -> doLogin.run());
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2; panel.add(loginBtn, gbc);
        return panel;
    }

    private void authenticate(String user, String pin) {
        if (user.isEmpty() || pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both your ID/username and PIN/password.",
                    "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (students.containsKey(user) && students.get(user)[1].equals(pin)) {
            removeCardIfPresent("Student");
            cardPanel.add(createReportingForm(user), "Student");
            cardLayout.show(cardPanel, "Student");
        } else if (user.equals(ADMIN_USER) && pin.equals(ADMIN_PIN)) {
            removeCardIfPresent("Admin");
            showAdminPanel();
            cardLayout.show(cardPanel, "Admin");
        } else {
            JOptionPane.showMessageDialog(this, "Login Failed! Check your ID/username and PIN.",
                    "Login", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeCardIfPresent(String name) {
        for (Component c : cardPanel.getComponents()) {
            if (c instanceof JComponent) {
                Object tag = ((JComponent) c).getClientProperty("cardName");
                if (name.equals(tag)) {
                    cardPanel.remove(c);
                    break;
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Student: report a maintenance issue
    // ---------------------------------------------------------------------

    private JPanel createReportingForm(String studentNum) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.putClientProperty("cardName", "Student");
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel header = new JLabel("Submit Maintenance Report", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(header, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(0, 1, 10, 10));

        studentFormBlockBox = new JComboBox<>(campusMap.keySet().toArray(new String[0]));
        studentFormRoomBox = new JComboBox<>();
        studentFormIssueBox = new JComboBox<>(availableIssues.toArray(new String[0]));
        JTextArea descArea = new JTextArea(5, 20);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        refreshRoomBox();
        studentFormBlockBox.addActionListener(e -> refreshRoomBox());

        formPanel.add(new JLabel("Block:")); formPanel.add(studentFormBlockBox);
        formPanel.add(new JLabel("Room:")); formPanel.add(studentFormRoomBox);
        formPanel.add(new JLabel("Issue:")); formPanel.add(studentFormIssueBox);
        formPanel.add(new JLabel("Details:")); formPanel.add(new JScrollPane(descArea));

        JButton submitBtn = new JButton("Submit Report");
        submitBtn.addActionListener(e -> submitReport(
                studentNum,
                (String) studentFormBlockBox.getSelectedItem(),
                (String) studentFormRoomBox.getSelectedItem(),
                (String) studentFormIssueBox.getSelectedItem(),
                descArea.getText(),
                descArea));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> cardLayout.show(cardPanel, "Login"));

        JPanel southPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        southPanel.add(logoutBtn);
        southPanel.add(submitBtn);

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshRoomBox() {
        String selectedBlock = (String) studentFormBlockBox.getSelectedItem();
        List<String> rooms = selectedBlock != null ? campusMap.get(selectedBlock) : null;
        if (rooms == null) rooms = new ArrayList<>();
        studentFormRoomBox.setModel(new DefaultComboBoxModel<>(rooms.toArray(new String[0])));
    }

    private void submitReport(String sNum, String block, String room, String issue, String desc, JTextArea descArea) {
        if (block == null) {
            JOptionPane.showMessageDialog(this, "No blocks exist yet. Please contact management.",
                    "Cannot Submit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (room == null) {
            JOptionPane.showMessageDialog(this, "The selected block has no rooms. Please choose another block or contact management.",
                    "Cannot Submit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (issue == null) {
            JOptionPane.showMessageDialog(this, "No issue types are available. Please contact management.",
                    "Cannot Submit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String details = desc == null ? "" : desc.trim();
        if (details.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please describe the problem in the Details box.",
                    "Cannot Submit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] report = {sNum, block, room, issue, details, "Pending"};
        reports.add(report);
        persist();
        if (adminReportsTableModel != null) {
            adminReportsTableModel.addRow(report);
        }
        if (descArea != null) descArea.setText("");
        JOptionPane.showMessageDialog(this, "Report Logged Successfully!");
    }

    // ---------------------------------------------------------------------
    // Admin
    // ---------------------------------------------------------------------

    private void showAdminPanel() {
        JPanel adminPanel = new JPanel(new BorderLayout());
        adminPanel.putClientProperty("cardName", "Admin");
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("View Reports", createReportsManagementTab());
        tabbedPane.addTab("Manage Students", createStudentManagementTab());
        tabbedPane.addTab("Manage Campus Map", createCampusManagementTab());
        tabbedPane.addTab("Manage Issue Types", createIssueManagementTab());

        adminPanel.add(tabbedPane, BorderLayout.CENTER);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> cardLayout.show(cardPanel, "Login"));
        adminPanel.add(logoutButton, BorderLayout.SOUTH);

        cardPanel.add(adminPanel, "Admin");
    }

    private void updateComboBoxes() {
        if (studentFormBlockBox != null) {
            studentFormBlockBox.setModel(new DefaultComboBoxModel<>(campusMap.keySet().toArray(new String[0])));
            refreshRoomBox();
        }
        if (studentFormIssueBox != null) {
            studentFormIssueBox.setModel(new DefaultComboBoxModel<>(availableIssues.toArray(new String[0])));
        }
    }

    private JPanel createReportsManagementTab() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columnNames = {"Student No.", "Block", "Room", "Issue", "Description", "Status"};
        adminReportsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // reports are read-only in the table; use the button to resolve
            }
        };

        for (String[] report : reports) {
            adminReportsTableModel.addRow(report);
        }

        JTable reportTable = new JTable(adminReportsTableModel);
        JScrollPane scrollPane = new JScrollPane(reportTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton resolveButton = new JButton("Mark Selected as Resolved");
        resolveButton.addActionListener(e -> {
            int selectedRow = reportTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a report row first.",
                        "Nothing Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = reportTable.convertRowIndexToModel(selectedRow);
            adminReportsTableModel.setValueAt("Resolved", modelRow, 5);
            reports.get(modelRow)[5] = "Resolved";
            persist();
        });
        panel.add(resolveButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStudentManagementTab() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JPasswordField pinField = new JPasswordField();

        panel.add(new JLabel("Student ID:")); panel.add(idField);
        panel.add(new JLabel("Full Name:")); panel.add(nameField);
        panel.add(new JLabel("PIN:")); panel.add(pinField);

        JButton addButton = new JButton("Add Student");
        addButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String pin = new String(pinField.getPassword()).trim();
            if (id.isEmpty() || name.isEmpty() || pin.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Student ID, Full Name and PIN are all required.",
                        "Add Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (id.equals(ADMIN_USER)) {
                JOptionPane.showMessageDialog(this, "'" + ADMIN_USER + "' is reserved and cannot be used as a student ID.",
                        "Add Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (students.containsKey(id)) {
                JOptionPane.showMessageDialog(this, "A student with ID " + id + " already exists.",
                        "Add Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
            students.put(id, new String[]{name, pin, "N/A", "N/A"});
            persist();
            idField.setText(""); nameField.setText(""); pinField.setText("");
            JOptionPane.showMessageDialog(this, "Student Added: " + name);
        });
        panel.add(addButton);

        JButton removeButton = new JButton("Remove Student (by ID)");
        removeButton.addActionListener(e -> {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter the Student ID to remove.",
                        "Remove Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!students.containsKey(id)) {
                JOptionPane.showMessageDialog(this, "No student found with ID " + id + ".",
                        "Remove Student", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove student " + id + "? This cannot be undone.",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            students.remove(id);
            persist();
            idField.setText("");
            JOptionPane.showMessageDialog(this, "Student Removed: " + id);
        });
        panel.add(removeButton);

        return panel;
    }

    private JPanel createCampusManagementTab() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JComboBox<String> blockSelector = new JComboBox<>(campusMap.keySet().toArray(new String[0]));
        JTextField blockNameField = new JTextField();
        JTextField roomNameField = new JTextField();

        panel.add(new JLabel("Block Name (Add New):")); panel.add(blockNameField);
        JButton addBlockBtn = new JButton("Add Block");
        addBlockBtn.addActionListener(e -> {
            String block = blockNameField.getText().trim();
            if (block.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a block name.",
                        "Add Block", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (campusMap.containsKey(block)) {
                JOptionPane.showMessageDialog(this, "Block '" + block + "' already exists.",
                        "Add Block", JOptionPane.WARNING_MESSAGE);
                return;
            }
            campusMap.put(block, new ArrayList<>());
            blockSelector.addItem(block);
            updateComboBoxes();
            persist();
            blockNameField.setText("");
            JOptionPane.showMessageDialog(this, "Block Added: " + block);
        });
        panel.add(addBlockBtn);

        panel.add(new JLabel("Select Block:")); panel.add(blockSelector);
        panel.add(new JLabel("Room Name (Add/Sub):")); panel.add(roomNameField);

        JButton addRoomBtn = new JButton("Add Room");
        addRoomBtn.addActionListener(e -> {
            String block = (String) blockSelector.getSelectedItem();
            String room = roomNameField.getText().trim();
            if (block == null) {
                JOptionPane.showMessageDialog(this, "Add a block first.",
                        "Add Room", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (room.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter a room name.",
                        "Add Room", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (campusMap.get(block).contains(room)) {
                JOptionPane.showMessageDialog(this, "Room '" + room + "' already exists in " + block + ".",
                        "Add Room", JOptionPane.WARNING_MESSAGE);
                return;
            }
            campusMap.get(block).add(room);
            updateComboBoxes();
            persist();
            roomNameField.setText("");
            JOptionPane.showMessageDialog(this, "Room Added: " + room + " (" + block + ")");
        });
        panel.add(addRoomBtn);

        JButton subRoomBtn = new JButton("Subtract Room");
        subRoomBtn.addActionListener(e -> {
            String block = (String) blockSelector.getSelectedItem();
            String room = roomNameField.getText().trim();
            if (block == null || room.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Select a block and enter the room name to remove.",
                        "Remove Room", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (campusMap.get(block).remove(room)) {
                updateComboBoxes();
                persist();
                roomNameField.setText("");
                JOptionPane.showMessageDialog(this, "Room Removed: " + room + " (" + block + ")");
            } else {
                JOptionPane.showMessageDialog(this, "Room '" + room + "' was not found in " + block + ".",
                        "Remove Room", JOptionPane.WARNING_MESSAGE);
            }
        });
        panel.add(subRoomBtn);

        return panel;
    }

    private JPanel createIssueManagementTab() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JTextField issueField = new JTextField();

        panel.add(new JLabel("Issue Name (Add/Sub):")); panel.add(issueField);

        JButton addIssueBtn = new JButton("Add Issue Type");
        addIssueBtn.addActionListener(e -> {
            String issue = issueField.getText().trim();
            if (issue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter an issue name.",
                        "Add Issue", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (availableIssues.contains(issue)) {
                JOptionPane.showMessageDialog(this, "Issue type '" + issue + "' already exists.",
                        "Add Issue", JOptionPane.WARNING_MESSAGE);
                return;
            }
            availableIssues.add(issue);
            updateComboBoxes();
            persist();
            issueField.setText("");
            JOptionPane.showMessageDialog(this, "Issue Added: " + issue);
        });
        panel.add(addIssueBtn);

        JButton subIssueBtn = new JButton("Subtract Issue Type");
        subIssueBtn.addActionListener(e -> {
            String issue = issueField.getText().trim();
            if (issue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter the issue name to remove.",
                        "Remove Issue", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (availableIssues.remove(issue)) {
                updateComboBoxes();
                persist();
                issueField.setText("");
                JOptionPane.showMessageDialog(this, "Issue Removed: " + issue);
            } else {
                JOptionPane.showMessageDialog(this, "Issue type '" + issue + "' was not found.",
                        "Remove Issue", JOptionPane.WARNING_MESSAGE);
            }
        });
        panel.add(subIssueBtn);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }
}
