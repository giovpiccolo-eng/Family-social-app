import nodemailer from "nodemailer";

export interface SendOptions {
  to: string;
  subject: string;
  html: string;
  text: string;
}

/**
 * Send an email via Gmail SMTP using an App Password.
 * Requires GMAIL_USER and GMAIL_APP_PASSWORD in the environment.
 */
export async function sendEmail(opts: SendOptions): Promise<void> {
  const user = process.env.GMAIL_USER;
  const pass = process.env.GMAIL_APP_PASSWORD;
  if (!user || !pass) {
    throw new Error(
      "GMAIL_USER and GMAIL_APP_PASSWORD must be set. Create an App Password at https://myaccount.google.com/apppasswords"
    );
  }

  const transporter = nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: { user, pass: pass.replace(/\s+/g, "") },
  });

  await transporter.sendMail({
    from: `"Morning Brief" <${user}>`,
    to: opts.to,
    subject: opts.subject,
    text: opts.text,
    html: opts.html,
  });
}
