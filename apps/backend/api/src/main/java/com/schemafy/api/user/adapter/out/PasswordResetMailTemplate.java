package com.schemafy.api.user.adapter.out;

final class PasswordResetMailTemplate {

  static final String SUBJECT = "Reset your Schemafy password";
  private static final String LINK_PLACEHOLDER = "{{RESET_LINK}}";

  private PasswordResetMailTemplate() {}

  static String text(String resetLink) {
    return """
        Schemafy password reset

        Use the link below to reset your password. It expires in 10 minutes:

        {{RESET_LINK}}

        If you did not request a password reset, you can safely ignore this email.
        """.replace(LINK_PLACEHOLDER, resetLink);
  }

  static String html(String resetLink) {
    return """
        <!doctype html>
        <html lang="en"><body style="margin:0;padding:32px;background:#f6f7f9;font-family:Arial,sans-serif;color:#141414;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"><tr><td align="center">
            <table role="presentation" width="560" cellspacing="0" cellpadding="0" style="max-width:100%%;background:#fff;border:1px solid #e5e7eb;border-radius:8px;">
              <tr><td style="padding:32px;">
                <p style="margin:0 0 12px;font-size:20px;font-weight:700;">Schemafy</p>
                <h1 style="margin:0;font-size:28px;">Reset your password</h1>
                <p style="line-height:1.6;color:#555;">Use the button below to reset your password. This link expires in <strong>10 minutes</strong>.</p>
                <p style="margin:28px 0;"><a href="{{RESET_LINK}}" style="display:inline-block;padding:12px 18px;background:#141414;color:#fff;text-decoration:none;border-radius:6px;">Reset password</a></p>
                <p style="line-height:1.6;color:#757575;font-size:13px;">If you did not request a password reset, you can safely ignore this email.</p>
              </td></tr>
            </table>
          </td></tr></table>
        </body></html>
        """
        .replace(LINK_PLACEHOLDER, resetLink);
  }

}
