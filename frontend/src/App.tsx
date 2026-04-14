import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'

import { createGoal, deleteGoal, listGoals, type Currency } from './api'
import { ParticlesBackground } from './components/ParticlesBackground'

const GoalCharts = lazy(() => import('./components/GoalCharts').then((module) => ({ default: module.GoalCharts })))

type Language = 'en' | 'pt-BR'

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

const copy: Record<Language, Record<string, string>> = {
  en: {
    appTitle: 'Game Dashboard',
    heroTitle: 'Plan time and cost for each game with a clear and modern view',
    heroCopy: 'Offline app with React frontend + local Java backend. Data is stored in local SQLite, with no external services.',
    localProfile: 'Local profile',
    profilePlaceholder: 'Your name',
    status: 'Status',
    backendOnline: 'Local Java backend connected',
    backendOffline: 'Local Java backend offline',
    gamesInDb: 'game(s) saved in local database',
    cardsGames: 'Games',
    cardsGamesHelper: 'No registration limit',
    cardsHours: 'Planned hours',
    cardsHoursHelper: 'Total estimate',
    cardsAvg: 'Average time',
    cardsAvgHelper: 'At current pace',
    cardsInvest: 'Total investment',
    cardsInvestHelper: 'Sum of all games',
    gameRegister: 'Game registration',
    compactMode: 'Compact mode',
    essentialFields: 'Essential fields',
    expand: 'Expand',
    collapse: 'Collapse',
    planning: 'Planning',
    gameName: 'Game name',
    gameNamePlaceholder: 'e.g. Death Stranding',
    totalHours: 'Estimated total hours',
    hoursPerDay: 'Hours per day',
    daysPerWeek: 'Days per week',
    gamePrice: 'Game price',
    currency: 'Currency',
    clear: 'Clear',
    saveGame: 'Save game',
    gamesRegistered: 'Registered games',
    visible: 'visible',
    searchGame: 'Search game...',
    daysShort: 'days',
    perSession: 'per session',
    remove: 'Remove',
    selectGame: 'Select a game',
    weeksEstimated: 'estimated weeks',
    noSelection: 'No selection',
    showcaseAria: 'Game showcase carousel',
    showcaseTitle: 'Game Showcase',
    kpiCostSession: 'Cost per session',
    kpiCostHour: 'Cost per hour',
    kpiSessions: 'Required sessions',
    kpiWeekly: 'Weekly pace',
    loadingCharts: 'Loading charts...',
    decisionSummary: 'Decision summary',
    gameValue: 'Game value',
    estimatedTotalTime: 'Estimated total time',
    daysToFinish: 'Days to finish',
    emptyState: 'Register a game and click it to see cost vs time in the dashboard.',
    saveSuccess: 'Game saved to local Java backend.',
    saveError: 'Could not save. Check if local Java backend is running.',
    deleteSuccess: 'Game removed from local Java backend.',
    deleteError: 'Could not remove. Check if local Java backend is running.',
    backendError: 'Java backend unavailable. Start local server to use the app.',
    langLabel: 'Language',
    brlLabel: 'Brazilian Real (BRL)',
    usdLabel: 'US Dollar (USD)',
    eurLabel: 'Euro (EUR)',
    brlPrefix: 'BRL ',
    valueGreat: 'Great value',
    valueGreatHint: 'Excellent cost per hour',
    valueGood: 'Worth it',
    valueGoodHint: 'Balanced cost per hour',
    valueWarn: 'Review carefully',
    valueWarnHint: 'High cost per hour',
  },
  'pt-BR': {
    appTitle: 'Game Dashboard',
    heroTitle: 'Planeje tempo e custo de cada jogo com uma visao clara e moderna',
    heroCopy: 'Aplicacao offline com frontend React + backend Java local. Os dados sao persistidos no SQLite da maquina, sem servicos externos.',
    localProfile: 'Perfil local',
    profilePlaceholder: 'Seu nome',
    status: 'Status',
    backendOnline: 'Backend Java local conectado',
    backendOffline: 'Backend Java local offline',
    gamesInDb: 'jogo(s) cadastrados no banco local',
    cardsGames: 'Jogos',
    cardsGamesHelper: 'Sem limite de cadastro',
    cardsHours: 'Horas planejadas',
    cardsHoursHelper: 'Estimativa total',
    cardsAvg: 'Prazo medio',
    cardsAvgHelper: 'No ritmo atual',
    cardsInvest: 'Investimento total',
    cardsInvestHelper: 'Soma de todos os jogos',
    gameRegister: 'Cadastro de jogo',
    compactMode: 'Modo compacto',
    essentialFields: 'Campos essenciais',
    expand: 'Expandir',
    collapse: 'Compactar',
    planning: 'Planejamento',
    gameName: 'Nome do jogo',
    gameNamePlaceholder: 'Ex.: Death Stranding',
    totalHours: 'Horas totais estimadas',
    hoursPerDay: 'Horas por dia',
    daysPerWeek: 'Dias por semana',
    gamePrice: 'Preco do jogo',
    currency: 'Moeda',
    clear: 'Limpar',
    saveGame: 'Salvar jogo',
    gamesRegistered: 'Jogos cadastrados',
    visible: 'visiveis',
    searchGame: 'Buscar jogo...',
    daysShort: 'dias',
    perSession: 'por sessao',
    remove: 'Remover',
    selectGame: 'Selecione um jogo',
    weeksEstimated: 'semanas estimadas',
    noSelection: 'Sem selecao',
    showcaseAria: 'Carrossel de jogos',
    showcaseTitle: 'Game Showcase',
    kpiCostSession: 'Custo por sessao',
    kpiCostHour: 'Custo por hora',
    kpiSessions: 'Sessoes necessarias',
    kpiWeekly: 'Ritmo semanal',
    loadingCharts: 'Carregando graficos...',
    decisionSummary: 'Resumo de decisao',
    gameValue: 'Valor do jogo',
    estimatedTotalTime: 'Tempo total estimado',
    daysToFinish: 'Dias para finalizar',
    emptyState: 'Cadastre um jogo e clique nele para ver custo x tempo no dashboard.',
    saveSuccess: 'Jogo salvo no backend Java local com sucesso.',
    saveError: 'Nao foi possivel salvar. Verifique se o backend Java esta rodando.',
    deleteSuccess: 'Jogo removido do backend Java local.',
    deleteError: 'Nao foi possivel remover. Verifique se o backend Java esta rodando.',
    backendError: 'Backend Java indisponivel. Inicie o servidor local para usar o app.',
    langLabel: 'Idioma',
    brlLabel: 'Real (BRL)',
    usdLabel: 'Dollar (USD)',
    eurLabel: 'Euro (EUR)',
    brlPrefix: 'R$ ',
    valueGreat: 'Vale muito a pena',
    valueGreatHint: 'Excelente custo por hora',
    valueGood: 'Vale a pena',
    valueGoodHint: 'Custo por hora equilibrado',
    valueWarn: 'Analise com cuidado',
    valueWarnHint: 'Custo por hora elevado',
  },
}

function safeNumber(value: string, fallback = 0) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

function moneyFormatter(currency: Currency) {
  const locale = currency === 'USD' ? 'en-US' : currency === 'EUR' ? 'de-DE' : 'pt-BR'
  return new Intl.NumberFormat(locale, { style: 'currency', currency })
}

function getValueRating(costPerHour: number, language: Language) {
  const t = copy[language]
  if (costPerHour <= 2.5) {
    return { label: t.valueGreat, tone: 'good', hint: t.valueGreatHint }
  }

  if (costPerHour <= 6) {
    return { label: t.valueGood, tone: 'mid', hint: t.valueGoodHint }
  }

  return { label: t.valueWarn, tone: 'warn', hint: t.valueWarnHint }
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
  const [language, setLanguage] = useState<Language>(() => {
    const saved = localStorage.getItem('gd_lang')
    return saved === 'pt-BR' ? 'pt-BR' : 'en'
  })
  const [profileName, setProfileName] = useState(() => localStorage.getItem('gd_profile') ?? 'player-1')
  const [games, setGames] = useState<GamePlan[]>([])
  const [selectedGameId, setSelectedGameId] = useState<number | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [isBackendOnline, setIsBackendOnline] = useState(true)
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

  const t = copy[language]

  function handleProfileChange(value: string) {
    const nextProfile = value.trim() || 'player-1'
    setProfileName(nextProfile)
    localStorage.setItem('gd_profile', nextProfile)
  }

  const loadGoals = useCallback(async () => {
    try {
      const loaded = await listGoals()
      setGames(loaded)
      setSelectedGameId((current) => {
        if (current && loaded.some((item) => item.id === current)) {
          return current
        }
        return loaded[0]?.id ?? null
      })
      setIsBackendOnline(true)
    } catch {
      setIsBackendOnline(false)
      setNotice(t.backendError)
    }
  }, [t.backendError])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadGoals()
    }, 0)

    return () => window.clearTimeout(timer)
  }, [loadGoals])

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

  useEffect(() => {
    localStorage.setItem('gd_lang', language)
    document.documentElement.lang = language
  }, [language])

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
    () => (selectedGame ? getValueRating(selectedGame.costPerHour, language) : null),
    [selectedGame, language],
  )

  const summaryCards = useMemo(() => {
    const totalHours = enrichedGames.reduce((acc, item) => acc + item.estimatedHours, 0)
    const totalInvestment = enrichedGames.reduce((acc, item) => acc + item.price, 0)
    const avgDays =
      enrichedGames.length === 0
        ? 0
        : enrichedGames.reduce((acc, item) => acc + item.daysToFinish, 0) / enrichedGames.length

    return [
      { label: t.cardsGames, value: String(enrichedGames.length), helper: t.cardsGamesHelper },
      { label: t.cardsHours, value: `${totalHours.toFixed(1)}h`, helper: t.cardsHoursHelper },
      { label: t.cardsAvg, value: `${avgDays.toFixed(1)} ${t.daysShort}`, helper: t.cardsAvgHelper },
      {
        label: t.cardsInvest,
        value: `${t.brlPrefix}${totalInvestment.toFixed(2)}`,
        helper: t.cardsInvestHelper,
      },
    ]
  }, [enrichedGames, t])

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

  async function handleCreateGoal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!goalDraft.title.trim()) {
      return
    }

    try {
      const created = await createGoal({
        title: goalDraft.title.trim(),
        estimatedHours: Math.max(1, safeNumber(goalDraft.estimatedHours, 1)),
        hoursPerDay: Math.max(0.1, safeNumber(goalDraft.hoursPerDay, 1)),
        daysPerWeek: Math.max(1, Math.min(7, Math.round(safeNumber(goalDraft.daysPerWeek, 5)))),
        price: Math.max(0, safeNumber(goalDraft.price, 0)),
        currency: goalDraft.currency,
      })

      setGames((current) => [created, ...current])
      setSelectedGameId(created.id)
      setGoalDraft((current) => ({ ...initialGoalDraft, currency: current.currency }))
      setIsBackendOnline(true)
      setNotice(t.saveSuccess)
    } catch {
      setIsBackendOnline(false)
      setNotice(t.saveError)
    }
  }

  async function handleDeleteGame(gameId: number) {
    try {
      await deleteGoal(gameId)
      setGames((current) => current.filter((item) => item.id !== gameId))
      setSelectedGameId((current) => (current === gameId ? null : current))
      setIsBackendOnline(true)
      setNotice(t.deleteSuccess)
    } catch {
      setIsBackendOnline(false)
      setNotice(t.deleteError)
    }
  }

  return (
    <main className="page-shell">
      <ParticlesBackground />

      <header className="hero-panel reveal reveal-1">
        <div className="hero-top-row">
          <p className="eyebrow">{t.appTitle}</p>
          <div className="language-switch" role="group" aria-label={t.langLabel}>
            <button
              type="button"
              className={language === 'en' ? 'lang-btn active' : 'lang-btn'}
              onClick={() => setLanguage('en')}
            >
              EN
            </button>
            <button
              type="button"
              className={language === 'pt-BR' ? 'lang-btn active' : 'lang-btn'}
              onClick={() => setLanguage('pt-BR')}
            >
              PT-BR
            </button>
          </div>
        </div>
        <h1>{t.heroTitle}</h1>
        <p className="hero-copy">{t.heroCopy}</p>

        <div className="hero-meta">
          <label className="inline-field">
            <span>{t.localProfile}</span>
            <input value={profileName} onChange={(event) => handleProfileChange(event.target.value)} placeholder={t.profilePlaceholder} />
          </label>
          <div className="sync-chip">
            <span>{t.status}</span>
            <strong>{isBackendOnline ? t.backendOnline : t.backendOffline}</strong>
            <small>{games.length} {t.gamesInDb}</small>
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
            <h2>{t.gameRegister}</h2>
            <div className="panel-actions">
              <span>{isCompactPanel ? t.compactMode : t.essentialFields}</span>
              <button
                type="button"
                className="ghost-btn"
                onClick={() => setIsCompactPanel((current) => !current)}
              >
                {isCompactPanel ? t.expand : t.collapse}
              </button>
            </div>
          </div>

          {!isCompactPanel ? (
            <form className="goal-form" onSubmit={handleCreateGoal}>
              <h3>{t.planning}</h3>

              <label className="field-wrap">
                <span>{t.gameName}</span>
                <input
                  value={goalDraft.title}
                  onChange={(event) => setGoalDraft((current) => ({ ...current, title: event.target.value }))}
                  placeholder={t.gameNamePlaceholder}
                  required
                />
              </label>

              <div className="goal-form-row">
                <label className="field-wrap compact">
                  <span>{t.totalHours}</span>
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
                  <span>{t.hoursPerDay}</span>
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
                  <span>{t.daysPerWeek}</span>
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
                  <span>{t.gamePrice}</span>
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
                  <span>{t.currency}</span>
                  <select
                    value={goalDraft.currency}
                    onChange={(event) =>
                      setGoalDraft((current) => ({ ...current, currency: event.target.value as Currency }))
                    }
                  >
                    <option value="BRL">{t.brlLabel}</option>
                    <option value="USD">{t.usdLabel}</option>
                    <option value="EUR">{t.eurLabel}</option>
                  </select>
                </label>
              </div>

              <div className="goal-actions">
                <button type="button" onClick={() => setGoalDraft(initialGoalDraft)}>
                  {t.clear}
                </button>
                <button type="submit">{t.saveGame}</button>
              </div>
            </form>
          ) : null}

          <div className="list-head">
            <h3>{t.gamesRegistered}</h3>
            <span>{filteredGames.length} {t.visible}</span>
          </div>

          <input
            className="list-search"
            placeholder={t.searchGame}
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />

          <ul className="goal-list">
            {filteredGames.map((game) => (
              <li key={game.id} className={game.id === selectedGameId ? 'goal-item active' : 'goal-item'}>
                <button type="button" onClick={() => setSelectedGameId(game.id)}>
                  <strong>{game.title}</strong>
                  <small>
                    {game.daysToFinish.toFixed(1)} {t.daysShort} | {moneyFormatter(game.currency).format(game.costPerSession)} {t.perSession}
                  </small>
                </button>
                <button type="button" className="danger" onClick={() => handleDeleteGame(game.id)}>
                  {t.remove}
                </button>
              </li>
            ))}
          </ul>
        </aside>

        <section className="glass-panel detail-panel">
          <div className="section-head">
            <h2>{selectedGame?.title ?? t.selectGame}</h2>
            <span>{selectedGame ? `${selectedGame.weeksToFinish.toFixed(1)} ${t.weeksEstimated}` : t.noSelection}</span>
          </div>

          {valueRating ? (
            <div className={`value-chip ${valueRating.tone}`}>
              <strong>{valueRating.label}</strong>
              <span>{valueRating.hint}</span>
            </div>
          ) : null}

          {filteredGames.length > 0 ? (
            <section className="showcase-wrap" aria-label={t.showcaseAria}>
              <h3>{t.showcaseTitle}</h3>
              <div className="showcase-carousel">
                {filteredGames.slice(0, 12).map((game) => (
                  <button
                    key={game.id}
                    type="button"
                    className={game.id === selectedGameId ? 'showcase-card active' : 'showcase-card'}
                    onClick={() => setSelectedGameId(game.id)}
                  >
                    <strong>{game.title}</strong>
                    <span>{game.daysToFinish.toFixed(0)} {t.daysShort}</span>
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          {selectedGame ? (
            <>
              <section className="kpi-grid">
                <article className="kpi-card">
                  <span>{t.kpiCostSession}</span>
                  <strong key={`kpi-session-${selectedGame.id}`} className="kpi-value">
                    {moneyFormatter(selectedGame.currency).format(selectedGame.costPerSession)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>{t.kpiCostHour}</span>
                  <strong key={`kpi-hour-${selectedGame.id}`} className="kpi-value">
                    {moneyFormatter(selectedGame.currency).format(selectedGame.costPerHour)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>{t.kpiSessions}</span>
                  <strong key={`kpi-sessions-${selectedGame.id}`} className="kpi-value">
                    {selectedGame.sessionsNeeded.toFixed(1)}
                  </strong>
                </article>
                <article className="kpi-card">
                  <span>{t.kpiWeekly}</span>
                  <strong key={`kpi-weekly-${selectedGame.id}`} className="kpi-value">
                    {selectedGame.weeklyHours.toFixed(1)}h
                  </strong>
                </article>
              </section>

              <Suspense fallback={<div className="loading">{t.loadingCharts}</div>}>
                <GoalCharts
                  language={language}
                  finishChartData={finishChartData}
                  projectionData={projectionData}
                  selectedTitle={selectedGame.title}
                  estimatedHours={selectedGame.estimatedHours}
                />
              </Suspense>

              <div className="sessions-log">
                <h3>{t.decisionSummary}</h3>
                <ul>
                  <li>
                    <span>{t.gameValue}</span>
                    <strong>{moneyFormatter(selectedGame.currency).format(selectedGame.price)}</strong>
                  </li>
                  <li>
                    <span>{t.estimatedTotalTime}</span>
                    <strong>{selectedGame.estimatedHours.toFixed(1)}h</strong>
                  </li>
                  <li>
                    <span>{t.daysToFinish}</span>
                    <strong>{selectedGame.daysToFinish.toFixed(1)} {t.daysShort}</strong>
                  </li>
                </ul>
              </div>
            </>
          ) : (
            <div className="empty-state">{t.emptyState}</div>
          )}
        </section>
      </section>
    </main>
  )
}

export default App
