package br.gov.sp.cps.fatec.pdv.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProdutoControllerValidationTest {

    @Test
    @DisplayName("Deve aceitar preço decimal com ponto ou vírgula")
    void deveInterpretarSeparadoresDecimais() {
        assertEquals(new BigDecimal("19.99"), ProdutoController.parsePreco("19.99"));
        assertEquals(new BigDecimal("19.99"), ProdutoController.parsePreco(" 19,99 "));
    }

    @Test
    @DisplayName("Deve rejeitar preço vazio, agrupado ou com mais de duas casas")
    void deveRejeitarPrecoMalFormatado() {
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parsePreco(""));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parsePreco("1.234,56"));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parsePreco("19,999"));
    }

    @Test
    @DisplayName("Deve validar estoque como inteiro não negativo dentro do limite Java")
    void deveValidarEstoque() {
        assertEquals(0, ProdutoController.parseQuantidadeEstoque("0"));
        assertEquals(Integer.MAX_VALUE, ProdutoController.parseQuantidadeEstoque("2147483647"));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parseQuantidadeEstoque(""));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parseQuantidadeEstoque("-1"));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parseQuantidadeEstoque("2147483648"));
        assertThrows(IllegalArgumentException.class, () -> ProdutoController.parseQuantidadeEstoque("1.5"));
    }
}
