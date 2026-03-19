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
  const montoNum = parseFloat(formatPeso(monto));
  const tasaServicio = (montoNum * 0.02).toFixed(2);
  const totalConTasa = (montoNum + parseFloat(tasaServicio)).toFixed(2);

  return `<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PlusPagos — Portal de Pagos</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: white;
            height: 100vh;
            overflow: hidden;
        }
        .root { display: flex; height: 100vh; }

        /* ── Columna izquierda ── */
        .left {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: flex-start;
            width: 40%;
            background: linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%);
            padding: 48px;
            gap: 48px;
            flex-shrink: 0;
        }
        .logo-wrap { display: flex; align-items: center; gap: 12px; }
        .logo-box {
            width: 36px; height: 36px; background: white;
            border-radius: 6px; display: flex; align-items: center; justify-content: center;
        }
        .logo-r { color: #005EA2; font-weight: 700; font-size: 20px; }
        .logo-name { color: white; font-weight: 700; font-size: 18px; }
        .left-title { color: white; font-weight: 700; font-size: 28px; line-height: 1.3; }
        .left-sub { color: rgba(255,255,255,0.75); font-size: 14px; margin-top: 8px; line-height: 1.6; }
        .bullets { display: flex; flex-direction: column; gap: 12px; }
        .bullet { display: flex; align-items: center; gap: 12px; }
        .dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.5); flex-shrink: 0; }
        .bullet-text { color: rgba(255,255,255,0.85); font-size: 13px; }
        .left-footer { color: rgba(255,255,255,0.5); font-size: 11px; margin-top: 8px; }

        /* ── Columna derecha ── */
        .right {
            flex: 1;
            background: white;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 48px;
            overflow-y: auto;
        }
        .form-wrap { width: 100%; max-width: 420px; }

        .page-title { font-size: 28px; font-weight: 700; color: #1B1B1B; margin-bottom: 4px; }
        .page-sub { font-size: 14px; color: #71767A; margin-bottom: 4px; }
        .page-caption { font-size: 11px; color: #A9AEB1; text-align: center; margin-bottom: 28px; }

        /* Tarjeta resumen de monto */
        .amount-card {
            background: #F0F4F8;
            border-radius: 12px;
            border: 1px solid #DFE1E2;
            padding: 20px 24px;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .amount-label { font-size: 12px; font-weight: 600; color: #71767A; text-transform: uppercase; letter-spacing: 0.4px; }
        .amount-value { font-size: 32px; font-weight: 700; color: #1B1B1B; letter-spacing: -1px; margin-top: 4px; }
        .amount-badge {
            background: #E6F4EA; color: #1A6330;
            font-size: 11px; font-weight: 600;
            padding: 4px 10px; border-radius: 20px;
        }

        /* Info transacción */
        .txn-box {
            background: #F8F9FA;
            border-radius: 8px;
            border: 1px solid #DFE1E2;
            border-left: 4px solid #005EA2;
            padding: 16px;
            margin-bottom: 20px;
        }
        .txn-label { font-size: 11px; font-weight: 600; color: #71767A; text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 3px; }
        .txn-value { font-size: 13px; color: #1B1B1B; font-weight: 500; word-break: break-all; }
        .txn-divider { border: none; border-top: 1px solid #DFE1E2; margin: 12px 0; }

        /* Resumen pago */
        .resumen-section { margin-bottom: 20px; }
        .section-label { font-size: 11px; font-weight: 600; color: #71767A; text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 10px; }
        .resumen-box { background: #F0F4F8; border-radius: 8px; border: 1px solid #DFE1E2; padding: 14px 16px; }
        .resumen-row { display: flex; justify-content: space-between; font-size: 13px; color: #565C65; padding: 3px 0; }
        .resumen-total {
            display: flex; justify-content: space-between;
            font-size: 15px; font-weight: 700; color: #1B1B1B;
            border-top: 1px solid #DFE1E2; margin-top: 8px; padding-top: 10px;
        }

        /* Botones */
        .btn-confirm {
            width: 100%; padding: 14px;
            background: #005EA2; color: white;
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 600;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,94,162,0.3);
            transition: background 0.15s;
            margin-bottom: 10px;
        }
        .btn-confirm:hover { background: #0F4A7C; }
        .btn-cancel {
            width: 100%; padding: 12px;
            background: white; color: #71767A;
            border: 1px solid #DFE1E2; border-radius: 8px;
            font-size: 14px; font-weight: 500;
            cursor: pointer;
            transition: background 0.15s;
        }
        .btn-cancel:hover { background: #F0F0F0; }

        /* Seguridad */
        .security { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 16px; }
        .security-icon { font-size: 13px; }
        .security-text { font-size: 11px; color: #A9AEB1; }
        .badge-ssl { background: #E6F4EA; color: #1A6330; font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }

        /* Campo de tarjeta */
        .card-section { margin-bottom: 20px; }
        .card-input-wrap {
            position: relative;
            display: flex;
            align-items: center;
        }
        .card-icon {
            position: absolute;
            right: 12px;
            display: flex;
            align-items: center;
            pointer-events: none;
        }
        .card-input {
            width: 100%;
            padding: 12px 44px 12px 14px;
            border: 1px solid #DFE1E2;
            border-radius: 8px;
            font-size: 16px;
            font-family: 'Segoe UI', monospace;
            letter-spacing: 1.5px;
            color: #1B1B1B;
            background: white;
            outline: none;
            transition: box-shadow 0.15s, border-color 0.15s;
        }
        .card-input:focus {
            border-color: #005EA2;
            box-shadow: 0 0 0 3px rgba(0,94,162,0.12);
        }
        .card-input::placeholder { color: #A9AEB1; letter-spacing: 1.5px; }
        .card-hint { font-size: 11px; color: #A9AEB1; margin-top: 6px; }

        /* Campos del formulario */
        .form-field { margin-bottom: 16px; }
        .form-label { display: block; font-size: 11px; font-weight: 600; color: #71767A; text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 6px; }
        .form-input {
            width: 100%; padding: 12px 14px;
            border: 1px solid #DFE1E2; border-radius: 8px;
            font-size: 14px; color: #1B1B1B;
            background: white; outline: none;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            transition: box-shadow 0.15s, border-color 0.15s;
        }
        .form-input:focus { border-color: #005EA2; box-shadow: 0 0 0 3px rgba(0,94,162,0.12); }
        .form-input::placeholder { color: #A9AEB1; }
        .form-row { display: flex; gap: 12px; margin-bottom: 16px; }
        .form-row .form-field { flex: 1; margin-bottom: 0; }
        .form-error { color: #D42A2A; font-size: 13px; margin-bottom: 12px; display: none; }

        /* Loading state */
        .loading-overlay {
            display: none;
            position: fixed; inset: 0;
            background: rgba(255,255,255,0.85);
            align-items: center; justify-content: center;
            flex-direction: column; gap: 16px;
            z-index: 100;
        }
        .spinner {
            width: 40px; height: 40px;
            border: 3px solid #DFE1E2;
            border-top-color: #005EA2;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }
        .loading-text { font-size: 14px; color: #71767A; font-weight: 500; }

        @media (max-width: 900px) {
            .left { display: none; }
            .root { flex-direction: column; }
            .right { width: 100%; padding: 24px; }
        }
    </style>
</head>
<body>
    <div class="loading-overlay" id="loadingOverlay">
        <div class="spinner"></div>
        <div class="loading-text">Procesando pago...</div>
    </div>

    <div class="root">
        <div class="left">
            <div class="logo-wrap">
                <div class="logo-box"><span class="logo-r">R</span></div>
                <span class="logo-name">RDAM</span>
            </div>
            <div>
                <div class="left-title">Sistema de Gestion de Certificados Digitales</div>
                <div class="left-sub">Plataforma oficial para la gestion integral de certificados. Pago seguro y verificado.</div>
            </div>
            <div class="bullets">
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Transaccion cifrada con TLS</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Certificado emitido al instante</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Comprobante enviado por email</span></div>
                <div class="left-footer">Poder Judicial de la Provincia de Santa Fe — 2026</div>
            </div>
        </div>

        <div class="right">
            <div class="form-wrap">
                <div class="page-title">PlusPagos</div>
                <div class="page-sub">Confirma los detalles antes de pagar</div>
                <div class="page-caption">Registro de Actos y Documentos del Ambito de la Magistratura</div>

                <!-- Monto destacado -->
                <div class="amount-card">
                    <div>
                        <div class="amount-label">Total a pagar</div>
                        <div class="amount-value">$${totalConTasa} ARS</div>
                    </div>
                    <div class="amount-badge">Pago seguro</div>
                </div>

                <!-- Info de transaccion -->
                <div class="txn-box">
                    <div class="txn-label">ID de Transaccion</div>
                    <div class="txn-value">${transaccionId}</div>
                    <hr class="txn-divider">
                    <div class="txn-label">Descripcion</div>
                    <div class="txn-value">${descripcion}</div>
                </div>

                <!-- Resumen -->
                <div class="resumen-section">
                    <div class="section-label">Resumen del pago</div>
                    <div class="resumen-box">
                        <div class="resumen-row"><span>Monto del tramite</span><span>$${montoNum.toFixed(2)} ARS</span></div>
                        <div class="resumen-row"><span>Tasa de servicio (2%)</span><span>$${tasaServicio} ARS</span></div>
                        <div class="resumen-total"><span>Total</span><span>$${totalConTasa} ARS</span></div>
                    </div>
                </div>

                <!-- Datos de la tarjeta -->
                <div class="card-section">
                    <div class="section-label">Datos de la tarjeta</div>

                    <!-- Número de tarjeta -->
                    <div class="form-field">
                        <div class="card-input-wrap">
                            <input
                                type="text"
                                id="cardNumber"
                                class="card-input"
                                placeholder="0000 0000 0000 0000"
                                maxlength="19"
                                autocomplete="off"
                                inputmode="numeric"
                                pattern="[0-9]*"
                            />
                            <div class="card-icon" id="cardIcon">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                                    <rect x="2" y="5" width="20" height="14" rx="2" stroke="#A9AEB1" stroke-width="1.5"/>
                                    <line x1="2" y1="10" x2="22" y2="10" stroke="#A9AEB1" stroke-width="1.5"/>
                                    <rect x="5" y="13" width="6" height="2" rx="1" fill="#DFE1E2"/>
                                </svg>
                            </div>
                        </div>
                        <div class="card-hint">Solo visual — el número no se envía al servidor</div>
                    </div>

                    <!-- Nombre y Apellido -->
                    <div class="form-field">
                        <label class="form-label">Nombre y Apellido</label>
                        <input type="text" id="cardName" class="form-input" placeholder="Tal como figura en la tarjeta" oninput="this.value = this.value.toUpperCase()" />
                    </div>

                    <!-- Vencimiento / CVC / DNI -->
                    <div class="form-row">
                        <div class="form-field">
                            <label class="form-label">Vencimiento</label>
                            <input type="text" id="cardExpiry" class="form-input" placeholder="MM/AA" maxlength="5" inputmode="numeric" pattern="[0-9]*" autocomplete="off" />
                        </div>
                        <div class="form-field">
                            <label class="form-label">CVC</label>
                            <input type="text" id="cardCvc" class="form-input" placeholder="•••" maxlength="3" inputmode="numeric" pattern="[0-9]*" autocomplete="off" />
                        </div>
                        <div class="form-field">
                            <label class="form-label">DNI del Titular</label>
                            <input type="text" id="cardDni" class="form-input" placeholder="12345678" maxlength="8" inputmode="numeric" />
                        </div>
                    </div>

                    <div class="form-error" id="formError">Completá todos los campos de la tarjeta para continuar.</div>
                </div>

                <!-- Formularios ocultos -->
                <form id="paymentForm" method="POST" action="/pluspagos/confirmar">
                    <input type="hidden" name="transaccionId" value="${transaccionId}">
                </form>
                <form id="cancelForm" method="POST" action="/pluspagos/cancelar">
                    <input type="hidden" name="transaccionId" value="${transaccionId}">
                </form>

                <!-- Botones -->
                <button class="btn-confirm" onclick="confirmarPago()">
                    Confirmar y pagar $${totalConTasa} ARS
                </button>
                <button class="btn-cancel" onclick="document.getElementById('cancelForm').submit()">
                    Cancelar y volver al sistema RDAM
                </button>

                <div class="security">
                    <span class="security-icon">🔒</span>
                    <span class="security-text">Transaccion cifrada y segura</span>
                    <span class="badge-ssl">SSL</span>
                </div>
            </div>
        </div>
    </div>

    <script>
        var ICON_GENERIC = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none"><rect x="2" y="5" width="20" height="14" rx="2" stroke="#A9AEB1" stroke-width="1.5"/><line x1="2" y1="10" x2="22" y2="10" stroke="#A9AEB1" stroke-width="1.5"/><rect x="5" y="13" width="6" height="2" rx="1" fill="#DFE1E2"/></svg>';
        var ICON_VISA = '<svg width="24" height="24" viewBox="0 0 48 32" fill="none"><rect width="48" height="32" rx="4" fill="#1A1F71"/><path d="M19.5 21H17L18.9 11H21.4L19.5 21ZM15.2 11L12.8 18L12.5 16.5L11.6 12C11.6 12 11.5 11 10.2 11H6.1L6 11.2C6 11.2 7.5 11.5 9.2 12.5L11.4 21H14L18 11H15.2ZM36 21H38.5L36.3 11H34.3C33.2 11 32.9 11.8 32.9 11.8L29 21H31.5L32 19.5H35.1L35.4 21H36ZM32.8 17.5L34.1 13.7L34.9 17.5H32.8ZM28.5 13.5L28.8 11.8C28.8 11.8 27.5 11.3 26.1 11.3C24.6 11.3 21.5 12 21.5 14.7C21.5 17.2 25 17.2 25 18.5C25 19.8 21.9 19.5 20.7 18.7L20.4 20.5C20.4 20.5 21.7 21.1 23.5 21.1C25.3 21.1 28.2 20.1 28.2 17.6C28.2 15 24.7 14.8 24.7 13.7C24.7 12.6 27.1 12.8 28.5 13.5Z" fill="white"/></svg>';
        var ICON_MC = '<svg width="24" height="24" viewBox="0 0 48 32" fill="none"><rect width="48" height="32" rx="4" fill="#252525"/><circle cx="19" cy="16" r="9" fill="#EB001B"/><circle cx="29" cy="16" r="9" fill="#F79E1B"/><path d="M24 9.3A9 9 0 0 1 27.5 16A9 9 0 0 1 24 22.7A9 9 0 0 1 20.5 16A9 9 0 0 1 24 9.3Z" fill="#FF5F00"/></svg>';

        // Número de tarjeta — formateo y detección de franquicia
        document.getElementById('cardNumber').addEventListener('input', function() {
            var digits = this.value.replace(/\D/g, '').slice(0, 16);
            var formatted = digits.replace(/(\d{4})(?=\d)/g, '$1 ');
            if (this.value !== formatted) {
                var pos = this.selectionStart;
                this.value = formatted;
                this.selectionStart = pos;
                this.selectionEnd = pos;
            }

            var icon = document.getElementById('cardIcon');
            if (digits.charAt(0) === '4') {
                icon.innerHTML = ICON_VISA;
            } else if (digits.charAt(0) === '5') {
                icon.innerHTML = ICON_MC;
            } else {
                icon.innerHTML = ICON_GENERIC;
            }
        });

        // Vencimiento — auto-slash al tercer carácter (sin duplicar)
        document.getElementById('cardExpiry').addEventListener('input', function() {
            var val = this.value.replace(/\D/g, '').slice(0, 4);
            if (val.length >= 3) {
                var month = val.slice(0, 2);
                var year = val.slice(2);
                this.value = month + '/' + year;
            } else {
                this.value = val;
            }
        });

        // CVC — solo dígitos
        document.getElementById('cardCvc').addEventListener('input', function() {
            this.value = this.value.replace(/\D/g, '').slice(0, 3);
        });

        // DNI — solo dígitos
        document.getElementById('cardDni').addEventListener('input', function() {
            this.value = this.value.replace(/\D/g, '').slice(0, 8);
        });

        function confirmarPago() {
            var cardNum = document.getElementById('cardNumber').value.replace(/\s/g, '');
            var cardName = document.getElementById('cardName').value.trim();
            var cardExpiry = document.getElementById('cardExpiry').value.trim();
            var cardCvc = document.getElementById('cardCvc').value.trim();
            var cardDni = document.getElementById('cardDni').value.trim();

            if (!cardNum || !cardName || !cardExpiry || !cardCvc || !cardDni) {
                document.getElementById('formError').style.display = 'block';
                return;
            }
            document.getElementById('formError').style.display = 'none';
            document.getElementById('loadingOverlay').style.display = 'flex';
            document.getElementById('paymentForm').submit();
        }
    </script>
</body>
</html>`;
}

function generateCancelPageHTML(urlVolver) {
  const url = urlVolver || process.env.FRONTEND_URL || 'http://localhost:5173/ciudadano/solicitudes';
  return `<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Pago cancelado — RDAM</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: white; height: 100vh; overflow: hidden;
        }
        .root { display: flex; height: 100vh; }
        .left {
            display: flex; flex-direction: column;
            justify-content: center; align-items: flex-start;
            width: 40%;
            background: linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%);
            padding: 48px; gap: 48px; flex-shrink: 0;
        }
        .logo-wrap { display: flex; align-items: center; gap: 12px; }
        .logo-box { width: 36px; height: 36px; background: white; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
        .logo-r { color: #005EA2; font-weight: 700; font-size: 20px; }
        .logo-name { color: white; font-weight: 700; font-size: 18px; }
        .left-title { color: white; font-weight: 700; font-size: 28px; line-height: 1.3; }
        .left-sub { color: rgba(255,255,255,0.75); font-size: 14px; margin-top: 8px; line-height: 1.6; }
        .bullets { display: flex; flex-direction: column; gap: 12px; }
        .bullet { display: flex; align-items: center; gap: 12px; }
        .dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.5); flex-shrink: 0; }
        .bullet-text { color: rgba(255,255,255,0.85); font-size: 13px; }
        .left-footer { color: rgba(255,255,255,0.5); font-size: 11px; margin-top: 8px; }

        .right {
            flex: 1; background: white;
            display: flex; align-items: center; justify-content: center;
            padding: 48px; overflow-y: auto;
        }
        .cancel-wrap { width: 100%; max-width: 420px; text-align: center; }
        .cancel-icon {
            width: 64px; height: 64px; margin: 0 auto 20px;
        }
        .cancel-title { font-size: 28px; font-weight: 700; color: #1B1B1B; margin-bottom: 8px; }
        .cancel-sub { font-size: 14px; color: #71767A; margin-bottom: 8px; line-height: 1.6; }
        .cancel-caption { font-size: 11px; color: #A9AEB1; text-align: center; margin-bottom: 28px; }
        .btn-primary {
            width: 100%; padding: 14px;
            background: #005EA2; color: white;
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 600; cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,94,162,0.3);
            margin-bottom: 12px; transition: background 0.15s;
        }
        .btn-primary:hover { background: #0F4A7C; }
        .help-text { font-size: 13px; color: #71767A; text-align: center; }

        @media (max-width: 900px) {
            .left { display: none; }
            .root { flex-direction: column; }
            .right { width: 100%; padding: 24px; }
        }
    </style>
</head>
<body>
    <div class="root">
        <div class="left">
            <div class="logo-wrap">
                <div class="logo-box"><span class="logo-r">R</span></div>
                <span class="logo-name">RDAM</span>
            </div>
            <div>
                <div class="left-title">Sistema de Gestión de Certificados Digitales</div>
                <div class="left-sub">Podés reintentar el pago cuando quieras.</div>
            </div>
            <div class="bullets">
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Tu solicitud sigue en estado Aprobada</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">No se realizó ningún cobro</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Podés volver a intentar el pago</span></div>
            </div>
            <div class="left-footer">Poder Judicial de la Provincia de Santa Fe — 2026</div>
        </div>

        <div class="right">
            <div class="cancel-wrap">
                <div class="cancel-icon">
                    <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                        <circle cx="32" cy="32" r="30" stroke="#71767A" stroke-width="2.5" fill="none"/>
                        <line x1="22" y1="22" x2="42" y2="42" stroke="#71767A" stroke-width="2.5" stroke-linecap="round"/>
                        <line x1="42" y1="22" x2="22" y2="42" stroke="#71767A" stroke-width="2.5" stroke-linecap="round"/>
                    </svg>
                </div>
                <div class="cancel-title">Pago cancelado</div>
                <div class="cancel-sub">No se procesó ningún cobro. Tu solicitud permanece en estado Aprobada y podés retomar el pago cuando lo desees.</div>
                <div class="cancel-caption">Registro de Actos y Documentos del Ámbito de la Magistratura</div>

                <button class="btn-primary" onclick="window.location.href='${url}'">
                    Volver al sistema RDAM
                </button>
                <div class="help-text">¿Necesitás ayuda? Contactá a soporte@rdam.gob.ar</div>
            </div>
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
    <title>Error — Portal de Pagos</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: white; height: 100vh; overflow: hidden;
        }
        .root { display: flex; height: 100vh; }
        .left {
            display: flex; flex-direction: column;
            justify-content: center; align-items: flex-start;
            width: 40%;
            background: linear-gradient(160deg, #0F4A7C 0%, #005EA2 50%, #2378C3 100%);
            padding: 48px; gap: 48px; flex-shrink: 0;
        }
        .logo-wrap { display: flex; align-items: center; gap: 12px; }
        .logo-box { width: 36px; height: 36px; background: white; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
        .logo-r { color: #005EA2; font-weight: 700; font-size: 20px; }
        .logo-name { color: white; font-weight: 700; font-size: 18px; }
        .left-title { color: white; font-weight: 700; font-size: 28px; line-height: 1.3; }
        .left-sub { color: rgba(255,255,255,0.75); font-size: 14px; margin-top: 8px; line-height: 1.6; }
        .bullets { display: flex; flex-direction: column; gap: 12px; }
        .bullet { display: flex; align-items: center; gap: 12px; }
        .dot { width: 6px; height: 6px; border-radius: 50%; background: rgba(255,255,255,0.5); flex-shrink: 0; }
        .bullet-text { color: rgba(255,255,255,0.85); font-size: 13px; }
        .left-footer { color: rgba(255,255,255,0.5); font-size: 11px; margin-top: 8px; }

        .right {
            flex: 1; background: white;
            display: flex; align-items: center; justify-content: center;
            padding: 48px; overflow-y: auto;
        }
        .error-wrap { width: 100%; max-width: 420px; }
        .error-icon {
            width: 64px; height: 64px; margin-bottom: 20px;
        }
        .error-title { font-size: 28px; font-weight: 700; color: #1B1B1B; margin-bottom: 4px; }
        .error-sub { font-size: 14px; color: #71767A; margin-bottom: 24px; line-height: 1.6; }

        .error-detail {
            background: #FEF2F2; border-radius: 8px;
            border: 1px solid #FECACA; border-left: 4px solid #D42A2A;
            padding: 16px; margin-bottom: 24px;
        }
        .error-detail-label { font-size: 11px; font-weight: 600; color: #71767A; text-transform: uppercase; letter-spacing: 0.4px; margin-bottom: 6px; }
        .error-detail-msg { font-size: 13px; color: #1B1B1B; line-height: 1.5; }
        .error-ts { font-size: 11px; color: #A9AEB1; margin-top: 8px; font-family: monospace; }

        .btn-retry {
            width: 100%; padding: 14px;
            background: #005EA2; color: white;
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 600; cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,94,162,0.3);
            margin-bottom: 10px; transition: background 0.15s;
        }
        .btn-retry:hover { background: #0F4A7C; }
        .btn-back {
            width: 100%; padding: 12px;
            background: white; color: #71767A;
            border: 1px solid #DFE1E2; border-radius: 8px;
            font-size: 14px; font-weight: 500; cursor: pointer;
            transition: background 0.15s;
        }
        .btn-back:hover { background: #F0F0F0; }

        @media (max-width: 900px) {
            .left { display: none; }
            .root { flex-direction: column; }
            .right { width: 100%; padding: 24px; }
        }
    </style>
</head>
<body>
    <div class="root">
        <div class="left">
            <div class="logo-wrap">
                <div class="logo-box"><span class="logo-r">R</span></div>
                <span class="logo-name">RDAM</span>
            </div>
            <div>
                <div class="left-title">Sistema de Gestión de Certificados Digitales</div>
                <div class="left-sub">Plataforma oficial para la gestión integral de certificados.</div>
            </div>
            <div class="bullets">
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Transacción cifrada con TLS</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Gestión de solicitudes en tiempo real</span></div>
                <div class="bullet"><div class="dot"></div><span class="bullet-text">Emisión digital de certificados</span></div>
            </div>
            <div class="left-footer">Poder Judicial de la Provincia de Santa Fe — 2026</div>
        </div>

        <div class="right">
            <div class="error-wrap">
                <div class="error-icon">
                    <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                        <circle cx="32" cy="32" r="30" stroke="#D42A2A" stroke-width="2.5" fill="none"/>
                        <line x1="22" y1="22" x2="42" y2="42" stroke="#D42A2A" stroke-width="2.5" stroke-linecap="round"/>
                        <line x1="42" y1="22" x2="22" y2="42" stroke="#D42A2A" stroke-width="2.5" stroke-linecap="round"/>
                    </svg>
                </div>
                <div class="error-title">${titulo}</div>
                <div class="error-sub">${mensaje}</div>

                <div class="error-detail">
                    <div class="error-detail-label">Detalle del error</div>
                    <div class="error-detail-msg">${mensaje}</div>
                    <div class="error-ts">${new Date().toISOString()}</div>
                </div>

                <button class="btn-retry" onclick="window.location.reload()">
                    Reintentar
                </button>
                <button class="btn-back" onclick="window.history.back()">
                    Volver
                </button>
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
      // Transacción no encontrada = ya fue cancelada antes
      // Asumir éxito silencioso y mostrar pantalla de cancelación
      console.log('[POST /pluspagos/cancelar] Transacción ya procesada, retornando éxito idempotente');
      const fallbackUrl = process.env.FRONTEND_URL || 'http://localhost:5173/ciudadano/solicitudes';
      const html = generateCancelPageHTML(fallbackUrl);
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(html);
      return;
    }

    console.log(`[POST /pluspagos/cancelar] Transacción cancelada: ${transaccionId}`);

    const urlError = transaction.urlError;
    // Limpiar transacción
    transactions.delete(transaccionId);

    // Mostrar página de cancelación con URL de vuelta
    const html = generateCancelPageHTML(urlError);
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(html);

    console.log(`[POST /pluspagos/cancelar] ✓ Página de cancelación enviada`);
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