import { useEffect, useRef } from 'react'
import lottie, { type AnimationItem } from 'lottie-web/build/player/lottie_light'
import notificationAnimation from '../assets/notification.json'

const MIN_DELAY = 8_000
const MAX_DELAY = 14_000

export default function NotificationBell({ unread }: { unread: boolean }) {
  const container = useRef<HTMLSpanElement>(null)
  const animation = useRef<AnimationItem | null>(null)

  useEffect(() => {
    if (!container.current) return
    animation.current = lottie.loadAnimation({
      container: container.current,
      renderer: 'svg',
      loop: false,
      autoplay: false,
      animationData: notificationAnimation,
    })
    animation.current.goToAndStop(0, true)
    return () => { animation.current?.destroy(); animation.current = null }
  }, [])

  useEffect(() => {
    const player = animation.current
    if (!player) return
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    if (!unread || reducedMotion) {
      player.goToAndStop(0, true)
      return
    }

    let timeout: number | undefined
    const play = () => {
      player.goToAndStop(0, true)
      player.play()
      const delay = MIN_DELAY + Math.random() * (MAX_DELAY - MIN_DELAY)
      timeout = window.setTimeout(play, delay)
    }
    play()
    return () => window.clearTimeout(timeout)
  }, [unread])

  return <span ref={container} className="notification-animation" aria-hidden="true" />
}
