const http = require('http');
const crypto = require('crypto');
const url = require('url');
const querystring = require('querystring');
const fs = require('fs');
const path = require('path');

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
            background: #F4F6F9;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* ── Header institucional ── */
        .inst-header {
            background: #005EA2;
            padding: 12px 24px;
            display: flex;
            align-items: center;
            gap: 12px;
            flex-shrink: 0;
        }
        .inst-logo { height: 36px; filter: brightness(0) invert(1); }
        .inst-title { color: white; font-weight: 700; font-size: 14px; }
        .inst-sub { color: rgba(255,255,255,0.85); font-size: 11px; }
        .inst-right { margin-left: auto; color: rgba(255,255,255,0.75); font-size: 11px; }

        /* ── Contenido central ── */
        .page-content {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px 16px;
        }
        .payment-card {
            background: white;
            border-radius: 12px;
            box-shadow: 0 4px 24px rgba(0,0,0,0.08);
            padding: 32px;
            width: 100%;
            max-width: 480px;
        }

        /* ── Título ── */
        .payment-title { font-size: 18px; font-weight: 700; color: #1B1B1B; margin-bottom: 4px; }
        .payment-subtitle { font-size: 12px; color: #71767A; margin-bottom: 20px; line-height: 1.5; }

        /* Tarjeta resumen de monto */
        .amount-card {
            background: #F0F4F8;
            border-radius: 10px;
            border: 1px solid #DFE1E2;
            padding: 18px 20px;
            margin-bottom: 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .amount-label { font-size: 11px; font-weight: 600; color: #5A5A5A; text-transform: uppercase; letter-spacing: 0.4px; }
        .amount-value { font-size: 36px; font-weight: 700; color: #1B1B1B; letter-spacing: -1px; margin-top: 2px; }
        .amount-badge {
            background: #E6F4EA; color: #1A6330;
            font-size: 11px; font-weight: 600;
            padding: 4px 10px; border-radius: 20px;
            white-space: nowrap;
        }

        /* Resumen del pago */
        .section-label { font-size: 11px; font-weight: 600; color: #444444; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; display: block; }
        .resumen-box {
            background: #EEF4FF;
            border-radius: 8px;
            border: 1px solid #C9D9F5;
            border-left: 3px solid #005EA2;
            padding: 14px 16px;
            margin-bottom: 20px;
        }
        .resumen-row { display: flex; justify-content: space-between; font-size: 13px; color: #5A5A5A; padding: 3px 0; }
        .resumen-total {
            display: flex; justify-content: space-between;
            font-size: 15px; font-weight: 700; color: #1B1B1B;
            border-top: 1px solid #C9D9F5; margin-top: 8px; padding-top: 10px;
        }
        .resumen-txn { border-top: 1px solid #C9D9F5; margin-top: 10px; padding-top: 8px; }
        .resumen-txn-id { font-size: 10px; color: #5A5A5A; word-break: break-all; }
        .resumen-txn-desc { font-size: 11px; color: #5A5A5A; margin-top: 2px; }

        /* Campos del formulario */
        .card-section { margin-bottom: 4px; }
        .form-field { margin-bottom: 16px; }
        .form-label {
            font-size: 11px;
            font-weight: 600;
            color: #444444;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 6px;
            display: block;
        }
        .form-label-dni {
            display: block;
            font-size: 13px;
            font-weight: 700;
            color: #1B1B1B;
            margin-bottom: 6px;
        }
        .form-input {
            width: 100%; padding: 11px 14px;
            border: 1px solid #DFE1E2; border-radius: 8px;
            font-size: 14px; color: #1B1B1B;
            background: white; outline: none;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            transition: box-shadow 0.15s, border-color 0.15s;
        }
        .form-input:focus { border-color: #005EA2; box-shadow: 0 0 0 3px rgba(0,94,162,0.12); }
        .form-input::placeholder { color: #BABFC4; }
        .field-hint { font-size: 10px; color: #71767A; margin-top: 4px; }

        /* Campo número de tarjeta */
        .card-input-wrap { position: relative; display: flex; align-items: center; }
        .card-network-icon {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            display: flex;
            align-items: center;
            pointer-events: none;
        }
        .card-input {
            width: 100%;
            padding: 11px 60px 11px 14px;
            border: 1px solid #DFE1E2;
            border-radius: 8px;
            font-size: 16px;
            font-family: 'Segoe UI', monospace;
            letter-spacing: 2px;
            color: #1B1B1B;
            background: white;
            outline: none;
            transition: box-shadow 0.15s, border-color 0.15s;
        }
        .card-input:focus { border-color: #005EA2; box-shadow: 0 0 0 3px rgba(0,94,162,0.12); }
        .card-input::placeholder { color: #BABFC4; letter-spacing: 2px; }

        /* Grid 3 columnas para Venc/CVC/DNI */
        .card-row-3 {
            display: grid;
            grid-template-columns: 1fr 1fr 1.5fr;
            gap: 12px;
            margin-bottom: 0;
        }
        .card-row-3 .form-field { margin-bottom: 0; }

        /* Error banner */
        .error-banner {
            display: none;
            background: #FEF2F2;
            border: 1px solid #FECACA;
            border-left: 4px solid #D42A2A;
            border-radius: 6px;
            padding: 12px 16px;
            margin-bottom: 16px;
            font-size: 13px;
            color: #B91C1C;
            line-height: 1.4;
        }
        .form-error { color: #D42A2A; font-size: 12px; margin-top: 12px; margin-bottom: 8px; display: none; }

        /* Microcopy de seguridad */
        .security-micro {
            font-size: 12px;
            color: #00A91C;
            text-align: center;
            margin: 16px 0 12px;
        }

        /* Botones */
        .btn-confirm {
            width: 100%; padding: 14px;
            background: #005EA2; color: white;
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 600;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0,94,162,0.25);
            transition: background 0.15s;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .btn-confirm:hover:not(:disabled) { background: #0F4A7C; }
        .btn-confirm:disabled { background: #6B9DC4; cursor: not-allowed; box-shadow: none; }
        .btn-cancel {
            width: 100%;
            padding: 12px;
            border: 1.5px solid #DCDEE0;
            border-radius: 8px;
            background: white;
            color: #71767A;
            font-size: 14px;
            cursor: pointer;
            margin-top: 8px;
            transition: border-color 0.2s, color 0.2s;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        .btn-cancel:hover { border-color: #005EA2; color: #005EA2; }

        /* Spinner inline */
        .btn-spinner {
            display: inline-block;
            width: 14px;
            height: 14px;
            border: 2px solid rgba(255,255,255,0.3);
            border-top-color: white;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
            margin-right: 8px;
            flex-shrink: 0;
        }
        @keyframes spin { to { transform: rotate(360deg); } }

        /* Footer */
        .page-footer {
            text-align: center;
            padding: 16px;
            color: rgba(0,0,0,0.4);
            font-size: 11px;
        }

        /* Seguridad inferior */
        .security { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 16px; }
        .security-text-sm { font-size: 11px; color: #5A5A5A; }
        .badge-ssl { background: #E6F4EA; color: #1A6330; font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px; }

        @media (max-width: 540px) {
            .payment-card { padding: 20px 16px; }
            .card-row-3 { grid-template-columns: 1fr 1fr; }
            .card-row-3 .form-field:last-child { grid-column: 1 / -1; }
        }
    </style>
</head>
<body>
    <!-- Header institucional -->
    <div class="inst-header">
        <img src="/logo" class="inst-logo" alt="Poder Judicial" onerror="this.style.display='none'">
        <div>
            <div class="inst-title">PODER JUDICIAL</div>
            <div class="inst-sub">PROVINCIA DE SANTA FE</div>
        </div>
        <div class="inst-right">Portal de Pagos Oficiales</div>
    </div>

    <!-- Contenido central -->
    <div class="page-content">
        <div class="payment-card">
            <div class="payment-title">PlusPagos</div>
            <div class="payment-subtitle">Confirmá los detalles antes de pagar · Registro de Actos y Documentos del Ámbito de la Magistratura</div>

            <!-- Monto destacado — elemento más prominente -->
            <div class="amount-card">
                <div>
                    <div class="amount-label">Total a pagar</div>
                    <div class="amount-value">$${totalConTasa} ARS</div>
                </div>
                <div class="amount-badge">Pago seguro</div>
            </div>

            <!-- Resumen del pago con ID de transacción en texto pequeño gris -->
            <span class="section-label">Resumen del pago</span>
            <div class="resumen-box">
                <div class="resumen-row"><span>Monto del trámite</span><span>$${montoNum.toFixed(2)} ARS</span></div>
                <div class="resumen-row"><span>Tasa de servicio (2%)</span><span>$${tasaServicio} ARS</span></div>
                <div class="resumen-total"><span>Total</span><span>$${totalConTasa} ARS</span></div>
                <div class="resumen-txn">
                    <div class="resumen-txn-id">ID: ${transaccionId}</div>
                    <div class="resumen-txn-desc">${descripcion}</div>
                </div>
            </div>

            <!-- Datos de la tarjeta -->
            <div class="card-section">
                <span class="section-label">Datos de la tarjeta</span>

                <!-- Error banner: se muestra en error sin borrar el formulario -->
                <div class="error-banner" id="errorBanner"></div>

                <!-- Número de tarjeta con máscara y detección de red -->
                <div class="form-field">
                    <div class="card-input-wrap">
                        <input
                            type="text"
                            id="cardNumber"
                            class="card-input"
                            placeholder="0000-0000-0000-0000"
                            maxlength="19"
                            autocomplete="off"
                            inputmode="numeric"
                            pattern="[0-9]*"
                        />
                        <div class="card-network-icon" id="cardNetworkIcon">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#AAAAAA" stroke-width="1.5">
                                <rect x="2" y="5" width="20" height="14" rx="2"/>
                                <line x1="2" y1="10" x2="22" y2="10"/>
                            </svg>
                        </div>
                    </div>
                </div>

                <!-- Nombre y Apellido -->
                <div class="form-field">
                    <label class="form-label">Nombre y Apellido</label>
                    <input type="text" id="cardName" class="form-input" placeholder="Tal como figura en la tarjeta" oninput="this.value = this.value.toUpperCase()" />
                </div>

                    <!-- Vencimiento / CVC / DNI — grid 3 columnas -->
                    <div class="card-row-3">
                        <div class="form-field">
                            <label class="form-label">Vencimiento</label>
                            <input type="text" id="cardExpiry" class="form-input" placeholder="MM/AA" maxlength="6" inputmode="numeric" autocomplete="off" />
                    </div>
                    <div class="form-field">
                        <label class="form-label">CVC</label>
                        <input type="text" id="cardCvc" class="form-input" placeholder="•••" maxlength="3" inputmode="numeric" pattern="[0-9]*" autocomplete="off" />
                    </div>
                    <div class="form-field">
                        <label class="form-label-dni">DNI del titular (requerido)</label>
                        <input type="text" id="cardDni" class="form-input" placeholder="Sin puntos ni espacios" maxlength="8" inputmode="numeric" />
                        <div class="field-hint">Ej: 12345678</div>
                    </div>
                </div>
            </div>

            <!-- Microcopy de seguridad -->
            <div class="security-micro">🔒 Tus datos están protegidos. Esta conexión es cifrada.</div>

            <div class="form-error" id="formError">Completá todos los campos de la tarjeta para continuar.</div>

            <!-- Formulario oculto para cancelar -->
            <form id="cancelForm" method="POST" action="/pluspagos/cancelar">
                <input type="hidden" name="transaccionId" value="${transaccionId}">
            </form>

            <!-- Botón confirmar con spinner anti-doble clic -->
            <button class="btn-confirm" id="btnConfirm" onclick="confirmarPago()">
                Confirmar y pagar $${totalConTasa} ARS
            </button>
            <button class="btn-cancel" onclick="document.getElementById('cancelForm').submit()">
                Cancelar y volver al sistema RDAM
            </button>

            <div class="security">
                <span>🔒</span>
                <span class="security-text-sm">Transacción cifrada y segura</span>
                <span class="badge-ssl">SSL</span>
            </div>
        </div>
    </div>

    <div class="page-footer">Poder Judicial de la Provincia de Santa Fe — 2026</div>

    <script>
        // Detección de red de tarjeta — icono SVG/texto dentro del input
        function getNetworkIcon(first) {
            if (first === '4') {
                return '<span style="font-size:13px;font-weight:900;color:#1A1F71;letter-spacing:-1px;font-style:italic;">VISA</span>';
            } else if (first === '5') {
                return '<svg width="38" height="24" viewBox="0 0 38 24"><circle cx="13" cy="12" r="10" fill="#EB001B" opacity="0.9"/><circle cx="25" cy="12" r="10" fill="#F79E1B" opacity="0.9"/><path d="M19 4.8a10 10 0 0 1 0 14.4A10 10 0 0 1 19 4.8z" fill="#FF5F00"/></svg>';
            } else {
                return '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#AAAAAA" stroke-width="1.5"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>';
            }
        }

        // Número de tarjeta — solo dígitos, máximo 16
        document.getElementById('cardNumber').addEventListener('input', function() {
            const digits = this.value.replace(/\D/g, '').substring(0, 16);
            this.value = digits;
            document.getElementById('cardNetworkIcon').innerHTML = getNetworkIcon(digits[0] || '');
        });

        // Vencimiento — solo dígitos, máximo 4
        document.getElementById('cardExpiry').addEventListener('input', function() {
            this.value = this.value.replace(/\D/g, '').substring(0, 4);
        });

        // CVC — solo dígitos, máximo 3
        document.getElementById('cardCvc').addEventListener('input', function() {
            this.value = this.value.replace(/\D/g, '').slice(0, 3);
        });

        // DNI — solo dígitos
        document.getElementById('cardDni').addEventListener('input', function() {
            this.value = this.value.replace(/\D/g, '').slice(0, 8);
        });

        function showErrorBanner(msg) {
            var banner = document.getElementById('errorBanner');
            banner.textContent = msg;
            banner.style.display = 'block';
            banner.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }

        async function confirmarPago() {
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
            document.getElementById('errorBanner').style.display = 'none';

            var btn = document.getElementById('btnConfirm');
            var originalHTML = btn.innerHTML;

            // 1. Deshabilitar el botón inmediatamente
            btn.disabled = true;
            // 2. Cambiar texto a "Procesando..." con spinner CSS
            btn.innerHTML = '<span class="btn-spinner"></span>Procesando...';

            // 3. POST a /pluspagos/confirmar vía fetch (AJAX) — sin limpiar el formulario ante error
            try {
                var response = await fetch('/pluspagos/confirmar', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: 'transaccionId=${transaccionId}'
                });

                if (response.ok) {
                    var data = await response.json();
                    window.location.href = data.redirectUrl || '/';
                } else {
                    var errData = {};
                    try { errData = await response.json(); } catch (e) {}
                    showErrorBanner(errData.error || 'Hubo un error al procesar el pago. Por favor intentá nuevamente.');
                    btn.disabled = false;
                    btn.innerHTML = originalHTML;
                }
            } catch (err) {
                showErrorBanner('Hubo un error al procesar el pago. Por favor intentá nuevamente.');
                btn.disabled = false;
                btn.innerHTML = originalHTML;
            }
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

    const isAjax = req.headers['x-requested-with'] === 'XMLHttpRequest';

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

        console.log(
            `[POST /pluspagos/confirmar] ✓ Redirigiendo a: ${transaction.urlSuccess}`
        );

        if (isAjax) {
            // Responder con JSON para que el cliente navegue sin perder el formulario en caso de error
            res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ redirectUrl: transaction.urlSuccess }));
        } else {
            res.writeHead(302, { Location: transaction.urlSuccess });
            res.end();
        }
    } catch (error) {
        console.error(`[POST /pluspagos/confirmar] ✗ Error:`, error.message);

        if (isAjax) {
            res.writeHead(500, { 'Content-Type': 'application/json; charset=utf-8' });
            res.end(JSON.stringify({ error: 'Hubo un error al procesar el pago. Por favor intentá nuevamente.' }));
        } else {
            const html = generateErrorPageHTML(
                'Error Procesando Pago',
                `No se pudo procesar el pago: ${error.message}`
            );
            res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(html);
        }
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
            } else if (pathname === '/logo' && method === 'GET') {
                // Servir logo institucional si existe; 404 silencioso si no
                const logoPath = path.join(__dirname, 'logo.png');
                fs.readFile(logoPath, (err, data) => {
                    if (err) {
                        res.writeHead(404);
                        res.end();
                    } else {
                        res.writeHead(200, { 'Content-Type': 'image/png' });
                        res.end(data);
                    }
                });
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