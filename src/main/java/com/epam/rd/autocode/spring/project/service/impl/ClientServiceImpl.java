package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    private final ModelMapper mapper;

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

        mapper.map(clientDTO, existingClient);

        Client savedClient = clientRepository.save(existingClient);

        return convertClientToClientDTO(savedClient);
    }

    @Override
    @Transactional
    public void deleteClientByEmail(String email) {
        if(!clientRepository.existsByEmail(email)) {
            throw new NotFoundException("The client with email: " + email + " not found");
        }

        clientRepository.deleteByEmail(email);
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        if (clientRepository.existsByEmail(clientDTO.getEmail())) {
            throw new AlreadyExistException("The client with email: " + clientDTO.getEmail() + " already exists");
        }

        Client savedClient = clientRepository.save( convertClientDTOToClient(clientDTO) );
        return convertClientToClientDTO(savedClient);
    }

    private ClientDTO convertClientToClientDTO(Client client) {
        return mapper.map(client, ClientDTO.class);
    }

    private Client convertClientDTOToClient(ClientDTO clientDTO) {
        return mapper.map(clientDTO, Client.class);
    }
}
