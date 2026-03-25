package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidade que representa um artigo noticioso extraído de um feed RSS parceiro.
 */
@Entity
@Table(name = "articles")
public class Article {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String titulo;

  @Column(nullable = false)
  private String autor;

  @Column(nullable = false)
  private LocalDateTime dataPublicacao;

  @Column(nullable = false, unique = true, length = 1000)
  private String urlOriginal;

  @Lob
  @Column(nullable = false)
  private String conteudoPrincipal;

  @ManyToOne(optional = false)
  @JoinColumn(name = "source_id", nullable = false)
  private RssSource source;

  /**
   * Construtor padrão necessário pelo JPA.
   */
  public Article() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getAutor() {
    return autor;
  }

  public void setAutor(String autor) {
    this.autor = autor;
  }

  public LocalDateTime getDataPublicacao() {
    return dataPublicacao;
  }

  public void setDataPublicacao(LocalDateTime dataPublicacao) {
    this.dataPublicacao = dataPublicacao;
  }

  public String getUrlOriginal() {
    return urlOriginal;
  }

  public void setUrlOriginal(String urlOriginal) {
    this.urlOriginal = urlOriginal;
  }

  public String getConteudoPrincipal() {
    return conteudoPrincipal;
  }

  public void setConteudoPrincipal(String conteudoPrincipal) {
    this.conteudoPrincipal = conteudoPrincipal;
  }

  public RssSource getSource() {
    return source;
  }

  public void setSource(RssSource source) {
    this.source = source;
  }
}