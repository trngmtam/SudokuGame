import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.*;

interface Panel {
    public JPanel createMenuPanel();
    public JPanel createGamePanel();
    public JPanel createRulesPanel();
    public JPanel createSettingsPanel();
}

public class SudokuGame extends JFrame implements Panel {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JPanel gamePanel;
    private JButton[][] cells = new JButton[9][9];
    private int[][] solution = new int[9][9];
    private int[][] puzzle = new int[9][9];
    private JButton resetButton, newButton, solveButton, rulesButton;
    private JMenuBar menuBar;
    private boolean musicPlaying = false;
    private Clip clip;
    private JLabel mistakeLabel;
    private int totalCellsToFill = 0;
    private int mistakeCount = 0;
    private Clip musicClip;
    private FloatControl volumeControl;
    private boolean isMuted = false;
    private float previousVolume = 0.7f; // Default volume
    private ArrayList<Point> emptyCells = new ArrayList<>();

    public SudokuGame() { // Main Panel
        setTitle("Sudoku Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // Create menu bar
        createMenuBar();
        
        // Main panel with CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create menu panel
        JPanel menuPanel = createMenuPanel();
        mainPanel.add(menuPanel, "menu");
        
        // Create game panel
        gamePanel = createGamePanel();
        mainPanel.add(gamePanel, "game");
        
        // Create rules panel
        JPanel rulesPanel = createRulesPanel();
        mainPanel.add(rulesPanel, "rules");
        
        // Create settings panel
        JPanel settingsPanel = createSettingsPanel();
        mainPanel.add(settingsPanel, "settings");
        
        add(mainPanel);
        cardLayout.show(mainPanel, "menu");
    }
    
    private void createMenuBar() {
        menuBar = new JMenuBar();
        
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        newGameItem.addActionListener(e -> showLevelSelection());
        exitItem.addActionListener(e -> System.exit(0));
        
        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem rulesItem = new JMenuItem("Show Rules");
        rulesItem.addActionListener(e -> cardLayout.show(mainPanel, "rules"));
        helpMenu.add(rulesItem);
        
        JMenu settingsMenu = new JMenu("Settings");
        
        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        menuBar.add(settingsMenu);
        
        setJMenuBar(menuBar);
    }

    public JPanel createMenuPanel() {
        // Create a panel with absolute positioning for better control
        JPanel panel = new JPanel(null) {
            private ImageIcon backgroundIcon = new ImageIcon("E:/Sudoku.gif");

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setBorder(new EmptyBorder(50, 50, 50, 50));

        // Calculate the center position for the buttons
        int buttonWidth = 150;
        int buttonHeight = 50;
        int panelWidth = 800;
        int panelHeight = 600;
        int xPos = (panelWidth - (4 * buttonWidth + 3 * 20)) / 2;
        int yPos = panelHeight / 2;

        // Start Game Button
        JButton startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.PLAIN, 18));
        startButton.setBounds(xPos, yPos, buttonWidth, buttonHeight);
        startButton.addActionListener(e -> showLevelSelection());
        panel.add(startButton);

        // Game Rules Button
        JButton rulesButton = new JButton("Game Rules");
        rulesButton.setFont(new Font("Arial", Font.PLAIN, 18));
        rulesButton.setBounds(xPos + buttonWidth + 20, yPos, buttonWidth, buttonHeight);
        rulesButton.addActionListener(e -> cardLayout.show(mainPanel, "rules"));
        panel.add(rulesButton);

        // Settings Button
        JButton settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("Arial", Font.PLAIN, 18));
        settingsButton.setBounds(xPos + 2 * (buttonWidth + 20), yPos, buttonWidth, buttonHeight);
        settingsButton.addActionListener(e -> cardLayout.show(mainPanel, "settings"));
        panel.add(settingsButton);

        // Quit Button
        JButton quitButton = new JButton("Quit");
        quitButton.setFont(new Font("Arial", Font.PLAIN, 18));
        quitButton.setBounds(xPos + 3 * (buttonWidth + 20), yPos, buttonWidth, buttonHeight);
        quitButton.addActionListener(e -> System.exit(0));
        panel.add(quitButton);

        return panel;
    }
    
    private void showLevelSelection() {
        String[] options = {"Easy", "Medium", "Hard"};
        int choice = JOptionPane.showOptionDialog(this, 
            "Select difficulty level:", 
            "New Game", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            options, 
            options[0]);
        
        if (choice != JOptionPane.CLOSED_OPTION) {
            generatePuzzle(choice);
            updateBoard();
            cardLayout.show(mainPanel, "game");
        }
    }

    public JPanel createGamePanel() {
        // Create the main panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout(10, 10)) {
            private ImageIcon backgroundIcon = new ImageIcon("E:/Sudoku.png"); // Corrected file path

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw the background image
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create the Sudoku board with a transparent background
        JPanel boardPanel = new JPanel(new GridLayout(9, 9, 1, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Set transparent background to allow the main panel's background to show through
                setOpaque(false);
            }
        };
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        // Create the buttons (Sudoku cells) with a transparent background
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col] = new JButton();
                cells[row][col].setFont(new Font("Arial", Font.BOLD, 20));

                // Set semi-transparent background color for 3x3 blocks
                if ((row / 3 + col / 3) % 2 == 0) {
                    cells[row][col].setBackground(new Color(240, 240, 240, 150)); // Adjust transparency
                } else {
                    cells[row][col].setBackground(new Color(220, 220, 220, 150)); // Adjust transparency
                }

                // Transparent button settings
                cells[row][col].setOpaque(true);
                cells[row][col].setBorderPainted(false);

                final int r = row;
                final int c = col;
                cells[row][col].addActionListener(e -> cellClicked(r, c));

                boardPanel.add(cells[row][col]);
            }
        }

        // Create button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 0));

        resetButton = new JButton("Reset Board");
        resetButton.addActionListener(e -> resetBoard());

        newButton = new JButton("New Board");
        newButton.addActionListener(e -> showLevelSelection());

        solveButton = new JButton("Solve Board");
        solveButton.addActionListener(e -> solveBoard());

        rulesButton = new JButton("Game Rules");
        rulesButton.addActionListener(e -> cardLayout.show(mainPanel, "rules"));

        JButton checkButton = new JButton("Check Result");
        checkButton.addActionListener(e -> checkResults());

        buttonPanel.add(checkButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(newButton);
        buttonPanel.add(solveButton);
        buttonPanel.add(rulesButton);

        // Add mistake label below the button panel
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.CENTER);

        mistakeLabel = new JLabel("Mistakes: 0/0", SwingConstants.CENTER);
        mistakeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mistakeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mistakeLabel.setOpaque(true);
        mistakeLabel.setBackground(new Color(240, 240, 240));


        panel.add(boardPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void checkResults() {
    int correctCount = 0;
    int wrongCount = 0;
    emptyCells.clear();
    
    for (int row = 0; row < 9; row++) {
        for (int col = 0; col < 9; col++) {
            // Reset cell appearance first
            cells[row][col].setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            if (puzzle[row][col] == 0) {
                // Empty cell - mark with red border
                cells[row][col].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                emptyCells.add(new Point(row, col));
                wrongCount++;
            } else if (puzzle[row][col] != solution[row][col]) {
                // Wrong answer
                cells[row][col].setForeground(Color.RED);
                cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.BOLD));
                wrongCount++;
            } else {
                // Correct answer
                cells[row][col].setForeground(Color.GREEN);
                cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.BOLD));
                correctCount++;
            }
        }
    }
    
    // Show correct answers for empty cells
    for (Point p : emptyCells) {
        int row = p.x;
        int col = p.y;
        cells[row][col].setText(Integer.toString(solution[row][col]));
        cells[row][col].setForeground(Color.RED);
        cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.BOLD));
    }
    
    mistakeCount = wrongCount;
    updateMistakeLabel();
}

    private void updateMistakeLabel() {
        mistakeLabel.setText(String.format("Mistakes: %d/%d", mistakeCount, totalCellsToFill));
        mistakeLabel.setForeground(mistakeCount > 0 ? Color.RED : Color.GREEN);
    }

    private void countEmptyCells() {
        totalCellsToFill = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] == 0) {
                    totalCellsToFill++;
                }
            }
        }
        mistakeCount = 0; // Reset mistake count when starting new game
        updateMistakeLabel();
    }
    
    private void cellClicked(int row, int col) {
        if (puzzle[row][col] != 0) return; // Don't allow editing of initial numbers
        
        String input = JOptionPane.showInputDialog(this, "Enter number (1-9):");
        try {
            int num = Integer.parseInt(input);
            if (num >= 1 && num <= 9) {
                cells[row][col].setText(Integer.toString(num));
                puzzle[row][col] = num;
                if (isBoardComplete() && isSolutionCorrect()) {
                JOptionPane.showMessageDialog(this, "Congratulations! You solved the puzzle!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a number between 1 and 9.", 
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Invalid input. Please enter a number between 1 and 9.", 
            "Invalid Input", JOptionPane.WARNING_MESSAGE);
    }
}
    
    private boolean isBoardComplete() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] == 0) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean isSolutionCorrect() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] != solution[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void resetBoard() {
    mistakeCount = 0;
    updateMistakeLabel();
    emptyCells.clear();
    
    for (int row = 0; row < 9; row++) {
        for (int col = 0; col < 9; col++) {
            cells[row][col].setBorder(BorderFactory.createLineBorder(Color.GRAY));
            
            if (cells[row][col].getText().equals("")) continue;
            
            if (puzzle[row][col] != 0) {
                cells[row][col].setForeground(Color.BLACK);
                cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.PLAIN));
            } else {
                cells[row][col].setText("");
                puzzle[row][col] = 0;
            }
        }
    }
}
    
    private void solveBoard() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col].setText(Integer.toString(solution[row][col]));
                cells[row][col].setForeground(Color.BLUE);
            }
        }
    }
    
    private void updateBoard() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (puzzle[row][col] != 0) {
                    cells[row][col].setText(Integer.toString(puzzle[row][col]));
                    cells[row][col].setForeground(Color.BLACK);
                    cells[row][col].setEnabled(false);
                } else {
                    cells[row][col].setText("");
                    cells[row][col].setEnabled(true);
                }
            }
        }
    }

    public JPanel createRulesPanel() {
        // Create the main panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout());

        // Panel to display the GIF background
        JPanel gifPanel = new JPanel() {
            private ImageIcon backgroundIcon = new ImageIcon("E:/Sudoku (2).gif"); // Corrected file path

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw the GIF to completely fill the panel
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };

        // Set the panel size to match the JFrame size to avoid blank space
        gifPanel.setPreferredSize(new Dimension(800, 600));

        // Back button at the bottom
        JButton backButton = new JButton("Back to Menu");
        backButton.setFont(new Font("Arial", Font.PLAIN, 18));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        // Adding components to the main panel
        panel.add(gifPanel, BorderLayout.CENTER);
        panel.add(backButton, BorderLayout.SOUTH);

        return panel;
    }

    public JPanel createSettingsPanel() {
        // Create the main panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout());

        // Panel to display the GIF background
        JPanel gifPanel = new JPanel(null) {
            private ImageIcon backgroundIcon = new ImageIcon("E:/Sudoku (3).gif"); // Corrected file path

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw the GIF to completely fill the panel
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };

        // Set the panel size to match the JFrame size to avoid blank space
        gifPanel.setPreferredSize(new Dimension(800, 600));

        // Create button panel to choose between 3 music
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        JButton funnyButton = new JButton("Funny");
        JButton chillButton = new JButton("Chill");
        JButton thrillingButton = new JButton("Thrilling");

        // Music buttons
        funnyButton.addActionListener(e -> playMusic("funny.wav"));
        chillButton.addActionListener(e -> playMusic("chill.wav"));
        thrillingButton.addActionListener(e -> playMusic("thrilling.wav"));

        buttonPanel.add(funnyButton);
        buttonPanel.add(chillButton);
        buttonPanel.add(thrillingButton);

        // Volume Panel
        JPanel volumePanel = new JPanel();

        // Icons of slider
        ImageIcon volumeOnIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("/audio/volumeON.png"))
                        .getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)); // for scaling image
        ImageIcon volumeOffIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("/audio/volumeOFF.png"))
                        .getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        JLabel volumeIconLabel = new JLabel(volumeOnIcon);

        // Volume slider
        JSlider volumeSlider = new JSlider(0, 100, (int)(previousVolume * 100));
        volumeSlider.addChangeListener(e -> {
            if (!isMuted && volumeControl != null) {
                int value = volumeSlider.getValue();
                float volume = (float) value / 100f;
                previousVolume = volume; // Save the adjusted volume
                float dB = (float) (Math.log10(volume) * 20);
                volumeControl.setValue(dB);
            }
        });

        // Toggle mute when clicking the icon
        volumeIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        volumeIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isMuted) {
                    // Unmute
                    isMuted = false;
                    volumeIconLabel.setIcon(volumeOnIcon);
                    volumeSlider.setValue((int)(previousVolume * 100));
                    if (volumeControl != null) {
                        float dB = (float) (Math.log10(previousVolume) * 20);
                        volumeControl.setValue(dB);
                    }
                } else {
                    // Mute
                    isMuted = true;
                    volumeIconLabel.setIcon(volumeOffIcon);
                    volumeSlider.setValue(0);
                    if (volumeControl != null) {
                        volumeControl.setValue(-80f); // Effectively mute
                    }
                }
            }
        });

        volumePanel.add(volumeIconLabel);
        volumePanel.add(volumeSlider);

        // Back button at the bottom
        JButton backButton = new JButton("Back to Menu");
        backButton.setFont(new Font("Arial", Font.PLAIN, 18));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        // Button to turn the music off
        JButton stopMusicButton = new JButton("Turn Off Music");
        stopMusicButton.addActionListener(e -> stopMusic());

        JPanel musicControlPanel = new JPanel();
        musicControlPanel.setLayout(new BoxLayout(musicControlPanel, BoxLayout.Y_AXIS));
        musicControlPanel.add(volumePanel);
        musicControlPanel.add(Box.createVerticalStrut(10)); // spacing
        musicControlPanel.add(stopMusicButton);

        // Adding components to the main panel
        panel.add(gifPanel, BorderLayout.CENTER);
        panel.add(backButton, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(musicControlPanel, BorderLayout.EAST);

        return panel;
    }

    private void stopMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }

    private void playMusic(String filename) {
        try {
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.close();
            }

            // Load from resources/audio/ directory in src
            InputStream audioSrc = getClass().getResourceAsStream("/audio/" + filename);
            if (audioSrc == null) {
                System.err.println("File not found: " + filename);
                return;
            }

            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);

            musicClip = AudioSystem.getClip();
            musicClip.open(audioStream);

            volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            musicClip.start();
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generatePuzzle(int difficulty) {
        // Generate a solved Sudoku board
        generateSolution();
        
        // Make a copy for the puzzle
        for (int i = 0; i < 9; i++) {
            System.arraycopy(solution[i], 0, puzzle[i], 0, 9);
        }
        
        // Remove numbers based on difficulty
        int cellsToRemove;
        switch (difficulty) {
            case 0: // Easy
                cellsToRemove = 40;
                break;
            case 1: // Medium
                cellsToRemove = 50;
                break;
            case 2: // Hard
                cellsToRemove = 60;
                break;
            default:
                cellsToRemove = 45;
        }
        
        Random random = new Random();
        while (cellsToRemove > 0) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);
            
            if (puzzle[row][col] != 0) {
                puzzle[row][col] = 0;
                cellsToRemove--;
            }
        }
        countEmptyCells();
    }
    
    private void generateSolution() {
        // Reset the solution board
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                solution[i][j] = 0;
            }
        }
        
        // Fill the diagonal 3x3 boxes (they are independent)
        fillDiagonalBoxes();
        
        // Solve the remaining puzzle
        solve(0, 0);
    }
    
    private void fillDiagonalBoxes() {
        for (int box = 0; box < 9; box += 3) {
            fillBox(box, box);
        }
    }
    
    private void fillBox(int row, int col) {
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int num;
                do {
                    num = random.nextInt(9) + 1;
                } while (!isValidInBox(row, col, num));
                
                solution[row + i][col + j] = num;
            }
        }
    }
    
    private boolean isValidInBox(int boxStartRow, int boxStartCol, int num) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (solution[boxStartRow + row][boxStartCol + col] == num) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private boolean solve(int row, int col) {
        if (row == 9) {
            return true;
        }
        
        if (col == 9) {
            return solve(row + 1, 0);
        }
        
        if (solution[row][col] != 0) {
            return solve(row, col + 1);
        }
        
        for (int num = 1; num <= 9; num++) {
            if (isValid(row, col, num)) {
                solution[row][col] = num;
                
                if (solve(row, col + 1)) {
                    return true;
                }
                
                solution[row][col] = 0;
            }
        }
        
        return false;
    }
    
    private boolean isValid(int row, int col, int num) {
        // Check row
        for (int c = 0; c < 9; c++) {
            if (solution[row][c] == num) {
                return false;
            }
        }
        
        // Check column
        for (int r = 0; r < 9; r++) {
            if (solution[r][col] == num) {
                return false;
            }
        }
        
        // Check 3x3 box
        int boxStartRow = row - row % 3;
        int boxStartCol = col - col % 3;
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (solution[boxStartRow + r][boxStartCol + c] == num) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SudokuGame game = new SudokuGame();
            game.setVisible(true);
        });
    }
}