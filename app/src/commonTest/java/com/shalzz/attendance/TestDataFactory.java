package com.shalzz.attendance;

import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Factory class that makes instances of data models with random field values.
 * The aim of this class is to help setting up test fixtures.
 */
public class TestDataFactory {

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    public static int makeInt(String string) {
        StringBuilder sb = new StringBuilder();
        for(char c : string.toCharArray()) {
            sb.append((int)c);
        }
        return Integer.valueOf(sb.toString());
    }

    public static User makeUser(String uniqueSuffix) {
        return User.builder()
                .setId(randomUuid())
                .setRoll_number(randomUuid())
                .setName("Name-" + uniqueSuffix)
                .setEmail("email" + uniqueSuffix + "@gmail.com")
                .setCourse("Course-" + uniqueSuffix)
                .setPhone("Phone-" + uniqueSuffix)
                .build();
    }

    public static Subject makeSubject(String uniqueSuffix) {
        return Subject.builder()
                .setId(makeInt(uniqueSuffix))
                .setName("Name-" + uniqueSuffix)
                .setAttended(1f)
                .setHeld(2f)
                .build();
    }

    public static Period makePeriod(String uniqueSuffix, Date date) {
        return Period.builder()
                .setId(makeInt(uniqueSuffix))
                .setName("Name-" + uniqueSuffix)
                .setDate(DateHelper.formatToTechnicalFormat(date))
                .setStart("Start-" + uniqueSuffix)
                .setEnd("End-" + uniqueSuffix)
                .setRoom("Room-" + uniqueSuffix)
                .setTeacher("Teacher-" + uniqueSuffix)
                .setBatch("Batch-" + uniqueSuffix)
                .setBatchid("Batchid-" + uniqueSuffix)
                .setAbsent(uniqueSuffix.length() % 2 == 0)
                .build();
    }

}