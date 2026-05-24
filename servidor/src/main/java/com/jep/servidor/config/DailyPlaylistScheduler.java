package com.jep.servidor.config;

import com.jep.servidor.service.DailyPlaylistService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Componente agendador responsável pela regeneração automática das playlists diárias
 * de todos os utilizadores registados na plataforma Podcastia.
 *
 * <p>Funciona em conjunto com o {@link DailyPlaylistService}, delegando para o método
 * {@link DailyPlaylistService#regenerateAllDailyPlaylists()} que percorre todos os
 * utilizadores ativos e gera (ou atualiza) as respetivas playlists personalizadas
 * para o dia atual.
 *
 * <p><b>Agendamento:</b> A tarefa principal está configurada com cron {@code "0 0 0 * * *"}
 * no fuso horário {@code Europe/Lisbon}, executando diariamente à meia-noite (00:00:00).
 * Isto garante que, no início de cada novo dia, todos os utilizadores têm uma playlist
 * fresca disponível com podcasts relevantes para as suas preferências.
 *
 * <p><b>Nota de redundância:</b> A anotação {@code @EnableScheduling} presente nesta
 * classe é redundante — o agendamento já foi globalmente ativado em
 * {@link com.jep.servidor.ServidorApplication} via {@code @EnableScheduling}. A sua
 * presença aqui não causa problemas mas é desnecessária.
 *
 * @see DailyPlaylistService
 * @see com.jep.servidor.model.DailyPlaylist
 */
@Component
@EnableScheduling
public class DailyPlaylistScheduler {

  /**
   * Serviço de playlists diárias injetado pelo Spring.
   * Responsável pela lógica de geração e atualização das playlists.
   */
  @Autowired
  private DailyPlaylistService dailyPlaylistService;

  /**
   * Tarefa agendada que regenera as playlists diárias de todos os utilizadores
   * à meia-noite, hora de Lisboa.
   *
   * <p>Expressão cron {@code "0 0 0 * * *"} decomposta:
   * <ul>
   *   <li>{@code 0} — segundo 0</li>
   *   <li>{@code 0} — minuto 0</li>
   *   <li>{@code 0} — hora 0 (meia-noite)</li>
   *   <li>{@code *} — qualquer dia do mês</li>
   *   <li>{@code *} — qualquer mês</li>
   *   <li>{@code *} — qualquer dia da semana</li>
   * </ul>
   *
   * <p>O fuso horário {@code Europe/Lisbon} garante que a execução ocorre à
   * meia-noite local (WET/WEST), independentemente do fuso do servidor.
   *
   * <p>Delega para {@link DailyPlaylistService#regenerateAllDailyPlaylists()}, que:
   * <ol>
   *   <li>Obtém todos os utilizadores registados.</li>
   *   <li>Para cada utilizador, gera ou atualiza a playlist diária com base
   *       nos seus pontos de preferência por categoria.</li>
   * </ol>
   *
   * <p>O progresso é registado no stdout com timestamps de início e fim,
   * útil para monitorizar a duração do processo em produção.
   */
  @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Lisbon")
  public void regenerateDailyPlaylistsAtMidnight() {
    System.out.println("Iniciando regeneração de playlists diárias em: " + LocalDateTime.now());
    dailyPlaylistService.regenerateAllDailyPlaylists();
    System.out.println("Regeneração de playlists diárias concluída em: " + LocalDateTime.now());
  }

  /**
   * Alternativa: Executa a regeneração a cada 6 horas para testes/desenvolvimento.
   * Comentado por padrão - descomente se precisar de testes frequentes.
   */
  // @Scheduled(fixedDelay = 21600000) // 6 horas em millisegundos
  // public void regenerateDailyPlaylistsEvery6Hours() {
  //   System.out.println("Iniciando regeneração de playlists diárias em: " +
  // LocalDateTime.now());
  //   dailyPlaylistService.regenerateAllDailyPlaylists();
  //   System.out.println("Regeneração de playlists diárias concluída em: " +
  // LocalDateTime.now());
  // }
}
