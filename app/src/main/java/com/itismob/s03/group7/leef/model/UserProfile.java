package com.itismob.s03.group7.leef.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/**
 * A LEEF user's profile, stored in Firestore at {@code users/{uid}}. The sign-in credentials
 * (email and password) live in Firebase Authentication; this holds everything else about the person.
 */
public final class UserProfile {

    public static final String COLLECTION = "users";

    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_SCHOOL_ORGANIZATION = "schoolOrganization";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_CREATED_AT = "createdAt";

    private final String uid;
    private final String fullName;
    private final String schoolOrganization;
    private final String email;
    private final UserRole role;

    public UserProfile(@NonNull String uid, @NonNull String fullName, @NonNull String schoolOrganization,
                       @NonNull String email, @NonNull UserRole role) {
        this.uid = uid;
        this.fullName = fullName;
        this.schoolOrganization = schoolOrganization;
        this.email = email;
        this.role = role;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    @NonNull
    public String getFullName() {
        return fullName;
    }

    @NonNull
    public String getSchoolOrganization() {
        return schoolOrganization;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public UserRole getRole() {
        return role;
    }

    /** First word of the full name, for friendly greetings. */
    @NonNull
    public String getFirstName() {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    /** The document written to Firestore when the account is created. */
    @NonNull
    public Map<String, Object> toNewDocument() {
        Map<String, Object> document = new HashMap<>();
        document.put(FIELD_FULL_NAME, fullName);
        document.put(FIELD_SCHOOL_ORGANIZATION, schoolOrganization);
        document.put(FIELD_EMAIL, email);
        document.put(FIELD_ROLE, role.getValue());
        document.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        return document;
    }
}
