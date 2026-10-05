export default function Header() {
  return (
    <header className="border-b border-[#eadfce] bg-white/95 backdrop-blur">
      <div className="w-full px-4 py-3 sm:px-6 lg:px-8">
        <div className="flex items-center justify-start gap-3">
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
              Consulta de asistencia por código de seguridad
            </p>
          </div>
        </div>
      </div>
    </header>
  )
}
