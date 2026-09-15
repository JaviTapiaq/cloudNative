package com.pedidos360.clientes.service;

import com.pedidos360.clientes.model.Cliente;
import com.pedidos360.clientes.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    public List<Cliente> findAll() {
        return repository.findAll();
    }

    public Optional<Cliente> findById(Long id) {
        return repository.findById(id);
    }

    public Cliente save(Cliente cliente) {
        return repository.save(cliente);
    }

    public Optional<Cliente> update(Long id, Cliente cliente) {
        return repository.findById(id).map(existing -> {
            existing.setNombre(cliente.getNombre());
            existing.setApellido(cliente.getApellido());
            existing.setEmail(cliente.getEmail());
            existing.setTelefono(cliente.getTelefono());
            existing.setDireccion(cliente.getDireccion());
            return repository.save(existing);
        });
    }

    public boolean delete(Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
