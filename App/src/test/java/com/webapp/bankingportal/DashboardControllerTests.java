package com.webapp.bankingportal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.webapp.bankingportal.util.JsonUtil;

public class DashboardControllerTests extends BaseTest {

    @Test
    public void test_dashboard_with_authorized_access() throws Exception {
        Map<String, String> userDetails = createAndLoginUserWithPin();

        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/dashboard")
                .header("Authorization", "Bearer " + userDetails.get("token")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.accountNumber").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.balance").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").exists());
    }

    @Test
    public void test_dashboard_with_unauthorized_access() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                .get("/api/dashboard"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
