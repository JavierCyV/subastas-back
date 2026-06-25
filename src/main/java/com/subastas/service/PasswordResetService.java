package com.subastas.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final ResendMailService mailService;

    private record Entry(String code, Instant expiry) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private static final int EXPIRY_MINUTES = 15;

    public void sendCode(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        store.put(email, new Entry(code, Instant.now().plusSeconds(EXPIRY_MINUTES * 60L)));

        mailService.send(
            email,
            "Código de recuperación — Subastas",
            "Tu código de recuperación es: " + code + "\n\n" +
            "Válido por " + EXPIRY_MINUTES + " minutos.\n" +
            "Si no solicitaste esto, ignorá este mensaje."
        );
    }

    public boolean verifyCode(String email, String code) {
        Entry entry = store.get(email);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiry())) {
            store.remove(email);
            return false;
        }
        return entry.code().equals(code);
    }

    public void invalidate(String email) {
        store.remove(email);
    }
}
