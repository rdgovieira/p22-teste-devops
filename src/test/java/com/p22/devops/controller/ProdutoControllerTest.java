package com.p22.devops.controller;

import com.p22.devops.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoControllerTest {

    private ProdutoController controller;

    @BeforeEach
    void setUp() {
        controller = new ProdutoController();
    }

    @Test
    void testCriarProdutoComSucesso() {
        Produto novoProduto = new Produto(null, "Jogo de Dominó Profissional", 120.50);
        ResponseEntity<Produto> response = controller.criar(novoProduto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Produto produto = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(2L, produto.getId());
        assertEquals("Jogo de Dominó Profissional", produto.getNome());
    }

    @Test
    void testListarTodos() {
        ResponseEntity<List<Produto>> response = controller.listarTodos();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<Produto> produtos = java.util.Objects.requireNonNull(response.getBody());
        assertFalse(produtos.isEmpty());
    }

    @Test
    void testBuscarPorIdEncontrado() {
        ResponseEntity<Produto> response = controller.buscarPorId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Produto produto = java.util.Objects.requireNonNull(response.getBody());
        assertEquals(1L, produto.getId());
    }

    @Test
    void testBuscarPorIdNaoEncontrado() {
        ResponseEntity<Produto> response = controller.buscarPorId(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testAtualizarProdutoNaoEncontrado() {
        // Caso de borda: tentando atualizar um produto que não existe
        Produto produtoAtualizado = new Produto(null, "Produto Fantasma", 0.0);
        ResponseEntity<Produto> response = controller.atualizar(999L, produtoAtualizado);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeletarProdutoEncontrado() {
        ResponseEntity<Void> response = controller.deletar(1L);
        
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}