package com.schemafy.api.user.adapter.out;

final class EmailVerificationMailTemplate {

  static final String SUBJECT = "Your Schemafy verification code";
  private static final String CODE_PLACEHOLDER = "{{CODE}}";
  private static final String SUBJECT_PLACEHOLDER = "{{SUBJECT}}";

  private EmailVerificationMailTemplate() {}

  static String text(String code) {
    return """
        Schemafy email verification

        Use this code to finish creating your account:

        {{CODE}}

        This code expires in 1 minute. If you did not request this, you can ignore this email.
        """.replace(CODE_PLACEHOLDER, code);
  }

  static String html(String code) {
    return """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>{{SUBJECT}}</title>
        </head>
        <body style="margin:0;padding:0;background:#f6f7f9;font-family:Inter,Arial,sans-serif;color:#141414;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f7f9;margin:0;padding:32px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
                  <tr>
                    <td style="padding:28px 32px 18px 32px;border-bottom:1px solid #e5e7eb;">
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          <td style="vertical-align:middle;">
                            <span style="display:inline-block;width:14px;height:14px;background:#141414;border-radius:3px;margin-right:10px;vertical-align:-2px;"></span>
                            <span style="font-size:20px;line-height:27px;font-weight:700;color:#141414;">Schemafy</span>
                          </td>
                          <td align="right" style="font-size:12px;line-height:16px;font-weight:500;color:#757575;">
                            Email verification
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:32px;">
                      <p style="margin:0 0 10px 0;font-size:12px;line-height:16px;font-weight:600;color:#2f6fd6;text-transform:uppercase;">Account setup</p>
                      <h1 style="margin:0;font-size:28px;line-height:35px;font-weight:700;color:#141414;">Verify your email</h1>
                      <p style="margin:14px 0 0 0;font-size:15px;line-height:23px;color:#757575;">
                        Enter this 6-digit code in Schemafy to finish creating your account.
                      </p>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:28px 0 22px 0;">
                        <tr>
                          <td align="center" style="background:#f6f7f9;border:1px solid #e5e7eb;border-radius:8px;padding:24px 16px;">
                            <div style="font-family:'Roboto Mono','SFMono-Regular',Consolas,monospace;font-size:36px;line-height:44px;font-weight:800;letter-spacing:8px;color:#141414;">{{CODE}}</div>
                          </td>
                        </tr>
                      </table>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:0 0 24px 0;">
                        <tr>
                          <td style="border-left:3px solid #1fa67a;background:#f7fbf9;padding:14px 16px;border-radius:6px;">
                            <p style="margin:0;font-size:14px;line-height:21px;color:#141414;">
                              This code expires in <strong>1 minute</strong>. For your security, do not share it with anyone.
                            </p>
                          </td>
                        </tr>
                      </table>
                      <p style="margin:0;font-size:13px;line-height:20px;color:#757575;">
                        If you did not request this email, no action is needed.
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:18px 32px;background:#141414;color:#ffffff;">
                      <p style="margin:0;font-size:12px;line-height:18px;color:#d8d8d8;">
                        Schemafy helps teams design, inspect, and collaborate on database schemas.
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """
        .replace(SUBJECT_PLACEHOLDER, SUBJECT)
        .replace(CODE_PLACEHOLDER, code);
  }

}
