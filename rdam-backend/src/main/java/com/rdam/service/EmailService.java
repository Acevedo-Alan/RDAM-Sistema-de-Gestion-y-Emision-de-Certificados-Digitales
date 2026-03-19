package com.rdam.service;

import com.rdam.service.event.CertificadoEmitidoEvent;
import com.rdam.service.event.PagoAprobadoEvent;
import com.rdam.service.event.SolicitudAprobadaEvent;
import com.rdam.service.event.SolicitudRechazadaEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        @Value("${otp.from:noreply@rdam.gob.ar}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void enviarOtp(String destinatario, String codigo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("RDAM \u2014 Verificaci\u00f3n de Acceso");
            helper.setText(construirHtmlOtp(codigo), true);

            mailSender.send(message);
            logger.info("action=OTP_SENT email={}", destinatario);

        } catch (MessagingException e) {
            logger.error("action=OTP_SEND_FAILED email={} error={}", destinatario, e.getMessage());
            throw new RuntimeException("Error al enviar el email de verificacion", e);
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPagoAprobado(PagoAprobadoEvent event) {
        logger.info("action=EMAIL_PAGO_APROBADO solicitudId={} email={}",
                event.solicitudId(), event.ciudadanoEmail());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.ciudadanoEmail());
            helper.setSubject("RDAM \u2014 Pago confirmado para su solicitud #" + event.solicitudId());
            helper.setText(construirHtmlPagoAprobado(event.solicitudId()), true);

            mailSender.send(message);
            logger.info("action=EMAIL_PAGO_APROBADO_SENT solicitudId={} email={}",
                    event.solicitudId(), event.ciudadanoEmail());

        } catch (MessagingException e) {
            logger.error("action=EMAIL_PAGO_APROBADO_FAILED solicitudId={} email={} error={}",
                    event.solicitudId(), event.ciudadanoEmail(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCertificadoEmitido(CertificadoEmitidoEvent event) {
        logger.info("action=EMAIL_CERTIFICADO_EMITIDO solicitudId={} certificado={} email={}",
                event.solicitudId(), event.numeroCertificado(), event.ciudadanoEmail());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.ciudadanoEmail());
            helper.setSubject("RDAM \u2014 Su certificado est\u00e1 disponible (#" + event.numeroCertificado() + ")");
            helper.setText(construirHtmlCertificadoEmitido(event.solicitudId(), event.numeroCertificado()), true);

            mailSender.send(message);
            logger.info("action=EMAIL_CERTIFICADO_EMITIDO_SENT solicitudId={} email={}",
                    event.solicitudId(), event.ciudadanoEmail());

        } catch (MessagingException e) {
            logger.error("action=EMAIL_CERTIFICADO_EMITIDO_FAILED solicitudId={} email={} error={}",
                    event.solicitudId(), event.ciudadanoEmail(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitudAprobada(SolicitudAprobadaEvent event) {
        logger.info("action=EMAIL_SOLICITUD_APROBADA solicitudId={} email={}",
                event.solicitudId(), event.ciudadanoEmail());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.ciudadanoEmail());
            helper.setSubject("RDAM \u2014 Su solicitud #" + event.solicitudId() + " fue aprobada");
            helper.setText(construirHtmlSolicitudAprobada(
                    event.solicitudId(), event.tipoCertificado(), event.montoArancel()), true);

            mailSender.send(message);
            logger.info("action=EMAIL_SOLICITUD_APROBADA_SENT solicitudId={} email={}",
                    event.solicitudId(), event.ciudadanoEmail());

        } catch (MessagingException e) {
            logger.error("action=EMAIL_SOLICITUD_APROBADA_FAILED solicitudId={} email={} error={}",
                    event.solicitudId(), event.ciudadanoEmail(), e.getMessage());
        }
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSolicitudRechazada(SolicitudRechazadaEvent event) {
        logger.info("action=EMAIL_SOLICITUD_RECHAZADA solicitudId={} email={}",
                event.solicitudId(), event.ciudadanoEmail());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(event.ciudadanoEmail());
            helper.setSubject("RDAM \u2014 Su solicitud #" + event.solicitudId() + " fue rechazada");
            helper.setText(construirHtmlSolicitudRechazada(
                    event.solicitudId(), event.tipoCertificado(), event.motivo()), true);

            mailSender.send(message);
            logger.info("action=EMAIL_SOLICITUD_RECHAZADA_SENT solicitudId={} email={}",
                    event.solicitudId(), event.ciudadanoEmail());

        } catch (MessagingException e) {
            logger.error("action=EMAIL_SOLICITUD_RECHAZADA_FAILED solicitudId={} email={} error={}",
                    event.solicitudId(), event.ciudadanoEmail(), e.getMessage());
        }
    }

    // ── Templates HTML ──

    private String construirHtmlOtp(String codigo) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#F9F9F9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F9F9F9;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="480" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:8px;border:1px solid #DCDEE0;overflow:hidden;">
                          
                          <!-- Header azul -->
                          <tr>
                            <td style="background:#005EA2;padding:24px 32px;">
                              <table cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="background:#FFFFFF;width:32px;height:32px;border-radius:4px;text-align:center;vertical-align:middle;">
                                    <span style="color:#005EA2;font-weight:700;font-size:18px;line-height:32px;">R</span>
                                  </td>
                                  <td style="padding-left:12px;color:#FFFFFF;font-size:16px;font-weight:600;vertical-align:middle;">RDAM</td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Cuerpo -->
                          <tr>
                            <td style="padding:40px 32px 32px 32px;">
                              <p style="margin:0 0 8px 0;font-size:22px;font-weight:700;color:#1B1B1B;">Verificación de Acceso</p>
                              <p style="margin:0 0 24px 0;font-size:14px;color:#71767A;line-height:1.6;">
                                Has solicitado iniciar sesión en el Sistema de Certificados Digitales.<br>
                                Utilizá el siguiente código para continuar.
                              </p>

                              <!-- Código OTP -->
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
                                <tr>
                                  <td align="center" style="background:#F0F6FF;border:2px solid #005EA2;border-radius:8px;padding:24px;">
                                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#005EA2;font-family:'Courier New',monospace;">
                                      {codigo}
                                    </span>
                                  </td>
                                </tr>
                              </table>

                              <!-- Advertencia -->
                              <table width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
                                <tr>
                                  <td style="background:#FFF3CD;border-left:4px solid #FFBE2E;border-radius:4px;padding:12px 16px;">
                                    <p style="margin:0;font-size:13px;color:#936F38;">
                                      Este código expirará en <strong>5 minutos</strong>.
                                    </p>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:0;font-size:13px;color:#A9AEB1;line-height:1.6;">
                                Si no solicitaste este código, podés ignorar este correo.<br>
                                Nadie del equipo de RDAM te pedirá este código.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background:#F9F9F9;border-top:1px solid #DCDEE0;padding:16px 32px;">
                              <p style="margin:0;font-size:12px;color:#A9AEB1;text-align:center;">
                                Poder Judicial de la Provincia de Santa Fe &mdash; 2026<br>
                                Sistema RDAM &mdash; Registro de Actos y Documentos del Ámbito de la Magistratura
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.replace("{codigo}", codigo);
    }

    private String construirHtmlPagoAprobado(Integer solicitudId) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f4f4;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:8px;
                                              box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#1a3a5c;padding:24px 32px;">
                                            <h2 style="margin:0;color:#ffffff;font-size:20px;font-weight:600;">
                                                RDAM &mdash; Pago Confirmado
                                            </h2>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:32px;">
                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Le informamos que el pago correspondiente a su solicitud ha sido
                                                <strong>recibido y confirmado</strong> exitosamente.
                                            </p>

                                            <!-- Highlight Block -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                                   style="margin:0 0 24px;">
                                                <tr>
                                                    <td align="center">
                                                        <div style="display:inline-block;background-color:#2d2d2d;
                                                                    border-radius:8px;padding:16px 32px;">
                                                            <p style="margin:0;color:#f0f0f0;font-size:16px;
                                                                      font-family:'Courier New',Courier,monospace;
                                                                      font-weight:700;">
                                                                Solicitud #%d
                                                            </p>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Su tr&aacute;mite contin&uacute;a en proceso. Recibir&aacute; otro
                                                correo electr&oacute;nico cuando su certificado est&eacute; listo
                                                para descargar.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:16px 32px 24px;border-top:1px solid #eeeeee;">
                                            <p style="margin:0;color:#999999;font-size:12px;line-height:1.5;
                                                      text-align:center;">
                                                Sistema de Certificados Digitales &mdash; RDAM
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(solicitudId);
    }

    private String construirHtmlCertificadoEmitido(Integer solicitudId, String numeroCertificado) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f4f4;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:8px;
                                              box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#1a3a5c;padding:24px 32px;">
                                            <h2 style="margin:0;color:#ffffff;font-size:20px;font-weight:600;">
                                                RDAM &mdash; Certificado Disponible
                                            </h2>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:32px;">
                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Su certificado ha sido <strong>emitido exitosamente</strong>
                                                y se encuentra disponible para su descarga.
                                            </p>

                                            <!-- Highlight Block -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                                   style="margin:0 0 24px;">
                                                <tr>
                                                    <td align="center">
                                                        <div style="display:inline-block;background-color:#2d2d2d;
                                                                    border-radius:8px;padding:16px 32px;">
                                                            <p style="margin:0;color:#f0f0f0;font-size:16px;
                                                                      font-family:'Courier New',Courier,monospace;
                                                                      font-weight:700;">
                                                                Certificado #%s
                                                            </p>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:0 0 8px;color:#333333;font-size:15px;line-height:1.6;">
                                                <strong>Solicitud:</strong> #%d
                                            </p>
                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Puede descargarlo iniciando sesi&oacute;n en el
                                                <strong>Sistema de Certificados Digitales</strong>.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:16px 32px 24px;border-top:1px solid #eeeeee;">
                                            <p style="margin:0;color:#999999;font-size:12px;line-height:1.5;
                                                      text-align:center;">
                                                Sistema de Certificados Digitales &mdash; RDAM
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(numeroCertificado, solicitudId);
    }

    private String construirHtmlSolicitudAprobada(Integer solicitudId, String tipoCertificado,
                                                   java.math.BigDecimal montoArancel) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f4f4;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:8px;
                                              box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#1a3a5c;padding:24px 32px;">
                                            <h2 style="margin:0;color:#ffffff;font-size:20px;font-weight:600;">
                                                RDAM &mdash; Solicitud Aprobada
                                            </h2>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:32px;">
                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Su solicitud de certificado ha sido <strong>aprobada</strong>.
                                            </p>

                                            <!-- Highlight Block -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                                   style="margin:0 0 24px;">
                                                <tr>
                                                    <td align="center">
                                                        <div style="display:inline-block;background-color:#2d2d2d;
                                                                    border-radius:8px;padding:16px 32px;
                                                                    text-align:left;">
                                                            <p style="margin:0 0 8px;color:#f0f0f0;font-size:15px;">
                                                                <strong>Solicitud:</strong> #%d
                                                            </p>
                                                            <p style="margin:0 0 8px;color:#f0f0f0;font-size:15px;">
                                                                <strong>Tipo:</strong> %s
                                                            </p>
                                                            <p style="margin:0;color:#f0f0f0;font-size:15px;">
                                                                <strong>Monto a pagar:</strong> $%s
                                                            </p>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Para continuar con el tr&aacute;mite, inicie sesi&oacute;n en el
                                                <strong>Sistema de Certificados Digitales</strong> y complete
                                                el pago del arancel correspondiente.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:16px 32px 24px;border-top:1px solid #eeeeee;">
                                            <p style="margin:0;color:#999999;font-size:12px;line-height:1.5;
                                                      text-align:center;">
                                                Sistema de Certificados Digitales &mdash; RDAM
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(solicitudId, tipoCertificado, montoArancel.toPlainString());
    }

    private String construirHtmlSolicitudRechazada(Integer solicitudId, String tipoCertificado,
                                                    String motivo) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                           style="background-color:#f4f4f4;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                                       style="background-color:#ffffff;border-radius:8px;
                                              box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;">

                                    <!-- Header -->
                                    <tr>
                                        <td style="background-color:#1a3a5c;padding:24px 32px;">
                                            <h2 style="margin:0;color:#ffffff;font-size:20px;font-weight:600;">
                                                RDAM &mdash; Solicitud Rechazada
                                            </h2>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:32px;">
                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Lamentamos informarle que su solicitud de certificado ha sido
                                                <strong>rechazada</strong>.
                                            </p>

                                            <!-- Highlight Block -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                                   style="margin:0 0 24px;">
                                                <tr>
                                                    <td align="center">
                                                        <div style="display:inline-block;background-color:#2d2d2d;
                                                                    border-radius:8px;padding:16px 32px;
                                                                    text-align:left;">
                                                            <p style="margin:0 0 8px;color:#f0f0f0;font-size:15px;">
                                                                <strong>Solicitud:</strong> #%d
                                                            </p>
                                                            <p style="margin:0 0 8px;color:#f0f0f0;font-size:15px;">
                                                                <strong>Tipo:</strong> %s
                                                            </p>
                                                            <p style="margin:0;color:#f0f0f0;font-size:15px;">
                                                                <strong>Motivo:</strong> %s
                                                            </p>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin:0 0 16px;color:#333333;font-size:15px;line-height:1.6;">
                                                Si lo desea, puede iniciar una nueva solicitud ingresando al
                                                <strong>Sistema de Certificados Digitales</strong>.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:16px 32px 24px;border-top:1px solid #eeeeee;">
                                            <p style="margin:0;color:#999999;font-size:12px;line-height:1.5;
                                                      text-align:center;">
                                                Sistema de Certificados Digitales &mdash; RDAM
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(solicitudId, tipoCertificado, motivo);
    }
}
