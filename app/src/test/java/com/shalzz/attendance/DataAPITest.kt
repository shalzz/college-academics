package com.shalzz.attendance

import com.shalzz.attendance.data.remote.DataAPI

import org.junit.Test
import junit.framework.Assert.assertEquals

class DataAPITest {

    @Test
    fun ApiEndpointIsCorrect() {
        assertEquals("https://academics.8bitlabs.tech/v3/prod/", DataAPI.ENDPOINT)
    }
}
