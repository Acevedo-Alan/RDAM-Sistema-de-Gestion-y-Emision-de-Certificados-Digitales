const http = require('http');
const crypto = require('crypto');
const url = require('url');
const querystring = require('querystring');

const PORT = process.env.PORT || 8081;
const PLUSPAGOS_SECRET = process.env.PLUSPAGOS_SECRET || 'mock-secret-desarrollo';
const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8080';

// Almacenamiento en memoria para datos de transacción
const transactions = new Map();

// ============================================================================
// Funciones de criptografía
// ============================================================================

function getKey(secret) {
  return crypto.createHash('sha256').update(secret).digest();
}

function decrypt(encryptedBase64, secret) {
  try {
    const combined = Buffer.from(encryptedBase64, 'base64');
    if (combined.length < 16) {
      throw new Error('Datos encriptados inválidos (menor a 16 bytes)');
    }

    const iv = combined.slice(0, 16);
    const ciphertext = combined.slice(16);
    const key = getKey(secret);

    const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
    let decrypted = decipher.update(ciphertext, undefined, 'utf8');
    decrypted += decipher.final('utf8');

    return decrypted;
  } catch (error) {
    console.error('Error desencriptando:', error.message);
    throw error;
  }
}

// ============================================================================
// Funciones auxiliares
// ============================================================================

function formatPeso(centavos) {
  const pesos = parseInt(centavos) / 100;
  return pesos.toFixed(2);
}

function getCurrentISOTimestamp() {
  return new Date().toISOString();
}

function parseFormData(body) {
  return querystring.parse(body);
}

// ============================================================================
// Funciones HTTP
// ============================================================================

function makeHttpRequest(targetUrl, payload) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(targetUrl);
    const postData = JSON.stringify(payload);

    const options = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port || (parsedUrl.protocol === 'https:' ? 443 : 80),
      path: parsedUrl.pathname + parsedUrl.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData),
      },
    };

    const protocol = parsedUrl.protocol === 'https:' ? require('https') : http;

    const req = protocol.request(options, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          body: data,
        });
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    req.write(postData);
    req.end();
  });
}

// ============================================================================
// Generadores de HTML
// ============================================================================

function generateConfirmationPageHTML(monto, descripcion, transaccionId) {
  const montoFormateado = `$${formatPeso(monto)} ARS`;

  return `<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Portal de Pagos Seguro - PlusPagos</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: white;
            height: 100vh;
            overflow: hidden;
        }
        
        .root-container {
            display: flex;
            height: 100vh;
        }
        
        .branding-column {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            gap: 48px;
            width: 50%;
            background: linear-gradient(135deg, #0F4A7C 0%, #0A3456 100%);
            padding: 60px 40px;
            flex-shrink: 0;
        }
        
        .branding-content {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
        }
        
        .branding-title {
            font-size: 32px;
            font-weight: 700;
            line-height: 1.2;
            color: white;
            letter-spacing: -0.5px;
        }
        
        .branding-subtitle {
            font-size: 14px;
            font-weight: 400;
            line-height: 1.6;
            color: rgba(255, 255, 255, 0.8);
        }
        
        .branding-footer {
            position: absolute;
            bottom: 40px;
            left: 40px;
            font-size: 12px;
            color: rgba(255, 255, 255, 0.5);
            font-weight: 400;
            letter-spacing: 0.3px;
        }
        
        .form-column {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            flex: 1;
            background: white;
            padding: 40px;
            position: relative;
            overflow-y: auto;
        }
        
        .form-wrapper {
            width: 100%;
            max-width: 450px;
            animation: slideInRight 0.4s ease-out;
        }
        
        @keyframes slideInRight {
            from {
                opacity: 0;
                transform: translateX(20px);
            }
            to {
                opacity: 1;
                transform: translateX(0);
            }
        }
        
        .form-header {
            margin-bottom: 32px;
        }
        
        .form-title {
            font-size: 24px;
            font-weight: 700;
            color: #1B1B1B;
            margin-bottom: 8px;
            letter-spacing: -0.5px;
        }
        
        .form-subtitle {
            font-size: 13px;
            color: #A9AEB1;
            font-weight: 400;
            display: block;
            text-align: center;
            line-height: 1.5;
        }
        
        .amount-highlight {
            background: #F0F4F9;
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 32px;
            text-align: center;
        }
        
        .amount-label {
            font-size: 12px;
            font-weight: 600;
            color: #6B7280;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 8px;
            display: block;
        }
        
        .amount-value {
            font-size: 36px;
            font-weight: 700;
            color: #059669;
            letter-spacing: -1px;
        }
        
        .form-section {
            margin-bottom: 24px;
        }
        
        .form-label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            margin-bottom: 8px;
            letter-spacing: 0.3px;
        }
        
        .form-input {
            width: 100%;
            padding: 12px 16px;
            font-size: 14px;
            border: 1px solid #D1D5DB;
            border-radius: 8px;
            font-family: 'Roboto', sans-serif;
            transition: all 0.2s ease;
            background: white;
            color: #1F2937;
        }
        
        .form-input:focus {
            outline: none;
            border-color: #0F4A7C;
            box-shadow: 0 0 0 3px rgba(15, 74, 124, 0.1);
        }
        
        .form-input::placeholder {
            color: #9CA3AF;
        }
        
        .form-row {
            display: flex;
            gap: 16px;
        }
        
        .form-col {
            flex: 1;
        }
        
        .transaction-info {
            background: #FAFBFC;
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 32px;
            border-left: 4px solid #0F4A7C;
        }
        
        .transaction-label {
            font-size: 12px;
            font-weight: 600;
            color: #6B7280;
            margin-bottom: 4px;
            letter-spacing: 0.3px;
            text-transform: uppercase;
        }
        
        .transaction-value {
            font-size: 13px;
            color: #1F2937;
            font-weight: 500;
            word-break: break-all;
        }
        
        .transaction-description {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px solid #E5E7EB;
        }
        
        .transaction-desc-label {
            font-size: 11px;
            font-weight: 600;
            color: #6B7280;
            margin-bottom: 6px;
            letter-spacing: 0.3px;
            text-transform: uppercase;
        }
        
        .transaction-desc-value {
            font-size: 13px;
            color: #374151;
            font-weight: 400;
            line-height: 1.5;
        }
        
        .button-group {
            display: flex;
            gap: 12px;
            margin-bottom: 24px;
        }
        
        .btn-primary {
            flex: 1;
            padding: 16px 24px;
            background: #059669;
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            box-shadow: 0 4px 12px rgba(5, 150, 105, 0.3);
        }
        
        .btn-primary:hover {
            background: #047857;
            box-shadow: 0 8px 24px rgba(5, 150, 105, 0.4);
            transform: translateY(-2px);
        }
        
        .btn-primary:active {
            transform: translateY(0);
        }
        
        .btn-secondary {
            flex: 1;
            padding: 16px 24px;
            background: #F3F4F6;
            color: #374151;
            border: 1px solid #D1D5DB;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .btn-secondary:hover {
            background: #E5E7EB;
            border-color: #9CA3AF;
        }
        
        .btn-secondary:active {
            transform: translateY(0);
        }
        
        .cancel-link {
            position: absolute;
            bottom: 32px;
            left: 50%;
            transform: translateX(-50%);
            font-size: 13px;
            color: #71767A;
            text-decoration: none;
            font-weight: 500;
            transition: color 0.2s ease;
            white-space: nowrap;
        }
        
        .cancel-link:hover {
            color: #0F4A7C;
        }
        
        @media (max-width: 1024px) {
            .branding-column {
                display: none;
            }
            
            .root-container {
                flex-direction: column;
            }
            
            .form-column {
                width: 100%;
            }
        }
        
        @media (max-width: 600px) {
            .form-column {
                padding: 24px;
            }
            
            .form-wrapper {
                max-width: 100%;
            }
            
            .form-title {
                font-size: 20px;
            }
            
            .amount-value {
                font-size: 28px;
            }
            
            .branding-column {
                padding: 32px 24px;
                gap: 32px;
            }
            
            .branding-title {
                font-size: 24px;
            }
        }
    </style>
</head>
<body>
    <div class="root-container">
        <div class="branding-column">
            <div class="branding-content">
                <div class="branding-title">Portal de Pagos Seguro</div>
                <div class="branding-subtitle">Estas a un paso de completar tu tramite en RDAM</div>
            </div>
            <div class="branding-footer">Mock de Integracion PlusPagos — Entorno de Desarrollo</div>
        </div>
        
        <div class="form-column">
            <div class="form-wrapper">
                <div class="form-header">
                    <div class="form-title">Detalles del Pago</div>
                    <span class="form-subtitle">Sistema de Recaudacion de la Provincia de Santa Fe</span>
                </div>
                
                <div class="amount-highlight">
                    <span class="amount-label">Monto a pagar</span>
                    <div class="amount-value">${montoFormateado}</div>
                </div>
                
                <div class="transaction-info">
                    <div class="transaction-label">ID de Transaccion</div>
                    <div class="transaction-value">${transaccionId}</div>
                    
                    <div class="transaction-description">
                        <div class="transaction-desc-label">Descripcion</div>
                        <div class="transaction-desc-value">${descripcion}</div>
                    </div>
                </div>
                
                <form id="paymentForm" method="POST" action="/pluspagos/confirmar" style="display: none;">
                    <input type="hidden" name="transaccionId" value="${transaccionId}">
                </form>
                
                <form id="cancelForm" method="POST" action="/pluspagos/cancelar" style="display: none;">
                    <input type="hidden" name="transaccionId" value="${transaccionId}">
                </form>
                
                <div class="button-group">
                    <button class="btn-primary" onclick="document.getElementById('paymentForm').submit(); return false;">
                        Confirmar Pago
                    </button>
                    <button class="btn-secondary" onclick="document.getElementById('cancelForm').submit(); return false;">
                        Cancelar
                    </button>
                </div>
            </div>
            
            <a href="#" class="cancel-link" onclick="document.getElementById('cancelForm').submit(); return false;">
                Cancelar y volver al sistema RDAM
            </a>
        </div>
    </div>
</body>
</html>`;
}

function generateErrorPageHTML(titulo, mensaje) {
  return `<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error - Portal de Pagos</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: white;
            height: 100vh;
            overflow: hidden;
        }
        
        .root-container {
            display: flex;
            height: 100vh;
        }
        
        .branding-column {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            gap: 48px;
            width: 50%;
            background: linear-gradient(135deg, #DC2626 0%, #991B1B 100%);
            padding: 60px 40px;
            flex-shrink: 0;
        }
        
        .branding-content {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
        }
        
        .branding-title {
            font-size: 32px;
            font-weight: 700;
            line-height: 1.2;
            color: white;
            letter-spacing: -0.5px;
        }
        
        .branding-subtitle {
            font-size: 14px;
            font-weight: 400;
            line-height: 1.6;
            color: rgba(255, 255, 255, 0.8);
        }
        
        .branding-footer {
            position: absolute;
            bottom: 40px;
            left: 40px;
            font-size: 12px;
            color: rgba(255, 255, 255, 0.5);
            font-weight: 400;
            letter-spacing: 0.3px;
        }
        
        .form-column {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            flex: 1;
            background: white;
            padding: 40px;
            position: relative;
            overflow-y: auto;
        }
        
        .error-wrapper {
            width: 100%;
            max-width: 450px;
            text-align: center;
            animation: slideInRight 0.4s ease-out;
        }
        
        @keyframes slideInRight {
            from {
                opacity: 0;
                transform: translateX(20px);
            }
            to {
                opacity: 1;
                transform: translateX(0);
            }
        }
        
        .error-icon {
            font-size: 64px;
            margin-bottom: 24px;
            display: block;
        }
        
        .error-title {
            font-size: 24px;
            font-weight: 700;
            color: #DC2626;
            margin-bottom: 12px;
            letter-spacing: -0.5px;
        }
        
        .error-message {
            font-size: 14px;
            color: #6B7280;
            line-height: 1.6;
            margin-bottom: 32px;
            font-weight: 400;
        }
        
        .error-code {
            background: #FEF2F2;
            border-radius: 8px;
            padding: 16px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            color: #7F1D1D;
            margin-bottom: 32px;
            word-break: break-all;
            border-left: 4px solid #DC2626;
        }
        
        .error-actions {
            display: flex;
            gap: 12px;
        }
        
        .btn-retry {
            flex: 1;
            padding: 12px 24px;
            background: #DC2626;
            color: white;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
        }
        
        .btn-retry:hover {
            background: #991B1B;
            box-shadow: 0 8px 24px rgba(220, 38, 38, 0.4);
            transform: translateY(-2px);
        }
        
        .btn-back {
            flex: 1;
            padding: 12px 24px;
            background: #F3F4F6;
            color: #374151;
            border: 1px solid #D1D5DB;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: all 0.2s ease;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .btn-back:hover {
            background: #E5E7EB;
            border-color: #9CA3AF;
        }
        
        @media (max-width: 1024px) {
            .branding-column {
                display: none;
            }
            
            .root-container {
                flex-direction: column;
            }
            
            .form-column {
                width: 100%;
            }
        }
        
        @media (max-width: 600px) {
            .form-column {
                padding: 24px;
            }
            
            .error-wrapper {
                max-width: 100%;
            }
            
            .error-title {
                font-size: 20px;
            }
            
            .branding-column {
                padding: 32px 24px;
                gap: 32px;
            }
            
            .branding-title {
                font-size: 24px;
            }
        }
    </style>
</head>
<body>
    <div class="root-container">
        <div class="branding-column">
            <div class="branding-content">
                <div class="branding-title">Portal de Pagos Seguro</div>
                <div class="branding-subtitle">Estas a un paso de completar tu tramite en RDAM</div>
            </div>
            <div class="branding-footer">Mock de Integracion PlusPagos — Entorno de Desarrollo</div>
        </div>
        
        <div class="form-column">
            <div class="error-wrapper">
                <span class="error-icon">!</span>
                <div class="error-title">${titulo}</div>
                <p class="error-message">${mensaje}</p>
                <div class="error-code">${new Date().toISOString()}</div>
                
                <div class="error-actions">
                    <button class="btn-retry" onclick="window.location.reload();">
                        Reintentar
                    </button>
                    <button class="btn-back" onclick="window.history.back();">
                        Volver
                    </button>
                </div>
            </div>
        </div>
    </div>
</body>
</html>`;
}

// ============================================================================
// POST /pluspagos
// Recibe datos encriptados y muestra página de confirmación
// ============================================================================
async function handlePlusPageosPost(req, res, body) {
  console.log('[POST /pluspagos] Recibiendo solicitud de pago encriptada...');

  try {
    const data = parseFormData(body);

    // Validar campos requeridos
    const requiredFields = [
      'Comercio',
      'TransaccionComercioId',
      'Monto',
      'Informacion',
      'CallbackSuccess',
      'UrlSuccess',
      'UrlError',
    ];

    for (const field of requiredFields) {
      if (!data[field]) {
        throw new Error(`Campo requerido faltante: ${field}`);
      }
    }

    console.log(`[POST /pluspagos] Desencriptando datos...`);

    // Desencriptar todos los campos encriptados
    const monto = decrypt(data.Monto, PLUSPAGOS_SECRET);
    const informacion = decrypt(data.Informacion, PLUSPAGOS_SECRET);
    const callbackSuccess = decrypt(data.CallbackSuccess, PLUSPAGOS_SECRET);
    const callbackCancel = data.CallbackCancel
      ? decrypt(data.CallbackCancel, PLUSPAGOS_SECRET)
      : null;
    const urlSuccess = decrypt(data.UrlSuccess, PLUSPAGOS_SECRET);
    const urlError = decrypt(data.UrlError, PLUSPAGOS_SECRET);

    console.log(`[POST /pluspagos] ✓ Desencriptación exitosa`);
    console.log(`  Comercio: ${data.Comercio}`);
    console.log(`  TransaccionId: ${data.TransaccionComercioId}`);
    console.log(`  Monto: ${monto} centavos`);
    console.log(`  CallbackSuccess: ${callbackSuccess}`);
    console.log(`  UrlSuccess: ${urlSuccess}`);
    console.log(`  UrlError: ${urlError}`);

    // Almacenar en memoria
    transactions.set(data.TransaccionComercioId, {
      comercio: data.Comercio,
      transaccionComercioId: data.TransaccionComercioId,
      monto,
      informacion,
      callbackSuccess,
      callbackCancel,
      urlSuccess,
      urlError,
      timestamp: Date.now(),
    });

    // Generar página de confirmación
    const html = generateConfirmationPageHTML(
      monto,
      informacion,
      data.TransaccionComercioId
    );

    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);

    console.log(`[POST /pluspagos] ✓ Página de confirmación enviada`);
  } catch (error) {
    console.error(`[POST /pluspagos] ✗ Error:`, error.message);

    const html = generateErrorPageHTML(
      'Error en Desencriptación',
      `No se pudo procesar la solicitud de pago: ${error.message}`
    );

    res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
  }
}

// ============================================================================
// POST /pluspagos/confirmar
// Procesa el pago, llama al webhook del backend y redirige
// ============================================================================
async function handleConfirmarPost(req, res, body) {
  console.log('[POST /pluspagos/confirmar] Procesando confirmación de pago...');

  try {
    const data = parseFormData(body);
    const transaccionId = data.transaccionId;

    if (!transaccionId) {
      throw new Error('ID de transacción no proporcionado');
    }

    const transaction = transactions.get(transaccionId);
    if (!transaction) {
      throw new Error(`Transacción no encontrada: ${transaccionId}`);
    }

    const montoArs = formatPeso(transaction.monto);
    const pluspagosTransaccionId = `PP-MOCK-${Date.now()}`;

    const webhookPayload = {
      Tipo: 'PAGO',
      TransaccionPlataformaId: pluspagosTransaccionId,
      TransaccionComercioId: transaction.transaccionComercioId,
      Monto: montoArs,
      EstadoId: '3',
      Estado: 'REALIZADA',
      FechaProcesamiento: getCurrentISOTimestamp(),
    };

    console.log(`[POST /pluspagos/confirmar] Llamando al webhook:`);
    console.log(`  URL: ${transaction.callbackSuccess}`);
    console.log(`  Payload:`, JSON.stringify(webhookPayload, null, 2));

    // Llamar al webhook del backend
    try {
      const response = await makeHttpRequest(
        transaction.callbackSuccess,
        webhookPayload
      );
      console.log(
        `[POST /pluspagos/confirmar] ✓ Webhook llamado. Status: ${response.statusCode}`
      );
    } catch (webhookError) {
      console.warn(
        `[POST /pluspagos/confirmar] ⚠ Error llamando webhook:`,
        webhookError.message
      );
      // Continuamos de todas formas, ya que el mock puede hacer redirect sin webhook
    }

    // Limpiar transacción
    transactions.delete(transaccionId);

    // Redirigir a URL de éxito
    console.log(
      `[POST /pluspagos/confirmar] ✓ Redirigiendo a: ${transaction.urlSuccess}`
    );

    res.writeHead(302, {
      Location: transaction.urlSuccess,
    });
    res.end();
  } catch (error) {
    console.error(`[POST /pluspagos/confirmar] ✗ Error:`, error.message);

    const html = generateErrorPageHTML(
      'Error Procesando Pago',
      `No se pudo procesar el pago: ${error.message}`
    );

    res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
  }
}

// ============================================================================
// POST /pluspagos/cancelar
// Redirige a URL de cancelación
// ============================================================================
async function handleCancelarPost(req, res, body) {
  console.log('[POST /pluspagos/cancelar] Procesando cancelación de pago...');

  try {
    const data = parseFormData(body);
    const transaccionId = data.transaccionId;

    if (!transaccionId) {
      throw new Error('ID de transacción no proporcionado');
    }

    const transaction = transactions.get(transaccionId);
    if (!transaction) {
      throw new Error(`Transacción no encontrada: ${transaccionId}`);
    }

    console.log(`[POST /pluspagos/cancelar] Transacción cancelada: ${transaccionId}`);
    console.log(`[POST /pluspagos/cancelar] Redirigiendo a: ${transaction.urlError}`);

    // Limpiar transacción
    transactions.delete(transaccionId);

    // Redirigir a URL de error/cancelación
    res.writeHead(302, {
      Location: transaction.urlError,
    });
    res.end();
  } catch (error) {
    console.error(`[POST /pluspagos/cancelar] ✗ Error:`, error.message);

    const html = generateErrorPageHTML(
      'Error en Cancelación',
      `No se pudo procesar la cancelación: ${error.message}`
    );

    res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);
  }
}

// ============================================================================
// Servidor HTTP
// ============================================================================

const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const method = req.method;

  // Log de cada request
  console.log(`\n[${new Date().toISOString()}] ${method} ${pathname}`);

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Leer body
  let body = '';
  req.on('data', (chunk) => {
    body += chunk.toString();
  });

  req.on('end', async () => {
    try {
      if (pathname === '/pluspagos' && method === 'POST') {
        await handlePlusPageosPost(req, res, body);
      } else if (pathname === '/pluspagos/confirmar' && method === 'POST') {
        await handleConfirmarPost(req, res, body);
      } else if (pathname === '/pluspagos/cancelar' && method === 'POST') {
        await handleCancelarPost(req, res, body);
      } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
      }
    } catch (error) {
      console.error('[Error interno]', error);
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end('500 Internal Server Error');
    }
  });
});

server.listen(PORT, () => {
  console.log(`\n${'='.repeat(70)}`);
  console.log(`✓ Mock PlusPagos Server escuchando en puerto ${PORT}`);
  console.log(`✓ PLUSPAGOS_SECRET: ${PLUSPAGOS_SECRET}`);
  console.log(`✓ BACKEND_URL: ${BACKEND_URL}`);
  console.log(`${'='.repeat(70)}\n`);
});

server.on('error', (error) => {
  console.error('Error en servidor:', error);
  process.exit(1);
});

process.on('SIGTERM', () => {
  console.log('\n[SIGTERM] Cerrando servidor...');
  server.close(() => {
    console.log('✓ Servidor cerrado');
    process.exit(0);
  });
});

process.on('SIGINT', () => {
  console.log('\n[SIGINT] Cerrando servidor...');
  server.close(() => {
    console.log('✓ Servidor cerrado');
    process.exit(0);
  });
});
