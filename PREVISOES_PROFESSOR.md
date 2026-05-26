# Previsões de Alterações — Apresentação Podcastia

Baseado no padrão observado nas apresentações anteriores (Twitter → hello world, likes → +10),
o professor pede alterações **simples, visíveis e imediatas** que demonstram que entendes o código.
Cada alteração abaixo está mapeada para o(s) ficheiro(s) exato(s) e linha(s) a modificar.

---

## 🔴 PRIORIDADE ALTA — Mais prováveis

---

### 1. Quando o utilizador gera um podcast, o título seja sempre "Hello World" (independentemente do tema escrito)

**Equivalente direto ao caso do Twitter.**

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Método `generatePodcast`, linha ~100:
```java
// ANTES
podcast.setTitulo(tema);

// DEPOIS
podcast.setTitulo("Hello World");
```

---

### 2. O botão de favorito (guardar podcast) adiciona sempre 2 favoritos em vez de 1

**Equivalente ao caso dos likes +10.**

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/PodcastFavoriteController.java`

Método `addFavorite` (linha ~175) e `toggleFavorite` (linha ~271), bloco "Adicionar":
```java
// ANTES
PodcastFavorite favorite = new PodcastFavorite(user, podcast);
favoriteRepository.save(favorite);

// DEPOIS (duplicar o save)
PodcastFavorite favorite = new PodcastFavorite(user, podcast);
favoriteRepository.save(favorite);
PodcastFavorite favorite2 = new PodcastFavorite(user, podcast);
favoriteRepository.save(favorite2);
```
> Nota: a constraint `UNIQUE(user_id, podcast_id)` vai impedir isto na prática — o professor pode querer que remova essa constraint ou que adicione um campo `count` ao modelo. Alternativa mais simples: ver ponto 3 abaixo.

---

### 3. O contador de podcasts gerados / duração aumenta X em vez de 1

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Linha ~95, estimativa de duração:
```java
// ANTES
int estimatedDurationMinutes = Math.max(1, wordCount / 150);

// DEPOIS (ex: duração sempre 10 minutos)
int estimatedDurationMinutes = 10;
```

---

### 4. O tema do podcast gerado seja sempre "Inteligência Artificial" independentemente do que o utilizador escreve

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Método `generatePodcast`, linha ~86:
```java
// ANTES
String script = generateScript(tema);
String cleanScript = cleanScript(script);
String filename = generateFilename(user, tema);
...
podcast.setTitulo(tema);

// DEPOIS
String temaFixo = "Inteligência Artificial";
String script = generateScript(temaFixo);
String cleanScript = cleanScript(script);
String filename = generateFilename(user, temaFixo);
...
podcast.setTitulo(temaFixo);
```

---

### 5. O podcast gerado seja sempre público (em vez de privado por defeito)

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Linha ~105:
```java
// ANTES
podcast.setPublico(false);

// DEPOIS
podcast.setPublico(true);
```

---

### 6. O botão "Gerar Podcast" do frontend diz sempre "Hello World" como tema enviado para o servidor

**Ficheiro:** `frontend/src/pages/GeneratePage.jsx`

Método `handleGenerate`, linha ~78:
```jsx
// ANTES
body: JSON.stringify({
  tema: tema.trim(),
  tags: selectedTags,
}),

// DEPOIS
body: JSON.stringify({
  tema: 'Hello World',
  tags: selectedTags,
}),
```

---

## 🟡 PRIORIDADE MÉDIA — Possíveis

---

### 7. O registo de um novo utilizador força sempre o mesmo username ("admin2", "user_teste", etc.)

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/RegistrationApiController.java`

Procura onde o objeto de registo é guardado e adiciona antes do save:
```java
// Exemplo
user.setUsername("HelloWorld");
```

---

### 8. O login com qualquer password aceita (ou rejeita sempre)

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/controller/AuthController.java`

Linha ~135:
```java
// ANTES
if (passwordEncoder.matches(request.password, user.getPassword())) {

// DEPOIS (aceita qualquer password)
if (true) {

// OU (rejeita sempre)
if (false) {
```

---

### 9. A duração do podcast mostrada no frontend seja multiplicada por 10

**Ficheiro:** `frontend/src/pages/TrendingPage.jsx`

Linha ~231, função `formatTime`:
```jsx
// ANTES
<span className="popular-duration">{formatTime((Number(podcast.duracao) || 0) * 60)}</span>

// DEPOIS
<span className="popular-duration">{formatTime((Number(podcast.duracao) || 0) * 60 * 10)}</span>
```

---

### 10. O título dos podcasts na lista seja sempre "Podcast Incrível" em vez do título real

**Ficheiro:** `frontend/src/features/podcasts/components/PodcastCard.jsx`

Linha 2:
```jsx
// ANTES
const title = podcast?.titulo || podcast?.title || 'Podcast'

// DEPOIS
const title = 'Podcast Incrível'
```

---

### 11. A mensagem de boas-vindas na HomePage diz outra coisa

**Ficheiro:** `frontend/src/pages/HomePage.jsx`

Linha ~194:
```jsx
// ANTES
<h2 id="home-title">Bem-vindo à Podcastia!</h2>
<p>Descobre os melhores podcasts baseados nos teus interesses</p>

// DEPOIS (exemplo)
<h2 id="home-title">Hello World!</h2>
<p>Este é o meu projeto.</p>
```

---

### 12. Os podcasts gerados têm sempre a tag "DESPORTO" em vez da escolhida pelo utilizador

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Linha ~104:
```java
// ANTES
podcast.setTags(tags != null && !tags.isEmpty() ? tags : List.of(PodcastTag.GERAL));

// DEPOIS
podcast.setTags(List.of(PodcastTag.DESPORTO));
```

---

## 🟢 PRIORIDADE BAIXA — Menos prováveis mas possíveis

---

### 13. Remover a validação de força de password no registo (aceitar passwords fracas)

**Ficheiro:** `frontend/src/pages/RegisterPage.jsx`

Linha 5:
```jsx
// ANTES
const PASSWORD_COMPLEXITY_REGEX = /^(?=.*[A-Z])(?=.*\d).{8,}$/

// DEPOIS (aceita tudo)
const PASSWORD_COMPLEXITY_REGEX = /.*/
```

---

### 14. O número máximo de caracteres do tema no GeneratePage muda de 200 para outro valor

**Ficheiro:** `frontend/src/pages/GeneratePage.jsx`

Linha ~195:
```jsx
// ANTES
maxLength={200}

// DEPOIS (ex: 5 caracteres — força erros, ou 9999 — sem limite prático)
maxLength={5}
```

---

### 15. A página de Trending mostra sempre os mesmos 3 podcasts (não aleatoriza)

**Ficheiro:** `frontend/src/pages/TrendingPage.jsx`

Linha ~48:
```jsx
// ANTES
const shuffled = [...asArray(data)].sort(() => Math.random() - 0.5)

// DEPOIS (sem shuffle)
const shuffled = [...asArray(data)]
```

---

### 16. O prompt enviado à IA Gemini inclui uma instrução diferente (ex: falar sempre de futebol)

**Ficheiro:** `servidor/src/main/java/com/jep/servidor/service/PodcastGenerationService.java`

Método `generateScript`, linha ~111, alterar o `prompt`:
```java
// Adicionar no início ou fim do prompt:
"- Relaciona sempre o tema com futebol.\n"
```

---

## 📁 Resumo dos ficheiros principais

| Ficheiro | O que controla |
|---|---|
| `servidor/.../service/PodcastGenerationService.java` | Geração do podcast (tema, duração, visibilidade, prompt IA) |
| `servidor/.../controller/PodcastGenerationController.java` | Endpoint `/api/podcasts/generate` |
| `servidor/.../controller/PodcastFavoriteController.java` | Guardar/remover favoritos |
| `servidor/.../controller/AuthController.java` | Login |
| `servidor/.../controller/RegistrationApiController.java` | Registo |
| `frontend/src/pages/GeneratePage.jsx` | Formulário de geração (frontend) |
| `frontend/src/pages/HomePage.jsx` | Página inicial com listas de podcasts |
| `frontend/src/pages/TrendingPage.jsx` | Página de tendências |
| `frontend/src/features/podcasts/components/PodcastCard.jsx` | Card individual de podcast |

---

> **Dica:** As alterações de backend (Java) requerem reiniciar o servidor com `./mvnw spring-boot:run`.
> As alterações de frontend (JSX) são aplicadas automaticamente pelo Vite (hot reload).
