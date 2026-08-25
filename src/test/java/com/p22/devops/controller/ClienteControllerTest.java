package com.p22.devops.controller;

import com.p22.devops.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClienteControllerTest {

    private ClienteController controller;

    @BeforeEach
    void setUp() {
        controller = new ClienteController();
    }

    @Test
    void testCriarClienteComSucesso() {
        Cliente novoCliente = new Cliente(null, "Bruno de Castro", "cri2bolado@gmail.com");
        ResponseEntity<Cliente> response = controller.criar(novoCliente);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Cliente cliente = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(2L, cliente.getId());
        assertEquals("Bruno de Castro", cliente.getNome());
    }

    @Test
    void testCriarClienteComNomeInvalido() {
        Cliente novoCliente = new Cliente(null, "   ", "valido@email.com");
        ResponseEntity<Cliente> response = controller.criar(novoCliente);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testCriarClienteComEmailInvalido() {
        Cliente novoCliente = new Cliente(null, "Carlos", "email-invalido-sem-arroba");
        ResponseEntity<Cliente> response = controller.criar(novoCliente);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testBuscarPorIdEncontrado() {
        ResponseEntity<Cliente> response = controller.buscarPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Cliente cliente = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(1L, cliente.getId());
    }

    @Test
    void testBuscarPorIdNaoEncontrado() {
        ResponseEntity<Cliente> response = controller.buscarPorId(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testAtualizarClienteComSucesso() {
        Cliente clienteAtualizado = new Cliente(null, "Rodrigo Modificado", "rodrigo.teste@teste.com");
        ResponseEntity<Cliente> response = controller.atualizar(1L, clienteAtualizado);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Cliente cliente = java.util.Objects.requireNonNull(response.getBody());
        assertEquals("Rodrigo Modificado", cliente.getNome());
    }

    @Test
    void testAtualizarClienteComEmailInvalido() {
        Cliente clienteAtualizado = new Cliente(null, "Rodrigo", "email-invalido");
        ResponseEntity<Cliente> response = controller.atualizar(1L, clienteAtualizado);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testDeletarClienteEncontrado() {
        ResponseEntity<Void> response = controller.deletar(1L);
        
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.buscarPorId(1L).getStatusCode());
    }

    @Test
    void testDeletarClienteNaoEncontrado() {
        ResponseEntity<Void> response = controller.deletar(99L);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}