package com.jep.servidor.config;

import com.jep.servidor.service.DailyPlaylistService;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador para regenerar as playlists diárias de todos os utilizadores.
 */
@Component
@EnableScheduling
public class DailyPlaylistScheduler {

  @Autowired
  private DailyPlaylistService dailyPlaylistService;

  /**
   * Executa a regeneração das playlists diárias todos os dias às 00:00 (meia-noite).
   * A expressão cron é: "0 0 0 * * *" que significa:
   * - 0 segundos
   * - 0 minutos
   * - 0 horas (meia-noite)
   * - * qualquer dia do mês
   * - * qualquer mês
   * - * qualquer dia da semana
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
