package com.example.csc311hw2;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Random;

/**
 * This class contains methods for the application to function.
 *
 * @author Auroba Ahmad
 */
public class GameController {
    //FXML annotations to add elements defined in FXML file
    @FXML
    private Rectangle rectangle;

    @FXML
    private Circle circle;

    @FXML
    private ListView<String> listview;

    @FXML
    private RadioButton rectBtn;

    @FXML
    private RadioButton circleBtn;

    @FXML
    private TextField totalTxtField;

    @FXML
    private TextField correctTxtField;

    @FXML
    private Button guessBtn;
    private ToggleGroup group = new ToggleGroup();

    /**
     * This method runs when the Guess button is clicked.
     */
    @FXML
    protected void onGuessButtonClick() {
        //get randomized shape by setting random to shapeChooser method
        Shape random = shapeChooser();
        Shape shapeSelected;
        int correctGuess = Integer.parseInt(correctTxtField.getText());
        int totalCount = Integer.parseInt(totalTxtField.getText());
        //database file path and URL
        String dbFilePath = ".//GuessRecord.accdb";
        String databaseURL = "jdbc:ucanaccess://" + dbFilePath;
        Connection conn;
        //database connection
        //throws exception if connection to database fails
        try {
            conn = DriverManager.getConnection(databaseURL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //check which shape is selected by user, then set shapeSelected to that shape
        if(rectBtn.isSelected()){
                shapeSelected = rectangle;
        } else{
                shapeSelected = circle;
        }
        //check if the randomly selected shape and shape selected by user match
        //if they match call AnimationGreen method on random
        //increment guess count and set it to the Correct Guess Count field
        if(random == shapeSelected){
              AnimationGreen(random);
              correctGuess++;
              correctTxtField.setText(Integer.toString(correctGuess));
        }
        //check if the randomly selected shape and shape selected by user match
        //if they do not match call AnimationRed method on random
        if(random != shapeSelected){
            AnimationRed(random);
        }
        //increment total count and set it to the Total Guess Count field
        totalCount++;
        totalTxtField.setText(Integer.toString(totalCount));
        //set correctShape to random and guessedShape to shapeSelected
        String correctShape = random.getId().toString().toUpperCase();
        String guessedShape = shapeSelected.getId().toString().toUpperCase();
        String correct = "correct: " + correctShape;
        String guessed = "guessed: " + guessedShape;
        //insert the guess data to the database
        insertDataToDB(conn, totalCount, correct, guessed);
        //print on console
        System.out.println(totalCount + ", " + correct + ", " + guessed);
    }

    /**
     * This method runs when the Show Guesses from DB button is clicked.
     */
    @FXML
    protected void onShowGuessesButtonClick() {
        //database file path and URL
        String dbFilePath = ".//GuessRecord.accdb";
        String databaseURL = "jdbc:ucanaccess://" + dbFilePath;
        Connection conn;
        //database connection
        //throws an exception if connection to database fails
        try {
        conn = DriverManager.getConnection(databaseURL);
        } catch (SQLException e) {
        throw new RuntimeException(e);
        }
        //get information from table Guesses in the database and show it in the listview
        try {
        String tableName = "Guesses";
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("select * from " + tableName);
        //iterates through the rows of the table and retrieves the information
        while (result.next()) {
            int guess = result.getInt("Guess");
            String correct = result.getString("CorrectShape");
            String guessed = result.getString("GuessedShape");
            //display the information on the listview
            ObservableList<String> grades = listview.getItems();
            grades.add(guess + ", " + correct + ", " + guessed);
        }
        } catch (SQLException except) {
        except.printStackTrace();
        }
    }

    /**
     * This method runs when the menuItem Exit is clicked
     */
    @FXML
    protected void onExitClicked(){
        //closes the application
        Platform.exit();
    }

    /**
     * This method runs when the menuItem New Game is clicked
     */
    @FXML
    protected void onNewGameClicked(){
        //database file path and URL
        String dbFilePath = ".//GuessRecord.accdb";
        String databaseURL = "jdbc:ucanaccess://" + dbFilePath;
        Connection conn;
        //database connection
        //if connection to db fails it throws an exception
        try {
            conn = DriverManager.getConnection(databaseURL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //call deleteRowsFromDB to clear database
        deleteRowsFromDB(conn);
        //clear listview
        listview.getItems().clear();
        //set total guess count and correct guess count to zero
        totalTxtField.setText("0");
        correctTxtField.setText("0");
    }

    /**
     * This method is called when the application runs and FXML file is loaded.
     */
    public void initialize(){
        System.out.println("initialize called");
        //call method to create database and table
        createDBandTable();
        //set shapes to not visible
        rectangle.setVisible(false);
        circle.setVisible(false);
        //set toggle group
        rectBtn.setToggleGroup(group);
        rectBtn.setSelected(true);
        circleBtn.setToggleGroup(group);
        circleBtn.setSelected(true);
        //only one radio button can be selected at a time
        if(rectBtn.isSelected()){
        circleBtn.setDisable(true);
        }
        else{
        circleBtn.setDisable(false);}
        //set total guess count and correct guess count to zero
        totalTxtField.setText("0");
        correctTxtField.setText("0");
    }

    /**
     * This method creates the database file and table.
     */
    public void createDBandTable(){
        //database file path and URL
        String dbFilePath = ".//GuessRecord.accdb";
        String databaseURL = "jdbc:ucanaccess://" + dbFilePath;
        File dbFile = new File(dbFilePath);
        //if database file does not exist create a file and table
        if (!dbFile.exists()) {
            //create file using file path
            try (Database db =
                         DatabaseBuilder.create(Database.FileFormat.V2010, new File(dbFilePath))) {
                System.out.println("The database file has been created.");
            } catch (IOException ioe) {
                ioe.printStackTrace(System.err);
            }
            //create table Guesses with columns Guess, CorrectShape, and GuessedShape
            try {
                Connection conn = DriverManager.getConnection(databaseURL);
                String sql;
                sql = "CREATE TABLE Guesses (Guess int, CorrectShape nvarchar(255), GuessedShape nvarchar(255))";
                Statement createTableStatement = conn.createStatement();
                createTableStatement.execute(sql);
                conn.commit();
                } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }
        }
    }

    /**
     * This method animates the shape and sets the fill to green.
     * @param shape variable for the shape that is passed in.
     */
    public void AnimationGreen(Shape shape){
        //disable the Guess button
        disableGuessBtn();
        //make shape visible
        shape.setVisible(true);
        //fill transition to make the shape green
        FillTransition ft = new FillTransition(Duration.seconds(2), shape);
        ft.setToValue(Color.GREEN);
        //translate transition to move the shape to the right by 100
        TranslateTransition translate = new TranslateTransition(Duration.seconds(2), shape);
        translate.setToX(100);
        //parallel transition to allow fill and translate transitions to run at the same time
        ParallelTransition pt = new ParallelTransition(ft,translate);
        //call setShapes method when transitions are finished
        pt.setOnFinished(e -> setShapes());
        pt.play();
    }

    /**
     * This method animates the shape and sets the fill to red.
     * @param shape variable for the shape that is passed in.
     */
    public void AnimationRed(Shape shape){
        //disable the Guess button
        disableGuessBtn();
        //make shape visible
        shape.setVisible(true);
        //fill transition to make the shape red
        FillTransition ft = new FillTransition(Duration.seconds(2), shape);
        ft.setToValue(Color.RED);
        //translate transition to move the shape to the right by 100
        TranslateTransition translate = new TranslateTransition(Duration.seconds(2), shape);
        translate.setToX(100);
        //parallel transition to allow fill and translate transitions to run at the same time
        ParallelTransition pt = new ParallelTransition(ft,translate);
        //call setShapes method when transitions are finished
        pt.setOnFinished(e -> setShapes());
        pt.play();
    }

    /**
     * This method animates the shapes back to their original spot and color.
     */
    public void setShapes(){
        //translate transition for the rectangle
        TranslateTransition translateRect = new TranslateTransition(Duration.seconds(3), rectangle);
        //sets the rectangle to its original spot
        double x = translateRect.getByX();
        translateRect.setToX(x);
        //fill transition changes the rectangles color back to gray
        FillTransition ftRectangle = new FillTransition(Duration.seconds(2), rectangle);
        ftRectangle.setToValue(Color.GREY);
        //parallel transition to allow fill and translate transitions to run at the same time
        ParallelTransition ptRectangle = new ParallelTransition(translateRect,ftRectangle);
        //make shapes invisible and enable Guess button when transitions finish
        ptRectangle.setOnFinished(e -> {
            rectangle.setVisible(false);
            guessBtn.setDisable(false);});
        ptRectangle.play();

        //translate transition for the circle
        TranslateTransition translateCircle = new TranslateTransition(Duration.seconds(3), circle);
        //sets the circle to its original spot
        double y = translateCircle.getByX();
        translateCircle.setToX(y);
        //fill transition changes the circle color back to gray
        FillTransition ftCircle = new FillTransition(Duration.seconds(2), circle);
        ftCircle.setToValue(Color.GREY);
        //parallel transition to allow fill and translate transitions to run at the same time
        ParallelTransition ptCircle = new ParallelTransition(translateCircle,ftCircle);
        //make shapes invisible and enable Guess button when transitions finish
        ptCircle.setOnFinished(e -> {
            circle.setVisible(false);
            guessBtn.setDisable(false);});
        ptCircle.play();
    }

    /**
     * This method randomly returns a rectangle or circle.
     * @return a random shape.
     */
    public Shape shapeChooser(){
        //array of shapes
        Shape[] shapes = {rectangle,circle};
        //select random shape from array
        Random random = new Random();
        int selected = random.nextInt(shapes.length);
        Shape shape;
        //set shape to rectangle or circle
        if(selected == 0){
            shape = rectangle;
        }
        else{
            shape = circle;
        }
        //return shape selected
        return shape;
    }

    /**
     * This method disables the Guess button.
     */
    public void disableGuessBtn(){
        //disable the Guess button
        guessBtn.setDisable(true);
    }

    /**
     * This method inserts the data into the table.
     * @param conn variable for connection.
     * @param Guess variable to hold the guess.
     * @param CorrectShape variable to hold the correct shape.
     * @param GuessedShape variable to hold the guessed shape.
     */
    public void insertDataToDB(Connection conn, int Guess, String CorrectShape, String GuessedShape){
        //sql string to insert data into the table Guesses
        String sql = "INSERT INTO Guesses (Guess, correctShape, GuessedShape) VALUES (?, ?, ?)";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement(sql);
            //each ? will be filled with data, from the variables, by the prepared statement class
            preparedStatement.setInt(1, Guess);
            preparedStatement.setString(2, CorrectShape);
            preparedStatement.setString(3, GuessedShape);
            //execute insertion
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //throw exception if connection fails
            throw new RuntimeException(e);
        }
    }

    /**
     * This method deletes the rows from the table.
     * @param conn variable for connection.
     */
    public void deleteRowsFromDB(Connection conn){
        //sql string to delete from table Guesses
        String sql = "DELETE FROM Guesses";
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = conn.prepareStatement(sql);
            //delete rows and return number of rows deleted
            int rowsDeleted = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            //throw exception if connection fails.
            throw new RuntimeException(e);
        }
    }
}

