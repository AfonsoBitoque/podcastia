package com.jep.servidor;

import com.jep.servidor.model.Article;
import com.jep.servidor.model.RssSource;
import com.jep.servidor.repository.ArticleRepository;
import com.jep.servidor.repository.RssSourceRepository;
import com.jep.servidor.service.RssService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test") // Garante que usa application-test.properties (ou application.properties de src/test)
@Transactional
class RssServiceTest {

    @Autowired
    private RssService rssService;

    @Autowired
    private RssSourceRepository rssSourceRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void setUp() {
        // Limpa os dados antes de cada teste
        articleRepository.deleteAll();
        rssSourceRepository.deleteAll();
    }

    @Test
    void shouldConsumeRssFeedAndSaveArticles() {
        // Arrange
        // Usamos um feed pequeno e confiável para testes.
        RssSource source = new RssSource("Observador", "https://observador.pt/feed/");
        rssSourceRepository.save(source);

        // Act
        rssService.consumeRssFeeds();

        // Assert
        List<Article> articles = articleRepository.findAll();
        
        // Verifica se artigos foram efetivamente guardados
        assertFalse(articles.isEmpty(), "Articles should have been downloaded and saved.");
        
        Article firstArticle = articles.get(0);
        assertNotNull(firstArticle.getTitulo(), "Title should not be null");
        assertNotNull(firstArticle.getUrlOriginal(), "URL should not be null");
        assertNotNull(firstArticle.getAutor(), "Author should not be null");
        assertNotNull(firstArticle.getDataPublicacao(), "Published date should not be null");
        
        // Critério de Aceitação: Verificar se a fonte foi devidamente associada
        assertNotNull(firstArticle.getSource(), "Source should be linked");
        assertTrue(firstArticle.getSource().getNome().equals("Observador"));
    }
}
