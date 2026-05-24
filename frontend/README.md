# Podcastia — Frontend

Aplicação web React + Vite para a plataforma **Podcastia**, um serviço de podcasts gerados por IA com funcionalidades sociais.

## Stack

- **React 18** + **Vite** (HMR, bundling)
- **TailwindCSS** — estilização
- **React Router v6** — navegação SPA
- **Vitest** — testes unitários e de componente
- **Playwright** — testes E2E

## Pré-requisitos

- Node.js 20+
- Backend Spring Boot a correr em `http://localhost:8080` (ver `../servidor/`)

## Comandos

```bash
# Instalar dependências
npm install

# Servidor de desenvolvimento (http://localhost:5173)
npm run dev

# Testes unitários (modo watch)
npm run test

# Testes unitários (modo CI, sem watch)
npm run test:ci

# Testes E2E com Playwright (requer backend + frontend a correr)
npm run test:e2e

# Testes E2E com interface gráfica (debugging)
npm run test:e2e:ui

# Linting ESLint
npm run lint

# Formatação (Prettier)
npm run format

# Build de produção (output em dist/)
npm run build
```

## Variáveis de Ambiente

Criar um ficheiro `.env.local` na pasta `frontend/` com:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Estrutura Principal

```
src/
├── components/     # Componentes reutilizáveis
├── pages/          # Páginas (uma por rota)
├── hooks/          # Custom hooks (useBackgroundAudio, useOnboardingGuard, ...)
├── services/       # Clientes HTTP e lógica de acesso à API
└── App.jsx         # Roteamento e ProtectedRoute
```
