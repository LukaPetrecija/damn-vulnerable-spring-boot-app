package com.groovycoder.dvsba.serialization;

import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Service
public class SerializationService {

    private static final String STORAGE_DIR = "/tmp/serialized/";

    public String serialize(UserProfile profile, String fileName) throws IOException {
        java.io.File dir = new java.io.File(STORAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = STORAGE_DIR + fileName;

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(profile);
        }

        return filePath;
    }
}