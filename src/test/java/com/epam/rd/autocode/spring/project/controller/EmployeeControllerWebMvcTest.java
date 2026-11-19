package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static com.epam.rd.autocode.spring.project.util.AbstractWebMvcTest.EMPLOYEE_EMAIL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// REST API of employees (/api/employees): employee-only, see SecurityConfigWebMvcTest for the access rules.
@WithMockUser(username = EMPLOYEE_EMAIL, roles = "EMPLOYEE")
class EmployeeControllerWebMvcTest extends AbstractWebMvcTest {

    // ---------- helpers ----------

    // a Map is used because EmployeeDTO.password is WRITE_ONLY and would be omitted when serializing the DTO
    private String employeeJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", "John Doe",
                "email", EMPLOYEE_EMAIL,
                "password", "StrongPass1!",
                "phone", "555-123-4567",
                "birthDate", "1990-05-15"));
    }

    // ---------- endpoints ----------

    @Test
    void getAllEmployees_shouldReturnList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(sampleEmployee()));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMPLOYEE_EMAIL));
    }

    @Test
    void getEmployeeByEmail_shouldReturnEmployee() throws Exception {
        when(employeeService.getEmployeeByEmail(EMPLOYEE_EMAIL)).thenReturn(sampleEmployee());

        mockMvc.perform(get("/api/employees/{email}", EMPLOYEE_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE_EMAIL));
    }

    @Test
    void addEmployee_shouldReturn201WithCreatedEmployee() throws Exception {
        when(employeeService.addEmployee(any(EmployeeDTO.class))).thenReturn(sampleEmployee());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMPLOYEE_EMAIL));
    }

    @Test
    void updateEmployeeByEmail_shouldReturnUpdatedEmployee() throws Exception {
        when(employeeService.updateEmployeeByEmail(eq(EMPLOYEE_EMAIL), any(EmployeeDTO.class)))
                .thenReturn(sampleEmployee());

        mockMvc.perform(put("/api/employees/{email}", EMPLOYEE_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMPLOYEE_EMAIL));
    }

    @Test
    void deleteEmployeeByEmail_shouldReturn204AndCallService() throws Exception {
        mockMvc.perform(delete("/api/employees/{email}", EMPLOYEE_EMAIL))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployeeByEmail(EMPLOYEE_EMAIL);
    }
}