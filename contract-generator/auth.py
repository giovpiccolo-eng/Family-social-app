"""
Google OAuth (Authlib) with Workspace-domain restriction.

Routes registered:
    GET  /login          -> kick off OAuth (or auto-login in dev)
    GET  /auth/callback  -> exchange code, verify domain, set session
    GET  /logout         -> clear session

Helpers:
    login_required(fn)   -> decorator: 302 to /login if no session
    current_user()       -> dict | None  ({email, name, picture})
"""

from __future__ import annotations

from functools import wraps
from urllib.parse import urlencode

from flask import (
    Blueprint,
    abort,
    current_app,
    flash,
    redirect,
    render_template,
    request,
    session,
    url_for,
)

import config


bp = Blueprint("auth", __name__)
_oauth = None  # lazily initialised — see init_auth(app)


def init_auth(app):
    """Attach Authlib OAuth client to the Flask app."""
    global _oauth
    if not config.IS_PROD:
        return  # No-op in dev mode; current_user() returns a stub.

    from authlib.integrations.flask_client import OAuth
    _oauth = OAuth(app)
    _oauth.register(
        name="google",
        client_id=config.GOOGLE_CLIENT_ID,
        client_secret=config.GOOGLE_CLIENT_SECRET,
        server_metadata_url="https://accounts.google.com/.well-known/openid-configuration",
        client_kwargs={"scope": "openid email profile"},
    )


# --------------------------------------------------------------------------
# Routes
# --------------------------------------------------------------------------

@bp.get("/login")
def login():
    if config.IS_DEV:
        # Auto-login as a fake dev user
        session["user"] = {
            "email": "dev@localhost",
            "name": "Sviluppo locale",
            "picture": "",
        }
        return redirect(url_for("index"))

    if not _oauth:
        abort(500, "OAuth not initialised — set GOOGLE_CLIENT_ID/SECRET.")

    redirect_uri = config.OAUTH_REDIRECT_URI or url_for("auth.callback", _external=True)
    # hd= restricts the Google account chooser to a single Workspace domain.
    extras = {}
    if config.ALLOWED_GOOGLE_DOMAIN:
        extras["hd"] = config.ALLOWED_GOOGLE_DOMAIN
    return _oauth.google.authorize_redirect(redirect_uri, **extras)


@bp.get("/auth/callback")
def callback():
    if config.IS_DEV:
        return redirect(url_for("index"))

    if not _oauth:
        abort(500, "OAuth not initialised.")

    token = _oauth.google.authorize_access_token()
    info = token.get("userinfo") or _oauth.google.userinfo(token=token)
    email = (info.get("email") or "").lower()
    if not email or not info.get("email_verified", True):
        flash("Email non verificata.", "error")
        return redirect(url_for("auth.login"))

    if not _email_allowed(email):
        flash(f"Accesso negato per {email}. "
              f"L'app è riservata al dominio {config.ALLOWED_GOOGLE_DOMAIN}.",
              "error")
        return render_template("login.html", error=True,
                               email=email,
                               domain=config.ALLOWED_GOOGLE_DOMAIN), 403

    session["user"] = {
        "email": email,
        "name": info.get("name") or email,
        "picture": info.get("picture") or "",
    }
    return redirect(url_for("index"))


@bp.get("/logout")
def logout():
    session.pop("user", None)
    return redirect(url_for("auth.login"))


# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------

def _email_allowed(email: str) -> bool:
    if not email:
        return False
    if email in config.ALLOWED_EMAILS:
        return True
    if not config.ALLOWED_GOOGLE_DOMAIN:
        return True   # No restriction configured
    return email.endswith("@" + config.ALLOWED_GOOGLE_DOMAIN.lower())


def current_user() -> dict | None:
    return session.get("user")


def login_required(fn):
    """Decorator: redirect to /login if no authenticated user."""
    @wraps(fn)
    def wrapper(*args, **kwargs):
        if config.IS_DEV and "user" not in session:
            # Dev: auto-login so the wizard "just works".
            session["user"] = {
                "email": "dev@localhost",
                "name": "Sviluppo locale",
                "picture": "",
            }
        if "user" not in session:
            return redirect(url_for("auth.login", next=request.url))
        return fn(*args, **kwargs)
    return wrapper
