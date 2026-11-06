package com.celiloglu.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Birdəfəlik lokal şifrə sıfırlama vasitəsi.
 * Tətbiq başlayanda avtomatik icra edilmir.
 * Yalnız tələb olunduqda terminaldan əl ilə başladılır.
 */
public class PasswordResetTool {

    private static final String TARGET_USERNAME = "tuncay";
    private static final String OUTPUT_FILE_NAME = "reset-password-tuncay.sql";
    private static final String RELATIVE_PRIVATE_DIR = "local-private/db";

    public static void main(String[] args) {
        try {
            System.out.println("=========================================================");
            System.out.println("  Celiloglu - 'tuncay' Hesabı Şifrə Sıfırlama Vasitəsi   ");
            System.out.println("=========================================================");
            System.out.println("Hədəf istifadəçi : " + TARGET_USERNAME);
            System.out.println("Qayda            : Minimum 12 simvol, maksimum 72 UTF-8 bayt (trim edilmir).");
            System.out.println("---------------------------------------------------------");

            char[] passwordChars;
            Console console = System.console();

            if (console != null) {
                passwordChars = console.readPassword("Yeni şifrəni daxil edin: ");
                if (passwordChars == null || passwordChars.length == 0) {
                    System.err.println("Xəta: Şifrə boş ola bilməz!");
                    System.exit(1);
                    return;
                }
                char[] confirmChars = console.readPassword("Yeni şifrəni təkrar daxil edin: ");
                if (!Arrays.equals(passwordChars, confirmChars)) {
                    System.err.println("Xəta: Daxil edilən şifrələr bir-biri ilə uyğun gəlmir!");
                    Arrays.fill(passwordChars, ' ');
                    if (confirmChars != null)
                        Arrays.fill(confirmChars, ' ');
                    System.exit(1);
                    return;
                }
                if (confirmChars != null) {
                    Arrays.fill(confirmChars, ' ');
                }
            } else {
                // Console null olduqda (məsələn, PowerShell boru xəttindən / pipeline
                // ötürüldükdə)
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    System.err.println("Xəta: Daxil edilən şifrə boşdur!");
                    System.exit(1);
                    return;
                }
                passwordChars = line.toCharArray();
            }

            String rawPassword = new String(passwordChars);
            Arrays.fill(passwordChars, ' ');

            // Şifrə qaydalarını yoxla (trim edilmədən)
            validatePassword(rawPassword);

            // Mövcud PasswordEncoder-in BCrypt (güc 10) qaydaları ilə hash-lə
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String hash = encoder.encode(rawPassword);

            // SQL faylını yarat
            Path outputPath = resolveOutputPath();
            generateSqlFile(outputPath, hash);

            System.out.println();
            System.out.println("[+] Şifrə uğurla BCrypt formatında hash-ləndi.");
            System.out.println("[+] SQL faylı hazırlandı: " + outputPath.toAbsolutePath().normalize());
            System.out.println("[i] Təhlükəsizlik qaydalarına əsasən şifrə və hash ekrana çıxarılmır.");
            System.out.println("[i] Canlı bazada avtomatik icra edilmədi.");
            System.out.println("=========================================================");
        } catch (IllegalArgumentException ex) {
            System.err.println("Validasiya Xətası: " + ex.getMessage());
            System.exit(1);
        } catch (Exception ex) {
            System.err.println("Gözlənilməz Xəta: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Şifrə boş ola bilməz!");
        }
        if (password.length() < 12) {
            throw new IllegalArgumentException(
                    "Şifrə ən azı 12 simvoldan ibarət olmalıdır! Cari simvol sayı: " + password.length());
        }
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            throw new IllegalArgumentException("Şifrə maksimum 72 bayt (UTF-8) ola bilər! Cari ölçü: " + bytes.length);
        }
    }

    private static Path resolveOutputPath() {
        if (Files.exists(Path.of("pom.xml"))) {
            return Path.of(RELATIVE_PRIVATE_DIR, OUTPUT_FILE_NAME);
        }
        if (Files.exists(Path.of("celiloglu/pom.xml"))) {
            return Path.of("celiloglu", RELATIVE_PRIVATE_DIR, OUTPUT_FILE_NAME);
        }
        if (Files.isDirectory(Path.of("celiloglu"))) {
            return Path.of("celiloglu", RELATIVE_PRIVATE_DIR, OUTPUT_FILE_NAME);
        }
        return Path.of(RELATIVE_PRIVATE_DIR, OUTPUT_FILE_NAME);
    }

    private static void generateSqlFile(Path path, String hash) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        String sqlContent = """
                -- ==============================================================================
                -- Celiloglu Backend - 'tuncay' Hesabının Şifrə Yenilənməsi
                -- ==============================================================================
                -- Qeyd: Bu skript yalnız 'tuncay' istifadəçisinin password hash-ini yeniləyir.
                -- Rol, aktivlik və digər hesab məlumatları dəyişdirilmir.
                -- Tranzaksiya daxilində (BEGIN ... COMMIT) icra olunur.
                -- ==============================================================================

                BEGIN;

                UPDATE users
                SET password = '%s'
                WHERE username = 'tuncay';

                COMMIT;
                """.formatted(hash);

        Files.writeString(path, sqlContent, StandardCharsets.UTF_8);
    }
}
