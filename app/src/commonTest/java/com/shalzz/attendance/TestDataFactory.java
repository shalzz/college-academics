package com.shalzz.attendance;

import com.shalzz.attendance.data.model.User;

import java.util.UUID;

/**
 * Factory class that makes instances of data models with random field values.
 * The aim of this class is to help setting up test fixtures.
 */
public class TestDataFactory {

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    public static User makeUser(String uniqueSuffix) {
        return User.builder()
                .setId(randomUuid())
                .setRoll_number(randomUuid())
                .setName("Name-" + uniqueSuffix)
                .setEmail("email" + uniqueSuffix + "@ribot.co.uk")
                .setCourse("Course-" + uniqueSuffix)
                .setPhone("Phone-" + uniqueSuffix)
                .build();
    }

}