package com.jep.servidor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidade que representa uma fonte parceira de feed RSS.
 * Estas fontes são usadas pelo sistema para consumo automático de novos artigos.
 */
@Entity
@Table(name = "rss_sources")
public class RssSource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String url;

  @Column(nullable = false)
  private boolean ativa = true;

  /**
   * Construtor padrão necessário pelo JPA.
   */
  public RssSource() {
  }

  /**
   * Cria uma nova fonte RSS com o nome e URL especificados.
   * Por padrão, a nova fonte é criada com o estado ativo (ativa = true).
   *
   * @param nome Nome de apresentação da fonte (ex: Observador).
   * @param url Link original do feed em formato XML/RSS.
   */
  public RssSource(String nome, String url) {
    this.nome = nome;
    this.url = url;
    this.ativa = true;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isAtiva() {
    return ativa;
  }

  public void setAtiva(boolean ativa) {
    this.ativa = ativa;
  }
}