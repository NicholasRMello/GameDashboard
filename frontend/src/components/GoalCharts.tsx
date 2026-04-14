import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

type GoalChartsProps = {
  finishChartData: Array<{ title: string; days: number }>
  projectionData: Array<{ label: string; hours: number }>
  selectedTitle: string
  estimatedHours: number
}

export function GoalCharts({ finishChartData, projectionData, selectedTitle, estimatedHours }: GoalChartsProps) {
  return (
    <>
      <div className="chart-block">
        <h3>Prazo estimado por jogo (dias)</h3>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={finishChartData} barCategoryGap="28%">
            <defs>
              <linearGradient id="daysGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#ffa43a" stopOpacity={1} />
                <stop offset="100%" stopColor="#ff7a30" stopOpacity={0.85} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
            <XAxis dataKey="title" tick={{ fill: '#f5f4ef', fontSize: 12 }} />
            <YAxis tick={{ fill: '#f5f4ef', fontSize: 12 }} />
            <Tooltip
              cursor={{ fill: 'rgba(255,255,255,0.04)' }}
              contentStyle={{ background: 'rgba(11, 14, 16, 0.92)', border: '1px solid rgba(255,255,255,0.16)', borderRadius: '10px' }}
              labelStyle={{ color: '#f5f4ef', fontWeight: 700, marginBottom: '4px' }}
              itemStyle={{ color: '#bab8af' }}
              formatter={(value) => [`${Number(value).toFixed(1)} dias`, 'Prazo estimado']}
            />
            <Bar dataKey="days" fill="url(#daysGradient)" radius={[8, 8, 2, 2]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="chart-block">
        <h3>Projecao de horas acumuladas ({selectedTitle})</h3>
        <ResponsiveContainer width="100%" height={220}>
          <AreaChart data={projectionData}>
            <defs>
              <linearGradient id="hoursArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#5be7b0" stopOpacity={0.7} />
                <stop offset="100%" stopColor="#5be7b0" stopOpacity={0.05} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
            <XAxis dataKey="label" tick={{ fill: '#f5f4ef', fontSize: 12 }} />
            <YAxis tick={{ fill: '#f5f4ef', fontSize: 12 }} />
            <Tooltip
              cursor={{ stroke: 'rgba(91,231,176,0.35)', strokeWidth: 1 }}
              contentStyle={{ background: 'rgba(11, 14, 16, 0.92)', border: '1px solid rgba(255,255,255,0.16)', borderRadius: '10px' }}
              labelStyle={{ color: '#f5f4ef', fontWeight: 700, marginBottom: '4px' }}
              itemStyle={{ color: '#bab8af' }}
              formatter={(value) => {
                const hours = Number(value)
                const completion = Math.min(100, (hours / Math.max(estimatedHours, 0.1)) * 100)
                return [`${hours.toFixed(1)}h (${completion.toFixed(1)}%)`, 'Horas acumuladas']
              }}
              labelFormatter={(label) => `${String(label)} - ${selectedTitle}`}
            />
            <ReferenceLine
              y={estimatedHours}
              stroke="rgba(255,164,58,0.9)"
              strokeDasharray="5 5"
              label={{ value: 'Meta', fill: '#ffcf8c', fontSize: 11, position: 'insideTopRight' }}
            />
            <Area
              type="monotone"
              dataKey="hours"
              stroke="#5be7b0"
              strokeWidth={2.6}
              fill="url(#hoursArea)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </>
  )
}
