import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
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
        // Use CardLayout to enable switching between panels in the same space, only
        // one panel is visible at a time
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

    private void createMenuBar() { // Create the Menu Bar at the top of all Panels
        menuBar = new JMenuBar();

        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        JMenuItem exitItem = new JMenuItem("Exit");

        newGameItem.addActionListener(e -> showLevelSelection());
        exitItem.addActionListener(e -> System.exit(0));

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        // Help menu: show the rules of the game
        JMenu helpMenu = new JMenu("Help");
        JMenuItem rulesItem = new JMenuItem("Show Rules");
        rulesItem.addActionListener(e -> cardLayout.show(mainPanel, "rules"));
        helpMenu.add(rulesItem);

        // Settings menu: for adjusting the audio settings (volume and music)
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem audioSettingsItem = new JMenuItem("Audio Settings");
        audioSettingsItem.addActionListener(e -> cardLayout.show(mainPanel, "settings"));
        settingsMenu.add(audioSettingsItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        menuBar.add(settingsMenu);

        setJMenuBar(menuBar);
    }

    private void showSettingsPopup() {
        // create a pop-up window for Audio Settings
        // false: allow interactions with the main game while the window is open
        JDialog dialog = new JDialog(this, "Settings", false);
        dialog.setSize(350, 250);
        // the window appears in the center of the main game
        dialog.setLocationRelativeTo(this);

        // create another panel for slider + music buttons
        JPanel contentPanel = createInlineSettingsPanel();
        dialog.add(contentPanel);
        dialog.setVisible(true);
    }

    // panel for slider + music buttons
    private JPanel createInlineSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        panel.setBackground(new Color(255, 255, 255, 230));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Volume Controls
        ImageIcon volumeOnIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("/audio/volumeON.png"))
                        .getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        ImageIcon volumeOffIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("/audio/volumeOFF.png"))
                        .getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        JLabel volumeIconLabel = new JLabel(volumeOnIcon);

        JSlider volumeSlider = new JSlider(0, 100, (int) (previousVolume * 100));
        volumeSlider.addChangeListener(e -> {
            if (!isMuted && volumeControl != null) {
                int value = volumeSlider.getValue();
                float volume = value / 100f;
                previousVolume = volume;
                float dB = (float) (Math.log10(volume) * 20);
                // Immediately adjust volume while sliding
                if (!volumeSlider.getValueIsAdjusting()) {
                    volumeControl.setValue(dB);
                } else {
                    // This makes the volume change during sliding
                    volumeControl.setValue(dB);
                }
            }
        });

        volumeIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        volumeIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isMuted) {
                    isMuted = false;
                    volumeIconLabel.setIcon(volumeOnIcon);
                    volumeSlider.setValue((int) (previousVolume * 100));
                    if (volumeControl != null) {
                        float dB = (float) (Math.log10(previousVolume) * 20);
                        volumeControl.setValue(dB);
                    }
                } else {
                    isMuted = true;
                    volumeIconLabel.setIcon(volumeOffIcon);
                    volumeSlider.setValue(0);
                    if (volumeControl != null) {
                        volumeControl.setValue(-80f);
                    }
                }
            }
        });

        JPanel volumePanel = new JPanel();
        volumePanel.add(volumeIconLabel);
        volumePanel.add(volumeSlider);

        // Music selection buttons
        JPanel musicButtonPanel = new JPanel(new FlowLayout());
        JButton funnyButton = new JButton("Funny");
        JButton chillButton = new JButton("Chill");
        JButton thrillingButton = new JButton("Thrilling");

        funnyButton.addActionListener(e -> playMusic("funny.wav"));
        chillButton.addActionListener(e -> playMusic("chill.wav"));
        thrillingButton.addActionListener(e -> playMusic("thrilling.wav"));

        musicButtonPanel.add(funnyButton);
        musicButtonPanel.add(chillButton);
        musicButtonPanel.add(thrillingButton);

        // Stop Music button
        JButton stopMusicButton = new JButton("Turn Off Music");
        stopMusicButton.addActionListener(e -> {
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.close();
                musicClip = null;
            } else {
                JOptionPane.showMessageDialog(this, "Music is already off.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panel.add(volumePanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(musicButtonPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(stopMusicButton);

        return panel;
    }

    public JPanel createMenuPanel() {
        // Create a panel with absolute positioning for better control
        JPanel panel = new JPanel(null) {
            private ImageIcon backgroundIcon = new ImageIcon("Sudoku.gif");

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
            private ImageIcon backgroundIcon = new ImageIcon("Sudoku.png"); // Corrected file path

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
        // In createGamePanel(), modify the cell creation code:
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col] = new JButton();
                cells[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                cells[row][col].setOpaque(true);
                cells[row][col].setBorderPainted(true);
                cells[row][col].setBorder(BorderFactory.createLineBorder(Color.GRAY));

                // Set solid background colors
                if ((row / 3 + col / 3) % 2 == 0) {
                    cells[row][col].setBackground(new Color(240, 240, 240));
                } else {
                    cells[row][col].setBackground(new Color(220, 220, 220));
                }

                final int r = row;
                final int c = col;
                cells[row][col].addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        cells[r][c].setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                    }
                    public void mouseExited(MouseEvent e) {
                        cells[r][c].setBorder(BorderFactory.createLineBorder(Color.GRAY));
                    }
                });

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

        // Set cell to appear selected
        cells[row][col].setBackground(new Color(200, 230, 255));

        String input = JOptionPane.showInputDialog(this, "Enter number (1-9):");

        // Reset cell appearance
        if ((row / 3 + col / 3) % 2 == 0) {
            cells[row][col].setBackground(new Color(240, 240, 240));
        } else {
            cells[row][col].setBackground(new Color(220, 220, 220));
        }

        if (input == null || input.trim().isEmpty()) {
            return; // User cancelled or entered nothing
        }

        try {
            int num = Integer.parseInt(input.trim());
            // Change this part:
            if (num >= 1 && num <= 9) {
                cells[row][col].setText(Integer.toString(num));
                puzzle[row][col] = num;
                cells[row][col].setForeground(Color.BLACK); // Changed to black
                cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.BOLD)); // Make bold

                if (isBoardComplete() && isSolutionCorrect()) {
                    JOptionPane.showMessageDialog(this, "Congratulations! You solved the puzzle!");
                }

            } else {
                JOptionPane.showMessageDialog(this, "Please enter a number between 1-9",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number (1-9)",
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

                if (puzzle[row][col] == 0) {
                    cells[row][col].setText("");
                    cells[row][col].setForeground(Color.BLACK);
                    cells[row][col].setEnabled(true);
                } else {
                    cells[row][col].setForeground(Color.BLACK);
                    cells[row][col].setEnabled(false);
                }

                // Reset background colors
                if ((row / 3 + col / 3) % 2 == 0) {
                    cells[row][col].setBackground(new Color(240, 240, 240));
                } else {
                    cells[row][col].setBackground(new Color(220, 220, 220));
                }
            }
        }
    }

    private void solveBoard() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cells[row][col].setText(Integer.toString(solution[row][col]));
                cells[row][col].setForeground(Color.BLUE); // Keep solved answers blue
                cells[row][col].setFont(cells[row][col].getFont().deriveFont(Font.BOLD));
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
            private ImageIcon backgroundIcon = new ImageIcon("Sudoku (2).gif"); // Corrected file path

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
        // Main panel with background
        JPanel panel = new JPanel(new BorderLayout()) {
            private ImageIcon backgroundIcon = new ImageIcon("Sudoku (3).gif");

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Center container for all components
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(100, 0, 0, 0)); // Top padding

        // Volume control panel (transparent background)
        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        volumePanel.setOpaque(false);

        // Volume icon
        ImageIcon volumeOnIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("volumeON.png"))
                        .getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
        ImageIcon volumeOffIcon = new ImageIcon(
                new ImageIcon(getClass().getResource("volumeOFF.png"))
                        .getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH));
        JLabel volumeIconLabel = new JLabel(volumeOnIcon);

        // Volume slider with transparent background
        JSlider volumeSlider = new JSlider(0, 100, (int)(previousVolume * 100)) {
            @Override
            public void updateUI() {
                super.updateUI();
                setOpaque(false);
                setBackground(new Color(0, 0, 0, 0));
                setForeground(Color.WHITE);
            }
        };
        volumeSlider.setPreferredSize(new Dimension(300, 40));
        volumeSlider.setOpaque(false);

        // Volume control setup
        volumeSlider.addChangeListener(e -> {
            if (!isMuted && volumeControl != null) {
                int value = volumeSlider.getValue();
                float volume = (float) value / 100f;
                previousVolume = volume;
                float dB = (float) (Math.log10(volume) * 20);
                volumeControl.setValue(dB);
            }
        });

        volumeIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        volumeIconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isMuted = !isMuted;
                volumeIconLabel.setIcon(isMuted ? volumeOffIcon : volumeOnIcon);
                if (volumeControl != null) {
                    volumeControl.setValue(isMuted ? -80f : (float)(Math.log10(previousVolume) * 20));
                }
                volumeSlider.setValue(isMuted ? 0 : (int)(previousVolume * 100));
            }
        });

        volumePanel.add(volumeIconLabel);
        volumePanel.add(volumeSlider);

        // Music buttons panel
        JPanel musicPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        musicPanel.setOpaque(false);
        musicPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JButton funnyButton = createMusicButton("Funny");
        JButton chillButton = createMusicButton("Chill");
        JButton thrillingButton = createMusicButton("Thrilling");

        funnyButton.addActionListener(e -> playMusic("funny.wav"));
        chillButton.addActionListener(e -> playMusic("chill.wav"));
        thrillingButton.addActionListener(e -> playMusic("thrilling.wav"));

        musicPanel.add(funnyButton);
        musicPanel.add(chillButton);
        musicPanel.add(thrillingButton);

        // Control buttons panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);
        controlPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        JButton stopMusicButton = createControlButton("Turn Off Music");
        stopMusicButton.addActionListener(e -> stopMusic());

        JButton backButton = createControlButton("Back to Menu");
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        controlPanel.add(stopMusicButton);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(backButton);

        // Add all components to center panel
        centerPanel.add(volumePanel);
        centerPanel.add(musicPanel);
        centerPanel.add(controlPanel);

        // Add center panel to main panel
        panel.add(Box.createVerticalGlue());
        panel.add(centerPanel);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // Helper method to create consistent music buttons
    private JButton createMusicButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setPreferredSize(new Dimension(120, 40));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return button;
    }

    // Helper method to create consistent control buttons
    private JButton createControlButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 35));
        button.setBackground(Color.WHITE);  // Changed to white
        button.setForeground(Color.BLACK);  // Changed to black
        button.setOpaque(true);             // Make sure it's opaque
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));
        return button;
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
            // Stop any currently playing music
            if (musicClip != null && musicClip.isRunning()) {
                musicClip.stop();
                musicClip.close();
            }

            // Try to load from classpath first
            InputStream audioSrc = getClass().getResourceAsStream("/" + filename);
            if (audioSrc == null) {
                // If not found in classpath, try direct file access
                audioSrc = new FileInputStream(filename);
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(audioSrc));

            musicClip = AudioSystem.getClip();
            musicClip.open(audioStream);

            // Set up volume control
            volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log10(previousVolume) * 20);
            volumeControl.setValue(dB);

            musicClip.start();
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error loading audio file: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Couldn't load audio file: " + filename,
                    "Audio Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (LineUnavailableException e) {
            System.err.println("Audio line unavailable: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error playing music: " + e.getMessage());
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