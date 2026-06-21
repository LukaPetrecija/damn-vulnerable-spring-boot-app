package com.groovycoder.dvsba.serialization;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.File;
import java.io.FileInputStream;
import java.io.StreamCorruptedException;

@Service
public class SecureDeserializationService {

    private static final byte[] SERIALIZATION_MAGIC = { (byte) 0xAC, (byte) 0xED, 0x00, 0x05 };

    public boolean hasValidMagicBytes(InputStream in) throws IOException {
        byte[] header = new byte[4];
        int bytesRead = in.read(header, 0, 4);

        if (bytesRead != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (header[i] != SERIALIZATION_MAGIC[i]) {
                return false;   
            }
        }
        return true;
    }

    public UserProfile secureDeserialize(String filePath) throws IOException, ClassNotFoundException {
        File file = new File(filePath);
        try (InputStream check = new FileInputStream(file)) {
            if (!hasValidMagicBytes(check)) {
                throw new StreamCorruptedException("Invalid file: not a Java serialization stream (bad magic bytes)");
            }
        }

        try (InputStream in = new FileInputStream(file);
        WhitelistObjectInputStream ois = new WhitelistObjectInputStream(in)) {
            Object obj = ois.readObject();
            if (!(obj instanceof UserProfile)) {
                throw new InvalidClassException("Deserialized object is not a UserProfile");
            }
            return (UserProfile) obj;
        }
    }
}