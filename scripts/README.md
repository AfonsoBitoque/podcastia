# Scripts de Manutenção do Podcastia

## seed_initial_podcasts.sh

Script para criar os 20 podcasts iniciais que vão aparecer na homepage dos novos utilizadores antes de terem preferências definidas.

### O que faz?

Cria 20 podcasts distribuídos por 4 categorias:
- **5 podcasts GERAL**: História, curiosidades, saúde, línguas, tecnologia
- **5 podcasts DESPORTO**: Futebol, treino, nutrição, modalidades olímpicas, mentalidade
- **5 podcasts POLITICA**: Sistema português, UE, democracias, movimentos políticos, geopolítica
- **5 podcasts FINANCAS**: Investimento, orçamento, economia, criptomoedas, impostos

### Pré-requisitos

1. Backend a correr (`./mvnw spring-boot:run`)
2. Frontend a correr (`npm run dev`)
3. Um utilizador registado e autenticado
4. Token JWT válido

### Como usar

1. **Faz login no site**
   - Abre http://localhost:5173/login
   - Entra com uma conta existente ou cria uma nova

2. **Obtém o token JWT**
   - Abre DevTools (F12)
   - Vai a Application → Local Storage → http://localhost:5173
   - Copia o valor da chave `token`

3. **Corre o script**
   ```bash
   cd /home/predm/Git/University/LES/try5/podcastia/scripts
   chmod +x seed_initial_podcasts.sh
   ./seed_initial_podcasts.sh eyJhbGciOiJIUzUxMiJ9...
   ```

   Substitui `eyJhbGciOiJIUzUxMiJ9...` pelo token que copiaste.

4. **Aguarda a conclusão**
   - Cada podcast demora ~30-60s a gerar (usa Gemini API + TTS)
   - Tempo total: 10-20 minutos

5. **Torna os podcasts públicos**
   - Vai ao perfil do utilizador que gerou os podcasts
   - Clica em "Os meus podcasts"
   - Altera a visibilidade de cada um para "Público"

### Notas importantes

- Os podcasts ficam guardados localmente em `servidor/generated-podcasts/`
- Cada podcast tem ~3-5 minutos de duração
- Os ficheiros MP3 são gerados usando `edge-tts` com voz portuguesa (pt-PT-RaquelNeural)
- Se a API do Gemini falhar, o script continua para o próximo podcast
