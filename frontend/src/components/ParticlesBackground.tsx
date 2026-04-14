import { useMemo } from 'react'

type Particle = {
  id: number
  size: number
  left: number
  delay: number
  duration: number
}

export function ParticlesBackground() {
  const particles = useMemo<Particle[]>(() => {
    return Array.from({ length: 32 }, (_, index) => ({
      id: index,
      size: 2 + (index % 5),
      left: (index * 13.7) % 100,
      delay: (index % 9) * 0.6,
      duration: 12 + (index % 8) * 2,
    }))
  }, [])

  return (
    <div className="particles-scene" aria-hidden="true">
      <div className="scan-grid" />
      {particles.map((particle) => (
        <span
          key={particle.id}
          className="particle-dot"
          style={{
            width: `${particle.size}px`,
            height: `${particle.size}px`,
            left: `${particle.left}%`,
            animationDelay: `${particle.delay}s`,
            animationDuration: `${particle.duration}s`,
          }}
        />
      ))}
    </div>
  )
}
