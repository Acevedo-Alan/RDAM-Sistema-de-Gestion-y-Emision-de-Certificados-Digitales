# 🔗 Ejemplo de Integración - Frontend RDAM

Cómo integrar el mock PlusPagos en tu frontend React.

---

## Caso de Uso: Solicitud de Pago

### Flujo Actual (Sin Mock)
```
Usuario → Click "Pagar" 
→ GET /api/solicitudes/{id}/pago-datos 
→ (intenta ir a pasarela de PlusPagos real en Internet) 
→ ❌ FALLA (no hay servidor real)
```

### Flujo con Mock
```
Usuario → Click "Pagar"
→ GET /api/solicitudes/{id}/pago-datos (retorna urlPasarela: localhost:8081)
→ POST http://localhost:8081/pluspagos (datos encriptados)
→ ✓ Ve página de confirmación bonita
→ Click "Confirmar" 
→ Mock llama POST webhook backend
→ Backend cambia solicitud a PAGADA
→ Redirect a UrlSuccess
→ ✓ Usuario ve "Pago realizado exitosamente"
```

---

## Supuesto: Endpoint Existente

Tu backend ya tiene algo como:

```java
@GetMapping("/api/solicitudes/{id}/pago-datos")
public ResponseEntity<?> obtenerDatosPago(@PathVariable Long id) {
    Solicitud solicitud = solicitudService.findById(id);
    
    // Datos para encriptar
    String monto = String.valueOf(solicitud.getMonto() * 100);  // Centavos
    String informacion = "Solicitud " + solicitud.getId();
    
    // URLs para webhooks y redirect
    String callbackSuccess = mkRequest.getBaseUrl() + "/api/pagos/webhook";
    String urlSuccess = mkRequest.getBaseUrl() + ":5173/solicitudes/" + id + "?pago=success";
    String urlError = mkRequest.getBaseUrl() + ":5173/solicitudes/" + id + "?pago=cancelled";
    
    // Encriptar (usando PlusPagosCryptoService)
    String montoEncriptado = encryptor.encrypt(monto);
    String informacionEncriptada = encryptor.encrypt(informacion);
    String callbackEncriptado = encryptor.encrypt(callbackSuccess);
    String urlSuccessEncriptada = encryptor.encrypt(urlSuccess);
    String urlErrorEncriptada = encryptor.encrypt(urlError);
    
    // Retornar
    return ok(new PagoDatosDTO(
        comercio: "RDAM",
        transaccionComercioId: "SOL-" + solicitud.getId(),
        monto: montoEncriptado,
        informacion: informacionEncriptada,
        callbackSuccess: callbackEncriptado,
        urlSuccess: urlSuccessEncriptada,
        urlError: urlErrorEncriptada,
        urlPasarela: "http://localhost:8081/pluspagos"  // ← El mock
    ));
}
```

---

## Componente React: DetalleSolicitudPage.jsx

Asumiendo que ya tienes un componente que muestra solicitud:

```jsx
import React, { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import api from '../../api/axios';

export default function DetalleSolicitudPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const [solicitud, setSolicitud] = useState(null);
  const [datosPago, setDatosPago] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Verificar si viene del redirect del pago
  React.useEffect(() => {
    const pagStatus = searchParams.get('pago');
    if (pagStatus === 'success') {
      alert('✓ ¡Pago realizado exitosamente!');
      // Recargar solicitud para ver estado actualizado
      cargarSolicitud();
    } else if (pagStatus === 'cancelled') {
      alert('ℹ Pago cancelado');
    }
  }, [searchParams]);

  // Cargar solicitud
  const cargarSolicitud = async () => {
    try {
      const { data } = await api.get(`/solicitudes/${id}`);
      setSolicitud(data);
      setError(null);
    } catch (err) {
      setError('Error cargando solicitud');
      console.error(err);
    }
  };

  // Obtener datos de pago (backend encripta)
  const handleSolicitarPago = async () => {
    setLoading(true);
    setError(null);
    
    try {
      console.log('[Frontend] Solicitando datos de pago...');
      const { data } = await api.get(`/solicitudes/${id}/pago-datos`);
      
      console.log('[Frontend] Datos recibidos:', {
        Comercio: data.Comercio,
        TransaccionComercioId: data.TransaccionComercioId,
        urlPasarela: data.urlPasarela,
        // No loguear datos encriptados (contienen info sensible)
      });
      
      setDatosPago(data);
      
      // Redirigir a mock inmediatamente
      redirigirAMock(data);
    } catch (err) {
      setError('Error obteniendo datos de pago: ' + err.message);
      console.error('[Frontend] Error:', err);
    } finally {
      setLoading(false);
    }
  };

  // Redirigir a pasarela (método 1: formulario POST)
  const redirigirAMock = (datos) => {
    console.log('[Frontend] Redirigiendo a mock...');
    console.log('[Frontend] urlPasarela:', datos.urlPasarela);
    
    // Crear formulario dinámico
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = datos.urlPasarela;
    
    // Agregar todos los campos encriptados
    Object.entries(datos).forEach(([key, value]) => {
      if (key !== 'urlPasarela') {  // No incluir URL de pasarela
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = key;
        input.value = value;
        form.appendChild(input);
      }
    });
    
    // Enviar formulario
    document.body.appendChild(form);
    console.log('[Frontend] Enviando POST a pasarela...');
    form.submit();
  };

  // Alternativa: Redirigir vía URL (GET)
  const redirigirAMockViaUrl = (datos) => {
    console.log('[Frontend] Redirigiendo vía URL...');
    
    const params = new URLSearchParams();
    Object.entries(datos).forEach(([key, value]) => {
      if (key !== 'urlPasarela') {
        params.append(key, value);
      }
    });
    
    window.location.href = `${datos.urlPasarela}?${params.toString()}`;
  };

  if (!solicitud) {
    return (
      <div className="p-6">
        <button onClick={cargarSolicitud} disabled={loading}>
          {loading ? 'Cargando...' : 'Cargar Solicitud'}
        </button>
      </div>
    );
  }

  const puedeHacerPago = solicitud.estado === 'PENDIENTE_PAGO' || solicitud.estado === 'PENDIENTE';
  const yaPagada = solicitud.estado === 'PAGADA';

  return (
    <div className="p-6">
      <h1>Solicitud #{solicitud.id}</h1>
      
      {/* Mostrar estado */}
      <div className="mb-6 p-4 bg-gray-100 rounded">
        <p><strong>Estado:</strong> {solicitud.estado}</p>
        <p><strong>Monto:</strong> ${solicitud.monto.toFixed(2)}</p>
        <p><strong>Descripción:</strong> {solicitud.descripcion}</p>
      </div>

      {/* Mostrar error si ocurre */}
      {error && (
        <div className="mb-6 p-4 bg-red-100 text-red-700 rounded">
          ❌ {error}
        </div>
      )}

      {/* Botón Pagar */}
      {puedeHacerPago ? (
        <button
          onClick={handleSolicitarPago}
          disabled={loading}
          className="px-6 py-3 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
        >
          {loading ? 'Procesando...' : '💳 Ir a Pagar'}
        </button>
      ) : yaPagada ? (
        <div className="p-4 bg-green-100 text-green-700 rounded">
          ✓ Esta solicitud ya está pagada
        </div>
      ) : (
        <div className="p-4 bg-yellow-100 text-yellow-700 rounded">
          ℹ No es posible pagar esta solicitud en su estado actual
        </div>
      )}

      {/* Debug: Mostrar datos de pago (opcional) */}
      {process.env.NODE_ENV === 'development' && datosPago && (
        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded">
          <details>
            <summary>📊 Debug: Datos de Pago (click para expandir)</summary>
            <pre className="mt-2 text-sm overflow-auto">
              {JSON.stringify(datosPago, null, 2)}
            </pre>
          </details>
        </div>
      )}
    </div>
  );
}
```

---

## Logs Esperados en Consola del Navegador

### Cuando hace click en "Pagar"
```
[Frontend] Solicitando datos de pago...
[Frontend] Datos recibidos: {
  Comercio: "RDAM",
  TransaccionComercioId: "SOL-123",
  urlPasarela: "http://localhost:8081/pluspagos",
  ...
}
[Frontend] Redirigiendo a mock...
[Frontend] urlPasarela: http://localhost:8081/pluspagos
[Frontend] Enviando POST a pasarela...
```

---

## Logs Esperados en Server de Mock

```bash
docker compose logs -f pluspagos

# Salida:
[2026-03-18T...] POST /pluspagos
[POST /pluspagos] Recibiendo solicitud de pago encriptada...
[POST /pluspagos] Desencriptando datos...
[POST /pluspagos] ✓ Desencriptación exitosa
  Comercio: RDAM
  TransaccionId: SOL-123
  Monto: 500000 centavos
[POST /pluspagos] ✓ Página de confirmación enviada
```

---

## Logs Esperados Cuando Usuario Confirma

### Console del Navegador (debería redirect)
```
La página se redirige a http://localhost:5173/solicitudes/123?pago=success
```

### Consola del Mock
```
[2026-03-18T...] POST /pluspagos/confirmar
[POST /pluspagos/confirmar] Procesando confirmación de pago...
[POST /pluspagos/confirmar] Llamando al webhook:
  URL: http://app:8080/api/pagos/webhook
  Payload: {
    "Tipo": "PAGO",
    "TransaccionPlataformaId": "PP-MOCK-1234567890",
    "TransaccionComercioId": "SOL-123",
    "Monto": "5000.00",
    "EstadoId": "3",
    "Estado": "REALIZADA",
    "FechaProcesamiento": "2026-03-18T..."
  }
[POST /pluspagos/confirmar] ✓ Webhook llamado. Status: 200
[POST /pluspagos/confirmar] ✓ Redirigiendo a: http://localhost:5173/...
```

### Backend
```
[INFO] POST /api/pagos/webhook
[INFO] Procesando pago para transacción: SOL-123
[INFO] Estado actualizado a: PAGADA
[INFO] Response: 200 OK
```

---

## Alternativa: Custom Hook

Si prefieres encapsular la lógica:

```jsx
// hooks/usePago.js
import { useState } from 'react';
import api from '../api/axios';

export const usePago = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const solicitarPago = async (solicitudId) => {
    setLoading(true);
    setError(null);
    
    try {
      // Obtener datos encriptados del backend
      const { data } = await api.get(`/solicitudes/${solicitudId}/pago-datos`);
      
      // Redirigir a pasarela
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = data.urlPasarela;
      
      Object.entries(data).forEach(([key, value]) => {
        if (key !== 'urlPasarela') {
          const input = document.createElement('input');
          input.type = 'hidden';
          input.name = key;
          input.value = value;
          form.appendChild(input);
        }
      });
      
      document.body.appendChild(form);
      form.submit();
    } catch (err) {
      setError(err.message);
      console.error('Error en pago:', err);
    } finally {
      setLoading(false);
    }
  };

  return { solicitarPago, loading, error };
};

// Uso en componente:
// const { solicitarPago, loading, error } = usePago();
// <button onClick={() => solicitarPago(id)}>Pagar</button>
```

---

## Manejo del Redirect de Regreso

```jsx
// Al montar el componente, verificar parámetros de query
React.useEffect(() => {
  const params = new URLSearchParams(window.location.search);
  const pagoStatus = params.get('pago');

  if (pagoStatus === 'success') {
    // Pago exitoso - mostrar mensaje y recargar
    showSuccessMessage('¡Pago realizado!');
    reloadSolicitud(); // Traer solicitud actualizada
    
  } else if (pagoStatus === 'cancelled') {
    // Usuario canceló - mostrar mensaje
    showWarningMessage('Pago cancelado');
    // Podrían volver a intentar
  }
}, []);
```

---

## Consideraciones de UX

```jsx
// 1. Deshabilitar botón mientras se procesa
<button disabled={loading || !puedeHacerPago}>
  {loading ? 'Redirigiendo a pasarela...' : 'Ir a Pagar'}
</button>

// 2. Mostrar mensaje de espera
{loading && (
  <div className="fixed inset-0 bg-black/50 flex items-center justify-center">
    <div className="bg-white p-6 rounded">
      <p className="animate-spin">⏳</p>
      <p>Redirigiendo a pasarela de pago...</p>
    </div>
  </div>
)}

// 3. Validar antes de permitir pago
const puedeHacerPago = solicitud && [
  'PENDIENTE_PAGO',
  'PENDIENTE',
  'SIN_PROCESAR'
].includes(solicitud.estado);
```

---

## Testing en Desarrollo

### Verificar el flujo sin backend:

```bash
# Terminal 1: Mock server
docker compose up -d pluspagos
docker compose logs -f pluspagos

# Terminal 2: Frontend
npm run dev
# Ir a http://localhost:5173

# Terminal 3: Listener mock backend (en lugar de backend real)
node -e "
const http = require('http');
http.createServer((req, res) => {
  console.log('[MOCK BACKEND] Recibido webhook:', req.url);
  if (req.method === 'POST') {
    let body = '';
    req.on('data', c => body += c);
    req.on('end', () => {
      console.log(JSON.parse(body));
      res.end('200 OK');
    });
  }
}).listen(8080);
console.log('Mock backend en :8080');
"
```

Luego:
1. Click "Pagar"
2. Ver redirect a mock
3. Ver página de confirmación
4. Click "Confirmar"
5. Mock hace POST a mock backend
6. Ver "200 OK" en terminal 3
7. Redirección a URL success

---

## Notas Importantes

- ✓ El mock solo funciona si el **backend encripta correctamente**
- ✓ El secret debe ser igual en backend y mock
- ✓ El formulario POST automático redirige la página
- ✓ Es normal que veas un instante en blanco (es la redirección)
- ✓ En móvil también funciona (responsive HTML)
- ✓ Los logs son tu amigo para debuggear

---

**Última actualización:** 2026-03-18
