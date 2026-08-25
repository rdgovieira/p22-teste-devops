package com.p22.devops.controller;

import com.p22.devops.model.Produto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "API de gerenciamento de produtos")
public class ProdutoController {
    private List<Produto> produtos = new ArrayList<>();
    private long nextId = 2L;

    @Value("${app.feature-flag:false}")
    private boolean featureFlag;

    @Value("${APP_MESSAGE:Bem-vindo a API!}")
    private String appMessage;

    public ProdutoController() {
        // Massa de dados inicial para testes
        produtos.add(new Produto(1L, "Renault Duster Dynamique 2016", 65000.00));
    }

    @GetMapping("/config")
    @Operation(summary = "Lê variáveis de ambiente", description = "Retorna variáveis de configuração injetadas via ambiente (requisito do teste)")
    public ResponseEntity<String> getConfig() {
        return ResponseEntity.ok("Feature Flag: " + featureFlag + " | Message: " + appMessage);
    }

    @PostMapping
    @Operation(summary = "Cria um novo produto", description = "Adiciona um novo produto ao sistema")
    public ResponseEntity<Produto> criar(@RequestBody Produto produto) {
        if (produto.getNome() == null || produto.getNome().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (produto.getPreco() == null || produto.getPreco() < 0) {
            return ResponseEntity.badRequest().build();
        }
        produto.setId(nextId++);
        produtos.add(produto);
        return ResponseEntity.ok(produto);
    }

    @GetMapping
    @Operation(summary = "Lista todos os produtos", description = "Retorna uma lista com todos os produtos cadastrados")
    public ResponseEntity<List<Produto>> listarTodos() {
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca produto por ID", description = "Retorna um produto específico através de seu ID")
    public ResponseEntity<Produto> buscarPorId(@Parameter(description = "ID do produto") @PathVariable Long id) {
        Optional<Produto> produto = produtos.stream().filter(p -> p.getId().equals(id)).findFirst();
        return produto.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um produto", description = "Atualiza os dados de um produto existente")
    public ResponseEntity<Produto> atualizar(@Parameter(description = "ID do produto") @PathVariable Long id, @RequestBody Produto produtoAtualizado) {
        for (Produto produto : produtos) {
            if (produto.getId().equals(id)) {
                if (produtoAtualizado.getNome() == null || produtoAtualizado.getNome().trim().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                if (produtoAtualizado.getPreco() == null || produtoAtualizado.getPreco() < 0) {
                    return ResponseEntity.badRequest().build();
                }
                produto.setNome(produtoAtualizado.getNome());
                produto.setPreco(produtoAtualizado.getPreco());
                return ResponseEntity.ok(produto);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um produto", description = "Remove um produto através de seu ID")
    public ResponseEntity<Void> deletar(@Parameter(description = "ID do produto") @PathVariable Long id) {
        boolean removido = produtos.removeIf(p -> p.getId().equals(id));
        if (removido) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}