package com.grapeai.gui;

import com.grapeai.security.NazKeyCipher;
import com.grapeai.io.CanopyDataExporter;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class CanopyVisualizerGUI extends JFrame {

    private static final int    LEAVES_PER_BRANCH      = 30;
    private static final double SPIRAL_TURNS_PER_METER = 2.0;
    private static final double METERS_PER_BRANCH      = 2.5;

    private static final int MAX_AGE       = 12;
    private static final int MIN_BRANCHES  = 8;
    private static final int MAX_BRANCHES  = 18;
    private static final int DEFAULT_AGE   = 1;
    private static final int DEFAULT_BRANCHES = 13;

    private final VisualizerPanel renderPanel;
    private final JSlider         ageSlider;
    private final JSlider         branchSlider;
    private final JLabel          metricsLabel;

    // Secure Crypto Management Fields
    private final JTextField      keyField;
    private final JTextField      snapshotField;

    // --- Chessboard Two-Dice Simulation Components ---
    private NazKeyCipher.ChessDiceEngine activeEngineState;
    private final JLabel          diceStatusLabel;

    public CanopyVisualizerGUI() {
        setTitle("🍇 GrapeAI — Secured 2D Coneoid Tensor Interface");
        setSize(1000, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialize active 4-Set chess engine on boot
        activeEngineState = NazKeyCipher.ChessDiceEngine.simulateTurn();

        renderPanel = new VisualizerPanel();
        add(renderPanel, BorderLayout.CENTER);
        
        // Setup specialized GUI status display
        diceStatusLabel = new JLabel();
        diceStatusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        updateDiceDashboardDisplay();

        add(buildControlPanel(), BorderLayout.SOUTH);

        // Run baseline setup
        updateModelParameters();
    }

    private JPanel buildControlPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(245, 245, 247));
        container.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // --- Age Row Panel ---
        JPanel ageRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ageRow.setOpaque(false);
        ageRow.add(new JLabel("⏳ Plant Development Age (" + DEFAULT_AGE + " to " + MAX_AGE + " Years Old): "));
        ageSlider = new JSlider(1, MAX_AGE, DEFAULT_AGE);
        ageSlider.setMajorTickSpacing(1);
        ageSlider.setPaintTicks(true);
        ageSlider.setPaintLabels(true);
        ageSlider.setPreferredSize(new Dimension(400, 45));
        ageSlider.addChangeListener(e -> updateModelParameters());
        ageRow.add(ageSlider);

        // --- Branch Row Panel ---
        JPanel branchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        branchRow.setOpaque(false);
        branchRow.add(new JLabel("🍇 Active Vine Primary Branch Counts:        "));
        branchSlider = new JSlider(MIN_BRANCHES, MAX_BRANCHES, DEFAULT_BRANCHES);
        branchSlider.setMajorTickSpacing(2);
        branchSlider.setMinorTickSpacing(1);
        branchSlider.setPaintTicks(true);
        branchSlider.setPaintLabels(true);
        branchSlider.setPreferredSize(new Dimension(400, 45));
        branchSlider.addChangeListener(e -> updateModelParameters());
        branchRow.add(branchSlider);

        // --- Chess Dice Dashboard Row ---
        JPanel diceRow = new JPanel(new BorderLayout());
        diceRow.setBorder(BorderFactory.createTitledBorder("🎲 Chessboard 4-Set Two-Dice Engine Status"));
        diceRow.setOpaque(false);
        
        JPanel diceLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        diceLabelPanel.setOpaque(false);
        diceLabelPanel.add(diceStatusLabel);
        
        JButton rollBtn = new JButton("🎲 Roll Engine State Change");
        rollBtn.addActionListener(e -> {
            activeEngineState = NazKeyCipher.ChessDiceEngine.simulateTurn();
            updateDiceDashboardDisplay();
            updateModelParameters();
        });
        
        diceRow.add(diceLabelPanel, BorderLayout.CENTER);
        diceRow.add(rollBtn, BorderLayout.EAST);

        // --- NazKeyCipher Security Dashboard Layout ---
        JPanel securityPanel = new JPanel(new GridBagLayout());
        securityPanel.setBorder(BorderFactory.createTitledBorder("🔒 NazKeyCipher Snapshot Encryption"));
        securityPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        securityPanel.add(new JLabel("Secret Key (Dynamic Modulo):"), gbc);
        
        gbc.gridx = 1; gbc.weightx = 1.0;
        keyField = new JTextField(9);
        keyField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        securityPanel.add(keyField, gbc);

        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton genKeyBtn = new JButton("Generate Key");
        genKeyBtn.addActionListener(e -> {
            keyField.setText(NazKeyCipher.generateKey(activeEngineState));
        });
        securityPanel.add(genKeyBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        securityPanel.add(new JLabel("Encrypted Snapshot Blob:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        snapshotField = new JTextField();
        snapshotField.setEditable(false);
        snapshotField.setFont(new Font("Monospaced", Font.PLAIN, 11));
        securityPanel.add(snapshotField, gbc);

        // Cryptographic Trigger Execution Group
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel actionBtnGroup = new JPanel(new FlowLayout());
        actionBtnGroup.setOpaque(false);
        
        JButton encryptBtn = new JButton("🔒 Encrypt & Save to Disk");
        encryptBtn.addActionListener(e -> executeSnapshotEncryption());
        
        JButton decryptBtn = new JButton("🔓 Open & Load From Disk");
        decryptBtn.addActionListener(e -> executeSnapshotDecryption());
        
        actionBtnGroup.add(encryptBtn);
        actionBtnGroup.add(decryptBtn);
        securityPanel.add(actionBtnGroup, gbc);

        // --- Metrics Status Line ---
        metricsLabel = new JLabel();
        metricsLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        metricsLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 0));

        container.add(ageRow);
        container.add(branchRow);
        container.add(Box.createVerticalStrut(5));
        container.add(diceRow);
        container.add(Box.createVerticalStrut(5));
        container.add(securityPanel);
        container.add(metricsLabel);
        return container;
    }

    private void updateDiceDashboardDisplay() {
        diceStatusLabel.setText(String.format(
            "<html>S1 (Topology): <b>%d</b> [Immune] | S2 (Castle): <b>%d</b> [-2 Penalty] | S3 (Synergy): <b>%d</b> [-1 Penalty] | S4 (Material): <b>%d</b> [-2 Penalty]<br>" +
            "🔑 Target Modulo Constraint Signature: <font color='blue'><b>index %% 6 == %d</b></font> | Matrix Multiplier: <b>x%d</b></html>",
            activeEngineState.set1Topology(), activeEngineState.set2CastleSymmetry(),
            activeEngineState.set3Synergy(), activeEngineState.set4Material(),
            activeEngineState.getDynamicModIndex(), activeEngineState.getStretchingMultiplier()
        ));
    }

    private void updateModelParameters() {
        int currentAge = ageSlider.getValue();
        int branchCount = branchSlider.getValue();

        int totalExpectedLeaves = branchCount * LEAVES_PER_BRANCH;
        double canopyMeters   = branchCount * METERS_PER_BRANCH;
        double ageRatio       = (double) currentAge / MAX_AGE;

        // Visual modification parameter affected by total dice weights
        double simulatedDensityMultiplier = 1.0 + (activeEngineState.getStretchingMultiplier() / 48.0);

        metricsLabel.setText(String.format(
            "📊 [AI STATUS] Age Ratio: %.4f (1:%d) | Active Leaf Array: %d | Canopy Footprint: %.1fm | System Density: %.2fx",
            ageRatio, MAX_AGE, (int)(totalExpectedLeaves * simulatedDensityMultiplier), canopyMeters, simulatedDensityMultiplier
        ));

        renderPanel.recalculatePoints(currentAge, totalExpectedLeaves, canopyMeters);
    }

    // ================================================================ Crypto Operations

    private void executeSnapshotEncryption() {
        char[] passwordBuffer = keyField.getText().trim().toCharArray();
        String inputKey = new String(passwordBuffer);
        int targetMod = activeEngineState.getDynamicModIndex();
        try {
            if (inputKey.contains(".")) {
                inputKey = NazKeyCipher.normalize(inputKey, targetMod);
            }
            NazKeyCipher.validateKey(inputKey, targetMod); 

            int currentAge = ageSlider.getValue();
            int branchCount = branchSlider.getValue();
            String rawStateString = "AGE=" + currentAge + ";BRANCHES=" + branchCount;
            byte[] plainBytes = rawStateString.getBytes(StandardCharsets.UTF_8);

            String encryptedBlob = NazKeyCipher.encrypt(inputKey, plainBytes, activeEngineState);
            snapshotField.setText(encryptedBlob);
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("💾 Export Encrypted Canopy Snapshot");
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
