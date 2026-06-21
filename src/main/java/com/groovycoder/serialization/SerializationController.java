package com.groovycoder.dvsba.serialization;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@RestController
public class SerializationController {

    private final SerializationService serializationService;
    private final SecureDeserializationService secureDeserializationService;

    public SerializationController(SerializationService serializationService,
        SecureDeserializationService secureDeserializationService) {
            this.serializationService = serializationService;
            this.secureDeserializationService = secureDeserializationService;
        }

    @GetMapping("/deserialize/valid")
    public ResponseEntity<?> validCase() {
        try {
            UserProfile profile = new UserProfile("user", "user@example.com", "USER");
            String path = serializationService.serialize(profile, "valid.ser");
            UserProfile result = secureDeserializationService.secureDeserialize(path);
            return ResponseEntity.ok("Deserialized successfully: " + result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/deserialize/nonwhitelisted")
    public ResponseEntity<?> nonWhitelistedCase() {
        try {
            String path = "/tmp/serialized/evil.ser";
            new File("/tmp/serialized/").mkdirs();
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
                java.util.HashMap<String, String> notAllowed = new java.util.HashMap<>();
                notAllowed.put("payload", "this class is not whitelisted");
                out.writeObject(notAllowed);
            }
            secureDeserializationService.secureDeserialize(path);
            return ResponseEntity.status(500).body("ERROR: non-whitelisted class was NOT rejected!");
        } catch (InvalidClassException e) {
            return ResponseEntity.status(403).body("REJECTED (whitelist): " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Other error: " + e.getMessage());
        }
    }

    @GetMapping("/deserialize/invalidfile")
    public ResponseEntity<?> invalidFileCase() {
        try {
            String path = "/tmp/serialized/notserialized.txt";
            new File("/tmp/serialized/").mkdirs();
            try (FileWriter w = new FileWriter(path)) {
                w.write("This is just plain text, not a serialized object.");
            }
            secureDeserializationService.secureDeserialize(path);
            return ResponseEntity.status(500).body("ERROR: invalid file was NOT rejected!");
        } catch (StreamCorruptedException e) {
            return ResponseEntity.status(400).body("REJECTED (magic bytes): " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Other error: " + e.getMessage());
        }
    }
}