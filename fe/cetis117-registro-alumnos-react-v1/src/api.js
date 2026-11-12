const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  'https://zdregistroalumnos-482614074090.us-central1.run.app'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body && !(options.body instanceof FormData) ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {}),
    },
  })

  let data = null

  try {
    data = await response.json()
  } catch {
    data = null
  }

  if (!response.ok) {
    const error = new Error(
      data?.error ||
        data?.mensaje ||
        `Error HTTP ${response.status}`,
    )
    error.detalles = data?.errores || []
    throw error
  }

  return data
}

export function validarAcceso(codigo) {
  return request(
    `/api/acceso/validar?codigo=${encodeURIComponent(codigo)}`,
  )
}

export function consultarAlumnos(codigo, filtros = {}) {
  const params = new URLSearchParams({ codigo })

  Object.entries(filtros).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim())
    }
  })

  return request(`/api/alumnos/consulta?${params.toString()}`)
}

export function obtenerGrupos(codigo) {
  return request(
    `/api/grupos?codigo=${encodeURIComponent(codigo)}`,
  )
}

export function crearAutorizacion(payload) {
  return request('/api/autorizaciones', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export { API_BASE_URL }

export function guardarCatalogo(tipo, datos, codigo) {
  return request(`/api/${tipo}`, {
    method: 'POST',
    headers: { 'X-Codigo-Sistema': codigo },
    body: JSON.stringify(datos),
  })
}

export function importarCatalogo(tipo, archivo, codigo) {
  const body = new FormData()
  body.append('file', archivo)
  return request(`/api/${tipo}/importar`, {
    method: 'POST', headers: { 'X-Codigo-Sistema': codigo }, body,
  })
}
