package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.ClientDTO;
import com.epam.rd.autocode.spring.project.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{email}")
    public ResponseEntity<ClientDTO> getClientByEmail(@PathVariable String email) {
        return ResponseEntity.ok(clientService.getClientByEmail(email));
    }

    @PostMapping
    public ResponseEntity<ClientDTO> addClient(@Valid @RequestBody ClientDTO clientDTO) {
        ClientDTO createdClient = clientService.addClient(clientDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @PutMapping("/{email}")
    public ResponseEntity<ClientDTO> updateClientByEmail(@PathVariable String email, @Valid @RequestBody ClientDTO clientDTO) {
        return ResponseEntity.ok(clientService.updateClientByEmail(email, clientDTO));
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Void> deleteClientByEmail(@PathVariable String email) {
        clientService.deleteClientByEmail(email);
        return ResponseEntity.noContent().build();
    }
}
