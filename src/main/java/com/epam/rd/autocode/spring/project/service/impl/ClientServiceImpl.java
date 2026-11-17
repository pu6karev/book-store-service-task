package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.dto.ProfileDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.ClientInUseException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::convertClientToClientDTO)
                .toList();
    }

    @Override
    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The client with email: " + email + " not found"));

        return convertClientToClientDTO(client);
    }

    @Override
    @Transactional
    public ClientDTO updateClientByEmail(String email, ClientDTO clientDTO) {
        Client existingClient = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The client with email: " + email + " not found"));

        // the mapper would overwrite the flag with the DTO's default (false) and silently unblock the client
        boolean blocked = existingClient.isBlocked();
        mapper.map(clientDTO, existingClient);
        existingClient.setBlocked(blocked);
        existingClient.setPassword(passwordEncoder.encode(clientDTO.getPassword()));

        Client savedClient = clientRepository.save(existingClient);
        log.debug("Client updated: {}", email);

        return convertClientToClientDTO(savedClient);
    }

    @Override
    @Transactional
    public void deleteClientByEmail(String email) {
        if(!clientRepository.existsByEmail(email)) {
            throw new NotFoundException("The client with email: " + email + " not found");
        }
        // orders.client_id is NOT NULL: a client with orders cannot be deleted (the order history is kept)
        if (orderRepository.existsByClientEmail(email)) {
            log.warn("Client {} cannot be deleted: has orders", email);
            throw new ClientInUseException("The client with email: " + email + " has orders and cannot be deleted");
        }

        clientRepository.deleteByEmail(email);
        log.info("Client deleted: {}", email);
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        if (clientRepository.existsByEmail(clientDTO.getEmail())
                || employeeRepository.existsByEmail(clientDTO.getEmail())) {
            throw new AlreadyExistException("The user with email: " + clientDTO.getEmail() + " already exists");
        }

        Client clientEntity = convertClientDTOToClient(clientDTO);
        clientEntity.setPassword(passwordEncoder.encode(clientDTO.getPassword()));

        Client savedClient = clientRepository.save(clientEntity);
        log.info("Client registered: {}", clientDTO.getEmail());
        return convertClientToClientDTO(savedClient);
    }

    @Override
    @Transactional
    public void updateProfile(String email, ProfileDTO profile) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The client with email: " + email + " not found"));
        client.setName(profile.getName());
        clientRepository.save(client);
        log.debug("Client profile updated: {}", email);
    }

    @Override
    @Transactional
    public void setBlocked(String email, boolean blocked) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("The client with email: " + email + " not found"));
        client.setBlocked(blocked);
        clientRepository.save(client);
        log.info("Client {}: {}", email, blocked ? "blocked" : "unblocked");
    }

    private ClientDTO convertClientToClientDTO(Client client) {
        return mapper.map(client, ClientDTO.class);
    }

    private Client convertClientDTOToClient(ClientDTO clientDTO) {
        return mapper.map(clientDTO, Client.class);
    }
}