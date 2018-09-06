package com.shalzz.attendance;

import com.shalzz.attendance.data.remote.DataAPI;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class DataAPITest {

    @Test
    public void ApiEndpointIsCorrect() {
        assertEquals(DataAPI.Companion.getENDPOINT(), "https://academics.8bitlabs.tech/v2/prod/");
    }
}
