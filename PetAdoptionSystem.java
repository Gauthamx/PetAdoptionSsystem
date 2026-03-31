import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class PetAdoptionSystem extends JFrame {

    // --- DATABASE CONFIG ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/PetAdoptionDB";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "gautham";

    // --- OLX THEME COLORS ---
    private static final Color OLX_BLUE = new Color(0, 47, 52);
    private static final Color OLX_TEAL = new Color(35, 229, 219);
    private static final Color BG_LIGHT = new Color(242, 244, 245);

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer = new JPanel(cardLayout);

    private int currentUserId = -1;
    private String currentUserRole = "";
    private String currentUserName = "";

    public PetAdoptionSystem() {
        setTitle("Pawsitive - Pet Marketplace");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainContainer.add(createLoginScreen(), "LOGIN");
        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
    }

    // --- 1. LOGIN & SIGNUP ---
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel logo = new JLabel("PAWSITIVE");
        logo.setFont(new Font("Arial Black", Font.BOLD, 40));
        logo.setForeground(OLX_BLUE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField u = new JTextField(); u.setMaximumSize(new Dimension(300, 40));
        JPasswordField p = new JPasswordField(); p.setMaximumSize(new Dimension(300, 40));

        JButton loginBtn = new JButton("LOGIN");
        styleBtn(loginBtn, OLX_BLUE, Color.WHITE);
        JButton signupBtn = new JButton("SIGN UP");
        styleBtn(signupBtn, OLX_TEAL, Color.BLACK);

        loginBtn.addActionListener(e -> login(u.getText(), new String(p.getPassword())));
        signupBtn.addActionListener(e -> showSignupPopup());

        box.add(logo); box.add(Box.createRigidArea(new Dimension(0, 30)));
        box.add(new JLabel("Username")); box.add(u);
        box.add(Box.createRigidArea(new Dimension(0, 10)));
        box.add(new JLabel("Password")); box.add(p);
        box.add(Box.createRigidArea(new Dimension(0, 30)));
        box.add(loginBtn); box.add(Box.createRigidArea(new Dimension(0, 10)));
        box.add(signupBtn);

        panel.add(box);
        return panel;
    }

    private void showSignupPopup() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        Object[] fields = {"Username:", username, "Password:", password};

        if (JOptionPane.showConfirmDialog(this, fields, "Create Account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement pst = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?,?,?)");
                pst.setString(1, username.getText());
                pst.setString(2, new String(password.getPassword()));
                pst.setString(3, "user");
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Account created! You can now login.");
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
        }
    }

    private void login(String u, String p) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
            pst.setString(1, u); pst.setString(2, p);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("user_id");
                currentUserRole = rs.getString("role");
                currentUserName = rs.getString("username");
                refreshDashboards();
            } else { JOptionPane.showMessageDialog(this, "Invalid Credentials!"); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- 2. DASHBOARDS ---
    private void refreshDashboards() {
        if (currentUserRole.equalsIgnoreCase("admin")) {
            mainContainer.add(createAdminDashboard(), "ADMIN");
            cardLayout.show(mainContainer, "ADMIN");
        } else {
            mainContainer.add(createUserDashboard(), "USER");
            cardLayout.show(mainContainer, "USER");
        }
    }

    private JPanel createUserDashboard() {
        JPanel main = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        header.setBackground(Color.WHITE);

        JButton sellBtn = new JButton("+ SELL PET");
        styleBtn(sellBtn, Color.WHITE, OLX_BLUE);
        sellBtn.setBorder(new LineBorder(OLX_BLUE, 1));
        sellBtn.addActionListener(e -> showAddPetPopup());

        JButton logoutBtn = new JButton("LOGOUT");
        styleBtn(logoutBtn, Color.RED, Color.WHITE);
        logoutBtn.addActionListener(e -> cardLayout.show(mainContainer, "LOGIN"));

        header.add(new JLabel("Welcome, " + currentUserName));
        header.add(sellBtn); header.add(logoutBtn);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Browse Pets", createBrowsePanel(false));
        tabs.addTab("My Ads", createMyAdsPanel());

        main.add(header, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);
        return main;
    }

    private JPanel createAdminDashboard() {
        JPanel admin = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutBtn = new JButton("LOGOUT");
        logoutBtn.addActionListener(e -> cardLayout.show(mainContainer, "LOGIN"));
        header.add(new JLabel("Admin Panel | ")); header.add(logoutBtn);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage All Pets", createBrowsePanel(true));
        tabs.addTab("Adoption Requests", createRequestsTable());

        admin.add(header, BorderLayout.NORTH);
        admin.add(tabs, BorderLayout.CENTER);
        return admin;
    }

    // --- 3. UI COMPONENTS ---
    private JComponent createBrowsePanel(boolean isAdmin) {
        JPanel grid = new JPanel(new GridLayout(0, 4, 15, 15));
        grid.setBackground(BG_LIGHT);
        grid.setBorder(new EmptyBorder(15, 15, 15, 15));

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM pets WHERE status='available'");
            while (rs.next()) {
                grid.add(createPetCard(rs.getInt("pet_id"), rs.getString("name"), rs.getString("breed"), rs.getInt("age"), isAdmin));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new JScrollPane(grid);
    }

    private JPanel createPetCard(int id, String name, String breed, int age, boolean isAdmin) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(220, 240));
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(new Color(230,230,230)));

        JLabel label = new JLabel("<html><div style='text-align:center;'>🐾<br><b>" + name + "</b><br>" + breed + "</div></html>", SwingConstants.CENTER);
        JButton btn = new JButton(isAdmin ? "DELETE" : "VIEW / ADOPT");
        styleBtn(btn, isAdmin ? Color.RED : OLX_TEAL, isAdmin ? Color.WHITE : Color.BLACK);

        btn.addActionListener(e -> {
            if (isAdmin) deletePet(id);
            else showPetDetails(id);
        });

        card.add(label, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    private void showPetDetails(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM pets WHERE pet_id=?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String details = "Name: " + rs.getString("name") + "\nBreed: " + rs.getString("breed") +
                        "\nAge: " + rs.getInt("age") + "\n\nDescription: " + rs.getString("description");

                int opt = JOptionPane.showConfirmDialog(this, details + "\n\nSend Adoption Request?", "Pet Details", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) sendAdoptionRequest(id);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void sendAdoptionRequest(int petId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement pst = conn.prepareStatement("INSERT INTO adoption_requests (pet_id, adopter_id, status) VALUES (?, ?, 'submitted')");
            pst.setInt(1, petId);
            pst.setInt(2, currentUserId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Request Sent Successfully!");
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Error: Status field mismatch in DB."); }
    }

    private void showAddPetPopup() {
        JTextField n = new JTextField();
        JTextField b = new JTextField();
        JTextField a = new JTextField(); // Age field
        JTextArea desc = new JTextArea(3, 20);
        Object[] msg = {"Name:", n, "Breed:", b, "Age (Numbers only):", a, "Description:", new JScrollPane(desc)};

        if (JOptionPane.showConfirmDialog(this, msg, "Post Pet", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                // Validate Age input
                int ageValue = Integer.parseInt(a.getText().trim());

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                    PreparedStatement pst = conn.prepareStatement(
                            "INSERT INTO pets (name, breed, age, owner_id, status, description) VALUES (?,?,?,?,'available',?)"
                    );
                    pst.setString(1, n.getText());
                    pst.setString(2, b.getText());
                    pst.setInt(3, ageValue);
                    pst.setInt(4, currentUserId);
                    pst.setString(5, desc.getText());

                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Pet Added Successfully!");
                    refreshDashboards();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Error: Please enter a valid number for Age.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
            }
        }
    }

    private void deletePet(int id) {
        if (JOptionPane.showConfirmDialog(this, "Delete this ad?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                conn.prepareStatement("DELETE FROM adoption_requests WHERE pet_id=" + id).executeUpdate();
                conn.prepareStatement("DELETE FROM pets WHERE pet_id=" + id).executeUpdate();
                JOptionPane.showMessageDialog(this, "Deleted!");
                refreshDashboards(); // Refresh UI
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private JComponent createMyAdsPanel() {
        String[] cols = {"ID", "Name", "Breed", "Age", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM pets WHERE owner_id=?");
            pst.setInt(1, currentUserId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) model.addRow(new Object[]{rs.getInt("pet_id"), rs.getString("name"), rs.getString("breed"), rs.getInt("age"), rs.getString("status")});
        } catch (Exception e) { e.printStackTrace(); }
        return new JScrollPane(new JTable(model));
    }

    private JComponent createRequestsTable() {
        String[] cols = {"Req ID", "Pet ID", "Adopter ID", "Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM adoption_requests");
            while (rs.next()) model.addRow(new Object[]{rs.getInt("request_id"), rs.getInt("pet_id"), rs.getInt("adopter_id"), rs.getString("status")});
        } catch (Exception e) { e.printStackTrace(); }
        return new JScrollPane(new JTable(model));
    }

    private void styleBtn(JButton b, Color bg, Color fg) {
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false); b.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PetAdoptionSystem().setVisible(true));
    }
}
