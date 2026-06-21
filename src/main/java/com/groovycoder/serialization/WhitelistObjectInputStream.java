package com.groovycoder.dvsba.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WhitelistObjectInputStream extends ObjectInputStream {

    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "com.groovycoder.dvsba.serialization.UserProfile",
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Long",
            "java.lang.Integer"
    ));

    public WhitelistObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String className = desc.getName();

        if (!WHITELIST.contains(className)) {
            throw new InvalidClassException("Unauthorized deserialization attempt of class: " + className);
        }

        return super.resolveClass(desc);
    }
}