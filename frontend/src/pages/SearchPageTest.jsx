import React, { useState, useEffect, useRef, useCallback } from 'react';
import '../styles/search-page.css';

const SearchPageTest = () => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [typingTimeout, setTypingTimeout] = useState(null);

    const observer = useRef();
    const size = 5;

    // Função de fetch
    const fetchResults = async (searchQuery, pageNum, reset = false) => {
        if (!searchQuery.trim()) {
            setResults([]);
            setHasMore(false);
            return;
        }

        setLoading(true);
        try {
            // Nota: Configura a porta correta se o servidor Java não estiver na 8080.
            const response = await fetch(`http://localhost:8080/api/search?q=${encodeURIComponent(searchQuery)}&page=${pageNum}&size=${size}`);
            const data = await response.json();

            setResults(prev => reset ? data : [...prev, ...data]);
            
            // Se vieram menos que 5 resultados, significa que não há mais. Total max 15-20 era o critério.
            setHasMore(data.length === size);
        } catch (error) {
            console.error("Erro na pesquisa:", error);
            if (reset) setResults([]);
        } finally {
            setLoading(false);
        }
    };

    // Lidar com o input (Debounce)
    const handleInputChange = (e) => {
        const value = e.target.value;
        setQuery(value);
        setPage(0);
        setHasMore(true);

        // Limpa o timeout anterior se o utilizador continuar a digitar (Debounce associado à digitação dinâmica)
        if (typingTimeout) {
            clearTimeout(typingTimeout);
        }

        // Aguarda 400ms após o utilizador deixar de digitar antes de disparar a pesquisa
        setTypingTimeout(
            setTimeout(() => {
                fetchResults(value, 0, true);
            }, 400)
        );
    };

    // Lidar com Scroll infinito (Intersect Observer na ultima div)
    const lastElementRef = useCallback(node => {
        if (loading) return;
        if (observer.current) observer.current.disconnect();

        observer.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                // Ao bater no fim da bottom, carrega a próxima página
                const nextPage = page + 1;
                setPage(nextPage);
                fetchResults(query, nextPage, false);
            }
        });

        if (node) observer.current.observe(node);
    }, [loading, hasMore, query, page]);

    return (
        <div className="search-container">
            <div className="search-header">
                <h1>Pesquisa de Utilizadores & Podcasts</h1>
                <input 
                    type="text" 
                    className="search-input" 
                    placeholder="Pesquise por autor ou título (ex: João)..." 
                    value={query}
                    onChange={handleInputChange}
                />
            </div>

            <div className="search-content">
                {query.trim() === '' ? (
                    <div className="search-empty">Comece a digitar para pesquisar...</div>
                ) : (
                    <div className="search-results">
                        {results.length > 0 ? (
                            results.map((item, index) => {
                                // Aplicar a ref ao último elemento para ativar a paginação
                                const isLastParams = results.length === index + 1;
                                
                                // Diferenciação visual da imagem
                                const imageClass = item.type === 'USER' ? 'image-user' : 'image-podcast';
                                
                                // Fallback para imagens
                                const imgSrc = item.imageUrl ? `http://localhost:8080${item.imageUrl}` : 'https://via.placeholder.com/50';

                                return (
                                    <div 
                                        className="search-item" 
                                        key={`${item.type}-${item.id}`} 
                                        ref={isLastParams ? lastElementRef : null}
                                    >
                                        <img src={imgSrc} alt="Capa/Perfil" className={`search-item-image ${imageClass}`} />
                                        <div className="search-item-info">
                                            <span className="search-item-title">{item.title}</span>
                                            <span className="search-item-subtitle">{item.subtitle}</span>
                                        </div>
                                    </div>
                                );
                            })
                        ) : !loading && (
                            <div className="search-empty">Não há resultados para "{query}".</div>
                        )}

                        {loading && <div className="search-loading">A carregar {page > 0 ? 'mais' : ''}...</div>}
                        {!hasMore && results.length > 0 && <div className="search-end">Fim dos resultados.</div>}
                    </div>
                )}
            </div>
        </div>
    );
};

export default SearchPageTest;
