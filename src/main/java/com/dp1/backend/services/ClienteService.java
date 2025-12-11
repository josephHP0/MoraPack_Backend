package com.dp1.backend.services;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dp1.backend.models.Cliente;
import com.dp1.backend.repository.ClienteRepository;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    private static final Logger logger = LogManager.getLogger(ClienteService.class);

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }
    
    public Cliente createCliente(Cliente cliente)
    {
        try {
            Cliente existingCliente = clienteRepository.findByEmail(cliente.getEmail());
            if (existingCliente != null) {
                // If a client with the same email already exists, return that client
                logger.info("Cliente ya existe con email: " + cliente.getEmail() + " - Devolviendo cliente existente con ID: " + existingCliente.getId());
                return existingCliente;
            }
            logger.info("Creando cliente con datos: " + cliente.getNombre() + " " + cliente.getApellido() + " " + cliente.getEmail() + " " );
            Cliente savedCliente = clienteRepository.save(cliente);
            logger.info("Cliente creado exitosamente con ID: " + savedCliente.getId());
            return savedCliente;
        } catch (Exception e) {
            logger.error("Error al crear cliente: " + e.getMessage(), e);
            throw new RuntimeException("Error al crear cliente: " + e.getMessage(), e);
        }
    }

    public Cliente getCliente(int id)
    {
        try {
            return clienteRepository.findById(id).get();
        } catch (Exception e) {
            return null;
        }
    }

    public Cliente updateCliente(Cliente cliente){
        try {
            if (cliente == null)
            {
                return null;
            }
            return clienteRepository.save(cliente);
        } catch (Exception e) {
            return null;
        }
    }
    public String deleteCliente(int id){
        try {
            Cliente cliente = clienteRepository.findById(id).get();
            if (cliente != null) {
                clienteRepository.delete(cliente);
            }
            else {
                return "Cliente no encontrado";
            }
            return "Cliente eliminado";
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }

    public ArrayList<Cliente> getClientes()
    {
        try {
            return (ArrayList<Cliente>) clienteRepository.findAll();
        } catch (Exception e) {
            return null;
        }
    }
}

