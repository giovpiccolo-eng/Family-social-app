# Quickstart — Going Live in ~15 Minutes

This is the **beginner-friendly** version of `DEPLOY.md`. If you've never
used Google Cloud before, follow this. Just the clicks and pastes you
actually have to do — everything else is automated.

> The whole thing costs **€0**. Cloud Run, Firestore and Cloud Storage
> all have a free tier far larger than what an HR team will ever use.
> You do need to attach a credit card so Google can verify you, but it
> won't be charged at HR volumes (~50 contracts/month).

## What you'll do

| Step | Where | Time |
|------|-------|------|
| 1. Create a Google Cloud project | Browser, Google Cloud Console | 2 min |
| 2. Enable billing | Browser, Google Cloud Console | 2 min |
| 3. Open Cloud Shell (a terminal in your browser) | Top-right icon | 5 sec |
| 4. Paste two commands and answer two prompts | Cloud Shell | 8 min (mostly waiting) |
| 5. Create the OAuth credentials | Browser, Google Cloud Console | 2 min |
| 6. Paste the redirect URL back into the OAuth client | Browser, Google Cloud Console | 30 sec |

That's it. You'll have a public URL your HR team can log in to.

---

## Step 1 — Create the project

1. Open **<https://console.cloud.google.com/projectcreate>** in a browser
   signed in with the Google account that will own this app.
2. **Project name:** `Ingenium Contracts` (or whatever you like).
   Underneath, Google will suggest a Project ID like
   `ingenium-contracts-12345`. **Copy that ID** — you'll paste it in
   step 4.
3. Leave Organization at default (or select your Workspace if it's there).
4. Click **CREATE**. Wait ~10 seconds for the new project to appear in
   the top bar.

## Step 2 — Enable billing

1. Open **<https://console.cloud.google.com/billing/linkedaccount>**.
2. Confirm your new project is selected in the top bar.
3. Click **LINK A BILLING ACCOUNT**.
4. If you have no billing account yet, click **MANAGE BILLING ACCOUNTS**
   → **CREATE ACCOUNT** → follow the 3 screens (name, address, card).
5. Back on the "Linked billing account" page, select the account you
   created and click **SET ACCOUNT**.

## Step 3 — Open Cloud Shell

In any Google Cloud Console page, click the **`>_` terminal icon** in
the top-right (next to the bell). A black terminal opens at the bottom
of the page.

This is a free Linux terminal with `gcloud` already installed and
already signed in to your Google account. You don't need to install
anything.

## Step 4 — Paste two commands

In Cloud Shell, paste these two commands one at a time:

```bash
git clone -b claude/laughing-johnson-ii4uvg https://github.com/giovpiccolo-eng/family-social-app.git
```

then

```bash
cd family-social-app/contract-generator && ./setup-cloud.sh
```

The script will ask you:

- **Project ID** — paste the ID you copied in step 1.
- **Workspace domain** — type your Google Workspace domain (e.g.
  `ingeniumeducationgroup.com`) to restrict sign-in to that domain. If
  you just have personal Gmail accounts, leave it blank.

Then it runs by itself for ~3 minutes, until it pauses and asks you to
create the OAuth client. That's step 5.

## Step 5 — Create the OAuth client

The script prints a clickable URL — open it. You're now in the
**Credentials** page of your project.

1. If Google asks you to configure the **OAuth consent screen**:
   - **User Type:** `Internal` if you have a Workspace, otherwise `External`.
   - **App name:** `Contract Generator`
   - **User support email:** your email
   - **Developer contact:** your email
   - Click **SAVE AND CONTINUE** through the next screens (no other
     changes needed) and then **BACK TO DASHBOARD**.

2. Back on **Credentials**, click **+ CREATE CREDENTIALS** at the top
   → **OAuth client ID**.
   - **Application type:** `Web application`
   - **Name:** `Contract Generator`
   - Leave **Authorized redirect URIs** empty for now.
   - Click **CREATE**.

3. A dialog appears with **Your Client ID** and **Your Client Secret**.
   - Click the copy icons next to each value.
   - Paste them into Cloud Shell when the script asks for them
     (Client ID first, then Client Secret — the secret won't show on
     screen while you type, that's normal).

The script then resumes and finishes deploying. It prints something like:

```
✓ Contract Generator è in produzione!
  URL: https://contract-generator-xxxxx-ew.a.run.app
```

**Copy that URL.**

## Step 6 — Paste the redirect URL back

The script's final message tells you to add a redirect URI to the OAuth
client. This is needed so Google knows where to send users after login.

1. Open <https://console.cloud.google.com/apis/credentials> and click
   the **Contract Generator** OAuth client.
2. Under **Authorized redirect URIs**, click **+ ADD URI**.
3. Paste your URL followed by `/auth/callback`. Example:
   ```
   https://contract-generator-xxxxx-ew.a.run.app/auth/callback
   ```
4. Click **SAVE**.

Wait ~30 seconds for Google to propagate. **Then open the URL from the
script** and click "Sign in with Google".

You're live. Send the URL to your HR team.

---

## Things that go wrong (and the fix)

| Symptom | Fix |
|---------|-----|
| `Error: Permission denied` in Cloud Shell | Re-run `gcloud auth login` then re-run the script. |
| Browser shows "Error 400: redirect_uri_mismatch" after sign-in | Step 6 wasn't done. Paste the redirect URI into the OAuth client. |
| "Access blocked: ... not in the user list" | OAuth consent screen is in "Testing" mode. Go to <https://console.cloud.google.com/apis/credentials/consent> → **PUBLISH APP**. |
| Sign-in works but you get "Accesso negato" | The signed-in email isn't in `ALLOWED_GOOGLE_DOMAIN`. Either sign in with a domain account, or in Cloud Shell run: `gcloud run services update contract-generator --region europe-west1 --remove-env-vars=ALLOWED_GOOGLE_DOMAIN`. |
| Generation works but no PDF | Cloud Run cold-start of LibreOffice can be slow. First PDF in a fresh container can take 30–60s; subsequent ones are fast. |

## After it's live

To push code updates later, in Cloud Shell:

```bash
cd ~/family-social-app
git pull
cd contract-generator
gcloud run deploy contract-generator --source . --region europe-west1
```

That's it. The env vars and secrets are preserved across deploys.
