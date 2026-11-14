package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.EmployeeDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::convertEmployeeToEmployeeDTO)
                .toList();
    }

    @Override
    public EmployeeDTO getEmployeeByEmail(String email) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The employee with email: " + email + " not found"));

        return convertEmployeeToEmployeeDTO(employee);
    }

    @Override
    @Transactional
    public EmployeeDTO updateEmployeeByEmail(String email, EmployeeDTO employeeDTO) {
        Employee existingEmployee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The employee with email: " + email + " not found"));

        mapper.map(employeeDTO, existingEmployee);
        existingEmployee.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));

        Employee savedEmployee = employeeRepository.save(existingEmployee);

        return convertEmployeeToEmployeeDTO(savedEmployee);
    }

    @Override
    @Transactional
    public void deleteEmployeeByEmail(String email) {
        if(!employeeRepository.existsByEmail(email)) {
            throw new NotFoundException("The employee with email: " + email + " not found");
        }

        employeeRepository.deleteByEmail(email);
    }

    @Override
    @Transactional
    public EmployeeDTO addEmployee(EmployeeDTO employeeDTO) {
        if (employeeRepository.existsByEmail(employeeDTO.getEmail())
            || clientRepository.existsByEmail(employeeDTO.getEmail())) {
            throw new AlreadyExistException("The user with email: " + employeeDTO.getEmail() + " already exists");
        }

        Employee employeeEntity = convertEmployeeDTOToEmployee(employeeDTO);
        employeeEntity.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));

        Employee savedEmployee = employeeRepository.save(employeeEntity);
        return convertEmployeeToEmployeeDTO(savedEmployee);
    }

    private EmployeeDTO convertEmployeeToEmployeeDTO(Employee employee) {
        return mapper.map(employee, EmployeeDTO.class);
    }

    private Employee convertEmployeeDTOToEmployee(EmployeeDTO employeeDTO) {
        return mapper.map(employeeDTO, Employee.class);
    }
}
