package com.p22.devops.controller;

import com.p22.devops.model.Cliente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "API de gerenciamento de clientes")
public class ClienteController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final List<Cliente> clientes = new CopyOnWriteArrayList<>();
    private final AtomicLong nextId = new AtomicLong(2L);

    public ClienteController() {
        // Massa de dados inicial para testes
        clientes.add(new Cliente(1L, "Rodrigo Vieira Rodrigues", "rdgovieira@gmail.com"));
    }

    @PostMapping
    @Operation(summary = "Cria um novo cliente", description = "Adiciona um novo cliente ao sistema com validação")
    public ResponseEntity<Cliente> criar(@Valid @RequestBody Cliente cliente) {
        if (!isClienteValido(cliente)) {
            return ResponseEntity.badRequest().build();
        }
        cliente.setId(nextId.getAndIncrement());
        clientes.add(cliente);
        return ResponseEntity.ok(cliente);
    }

    @GetMapping
    @Operation(summary = "Lista todos os clientes", description = "Retorna uma lista com todos os clientes cadastrados")
    public ResponseEntity<List<Cliente>> listarTodos() {
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca cliente por ID", description = "Retorna um cliente específico através de seu ID")
    public ResponseEntity<Cliente> buscarPorId(@Parameter(description = "ID do cliente") @PathVariable Long id) {
        Optional<Cliente> cliente = clientes.stream().filter(c -> c.getId().equals(id)).findFirst();
        return cliente.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente", description = "Atualiza os dados de um cliente existente com validação")
    public ResponseEntity<Cliente> atualizar(@Parameter(description = "ID do cliente") @PathVariable Long id, @Valid @RequestBody Cliente clienteAtualizado) {
        if (!isClienteValido(clienteAtualizado)) {
            return ResponseEntity.badRequest().build();
        }
        for (Cliente cliente : clientes) {
            if (cliente.getId().equals(id)) {
                cliente.setNome(clienteAtualizado.getNome());
                cliente.setEmail(clienteAtualizado.getEmail());
                return ResponseEntity.ok(cliente);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um cliente", description = "Remove um cliente através de seu ID")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do cliente") @PathVariable Long id) {
        boolean removido = clientes.removeIf(c -> c.getId().equals(id));
        if (removido) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isClienteValido(Cliente cliente) {
        if (cliente == null) return false;
        if (cliente.getNome() == null || cliente.getNome().trim().isEmpty()) return false;
        if (cliente.getEmail() == null || cliente.getEmail().trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(cliente.getEmail()).matches();
    }
}