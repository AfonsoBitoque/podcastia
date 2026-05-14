#!/bin/bash

# Script para criar os 20 podcasts iniciais do Podcastia
# Deve ser executado depois de fazer login no site e obter um token JWT válido
#
# USAGE:
# 1. Faz login no site (http://localhost:5173/login)
# 2. Abre DevTools (F12) → Application → Local Storage → copia o valor do 'token'
# 3. Corre: ./seed_initial_podcasts.sh <TOKEN_AQUI>

API_URL="http://localhost:8080/api/podcasts/generate"
TOKEN="$1"

if [ -z "$TOKEN" ]; then
    echo "Erro: Token JWT não fornecido"
    echo "Usage: $0 <TOKEN_JWT>"
    echo ""
    echo "Exemplo:"
    echo "  $0 eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIn0..."
    exit 1
fi

# Função para gerar podcast
generate_podcast() {
    local tema="$1"
    local tag="$2"

    echo "A gerar: $tema [$tag]..."

    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{\"tema\": \"$tema\", \"tags\": [\"$tag\"]}" \
        --max-time 300

    echo ""
}

echo "========================================="
echo "  Podcastia - Seed de Podcasts Iniciais"
echo "========================================="
echo ""
echo "Este script vai criar 20 podcasts iniciais:"
echo "  - 5 podcasts GERAL"
echo "  - 5 podcasts DESPORTO"
echo "  - 5 podcasts POLITICA"
echo "  - 5 podcasts FINANCAS"
echo ""
echo "NOTA: Cada podcast demora ~30-60s a gerar (Gemini + TTS)"
echo "Tempo total estimado: 10-20 minutos"
echo ""
read -p "Pressiona ENTER para continuar..."
echo ""

# GERAL (5)
echo "=== GERANDO PODCASTS GERAIS ==="
generate_podcast "História de Portugal" "GERAL"
generate_podcast "Curiosidades sobre o espaço sideral" "GERAL"
generate_podcast "A importância do sono para a saúde" "GERAL"
generate_podcast "Como aprender novas línguas" "GERAL"
generate_podcast "Tecnologias que vão mudar o futuro" "GERAL"

# DESPORTO (5)
echo ""
echo "=== GERANDO PODCASTS DE DESPORTO ==="
generate_podcast "História do futebol em Portugal" "DESPORTO"
generate_podcast "Treino de alta performance para atletas" "DESPORTO"
generate_podcast "Nutrição e suplementação para desportistas" "DESPORTO"
generate_podcast "Modalidades olímpicas pouco conhecidas" "DESPORTO"
generate_podcast "A mentalidade vencedora no desporto" "DESPORTO"

# POLITICA (5)
echo ""
echo "=== GERANDO PODCASTS DE POLITICA ==="
generate_podcast "Sistema político português explicado" "POLITICA"
generate_podcast "A União Europeia e o seu funcionamento" "POLITICA"
generate_podcast "História das democracias modernas" "POLITICA"
generate_podcast "Movimentos políticos do século XXI" "POLITICA"
generate_podcast "Relações internacionais e geopolítica" "POLITICA"

# FINANCAS (5)
echo ""
echo "=== GERANDO PODCASTS DE FINANCAS ==="
generate_podcast "Noções básicas de investimento para iniciantes" "FINANCAS"
generate_podcast "Gestão de orçamento pessoal e poupança" "FINANCAS"
generate_podcast "Economia portuguesa em contexto europeu" "FINANCAS"
generate_podcast "Criptomoedas e tecnologia blockchain" "FINANCAS"
generate_podcast "Impostos e fiscalidade em Portugal" "FINANCAS"

echo ""
echo "========================================="
echo "  Concluído!"
echo "========================================="
echo ""
echo "NOTA: Para tornar os podcasts públicos, acede ao perfil"
echo "do utilizador que os gerou e altera a visibilidade."
