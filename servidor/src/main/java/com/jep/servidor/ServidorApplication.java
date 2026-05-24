package com.jep.servidor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal da aplicação Podcastia — ponto de entrada do servidor Spring Boot.
 *
 * <p>Esta classe bootstrap o contexto da aplicação através da anotação
 * {@link SpringBootApplication}, que combina:
 * <ul>
 *   <li>{@code @Configuration} — marca a classe como fonte de definições de beans.</li>
 *   <li>{@code @EnableAutoConfiguration} — ativa a configuração automática do Spring Boot
 *       com base nas dependências presentes no classpath.</li>
 *   <li>{@code @ComponentScan} — ativa a deteção automática de componentes
 *       ({@code @Service}, {@code @Repository}, {@code @Controller}, etc.)
 *       no pacote {@code com.jep.servidor} e seus subpacotes.</li>
 * </ul>
 *
 * <p><b>Funcionalidades adicionais ativadas:</b>
 * <ul>
 *   <li>{@link EnableScheduling} — habilita a execução de tarefas agendadas anotadas com
 *       {@code @Scheduled}, usadas nomeadamente em:
 *       <ul>
 *         <li>{@code RssService} — consumo automático de feeds RSS a cada 2 horas.</li>
 *         <li>{@code DailyPlaylistScheduler} — geração diária de playlists personalizadas.</li>
 *         <li>{@code ChatMessageServiceImpl} — processamento da fila de notificações push.</li>
 *       </ul>
 *   </li>
 *   <li>{@link EnableAsync} — habilita a execução assíncrona de métodos anotados com
 *       {@code @Async}, usada em {@code NotificationServiceImpl} para enviar notificações
 *       sem bloquear a thread principal.</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ServidorApplication {

  /**
   * Método principal que inicia a aplicação Spring Boot.
   *
   * <p>Delega para {@link SpringApplication#run(Class, String[])} que:
   * <ol>
   *   <li>Cria e configura o contexto da aplicação ({@code ApplicationContext}).</li>
   *   <li>Regista todos os beans detetados por component scan.</li>
   *   <li>Inicia o servidor embebido (Tomcat por omissão).</li>
   *   <li>Executa todos os {@code CommandLineRunner} e {@code ApplicationRunner} registados,
   *       incluindo o {@code DataSeeder} para popular dados iniciais.</li>
   * </ol>
   *
   * @param args Argumentos passados na linha de comando (ex: {@code --server.port=8081}).
   *             São repassados ao contexto Spring e podem sobrepor propriedades do
   *             {@code application.properties}.
   */
  public static void main(String[] args) {
    SpringApplication.run(ServidorApplication.class, args);
  }

}
