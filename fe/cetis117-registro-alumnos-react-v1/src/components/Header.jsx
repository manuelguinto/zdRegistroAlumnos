import { LogOutIcon } from './Icons'

export default function Header({
  subtitle = 'Consulta de asistencia por código de seguridad',
  userLabel,
  onLogout,
}) {
  return (
    <header className="border-b border-[#eadfce] bg-white/95 backdrop-blur">
      <div className="flex w-full items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <div className="flex min-w-0 items-center justify-start gap-3">
          <img
            src="/logo-cetis117.jpg"
            alt="Logo CETIS 117"
            className="h-14 w-14 shrink-0 rounded-2xl object-contain shadow-sm sm:h-16 sm:w-16"
          />

          <div className="min-w-0">
            <p className="text-lg font-black tracking-[0.16em] text-[#b7832d] sm:text-xl">
              CETIS 117
            </p>

            <p className="truncate text-sm text-cetis-muted sm:text-base">
              {subtitle}
            </p>
          </div>
        </div>

        {onLogout && (
          <div className="flex shrink-0 items-center gap-2">
            {userLabel && (
              <span className="hidden rounded-full bg-cetis-cream px-3 py-2 text-xs font-bold text-cetis-wine md:inline-flex">
                {userLabel}
              </span>
            )}

            <button
              type="button"
              onClick={onLogout}
              className="btn-secondary gap-2 px-3 sm:px-4"
              title="Cerrar sesión"
            >
              <LogOutIcon />
              <span className="hidden sm:inline">Cerrar sesión</span>
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
