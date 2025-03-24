import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

class Transaction {
    private int id;
    private String category;
    private double amount;
    private String date;
    private String description;

    public Transaction(int id, String category, double amount, String date, String description) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return id + " - " + date + " - " + category + ": " + amount + " TL (" + description + ")";
    }
}

class FinanceManager {
    private Connection conn;

    public FinanceManager() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:finance.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, category TEXT, amount REAL, date TEXT, description TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void addTransaction(String category, double amount, String date, String description) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO transactions (category, amount, date, description) VALUES (?, ?, ?, ?)");
            pstmt.setString(1, category);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, date);
            pstmt.setString(4, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateTransaction(int id, String category, double amount, String date, String description) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("UPDATE transactions SET category = ?, amount = ?, date = ?, description = ? WHERE id = ?");
            pstmt.setString(1, category);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, date);
            pstmt.setString(4, description);
            pstmt.setInt(5, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void deleteTransaction(int id) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM transactions WHERE id = ?");
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public ResultSet getTransactions() {
        try {
            Statement stmt = conn.createStatement();
            return stmt.executeQuery("SELECT * FROM transactions");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

public class Main extends Application {
    private FinanceManager fm = new FinanceManager();
    private ListView<String> transactionList = new ListView<>();
    private PieChart pieChart = new PieChart();
    private TextField idField = new TextField();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Kişisel Finans Takip Uygulaması");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Kategori");

        TextField amountField = new TextField();
        amountField.setPromptText("Tutar");

        TextField dateField = new TextField();
        dateField.setPromptText("Tarih (YYYY-MM-DD)");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Açıklama");

        Button addButton = new Button("Ekle");
        addButton.setOnAction(e -> {
            String category = categoryField.getText();
            double amount = Double.parseDouble(amountField.getText());
            String date = dateField.getText();
            String description = descriptionField.getText();
            fm.addTransaction(category, amount, date, description);
            updateTransactionList();
            updatePieChart();
        });

        

        idField.setPromptText("ID");

        Button deleteButton = new Button("Sil");
        deleteButton.setOnAction(e -> {
            int id = Integer.parseInt(idField.getText());
            fm.deleteTransaction(id);
            updateTransactionList();
            updatePieChart();
        });

        Button updateButton = new Button("Güncelle");
        updateButton.setOnAction(e -> {
            int id = Integer.parseInt(idField.getText());
            String category = categoryField.getText();
            double amount = Double.parseDouble(amountField.getText());
            String date = dateField.getText();
            String description = descriptionField.getText();
            fm.updateTransaction(id, category, amount, date, description);
            updateTransactionList();
            updatePieChart();
        });

        VBox layout = new VBox(10);
        layout.getChildren().addAll(categoryField, amountField, dateField, descriptionField, addButton, idField, updateButton, deleteButton, transactionList, pieChart);

        Scene scene = new Scene(layout, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        updateTransactionList();
        updatePieChart();
    }

    private void updateTransactionList() {
        transactionList.getItems().clear();
        ResultSet rs = fm.getTransactions();
        try {
            while (rs.next()) {
                String transaction = rs.getInt("id") + " - " + rs.getString("date") + " - " + rs.getString("category") + ": " + rs.getDouble("amount") + " TL (" + rs.getString("description") + ")";
                transactionList.getItems().add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void updatePieChart() {
        pieChart.getData().clear();
        Map<String, Double> categoryTotals = new HashMap<>();
        try {
            ResultSet rs = fm.getTransactions();
            while (rs.next()) {
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
            }
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                PieChart.Data slice = new PieChart.Data(entry.getKey(), entry.getValue());
                pieChart.getData().add(slice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
