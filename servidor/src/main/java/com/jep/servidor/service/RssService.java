package com.jep.servidor.service;

import com.jep.servidor.model.Article;
import com.jep.servidor.model.RssSource;
import com.jep.servidor.repository.ArticleRepository;
import com.jep.servidor.repository.RssSourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Serviço responsável por consumir feeds RSS automaticamente e com base num agendamento predefinido.
 */
@Service
public class RssService {

  private static final Logger logger = LoggerFactory.getLogger(RssService.class);

  private final RssSourceRepository rssSourceRepository;
  private final ArticleRepository articleRepository;

  /**
   * Construtor para injeção de dependências.
   *
   * @param rssSourceRepository Repositório de fontes RSS.
   * @param articleRepository Repositório de artigos.
   */
  public RssService(RssSourceRepository rssSourceRepository, ArticleRepository articleRepository) {
    this.rssSourceRepository = rssSourceRepository;
    this.articleRepository = articleRepository;
  }

  /**
   * Processa os feeds RSS a cada 2 horas de forma automática.
   * A expressão cron "0 0 * /2 * * *" significa "no minuto 0 e segundo 0 de todas as horas divisíveis por 2".
   */
  @Scheduled(cron = "0 0 */2 * * *")
  public void consumeRssFeeds() {
    logger.info("Iniciando o consumo automático de feeds RSS...");
    List<RssSource> fontesAtivas = rssSourceRepository.findByAtivaTrue();

    for (RssSource fonte : fontesAtivas) {
      processarFonte(fonte);
    }
    logger.info("Consumo de feeds RSS concluído.");
  }

  /**
   * Lê uma determinada fonte RSS utilizando a biblioteca ROME e itera pelos seus artigos.
   *
   * @param fonte Entidade RssSource contendo os dados da fonte parceira.
   */
  private void processarFonte(RssSource fonte) {
    try {
      URL feedUrl = new URL(fonte.getUrl());
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed feed = input.build(new XmlReader(feedUrl));

      for (SyndEntry entry : feed.getEntries()) {
        processarEntrada(entry, fonte);
      }
    } catch (Exception e) {
      logger.error("Erro ao processar o feed RSS: " + fonte.getUrl(), e);
    }
  }

  /**
   * Analisa um artigo extraído de um RSS, valida duplicados e mapeia o seu conteúdo para a base de dados.
   * Se a data ou o autor não vierem preenchidos no feed, aplica os *fallbacks* respetivos.
   *
   * @param entry Artigo não processado lido pela biblioteca ROME.
   * @param fonte RssSource originária do artigo.
   */
  private void processarEntrada(SyndEntry entry, RssSource fonte) {
    String url = entry.getLink();

    // Regra: verificar se já existe na base de dados (evitar repetições)
    if (url == null || url.isEmpty() || articleRepository.existsByUrlOriginal(url)) {
      return;
    }

    Article article = new Article();
    article.setSource(fonte);
    article.setUrlOriginal(url);

    // Título
    String titulo = entry.getTitle() != null ? entry.getTitle() : "Sem Título";
    article.setTitulo(titulo);

    // Autor - Se não estiver disponível, preenche com 'Desconhecido'
    String autor = entry.getAuthor();
    if (autor == null || autor.trim().isEmpty()) {
      autor = "Desconhecido";
    }
    article.setAutor(autor);

    // Data de Publicação - Se não estiver disponível, usa a data atual do processamento
    Date publishedDate = entry.getPublishedDate();
    LocalDateTime dataPublicacao;
    if (publishedDate != null) {
      dataPublicacao = publishedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    } else {
      dataPublicacao = LocalDateTime.now();
    }
    article.setDataPublicacao(dataPublicacao);

    // Conteúdo Principal (procura a melhor correspondência dentro das tags XML)
    String conteudo = "";
    if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
      conteudo = entry.getDescription().getValue();
    } else if (!entry.getContents().isEmpty() && entry.getContents().get(0).getValue() != null) {
      conteudo = entry.getContents().get(0).getValue();
    }
    
    if (conteudo == null) {
        conteudo = "";
    }
    
    article.setConteudoPrincipal(conteudo);

    try {
      articleRepository.save(article);
      logger.debug("Artigo guardado: {}", titulo);
    } catch (Exception e) {
      logger.error("Erro ao guardar o artigo: " + url, e);
    }
  }
}