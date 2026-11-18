package com.grapeai.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Dispatched I/O sub-system managing local file system reading and writing.
 * Now integrated with the 4-Set Chessboard Dice Engine to automatically package
 * and unpack the dynamic game state alongside the cryptographic blob.
 */
public final class CanopyDataExporter {

    private static final String CORRELATION_DELIMITER = "::CHESS_DICE_STATE::";

    private CanopyDataExporter() {}

    /**
     * Packages the encrypted ciphertext with the active chessboard dice metrics 
     * and exports the consolidated snapshot to the file system.
     *
     * @param targetFile  The target file destination.
     * @param cipherText  The encrypted data string from NazKeyCipher.
     * @param set1        The Knight-focused Topology score (unpenalised).
     * @param set2        The Castle-Symmetry score (-2 pawn penalty applied).
     * @param set3        The Structural Synergy score (-1 pawn penalty applied).
     * @param set4        The Material Evaluation score (-2 pawn penalty applied).
     * @throws IOException If a file system write error occurs.
     */
    public static void exportSnapshot(File targetFile, String cipherText, int set1, int set2, int set3, int set4) throws IOException {
        if (!targetFile.getName().toLowerCase().endsWith(".dat")) {
            targetFile = new File(targetFile.getAbsolutePath() + ".dat");
        }

        // Format metadata header: [Set1]-[Set2]-[Set3]-[Set4]::CHESS_DICE_STATE::[Ciphertext]
        String structuredEnvelope = String.format("%d-%d-%d-%d%s%s", 
            set1, set2, set3, set4, CORRELATION_DELIMITER, cipherText);

        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            writer.write(structuredEnvelope);
        }
    }

    /**
     * Imports a snapshot and strips away the metadata payload, returning 
     * exclusively the functional ciphertext to be passed back to the decryption layer.
     */
    public static String importSnapshot(File sourceFile) throws IOException {
        byte[] encodedBytes = Files.readAllBytes(sourceFile.toPath());
        String fullEnvelope = new String(encodedBytes, StandardCharsets.UTF_8).trim();

        if (fullEnvelope.contains(CORRELATION_DELIMITER)) {
            String[] components = fullEnvelope.split(CORRELATION_DELIMITER, 2);
            return components[1]; // Returns purely the cipherText segment
        }
        return fullEnvelope; // Fallback for backwards compatibility with legacy plaintext blobs
    }

    /**
     * Parses the underlying file to extract the exact historical 4-set dice metrics.
     * This provides the necessary variables to reconstruct the matching ChessDiceEngine 
     * instance for iteration strength calculations.
     *
     * @return An integer array containing [set1, set2, set3, set4], or null if missing.
     */
    public static int[] parseHistoricalDiceState(File sourceFile) throws IOException {
        byte[] encodedBytes = Files.readAllBytes(sourceFile.toPath());
        String fullEnvelope = new String(encodedBytes, StandardCharsets.UTF_8).trim();

        if (fullEnvelope.contains(CORRELATION_DELIMITER)) {
            String[] components = fullEnvelope.split(CORRELATION_DELIMITER, 2);
            String[] metrics = components[0].split("-");
            if (metrics.length == 4) {
                return new int[] {
                    Integer.parseInt(metrics[0]),
                    Integer.parseInt(metrics[1]),
                    Integer.parseInt(metrics[2]),
                    Integer.parseInt(metrics[3])
                };
            }
        }
        return null;
    }
}
