import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'

import { ParticlesBackground } from './components/ParticlesBackground'

const GoalCharts = lazy(() => import('./components/GoalCharts').then((module) => ({ default: module.GoalCharts })))

type Currency = 'BRL' | 'USD' | 'EUR'

type GamePlan = {
  id: number
  title: string
  estimatedHours: number
  hoursPerDay: number
  daysPerWeek: number
  price: number
  currency: Currency
  createdAt: string
}

type GoalDraft = {
  title: string
  estimatedHours: string
  hoursPerDay: string
  daysPerWeek: string
  price: string
  currency: Currency
}

type EnrichedGame = GamePlan & {
  weeklyHours: number
  weeksToFinish: number
  daysToFinish: number
  sessionsNeeded: number
  costPerSession: number
  costPerHour: number
}

const initialGoalDraft: GoalDraft = {
  title: '',
  estimatedHours: '20',
  hoursPerDay: '2',
  daysPerWeek: '5',
  price: '299.9',
  currency: 'BRL',
}

function getStorageKey(profile: string) {
  return `gd_games_${profile.trim().toLowerCase()}`
}

function readGamesFromStorage(profile: string): GamePlan[] {
  const raw = localStorage.getItem(getStorageKey(profile))
  if (!raw) {
    return []
  }

  try {
    return JSON.parse(raw) as GamePlan[]
  } catch {
    return []
  }
}

function safeNumber(value: string, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function moneyFormatter(currency: Currency) {
  const locale = currency === 'USD' ? 'en-US' : currency === 'EUR' ? 'de-DE' : 'pt-BR'
  return new Intl.NumberFormat(locale, { style: 'currency', currency })
}

function getValueRating(costPerHour: number) {
  if (costPerHour <= 2.5) {
    return { label: 'Vale muito a pena', tone: 'good', hint: 'Excelente custo por hora' as const }
  }

  if (costPerHour <= 6) {
    return { label: 'Vale a pena', tone: 'mid', hint: 'Custo por hora equilibrado' as const }
  }

  return { label: 'Analise com cuidado', tone: 'warn', hint: 'Custo por hora elevado' as const }
}

function enrichGame(game: GamePlan): EnrichedGame {
  const weeklyHours = Math.max(0.1, game.hoursPerDay * game.daysPerWeek)
  const weeksToFinish = game.estimatedHours / weeklyHours
  const daysToFinish = weeksToFinish * 7
  const sessionsNeeded = Math.max(1, game.estimatedHours / Math.max(game.hoursPerDay, 0.1))
  const costPerSession = game.price / sessionsNeeded
  const costPerHour = game.price / Math.max(game.estimatedHours, 0.1)

  return {
    ...game,
    weeklyHours,
    weeksToFinish,
    daysToFinish,
    sessionsNeeded,
    costPerSession,
    costPerHour,
  }
}

function App() {
  const [profileName, setProfileName] = useState(() => localStorage.getItem('gd_profile') ?? 'player-1')
  const [games, setGames] = useState<GamePlan[]>(() => readGamesFromStorage(localStorage.getItem('gd_profile') ?? 'player-1'))
  const [selectedGameId, setSelectedGameId] = useState<number | null>(() => {
    const profile = localStorage.getItem('gd_profile') ?? 'player-1'
    return readGamesFromStorage(profile)[0]?.id ?? null
  })
  const [notice, setNotice] = useState<string | null>(null)
  const [searchText, setSearchText] = useState('')
  const [isCompactPanel, setIsCompactPanel] = useState(() => localStorage.getItem('gd_compact_panel') === '1')
  const [goalDraft, setGoalDraft] = useState<GoalDraft>(() => {
    const saved = localStorage.getItem('gd_goal_draft_local')
    if (!saved) {
      return initialGoalDraft
    }

    try {
      return JSON.parse(saved) as GoalDraft
    } catch {
      return initialGoalDraft
    }
  })

  function handleProfileChange(value: string) {
    const nextProfile = value.trim() || 'player-1'
    setProfileName(nextProfile)
    localStorage.setItem('gd_profile', nextProfile)

    const parsedGames = readGamesFromStorage(nextProfile)
    setGames(parsedGames)
    setSelectedGameId(parsedGames[0]?.id ?? null)
  }

  useEffect(() => {
    localStorage.setItem(getStorageKey(profileName), JSON.stringify(games))
  }, [games, profileName])

  useEffect(() => {
    localStorage.setItem('gd_goal_draft_local', JSON.stringify(goalDraft))
  }, [goalDraft])

  useEffect(() => {
    if (!notice) {
      return
    }
    const timer = window.setTimeout(() => setNotice(null), 2400)
    return () => window.clearTimeout(timer)
  }, [notice])

  useEffect(() => {
    localStorage.setItem('gd_compact_panel', isCompactPanel ? '1' : '0')
  }, [isCompactPanel])

  const enrichedGames = useMemo(() => games.map(enrichGame), [games])

  const filteredGames = useMemo(() => {
    const term = searchText.trim().toLowerCase()
    if (!term) {
      return enrichedGames
    }
    return enrichedGames.filter((game) => game.title.toLowerCase().includes(term))
  }, [enrichedGames, searchText])

  const selectedGame = useMemo(
    () => enrichedGames.find((game) => game.id === selectedGameId) ?? null,
    [enrichedGames, selectedGameId],
  )

  const valueRating = useMemo(
    () => (selectedGame ? getValueRating(selectedGame.costPerHour) : null),
    [selectedGame],
  )

  const summaryCards = useMemo(() => {
    const totalHours = enrichedGames.reduce((acc, item) => acc + item.estimatedHours, 0)
    const totalInvestment = enrichedGames.reduce((acc, item) => acc + item.price, 0)
    const avgDays =
      enrichedGames.length === 0
        ? 0
        : enrichedGames.reduce((acc, item) => acc + item.daysToFinish, 0) / enrichedGames.length

    return [
      { label: 'Jogos', value: String(enrichedGames.length), helper: 'Sem limite de cadastro' },
      { label: 'Horas planejadas', value: `${totalHours.toFixed(1)}h`, helper: 'Estimativa total' },
      { label: 'Prazo medio', value: `${avgDays.toFixed(1)} dias`, helper: 'No ritmo atual' },
      {
        label: 'Investimento total',
        value: `R$ ${totalInvestment.toFixed(2)}`,
        helper: 'Soma de todos os jogos',
      },
    ]
  }, [enrichedGames])

  const finishChartData = useMemo(
    () =>
      filteredGames.map((game) => ({
        title: game.title,
        days: Number(game.daysToFinish.toFixed(2)),
      })),
    [filteredGames],
  )

  const projectionData = useMemo(() => {
    if (!selectedGame) {
      return []
    }

    const points: Array<{ label: string; hours: number }> = []
    const maxWeeks = Math.min(32, Math.ceil(selectedGame.weeksToFinish) + 2)

    for (let week = 1; week <= maxWeeks; week++) {
      points.push({
        label: `S${week}`,
        hours: Math.min(selectedGame.estimatedHours, week * selectedGame.weeklyHours),
      })
    }
    return points
  }, [selectedGame])

  function handleCreateGoal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!goalDraft.title.trim()) {
      return
    }

    const game: GamePlan = {
      id: Date.now(),
      title: goalDraft.title.trim(),
      estimatedHours: Math.max(1, safeNumber(goalDraft.estimatedHours, 1)),
      hoursPerDay: Math.max(0.1, safeNumber(goalDraft.hoursPerDay, 1)),
      daysPerWeek: Math.max(1, Math.min(7, Math.round(safeNumber(goalDraft.daysPerWeek, 5)))),
      price: Math.max(0, safeNumber(goalDraft.price, 0)),
      currency: goalDraft.currency,
      createdAt: new Date().toISOString(),
    }

    setGames((current) => [game, ...current])
    setSelectedGameId(game.id)
    setGoalDraft((current) => ({ ...initialGoalDraft, currency: current.currency }))
    setNotice('Jogo salvo localmente com sucesso.')
  }

  function handleDeleteGame(gameId: number) {
    setGames((current) => current.filter((item) => item.id !== gameId))
    setSelectedGameId((current) => (current === gameId ? null : current))
    setNotice('Jogo removido deste perfil local.')
  }

  return (
    <main className="page-shell">
      <ParticlesBackground />

      <header className="hero-panel reveal reveal-1">
        <p className="eyebrow">Game Dashboard</p>
        <h1>Planeje tempo e custo de cada jogo com uma visao clara e moderna</h1>
        <p className="hero-copy">
          Aplicacao local-first: sem backend, sem chave de API e sem exportacao. Cada perfil salva seus dados no proprio navegador.
        </p>

        <div className="hero-meta">
          <label className="inline-field">
            <span>Perfil local</span>
            <input value={profileName} onChange={(event) => handleProfileChange(event.target.value)} placeholder="Seu nome" />
          </label>
          <div className="sync-chip">
            <span>Status</span>
            <strong>Dados salvos localmente neste navegador</strong>
            <small>{games.length} jogo(s) cadastrados neste perfil</small>
          </div>
        </div>
      </header>

      {notice ? <div className="banner-ok">{notice}</div> : null}

      <section className="stats-grid reveal reveal-2">
        {summaryCards.map((card) => (
          <article key={card.label} className="stat-card">
            <span>{card.label}</span>
            <strong>{card.value}</strong>
            <small>{card.helper}</small>
          </article>
        ))}
      </section>

      <section className="layout-grid reveal reveal-3">
        <aside className={`glass-panel goals-panel ${isCompactPanel ? 'is-compact' : ''}`}>
          <div className="section-head">
            <h2>Cadastro de jogo</h2>
            <div className="panel-actions">
              <span>{isCompactPanel ? 'Modo compacto' : 'Campos essenciais'}</span>
              <button
                type="button"
                className="ghost-btn"
                onClick={() => setIsCompactPanel((current) => !current)}
              >
                {isCompactPanel ? 'Expandir' : 'Compactar'}
              </button>
            </div>
          </div>

          {!isCompactPanel ? (
            <form className="goal-form" onSubmit={handleCreateGoal}>
              <h3>Planejamento</h3>

              <label className="field-wrap">
                <span>Nome do jogo</span>
                <input
                  value={goalDraft.title}
                  onChange={(event) => setGoalDraft((current) => ({ ...current, title: event.target.value }))}
                  placeholder="Ex.: Death Stranding"
                  required
                />
              </label>

              <div className="goal-form-row">
                <label className="field-wrap compact">
                  <span>Horas totais estimadas</span>
                  <input
                    value={goalDraft.estimatedHours}
                    onChange={(event) => setGoalDraft((current) => ({ ...current, estimatedHours: event.target.value }))}
                    type="number"
                    min={1}
                    step="0.5"
                    required
                  />
                </label>

                <label className="field-wrap compact">
                  <span>Horas por dia</span>
                  <input
                    value={goalDraft.hoursPerDay}
                    onChange={(event) => setGoalDraft((current) => ({ ...current, hoursPerDay: event.target.value }))}
                    type="number"
                    min={0.1}
                    step="0.1"
                    required
                  />
                </label>

                <label className="field-wrap compact">
                  <span>Dias por semana</span>
                  <input
                    value={goalDraft.daysPerWeek}
                    onChange={(event) => setGoalDraft((current) => ({ ...current, daysPerWeek: event.target.value }))}
                    type="number"
                    min={1}
                    max={7}
                    step="1"
                    required
                  />
                </label>
              </div>

              <div className="goal-form-row">
                <label className="field-wrap compact">
                  <span>Preco do jogo</span>
                  <input
                    value={goalDraft.price}
                    onChange={(event) => setGoalDraft((current) => ({ ...current, price: event.target.value }))}
                    type="number"
                    min={0}
                    step="0.01"
                    required
                  />
                </label>

                <label className="field-wrap compact">
                  <span>Moeda</span>
                  <select
                    value={goalDraft.currency}
                    onChange={(event) =>
                      setGoalDraft((current) => ({ ...current, currency: event.target.value as Currency }))
                    }
                  >
                    <option value="BRL">Real (BRL)</option>
                    <option value="USD">Dollar (USD)</option>
                    <option value="EUR">Euro (EUR)</option>
                  </select>
                </label>
              </div>

              <div className="goal-actions">
                <button type="button" onClick={() => setGoalDraft(initialGoalDraft)}>
                  Limpar
                </button>
                <button type="submit">Salvar jogo</button>
              </div>
            </form>
          ) : null}

          <div className="list-head">
            <h3>Jogos cadastrados</h3>
            <span>{filteredGames.length} visiveis</span>
          </div>

          <input
            className="list-search"
            placeholder="Buscar jogo..."
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />

          <ul className="goal-list">
            {filteredGames.map((game) => (
              <li key={game.id} className={game.id === selectedGameId ? 'goal-item active' : 'goal-item'}>
                <button type="button" onClick={() => setSelectedGameId(game.id)}>
                  <strong>{game.title}</strong>
                  <small>
                    {game.daysToFinish.toFixed(1)} dias | {moneyFormatter(game.currency).format(game.costPerSession)} por sessao
                  </small>
                </button>
                <button type="button" className="danger" onClick={() => handleDeleteGame(game.id)}>
                  Remover
                </button>
              </li>
            ))}
          </ul>
        </aside>

        <section className="glass-panel detail-panel">
          <div className="section-head">
            <h2>{selectedGame?.title ?? 'Selecione um jogo'}</h2>
            <span>{selectedGame ? `${selectedGame.weeksToFinish.toFixed(1)} semanas estimadas` : 'Sem selecao'}</span>
          </div>

          {valueRating ? (
            <div className={`value-chip ${valueRating.tone}`}>
              <strong>{valueRating.label}</strong>
              <span>{valueRating.hint}</span>
            </div>
          ) : null}

          {filteredGames.length > 0 ? (
            <section className="showcase-wrap" aria-label="Game showcase carousel">
              <h3>Game Showcase</h3>
              <div className="showcase-carousel">
                {filteredGames.slice(0, 12).map((game) => (
                  <button
                    key={game.id}
                    type="button"
                    className={game.id === selectedGameId ? 'showcase-card active' : 'showcase-card'}
                    onClick={() => setSelectedGameId(game.id)}
                  >
                    <strong>{game.title}</strong>
                    <span>{game.daysToFinish.toFixed(0)} dias</span>
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          {selectedGame ? (
            <>
              <section className="kpi-grid">
                <article className="kpi-card">
                  <span>Custo por sessao</span>
                  <strong key={`kpi-session-${selectedGame.id}`} className="kpi-value">
                    {moneyFormatter(selectedGame.currency).format(selectedGame.costPerSession)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>Custo por hora</span>
                  <strong key={`kpi-hour-${selectedGame.id}`} className="kpi-value">
                    {moneyFormatter(selectedGame.currency).format(selectedGame.costPerHour)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>Sessoes necessarias</span>
                  <strong key={`kpi-sessions-${selectedGame.id}`} className="kpi-value">
                    {selectedGame.sessionsNeeded.toFixed(1)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>Ritmo semanal</span>
                  <strong key={`kpi-weekly-${selectedGame.id}`} className="kpi-value">
                    {selectedGame.weeklyHours.toFixed(1)}h
                  </strong>
                </article>
              </section>

              <Suspense fallback={<div className="loading">Carregando graficos...</div>}>
                <GoalCharts
                  finishChartData={finishChartData}
                  projectionData={projectionData}
                  selectedTitle={selectedGame.title}
                  estimatedHours={selectedGame.estimatedHours}
                />
              </Suspense>

              <div className="sessions-log">
                <h3>Resumo de decisao</h3>
                <ul>
                  <li>
                    <span>Valor do jogo</span>
                    <strong>{moneyFormatter(selectedGame.currency).format(selectedGame.price)}</strong>
                  </li>
                  <li>
                    <span>Tempo total estimado</span>
                    <strong>{selectedGame.estimatedHours.toFixed(1)}h</strong>
                  </li>
                  <li>
                    <span>Dias para finalizar</span>
                    <strong>{selectedGame.daysToFinish.toFixed(1)} dias</strong>
                  </li>
                </ul>
              </div>
            </>
          ) : (
            <div className="empty-state">Cadastre um jogo e clique nele para ver custo x tempo no dashboard.</div>
          )}
        </section>
      </section>
    </main>
  )
}

export default App
