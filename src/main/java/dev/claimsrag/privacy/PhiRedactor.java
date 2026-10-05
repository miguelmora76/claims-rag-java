package dev.claimsrag.privacy;

import java.util.regex.Pattern;

/**
 * Best-effort pattern redaction applied to questions before they leave the service.
 * Demonstrates the pattern only. It is NOT a HIPAA de-identification method and misses names, addresses and free text.
 */
public final class PhiRedactor {

    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b\\(?\\d{3}\\)?[-. ]\\d{3}[-. ]\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[\\w.]+\\b");
    private static final Pattern MRN = Pattern.compile("(?i)\\b(mrn|member id|patient id)[:#\\s]*[A-Z0-9-]{5,}\\b");
    private static final Pattern DOB = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b");

    private PhiRedactor() {}

    public static String redact(String s) {
        String out = SSN.matcher(s).replaceAll("[SSN]");
        out = PHONE.matcher(out).replaceAll("[PHONE]");
        out = EMAIL.matcher(out).replaceAll("[EMAIL]");
        out = MRN.matcher(out).replaceAll("[ID]");
        out = DOB.matcher(out).replaceAll("[DATE]");
        return out;
    }
}
