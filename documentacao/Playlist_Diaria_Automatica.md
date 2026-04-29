# 🎵 Playlist Diária Automática - Documentação

## Visão Geral

A funcionalidade de **Playlist Diária** gera automaticamente uma playlist personalizada para cada utilizador baseada nas suas preferências (pontos). A playlist é atualizada diariamente com novos podcasts selecionados de acordo com os interesses do utilizador.

## Como Funciona

### 1. **Preferências do Utilizador**
O sistema utiliza os pontos do utilizador em cada categoria para determinar suas preferências:
- **DESPORTO** - Número de pontos em desporto
- **POLÍTICA** - Número de pontos em política
- **FINANÇAS** - Número de pontos em finanças
- **GERAL** - Número de pontos em tópicos gerais

### 2. **Geração da Playlist**
O serviço `DailyPlaylistService` executa os seguintes passos:
1. Calcula as preferências do utilizador baseado nos pontos
2. Busca todos os podcasts públicos e disponíveis
3. Ordena os podcasts por relevância em relação às preferências
4. Seleciona os melhores podcasts (máximo 20, com duração mínima de 30 minutos)
5. Cria a playlist com os itens ordenados por relevância

### 3. **Atualização Automática**
O agendador `DailyPlaylistScheduler` regenera todas as playlists diárias automaticamente:
- **Frequência**: Diariamente às 00:00 (meia-noite)
- **Fuso Horário**: Europe/Lisbon
- **Escopo**: Todos os utilizadores com status ACTIVE

## Endpoints da API

### 1. Obter Playlist de Hoje
```
GET /api/daily-playlists/today/{userId}
```
**Descrição**: Retorna a playlist diária do utilizador para hoje.

**Exemplo de Resposta (200 OK)**:
```json
{
  "id": 1,
  "playlistDate": "2024-01-15",
  "title": "Playlist Diária - 2024-01-15",
  "description": "Playlist diária com 12 podcasts selecionados com base nas tuas preferências de desporto. Desfruta do conteúdo curado especialmente para ti!",
  "totalDuration": 7200,
  "totalPodcasts": 12,
  "items": [
    {
      "id": 1,
      "podcastId": 5,
      "podcastTitle": "Podcast sobre Futebol",
      "position": 1,
      "podcastDuration": 600,
      "relevanceScore": 95.5
    },
    {
      "id": 2,
      "podcastId": 8,
      "podcastTitle": "Análise Desportiva",
      "position": 2,
      "podcastDuration": 720,
      "relevanceScore": 89.3
    }
  ],
  "createdAt": "2024-01-15T00:00:00",
  "updatedAt": "2024-01-15T00:00:00"
}
```

### 2. Obter Última Playlist
```
GET /api/daily-playlists/latest/{userId}
```
**Descrição**: Retorna a última playlist diária gerada para o utilizador.

**Exemplo de Resposta (200 OK)**: Mesma estrutura da resposta anterior.

### 3. Gerar Playlist (Manual)
```
POST /api/daily-playlists/generate/{userId}
```
**Descrição**: Força a geração/atualização manual da playlist diária para um utilizador.

**Exemplo de Resposta (200 OK)**: Mesma estrutura da resposta anterior.

## Fluxo de Dados

```
┌─────────────────────────────────────────────────────────┐
│           Utilizador com Preferências                   │
│  (Pontos: Desporto=100, Política=50, Finanças=75)       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│     DailyPlaylistService.generateOrUpdateDailyPlaylist  │
│                                                         │
│  1. Calcular preferências (pesos)                      │
│  2. Buscar podcasts públicos e disponíveis             │
│  3. Ordenar por relevância                            │
│  4. Selecionar melhores (máx 20)                       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│        Playlist Diária Criada/Atualizada                │
│                                                         │
│  - 12 podcasts selecionados                            │
│  - Duração total: 2 horas                              │
│  - Ordenados por relevância                            │
└─────────────────────────────────────────────────────────┘
```

## Tabelas de Base de Dados

### daily_playlists
```
┌─────────────────────────┐
│   daily_playlists       │
├─────────────────────────┤
│ id (PK)                 │
│ user_id (FK)            │
│ playlist_date           │
│ title                   │
│ description             │
│ total_duration          │
│ total_podcasts          │
│ created_at              │
│ updated_at              │
└─────────────────────────┘
Constraint: Único (user_id, playlist_date)
```

### daily_playlist_items
```
┌──────────────────────────┐
│  daily_playlist_items    │
├──────────────────────────┤
│ id (PK)                  │
│ daily_playlist_id (FK)   │
│ podcast_id (FK)          │
│ position                 │
│ relevance_score          │
└──────────────────────────┘
Constraint: Único (daily_playlist_id, podcast_id)
```

## Configuração

### Habilitar Agendador
O agendador é ativado automaticamente. Para desativar, remova a anotação `@EnableScheduling` em `DailyPlaylistScheduler.java`.

### Modificar Frequência de Regeneração
Edite o cron em `DailyPlaylistScheduler.java`:
```java
@Scheduled(cron = "0 0 0 * * *", zone = "Europe/Lisbon")
// Formato: segundo minuto hora dia_mês mês dia_semana
```

**Exemplos**:
- `0 0 0 * * *` - Todos os dias às 00:00
- `0 0 6 * * *` - Todos os dias às 06:00
- `0 0 */6 * * *` - A cada 6 horas
- `0 0 0 ? * MON` - Todas as segundas-feiras às 00:00

### Limitar Podcasts na Playlist
Edite a constante em `DailyPlaylistService.java`:
```java
private static final int MAX_PODCASTS_PER_PLAYLIST = 20; // Máximo
private static final int MIN_PLAYLIST_DURATION = 1800; // Mínimo em segundos (30 min)
```

## Exemplos de Uso

### Exemplo 1: Obter Playlist de Hoje
```bash
curl http://localhost:8080/api/daily-playlists/today/1
```

### Exemplo 2: Gerar Playlist Manual
```bash
curl -X POST http://localhost:8080/api/daily-playlists/generate/1
```

### Exemplo 3: Obter Última Playlist (JavaScript)
```javascript
fetch('/api/daily-playlists/latest/1')
  .then(response => response.json())
  .then(playlist => console.log(playlist))
  .catch(error => console.error('Erro:', error));
```

## Logs

O sistema gera logs quando a regeneração de playlists é executada:
```
Iniciando regeneração de playlists diárias em: 2024-01-15T00:00:00.123
Regeneração de playlists diárias concluída em: 2024-01-15T00:00:15.456
```

Erros durante a regeneração são capturados e registados:
```
Erro ao gerar playlist diária para utilizador 5: ...
```

## Notas Importantes

1. **Podcasts Públicos**: Apenas podcasts marcados como `publico = true` são incluídos nas playlists diárias.
2. **Disponibilidade**: Apenas podcasts com `available = true` são considerados.
3. **Utilizadores Ativos**: O agendador só regenera playlists para utilizadores com status `ACTIVE`.
4. **Preferências Variáveis**: A playlist é regenerada diariamente, refletindo mudanças nas preferências do utilizador.
5. **Unicidade**: Cada utilizador tem apenas uma playlist por dia (usa-se atualização se a playlist do dia já existe).

## Futuras Melhorias

- [ ] Incluir podcasts de utilizadores seguidos
- [ ] Adicionar recomendações baseadas em histórico de reprodução
- [ ] Permitir personalização do número máximo de podcasts
- [ ] Notificações quando a nova playlist é gerada
- [ ] Opção de "gerar nova" para utilizador pedir regeneração manual
- [ ] Estatísticas de podcasts adicionados às playlists
