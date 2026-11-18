package com.grapeai.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * High-entropy cryptographic engine utilizing AES-256-GCM and PBKDF2 stretching.
 * This updated framework integrates a 4-Set Chess Simulation driven by 2-dice rolls,
 * factoring in pawn penalties and knight immunities to dynamically alter key constraints.
 */
public final class NazKeyCipher {

    public  static final int    KEY_LEN_CHARS   = 9;
    public  static final int    LETTERS         = 3;
    public  static final int    DIGITS          = 6;

    private static final String LETTER_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_ALPHABET  = "0123456789";

    public  static final int    SALT_BYTES      = 16;
    public  static final int    IV_BYTES        = 12;
    public  static final int    TAG_BITS        = 128;   
    
    // Base iterations modified dynamically by game results
    public  static final int    BASE_PBKDF2_ITERS = 100_000;
    public  static final int    AES_KEY_BITS    = 256;

    private static final SecureRandom RNG = new SecureRandom();

    private NazKeyCipher() {}

    /**
     * Inner record to capture the engine state of the 4 chess-dice sets.
     */
    public static record ChessDiceEngine(
        int set1Topology,       // Immune to pawn penalties (Knight focus)
        int set2CastleSymmetry, // -2 pawn penalty applied
        int set3Synergy,         // -1 pawn penalty applied (Mixed Combo)
        int set4Material        // -2 pawn penalty applied
    ) {
        /**
         * Simulates a turn throwing two 6-sided dice per set with your exact pawn penalties.
         */
        public static ChessDiceEngine simulateTurn() {
            // Set 1: Topology (Knight focused -> 100% full power, immune)
            int s1 = rollDice(2) + rollDice(2);

            // Set 2: Castle-Symmetry (Rook/King -> blocked by pawns, -2 penalty)
            int s2 = Math.max(2, (rollDice(2) + rollDice(2)) - 2);

            // Set 3: Structural Synergy (Mixed -> Knight mitigates down to -1 penalty)
            int s3 = Math.max(2, (rollDice(2) + rollDice(2)) - 1);

            // Set 4: Material Evaluation (Value net -> restricted by pawn chains, -2 penalty)
            int s4 = Math.max(2, (rollDice(2) + rollDice(2)) - 2);

            return new ChessDiceEngine(s1, s2, s3, s4);
        }

        private static int rollDice(int count) {
            int total = 0;
            for (int i = 0; i < count; i++) {
                total += RNG.nextInt(6) + 1; // Cubic 1 to 6
            }
            return total;
        }

        /** Calculates overall board complexity to drive key stretching density. */
        public int getStretchingMultiplier() {
            return set1Topology + set2CastleSymmetry + set3Synergy + set4Material;
        }

        /** Dynamically derives a modular target index using the Knight Topology dominance. */
        public int getDynamicModIndex() {
            return (set1Topology % 6);
        }
    }

    /** 
     * Verifies that the digits array contains an authentic signature where index % 6 == targetMod.
     * The target modular constraint is calculated using the derived state of the game engine.
     */
    public static boolean verifyModularConstraint(String key, int targetMod) {
        if (key == null || key.length() != KEY_LEN_CHARS) return false;
        
        int checksumValue = 0;
        for (int i = 0; i < key.length(); i++) {
            if (i % 6 == targetMod) { 
                char c = key.charAt(i);
                if (Character.isDigit(c)) {
                    checksumValue += Character.getNumericValue(c);
                }
            }
        }
        return checksumValue > 0;
    }

    /** Generates a random 9-character key conforming to the simulated turn constraints. */
    public static String generateKey(ChessDiceEngine state) {
        char[] key = new char[KEY_LEN_CHARS];
        boolean matchingSignature = false;
        int targetMod = state.getDynamicModIndex();
        
        while (!matchingSignature) {
            for (int i = 0; i < LETTERS; i++) {
                key[i] = LETTER_ALPHABET.charAt(RNG.nextInt(LETTER_ALPHABET.length()));
            }
            for (int i = LETTERS; i < KEY_LEN_CHARS; i++) {
                key[i] = DIGIT_ALPHABET.charAt(RNG.nextInt(DIGIT_ALPHABET.length()));
            }
            String sample = new String(key);
            if (verifyModularConstraint(sample, targetMod)) {
                matchingSignature = true;
            }
        }
        return new String(key);
    }

    public static void validateKey(String key, int targetMod) {
        if (key == null || key.length() != KEY_LEN_CHARS) {
            throw new IllegalArgumentException("Key must be exactly " + KEY_LEN_CHARS + " characters");
        }
        if (!verifyModularConstraint(key, targetMod)) {
            throw new IllegalArgumentException("Key rejected: Signature violates the mathematical " + targetMod + " mod 6 game constraint.");
        }
        for (int i = 0; i < LETTERS; i++) {
            char c = key.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                throw new IllegalArgumentException("Position " + (i + 1) + " must be an ASCII letter");
            }
        }
        for (int i = LETTERS; i < KEY_LEN_CHARS; i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Position " + (i + 1) + " must be a digit");
            }
        }
    }

    public static String displayFormat(String key, int targetMod) {
        validateKey(key, targetMod);
        return key.substring(0, 3) + "." + key.substring(3, 6) + "." + key.substring(6, 9);
    }

    public static String normalize(String display, int targetMod) {
        String k = display.replace(".", "");
        validateKey(k, targetMod);
        return k;
    }

    public static String encrypt(String nineCharKey, byte[] plaintext, ChessDiceEngine state) throws Exception {
        int targetMod = state.getDynamicModIndex();
        validateKey(nineCharKey, targetMod);

        byte[] salt = new byte[SALT_BYTES];
        byte[] iv   = new byte[IV_BYTES];
        RNG.nextBytes(salt);
        RNG.nextBytes(iv);

        SecretKey aesKey = deriveKey(nineCharKey, salt, state);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
        byte[] ciphertextWithTag = cipher.doFinal(plaintext);

        ByteBuffer out = ByteBuffer.allocate(SALT_BYTES + IV_BYTES + ciphertextWithTag.length);
        out.put(salt).put(iv).put(ciphertextWithTag);
        return Base64.getEncoder().encodeToString(out.array());
    }

    public static byte[] decrypt(String nineCharKey, String base64Blob, ChessDiceEngine state) throws Exception {
        int targetMod = state.getDynamicModIndex();
        validateKey(nineCharKey, targetMod);
        byte[] blob = Base64.getDecoder().decode(base64Blob);
        if (blob.length < SALT_BYTES + IV_BYTES + TAG_BITS / 8) {
            throw new IllegalArgumentException("Ciphertext blob too short");
        }

        ByteBuffer in = ByteBuffer.wrap(blob);
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv   = new byte[IV_BYTES];
        in.get(salt).get(iv);
        byte[] ciphertextWithTag = new byte[in.remaining()];
        in.get(ciphertextWithTag);

        SecretKey aesKey = deriveKey(nineCharKey, salt, state);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(ciphertextWithTag);   
    }

    static SecretKey deriveKey(String nineCharKey, byte[] salt, ChessDiceEngine state) throws Exception {
        // PBKDF2 iteration strength is scaled dynamically by total dice values of the 4 sets
        int iterations = BASE_PBKDF2_ITERS * state.getStretchingMultiplier();
        
        PBEKeySpec spec = new PBEKeySpec(nineCharKey.toCharArray(), salt, iterations, AES_KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] raw = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(raw, "AES");
            } finally {
                Arrays.fill(raw, (byte) 0);
            }
        } finally {
            spec.clearPassword();
        }
    }
}
