package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.dto.ProfileDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
    private static final String EMAIL = "employee@example.com";
    private static final String RAW_PASSWORD = "Str0ng!Pass";
    private static final String ENCODED_PASSWORD = "encoded";

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        // the real ModelMapper is used on purpose: the mapping is part of the behaviour under test
        employeeService = new EmployeeServiceImpl(employeeRepository, clientRepository,
                new ModelMapper(), passwordEncoder);
    }

    // ---------- helpers ----------

    private static Employee employee(Long id, String name, String email) {
        return new Employee(id, name, email, "hash", "+380501234567", LocalDate.of(1990, 1, 1));
    }

    private static Employee employee() {
        return employee(1L, "Employee", EMAIL);
    }

    private static EmployeeDTO request() {
        return new EmployeeDTO("New Name", EMAIL, RAW_PASSWORD, "+380671112233", LocalDate.of(1985, 5, 5));
    }

    private static ProfileDTO profile(String name, String phone, LocalDate birthDate) {
        ProfileDTO dto = new ProfileDTO();
        dto.setName(name);
        dto.setPhone(phone);
        dto.setBirthDate(birthDate);
        return dto;
    }

    private void stubSaveReturnsArgument() {
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- getAllEmployees ----------

    @Test
    void getAllEmployees_returnsMappedList() {
        when(employeeRepository.findAll()).thenReturn(List.of(
                employee(1L, "First", "first@example.com"),
                employee(2L, "Second", "second@example.com")));

        List<EmployeeDTO> result = employeeService.getAllEmployees();

        assertThat(result).extracting(EmployeeDTO::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        assertThat(result).extracting(EmployeeDTO::getName).containsExactly("First", "Second");
    }

    // ---------- getEmployeeByEmail ----------

    @Test
    void getEmployeeByEmail_found_returnsDto() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee()));

        EmployeeDTO result = employeeService.getEmployeeByEmail(EMAIL);

        assertThat(result.getName()).isEqualTo("Employee");
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getPhone()).isEqualTo("+380501234567");
        assertThat(result.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void getEmployeeByEmail_notFound_throws() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeByEmail(EMAIL))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- updateEmployeeByEmail ----------

    @Test
    void updateEmployeeByEmail_existingEmployee_updatesFieldsEncodesPasswordAndKeepsId() {
        Employee existing = employee();
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        stubSaveReturnsArgument();

        EmployeeDTO result = employeeService.updateEmployeeByEmail(EMAIL, request());

        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getPhone()).isEqualTo("+380671112233");
        assertThat(existing.getBirthDate()).isEqualTo(LocalDate.of(1985, 5, 5));
        // the raw password from the request must never reach the entity
        assertThat(existing.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateEmployeeByEmail_notFound_throwsAndSavesNothing() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployeeByEmail(EMAIL, request()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(passwordEncoder);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ---------- deleteEmployeeByEmail ----------

    @Test
    void deleteEmployeeByEmail_existingEmployee_deletes() {
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(true);

        employeeService.deleteEmployeeByEmail(EMAIL);

        verify(employeeRepository).deleteByEmail(EMAIL);
    }

    @Test
    void deleteEmployeeByEmail_notFound_throwsAndDeletesNothing() {
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployeeByEmail(EMAIL))
                .isInstanceOf(NotFoundException.class);

        verify(employeeRepository, never()).deleteByEmail(EMAIL);
    }

    // ---------- addEmployee ----------

    @Test
    void addEmployee_newEmail_savesWithEncodedPassword() {
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        stubSaveReturnsArgument();

        EmployeeDTO result = employeeService.addEmployee(request());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.getPhone()).isEqualTo("+380671112233");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(1985, 5, 5));
        // only the BCrypt hash is stored, never the raw password
        assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void addEmployee_emailTakenByEmployee_throwsAndSavesNothing() {
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.addEmployee(request()))
                .isInstanceOf(AlreadyExistException.class);

        verifyNoInteractions(passwordEncoder);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void addEmployee_emailTakenByClient_throwsAndSavesNothing() {
        // security rule: an email is unique across both tables, otherwise the login would be ambiguous
        when(employeeRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(clientRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.addEmployee(request()))
                .isInstanceOf(AlreadyExistException.class);

        verifyNoInteractions(passwordEncoder);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    // ---------- updateProfile ----------

    @Test
    void updateProfile_existingEmployee_updatesProfileFieldsAndSaves() {
        Employee existing = employee();
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        employeeService.updateProfile(EMAIL, profile("New Name", "+380671112233", LocalDate.of(1985, 5, 5)));

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getPhone()).isEqualTo("+380671112233");
        assertThat(existing.getBirthDate()).isEqualTo(LocalDate.of(1985, 5, 5));
        // the profile does not edit the email or the password
        assertThat(existing.getEmail()).isEqualTo(EMAIL);
        assertThat(existing.getPassword()).isEqualTo("hash");
        verify(employeeRepository).save(existing);
    }

    @Test
    void updateProfile_notFound_throwsAndSavesNothing() {
        when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateProfile(EMAIL, profile("New Name", null, null)))
                .isInstanceOf(NotFoundException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }
}