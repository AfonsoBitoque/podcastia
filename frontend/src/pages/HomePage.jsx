import '../styles/home-page.css'

function HomePage() {
  return (
    <main className="home-page" aria-labelledby="home-title">
      <section className="home-card">
        <p className="home-kicker">Podcastia</p>
        <h1 id="home-title">Bem-vinda a HomePage</h1>
        <p>
          Login concluido com sucesso. Aqui podemos evoluir para dashboard, lista de podcasts e
          analytics.
        </p>
      </section>
    </main>
  )
}

export default HomePage
